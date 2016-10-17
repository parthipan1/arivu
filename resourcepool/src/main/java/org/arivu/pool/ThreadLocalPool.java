package org.arivu.pool;

import java.util.Map;

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

	/**
	 * 
	 */
	private final Threadlocal<State<T>> threadlocals = new Threadlocal<State<T>>( new Factory<State<T>>() {

		@Override
		public State<T> create(Map<String, Object> params) {
			final State<T> state = new State<T>(factory.create(params));
			logger.debug("Factory create "+state.t.hashCode()+" Thread "+Thread.currentThread().hashCode());
			return state;
		}
	}  , -1);
	
	/* (non-Javadoc)
	 * @see org.arivu.pool.AbstractPool#close()
	 */
	@Override
	public void close() throws Exception {
		super.close();
		threadlocals.close();
	}

	/* (non-Javadoc)
	 * @see org.arivu.pool.AbstractPool#get()
	 */
	@Override
	public T get(Map<String, Object> params) {
		State<T> lr = threadlocals.get(params);
		if(lr==null){
			lr = createNew(params);
			threadlocals.set(lr);
		}
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
	void releaseLink(State<T> state) {
		if( state!=null && state.checkExp(1, this) ){
			threadlocals.remove();
			closeExpConn(state);
		}
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
