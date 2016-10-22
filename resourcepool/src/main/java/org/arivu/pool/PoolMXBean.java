package org.arivu.pool;

/**
 * @author P
 *
 */
public interface PoolMXBean {
	/**
	 * @return maxPoolSize
	 */
	int getMaxPoolSize();
	/**
	 * @param size
	 */
	void setMaxPoolSize(int size);
	/**
	 * @return maxReuseCount
	 */
	int getMaxReuseCount();
	/**
	 * @param cnt
	 */
	void setMaxReuseCount(int cnt);
	/**
	 * @return lifeSpan
	 */
	int getLifeSpan();
	/**
	 * @param time
	 */
	void setLifeSpan(int time);
	/**
	 * @return idleTimeout
	 */
	int getIdleTimeout();
	/**
	 * @param idleTimeout
	 */
	void setIdleTimeout(int idleTimeout);
	/**
	 * @return orphanedTimeout
	 */
	long getOrphanedTimeout();
	/**
	 * @param oTimeout
	 */
	void setOrphanedTimeout(long oTimeout);
	/**
	 * @throws Exception
	 */
	void clear() throws Exception;
}
