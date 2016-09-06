package org.arivu.pool;

/**
 * @author P
 *
 * @param <T>
 */
public final class NonBlockingPool<T> extends AbstractPool<T> {

	/**
	 * @param factory
	 * @param klass
	 */
	public NonBlockingPool(PoolFactory<T> factory, Class<T> klass) {
		super(factory, klass);
	}

	/**
	 * @param factory
	 * @param klass
	 * @param maxPoolSize
	 * @param maxReuseCount
	 * @param lifeSpan
	 */
	public NonBlockingPool(PoolFactory<T> factory, Class<T> klass, int maxPoolSize, int maxReuseCount, int lifeSpan) {
		super(factory, klass, maxPoolSize, maxReuseCount, lifeSpan);
	}

	/* (non-Javadoc)
	 * @see org.arivu.pool.AbstractPool#blockOnGet()
	 */
	@Override
	boolean blockOnGet() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.arivu.pool.AbstractPool#signalOnRelease()
	 */
	@Override
	void signalOnRelease() {
	}

}
