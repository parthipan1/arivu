package org.arivu.pool;

import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.arivu.utils.lock.AtomicWFReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author P
 *
 * @param <T>
 */
abstract class AbstractPool<T> implements Pool<T> {

	/**
	 * 
	 */
	static final Logger logger = LoggerFactory.getLogger(AbstractPool.class);

	/**
	 * 
	 */
	long orphanedTimeout = 1000 * 60 * 60;// in milliSec

	/**
	 * 
	 */
	final PoolFactory<T> factory;
	/**
	 * 
	 */
	final Class<T> klass;

	/**
	 * 
	 */
	volatile boolean closed = false;

	/**
	 * 
	 */
	private int beanInstanceCnt = 0;

	/**
	 * 
	 */
	int maxPoolSize = 500;

	/**
	 * 
	 */
	int maxReuseCount = 2500;

	/**
	 * 
	 */
	int lifeSpan = 30000;

	/**
	 * 
	 */
	int idleTimeout = -1;

	final Lock cas = new AtomicWFReentrantLock();
	
	final Condition notEnough = cas.newCondition();
	/**
	 * 
	 */
	final LinkedReference<T> head = new LinkedReference<T>();

	public AbstractPool(PoolFactory<T> factory, Class<T> klass) {
		super();
		this.factory = factory;
		this.klass = klass;
		registerMXBean();
	}

	public AbstractPool(PoolFactory<T> factory, Class<T> klass, int maxPoolSize, int maxReuseCount, int lifeSpan) {
		this(factory, klass);
		this.maxPoolSize = maxPoolSize;
		this.maxReuseCount = maxReuseCount;
		this.lifeSpan = lifeSpan;
	}

	String beanNameStr = null;

	/**
	 * 
	 */
	private final void registerMXBean() {
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			beanNameStr = "org.arivu.pool:type=" + klass.getSimpleName() + "." + (beanInstanceCnt++);
			mbs.registerMBean(getResourcePoolMXBean(), new ObjectName(beanNameStr));
			logger.debug(" Jmx bean beanName " + beanNameStr + " registered!");
		} catch (InstanceAlreadyExistsException e) {
			registerMXBean();
		} catch (Exception e) {
			logger.error("Failed with Error::", e);
		}
	}

	/**
	 * @return
	 */
	private final PoolMXBean getResourcePoolMXBean() {
		@SuppressWarnings("resource")
		final AbstractPool<T> that = this;
		return new PoolMXBean() {

			@Override
			public void setMaxReuseCount(int cnt) {
				logger.debug(" Jmx bean beanName " + beanNameStr + " setMaxReuseCount new value " + cnt + " old value "
						+ maxReuseCount);
				that.setMaxReuseCount(cnt);
			}

			@Override
			public void setMaxPoolSize(int size) {
				logger.debug(" Jmx bean beanName " + beanNameStr + " setMaxPoolSize new value " + size + " old value "
						+ maxPoolSize);
				that.setMaxPoolSize(size);
			}

			@Override
			public void setLifeSpan(int time) {
				logger.debug(" Jmx bean beanName " + beanNameStr + " setLifeSpan new value " + time + " old value "
						+ lifeSpan);
				that.setLifeSpan(time);
			}

			@Override
			public int getMaxReuseCount() {
				return that.getMaxReuseCount();
			}

			@Override
			public int getMaxPoolSize() {
				return that.getMaxPoolSize();
			}

			@Override
			public int getLifeSpan() {
				return that.getLifeSpan();
			}

			@Override
			public void clear() throws Exception {
				logger.debug(" Jmx bean beanName " + beanNameStr + " clear! ");
				that.clear();
			}

			@Override
			public int getIdleTimeout() {
				return that.getIdleTimeout();
			}

			@Override
			public void setIdleTimeout(int timeout) {
				logger.debug(" Jmx bean beanName " + beanNameStr + " setIdleTimeout new value " + timeout
						+ " old value " + idleTimeout);
				that.setIdleTimeout(timeout);
			}

			@Override
			public long getOrphanedTimeout() {
				return that.orphanedTimeout;
			}

			@Override
			public void setOrphanedTimeout(long oTimeout) {
				logger.debug(" Jmx bean beanName " + beanNameStr + " setOrphanedTimeout new value " + oTimeout
						+ " old value " + that.orphanedTimeout);
				that.orphanedTimeout = oTimeout;
			}
		};
	}

	/**
	 * @param t
	 * @return
	 */
	final String getId(T t) {
		return String.valueOf(t.hashCode());
	}

	/**
	 * @return
	 */
	LinkedReference<T> poll() {
		
		LinkedReference<T> ref = head.right;
		while (ref != null) {
			if( ref.state!=null && ref.state.available.compareAndSet(true, false) ){
				return ref;
			}
			ref = ref.right;
			if (ref == null || ref.state == null || ref == head) {
				break;
			}
		}
		return null;
		
	}

	/**
	 * @param t
	 * @return
	 */
	boolean add(LinkedReference<T> t) {
		if (t != null) {
			t.state.available.set(true); 
		}
		return true;
	}

