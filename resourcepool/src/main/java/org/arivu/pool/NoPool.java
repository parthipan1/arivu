package org.arivu.pool;

import java.util.Map;

/**
 * @author P
 *
 * @param <T>
 */
public final class NoPool<T> extends AbstractPool<T> {

	/**
	 * @param factory
	 * @param klass
	 */
	public NoPool(PoolFactory<T> factory, Class<T> klass) {
		super(factory, klass);
	}

	/**
	 * @param factory
	 * @param klass
	 * @param maxReuseCount
	 * @param lifeSpan
	 */
	public NoPool(PoolFactory<T> factory, Class<T> klass, int maxReuseCount, int lifeSpan) {
		super(factory, klass, -1, maxReuseCount, lifeSpan);
	}

	/* (non-Javadoc)
	 * @see org.arivu.pool.AbstractPool#get()
	 */
	@Override
	public T get(final Map<String, Object> params) {
		return getProxyLinked(createNew(params, true));
	}

	/* (non-Javadoc)
	 * @see org.arivu.pool.AbstractPool#release(java.lang.AutoCloseable)
	 */
	@Override
	public void put(T t) {
		logger.debug("close " + t.hashCode());
		factory.close(t);
		final LinkedReference<T> ref = head.search(t);
		if (ref!=null) {
			nonBlockingRemove(ref);
		}
	}

	/* (non-Javadoc)
	 * @see org.arivu.pool.AbstractPool#releaseLink(org.arivu.pool.LinkedReference)
	 */
	@Override
	void releaseLink(final LinkedReference<T> ref) {
		if (ref!=null) {
			logger.debug("close " + ref.t.hashCode());
			factory.close(ref.t);
			nonBlockingRemove(ref);
		}
	}
	
}
