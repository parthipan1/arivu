package org.arivu.pool;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.arivu.datastructure.Threadlocal;
import org.arivu.datastructure.Threadlocal.Factory;

/**
 * @author P
 *
 * @param <T>
 */
public final class ThreadLocalPool<T> extends AbstractPool<T> {

	/**
	 * @param factory
	 * @param klass
	 */
	public ThreadLocalPool(PoolFactory<T> factory, Class<T> klass) {
		super(factory, klass);
		this.setMaxPoolSize(-1);
	}

	/**
	 * @param factory
	 * @param klass
	 * @param maxPoolSize
	 * @param maxReuseCount
	 * @param lifeSpan
	 */
	public ThreadLocalPool(PoolFactory<T> factory, Class<T> klass, int maxPoolSize, int maxReuseCount, int lifeSpan) {
		super(factory, klass, maxPoolSize, maxReuseCount, lifeSpan);
	}

	private final AtomicInteger ps = new AtomicInteger(0);
	
	/**
	 * 
	 */
	private final Threadlocal<LinkedReference<T>> threadlocals = new Threadlocal<LinkedReference<T>>( new Factory<LinkedReference<T>>() {

		@Override
		public LinkedReference<T> create(Map<String, Object> params) {
			final LinkedReference<T> c = new LinkedReference<T>(factory.create(params), ps);
			logger.debug("Factory create "+c.state.t.hashCode()+" Thread "+Thread.currentThread().hashCode());
//			System.out.println("Factory create "+c.t.hashCode()+" Thread "+Thread.currentThread().hashCode());
			return c;
		}
	}  , -1);
	
//	private final ThreadLocal<LinkedRef<T>> threadlocals = new ThreadLocal<LinkedRef<T>>();
	

	/* (non-Javadoc)
	 * @see org.arivu.pool.AbstractPool#close()
	 */
	@Override
	public void close() throws Exception {
		clear();
	}

	/* (non-Javadoc)
	 * @see org.arivu.pool.AbstractPool#get()
	 */
	@Override
	public T get(Map<String, Object> params) {
		LinkedReference<T> lr = threadlocals.get(params);
		if(lr==null){
			lr = createNew(params, true);
			threadlocals.set(lr);
		}
//		LinkedRef<T> lr = threadlocals.get(params);
		return getProxyLinked(lr);
	}

	/* (non-Javadoc)
	 * @see org.arivu.pool.AbstractPool#release(java.lang.AutoCloseable)
	 */
	@Override
	public void put(T t) {
		releaseLink(threadlocals.get());
	}

	/* (non-Javadoc)
	 * @see org.arivu.pool.AbstractPool#releaseLink(org.arivu.pool.AbstractPool.LinkedReference)
	 */
	@Override
	void releaseLink(LinkedReference<T> ref) {
//		logger.debug("release "+ref.t.hashCode());
		if( ref!=null && ref.state.checkExp(1, maxPoolSize, maxReuseCount, lifeSpan, idleTimeout) ){
//			logger.debug("releaseLink close "+ref.t.hashCode()+" Thread "+Thread.currentThread().hashCode());
			threadlocals.remove();
//			logger.debug("releaseLink close After remove Thread "+Thread.currentThread().hashCode());
			factory.close(ref.state.t);
			
			LinkedReference<T> search = head.search(ref.state.t);
			if(search!=null){
				search.remove();
			}
			
//			LinkedRef<T> linkedReference = threadlocals.get();
//			if( linkedReference != null )
//				logger.debug("releaseLink close After close Thread "+Thread.currentThread().hashCode()+" get "+linkedReference.t.hashCode());
//			else
//				logger.debug("releaseLink close After close Thread "+Thread.currentThread().hashCode()+" get null");
			
		}
	}

	/* (non-Javadoc)
	 * @see org.arivu.pool.AbstractPool#clear()
	 */
	@Override
	public void clear() {
		clearHead();
//		super.clear();
//		Collection<LinkedRef<T>> all = threadlocals.getAll();
//		threadlocals.clearAll();
//		for( LinkedRef<T> lr:all ){
//			logger.debug("close "+lr.t.hashCode());
//			factory.close(lr.t);
//		}
	}

	/* (non-Javadoc)
	 * @see org.arivu.pool.AbstractPool#setMaxPoolSize(int)
	 */
	@Override
	public void setMaxPoolSize(int size) {
		logger.warn(getClass().getName() + " " + this + " setMaxPoolSize(" + size + ") ignored!");
		super.setMaxPoolSize(-1);
	}

}
