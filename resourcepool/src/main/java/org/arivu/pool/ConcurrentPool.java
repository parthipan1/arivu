package org.arivu.pool;

/**
 * @author P
 *
 * @param <T>
 */
public final class ConcurrentPool<T> extends AbstractPool<T> {

	/**
	 * @param factory
	 * @param klass
	 */
	public ConcurrentPool(PoolFactory<T> factory, Class<T> klass) {
		super(factory, klass);
	}

	/**
	 * @param factory
	 * @param klass
	 * @param maxPoolSize
	 * @param maxReuseCount
	 * @param lifeSpan
	 */
	public ConcurrentPool(PoolFactory<T> factory, Class<T> klass, int maxPoolSize, int maxReuseCount, int lifeSpan) {
		super(factory, klass, maxPoolSize, maxReuseCount, lifeSpan);
	}

}