//	/**
//	 * @param action
//	 * @param params
//	 * @return
//	 */
//	final void exclusive(final LinkedRef<T> ref){
//		while( !cas.compareAndSet(false, true) ){}
//		ref.remove();
//		cas.set(false);
//	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.pool.Pool#get()
	 */
	@Override
	public T get(final Map<String, Object> params) {
		if (closed)
			return null;

		LinkedReference<T> poll = null;
		
		while ((poll = poll()) != null) {
			if (poll.state.checkExp(head.size(), maxPoolSize, maxReuseCount, lifeSpan, idleTimeout)) {
				closeExpConn(poll);// (poll);
			} else {
				logger.debug("reuse resource! " + poll.state.t.hashCode());
				return getProxyLinked(poll);
			}
		}
		
		cas.lock();
		if ( maxPoolSize > 0 && head.size() < maxPoolSize) {
			final int size = head.size();
			if( head.size.compareAndSet(size, size+1) ){
				if (head.size() <= maxPoolSize) {
					cas.unlock();
					return getProxyLinked(createNew(params, true));
				}else{
					head.size.decrementAndGet();
				}
			}
		}else if(maxPoolSize<=0){
			head.size.incrementAndGet();
			cas.unlock();
			return getProxyLinked(createNew(params, true));
		}
		cas.unlock();
		
		if (blockOnGet()) {
			return get(params);
		} else {
			return null;
		}

	}

