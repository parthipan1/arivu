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
			logger.debug(" Jmx bean beanName {} registered!",beanNameStr);
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
				logger.debug(" Jmx bean beanName {} setMaxReuseCount new value {} old value {}", beanNameStr, cnt, maxReuseCount);
				that.setMaxReuseCount(cnt);
			}

			@Override
			public void setMaxPoolSize(int size) {
				logger.debug(" Jmx bean beanName {} setMaxPoolSize new value {} old value {}", beanNameStr, size, maxPoolSize);
				that.setMaxPoolSize(size);
			}

			@Override
			public void setLifeSpan(int time) {
				logger.debug(" Jmx bean beanName {} setLifeSpan new value {} old value {}", beanNameStr, time, lifeSpan);
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
				logger.debug(" Jmx bean beanName {} clear! ", beanNameStr);
				that.clear();
			}

			@Override
			public int getIdleTimeout() {
				return that.getIdleTimeout();
			}

			@Override
			public void setIdleTimeout(int timeout) {
				logger.debug(" Jmx bean beanName {} setIdleTimeout new value {} old value {}", beanNameStr, timeout, idleTimeout);
				that.setIdleTimeout(timeout);
			}

			@Override
			public long getOrphanedTimeout() {
				return that.orphanedTimeout;
			}

			@Override
			public void setOrphanedTimeout(long oTimeout) {
				logger.debug(" Jmx bean beanName {} setOrphanedTimeout new value {} old value {}", beanNameStr, oTimeout, that.orphanedTimeout);
				that.orphanedTimeout = oTimeout;
			}
		};
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

		final DoublyLinkedList<State<T>> expired = new DoublyLinkedList<State<T>>();
		try {
			for (final State<T> state : list)
				if (state.available.compareAndSet(true, false)) {
					if (state.checkExp(list.size(), this)) {
						expired.add(state);
					} else {
						removeExpired(expired);
						return getProxyLinked(state);
					}
				}
		} catch (NullPointerException e) {
			logger.error("Failed with poll::", e);
		}
		removeExpired(expired);

		cas.lock();
		if (maxPoolSize > 0 && list.size() < maxPoolSize) {
			T proxyLinked = getProxyLinked(createNew(params));
			cas.unlock();
			return proxyLinked;
		} else if (maxPoolSize <= 0) {
			cas.unlock();
			return getProxyLinked(createNew(params));
		}
		cas.unlock();

		if (blockOnGet()) {
			return get(params);
		} else {
			return null;
		}

	}

	/**
	 * Remove expired resources from the pool.
	 * 
	 * @param expired
	 */
	void removeExpired(final DoublyLinkedList<State<T>> expired) {
		if (!expired.isEmpty()) {
			list.removeAll(expired);
			for (final State<T> es : expired)
				closeExpConn(es);
		}
	}

	/**
	 * Block call if the resources are not enough.
	 * 
	 * @return blockFlag
	 */
	boolean blockOnGet() {
		notEnough.awaitUninterruptibly();
		return true;
	}

	/**
	 * Once a resource is available to consume, then release a waiting consumer.
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
	 * Create a new Resource delegate to Factory create method. All the resource will be tracked.
	 * 
	 * @param params
	 * @return State
	 */
	final State<T> createNew(final Map<String, Object> params) {
		final T create = factory.create(params);
		final State<T> state = new State<T>(create);
//		state.dll =	list.addList(state);
		list.add(state);
		logger.debug("Created new Resource {} total({},{})maxPoolSize", state.t.hashCode(), list.size(), maxPoolSize);//
		return state;
	}

	/**
	 * Put back a resource released by the application. 
	 * 
	 * @param t
	 */
	@Override
	public void put(T t) {
		try {
			@SuppressWarnings("unchecked")
			DoublyLinkedList<State<T>> dll = (DoublyLinkedList<State<T>>) list.getBinaryTree()
					.get(DoublyLinkedList.get(new State<T>(t)));
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
				logger.debug("Unregister Jmx bean {}" , beanNameStr);
			} catch (Exception e) {
				logger.error("Failed with Error::", e);
			}
		}
	}

	/**
	 * Clear all available resources.
	 */
	@Override
	public void clear() {
		State<T> state = null;
		while ((state = list.poll()) != null) {
			closeExpConn(state);
		}
		list.clear();
	}

	private static final class ProxyInvoker<T> implements InvocationHandler{
		final State<T> state;
		final AbstractPool<T> pool;
		ProxyInvoker(State<T> state,AbstractPool<T> pool) {
			super();
			this.state = state;
			this.pool = pool;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			final String methodName = method.getName();
			logger.debug("Proxy methodName :: {} {}" , methodName, state.t.hashCode());
			if ("close".equals(methodName)) {
				state.released.set(true);
				this.pool.releaseLink(state);
				return Void.TYPE;
			} else if ("toString".equals(methodName)) {
				return state.t.toString();
			} else {
				if (state.released.get())
					throw new IllegalStateException(
							"Resource Proxy " + state.t.toString() + " already closed!");

				state.inc(IncType.GET);
				return method.invoke(state.t, args);
			}
		}
		
	}
	
	
	/**
	 * @param state
	 * @return tObject
	 */
	@SuppressWarnings("unchecked")
	final T getProxyLinked(final State<T> state) {
		if (state == null)
			return null;
		if (state.t instanceof AutoCloseable) {
			logger.debug("reuse resource! " + state.t.hashCode());
			if (state.proxy == null) {
				state.proxy = (T) Proxy.newProxyInstance(klass.getClassLoader(), new Class[] { klass },new ProxyInvoker<T>(state,this));
			} else {
				state.released.set(false);
			}
			return state.proxy;
		} else {
			logger.debug("reuse resource! {}" , state.t.hashCode());
			state.inc(IncType.GET);
			return state.t;
		}
	}

	/**
	 * Release a resource back to the pool, which was just used.
	 * 
	 * @param state
	 */
	void releaseLink(final State<T> state) {
		final boolean checkExp = state.checkExp(list.size(), this);// checkExp(lr.t);
		logger.debug("releaseLink resource! {} checkExp {}", state.t.hashCode(), checkExp);
		if (checkExp) {
			closeExpConn(state);
		} else {
			factory.clear(state.t);
			state.inc(IncType.RELEASE);
			logger.debug("Released resource! {}", state.t.hashCode());
			state.available.set(true);
		}
		signalOnRelease();
	}

	/**
	 * @param state
	 */
	final void closeExpConn(final State<T> state) {
		if (state != null) {
			logger.debug("Closed resource! {}", state.t.hashCode());
			
			if(state.dll==null)
				list.remove(state);
			else
				state.dll.removeRef();
			
			factory.close(state.t);
			state.proxy = null;
			state.dll = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.pool.Pool#getMaxPoolSize()
	 */
	@Override
	public int getMaxPoolSize() {
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
