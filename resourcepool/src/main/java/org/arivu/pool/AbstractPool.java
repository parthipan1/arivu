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

import org.arivu.datastructure.DoublyLinkedList;
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

	/**
	 * 
	 */
	final Lock cas = new AtomicWFReentrantLock();
	
	/**
	 * 
	 */
	final Condition notEnough = cas.newCondition();
	
	/**
	 * 
	 */
	final DoublyLinkedList<State<T>> list = new DoublyLinkedList<State<T>>(cas);

	/**
	 * @param factory
	 * @param klass
	 */
	AbstractPool(PoolFactory<T> factory, Class<T> klass) {
		super();
		this.factory = factory;
		this.klass = klass;
		registerMXBean();
	}

	/**
	 * @param factory
	 * @param klass
	 * @param maxPoolSize
	 * @param maxReuseCount
	 * @param lifeSpan
	 */
	AbstractPool(PoolFactory<T> factory, Class<T> klass, int maxPoolSize, int maxReuseCount, int lifeSpan) {
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

//	/**
//	 * @param t
//	 * @return
//	 */
//	final String getId(T t) {
//		return String.valueOf(t.hashCode());
//	}

	/**
	 * @return
	 */
	State<T> poll() {
		try {
			for( State<T> state:list )
				if(state.available.compareAndSet(true, false)) return state;
		} catch (NullPointerException e) {
			logger.error("Failed with poll::", e);
		}
		return null;
	}

	/**
	 * @param t
	 * @return
	 */
	boolean add(State<T> state) {
		if (state != null) {
			state.available.set(true); 
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.pool.Pool#get()
	 */
	@Override
	public T get(final Map<String, Object> params) {
		if (closed)
			return null;

		State<T> state = null;
		
		while ((state = poll()) != null) {
			if (state.checkExp(list.size(), maxPoolSize, maxReuseCount, lifeSpan, idleTimeout)) {
				closeExpConn(state);
			} else {
				return getProxyLinked(state);
			}
		}
		
		cas.lock();
		if ( maxPoolSize > 0 && list.size() < maxPoolSize) {
			T proxyLinked = getProxyLinked(createNew(params, true));
			cas.unlock();
			return proxyLinked;
		}else if(maxPoolSize<=0){
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

	/**
	 * @return
	 */
	boolean blockOnGet() {
		notEnough.awaitUninterruptibly();
		return true;
	}

	/**
	 * 
	 */
	void signalOnRelease() {
		notEnough.signal();
	}

	/**
	 * 
	 */
	final void clearWaitQueue() {
		notEnough.signalAll();
	}

	/**
	 * @param params
	 * @param addFlag
	 * @return
	 */
	final State<T> createNew(final Map<String, Object> params, final boolean addFlag) {
		final T create = factory.create(params);
		final State<T> state = new State<T>(create);
		if (addFlag) {
			list.add(state);
		}
		logger.debug("Created new Resource " + state.t.hashCode() + " total(" + list.size() + "," + maxPoolSize + ")maxPoolSize");//
		return state;
	}

	/**
	 * @param t
	 */
	@Override
	public void put(T t) {
		try {
			@SuppressWarnings("unchecked")
			DoublyLinkedList<State<T>> dll = (DoublyLinkedList<State<T>>)list.getBinaryTree().get(DoublyLinkedList.get(new State<T>(t)));
			if (dll != null) {
				releaseLink(dll.element());
			} else {
				signalOnRelease();
			}
		} catch (Throwable e) {
			logger.error("Failed with put ::", e);
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
			clear();
			clearWaitQueue();
			unregisterMXBean();
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
		clearHead();
	}

	/**
	 * 
	 */
	final void clearHead() {
		State<T> state = null;
		while( (state=list.poll()) != null ){
			closeExpConn(state);
		}
		list.clear();
	}

	/**
	 * @param t
	 * @return
	 */
	@SuppressWarnings("unchecked")
	final T getProxyLinked(final State<T> state) {
		if (state == null)
			return null;
		if (state.t instanceof AutoCloseable) {
			logger.debug("reuse resource! " + state.t.hashCode());
			if( state.proxy == null ){
				state.proxy = (T) Proxy.newProxyInstance(klass.getClassLoader(), new Class[] { klass }, new InvocationHandler() {
					
					@Override
					public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
						final String methodName = method.getName();
						logger.debug("Proxy methodName :: " + methodName + " " + state.t.hashCode());
						if ("close".equals(methodName)) {
							state.released.set(true);
							releaseLink(state);
							return Void.TYPE;
						} else if ("toString".equals(methodName)) {
							return state.t.toString();
						} else {
							if (state.released.get())
								throw new IllegalStateException("Resource Proxy " + state.t.toString() + " already closed!");
							
							state.inc(IncType.GET);
							return method.invoke(state.t, args);
						}
					}
					
				});
			}else{
				state.released.set(false);
			}
			return state.proxy;
		} else {
			logger.debug("reuse resource! " + state.t.hashCode());
			state.inc(IncType.GET);
			return state.t;
		}
	}

	/**
	 * @param ref
	 */
	void releaseLink(final State<T> state) {
		final boolean checkExp = state.checkExp(list.size(), maxPoolSize, maxReuseCount, lifeSpan, idleTimeout);// checkExp(lr.t);
		logger.debug("releaseLink resource! " + state.t.hashCode() + " checkExp " + checkExp);
		if (checkExp) {
			closeExpConn(state);
		} else {
			factory.clear(state.t);
			state.inc(IncType.RELEASE);
			logger.debug("Released resource! " + state.t.hashCode());
			add(state);
		}
		signalOnRelease();
	}

	/**
	 * @param lr
	 */
	final void closeExpConn(final State<T> state) {
		if (state != null) {
			logger.debug("Closed resource! " + state.t.hashCode());
			nonBlockingRemove(state);
			factory.close(state.t);
			state.proxy = null;
		}
	}

	void nonBlockingRemove(final State<T> state) {
		list.remove(state);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.pool.Pool#getMaxPoolSize()
	 */
	@Override
	public final int getMaxPoolSize() {
		return this.list.size();
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