//	final Synchronizer sync = new Synchronizer();

	/**
	 * @return
	 */
	boolean blockOnGet() {
//		sync.youShallNotPass();
		notEnough.awaitUninterruptibly();
		return true;
	}

	/**
	 * 
	 */
	void signalOnRelease() {
//		sync.youShallPass();
		notEnough.signal();
	}

	/**
	 * 
	 */
	final void clearWaitQueue() {
//		sync.allShallPass();
		notEnough.signalAll();
	}

	/**
	 * @param params
	 *            TODO
	 * @param addFlag
	 *            TODO
	 * @return
	 */
	final LinkedReference<T> createNew(final Map<String, Object> params, final boolean addFlag) {
		final T create = factory.create(params);
		final LinkedReference<T> lr = new LinkedReference<T>(create,head.size);
		if (addFlag) {
			head.add(lr);
		}
		logger.debug("Created new Resource " + lr.state.t.hashCode() + " total(" + head.size() + "," + maxPoolSize + ")maxPoolSize");//
		return lr;
	}

	/**
	 * @param t
	 */
	@Override
	public void put(T t) {
		LinkedReference<T> ref = head.search(t);
		if (ref != null) {
			releaseLink(ref);
		} else {
			signalOnRelease();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() throws Exception {
		if (!closed) {
			closed = true;
			logger.debug("close() before clear!");
			clear();
			logger.debug("close() after clear!\nbefore clear waitQueue");
			clearWaitQueue();
			logger.debug("close() after clear waitQueue!\nbefore clear mx bean");
			unregisterMXBean();
			logger.debug("close() after clear mx bean");
		}
	}

	/**
	 * 
	 */
	private void unregisterMXBean() {
		if (beanNameStr != null) {
			try {
				ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName(beanNameStr));
				logger.debug("Unregister Jmx bean " + beanNameStr);
			} catch (Exception e) {
				logger.error("Failed with Error::", e);
			}
		}
	}

	/**
	 * 
	 */
	@Override
	public void clear() {
		logger.debug("clear() before clearHead");
		clearHead();
		logger.debug("clear() after clearHead");
	}

	/**
	 * 
	 */
	final void clearHead() {
		LinkedReference<T> ref = head.right;
		while (ref != null) {
			if (ref == head) {
				break;
			}
			closeExpConn(ref);
			ref = ref.right;
		}
		head.clear();
	}

	/**
	 * @param t
	 * @return
	 */
	@SuppressWarnings("unchecked")
	final T getProxyLinked(final LinkedReference<T> lr) {
		if (lr == null)
			return null;
		if (lr.state.t instanceof AutoCloseable) {
			if( lr.state.proxy == null ){
				lr.state.proxy = (T) Proxy.newProxyInstance(klass.getClassLoader(), new Class[] { klass }, new InvocationHandler() {
					
					@Override
					public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
						final String methodName = method.getName();
						logger.debug("Proxy methodName :: " + methodName + " " + lr.state.t.hashCode());
						if ("close".equals(methodName)) {
							lr.state.released = true;
							releaseLink(lr);
							return Void.TYPE;
						} else if ("toString".equals(methodName)) {
							return lr.state.t.toString();
						} else {
							if (lr.state.released)
								throw new IllegalStateException("Resource Proxy " + lr.state.t.toString() + " already closed!");
							
							lr.state.inc(IncType.GET);
							return method.invoke(lr.state.t, args);
						}
					}
					
				});
			}else{
				lr.state.released = false;
			}
			return lr.state.proxy;
		} else {
			lr.state.inc(IncType.GET);
			return lr.state.t;
		}
	}

	/**
	 * @param ref
	 */
	void releaseLink(final LinkedReference<T> ref) {
		final boolean checkExp = ref.state.checkExp(head.size(), maxPoolSize, maxReuseCount, lifeSpan, idleTimeout);// checkExp(lr.t);
		logger.debug("releaseLink resource! " + ref.state.t.hashCode() + " checkExp " + checkExp);
		if (checkExp) {
			closeExpConn(ref);
		} else {
			factory.clear(ref.state.t);
			ref.state.inc(IncType.RELEASE);
			logger.debug("Released resource! " + ref.state.t.hashCode());
			add(ref);
		}
		signalOnRelease();
	}

	/**
	 * @param lr
	 */
	final void closeExpConn(final LinkedReference<T> lr) {
		if (lr != null && lr.state != null) {
			logger.debug("Closed resource! " + lr.state.t.hashCode());
			nonBlockingRemove(lr);
			factory.close(lr.state.t);
			lr.state.proxy = null;
		}
	}

	void nonBlockingRemove(final LinkedReference<T> lr) {
		cas.lock();
		head.size.decrementAndGet();
		lr.remove();
		cas.unlock();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.pool.Pool#getMaxPoolSize()
	 */
	@Override
	public final int getMaxPoolSize() {
		return this.head.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.pool.Pool#setMaxPoolSize(int)
	 */
	@Override
	public void setMaxPoolSize(int size) {
		this.maxPoolSize = size;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.pool.Pool#getMaxReuseCount()
	 */
	@Override
	public final int getMaxReuseCount() {
		return maxReuseCount;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.pool.Pool#setMaxReuseCount(int)
	 */
	@Override
	public final void setMaxReuseCount(int cnt) {
		this.maxReuseCount = cnt;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.pool.Pool#getLifeSpan()
	 */
	@Override
	public final int getLifeSpan() {
		return lifeSpan;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.pool.Pool#setLifeSpan(int)
	 */
	@Override
	public final void setLifeSpan(int time) {
		this.lifeSpan = time;
	}

	/**
	 * @return idleTimeout
	 */
	public final int getIdleTimeout() {
		return idleTimeout;
	}

	/**
	 * @param idleTimeout
	 */
	public final void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

}
enum IncType {
	GET, RELEASE
}
