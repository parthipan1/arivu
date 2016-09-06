package org.arivu.pool;

/**
 * @author P
 *
 */
public interface PoolMXBean {
	/**
	 * @return
	 */
	int getMaxPoolSize();
	/**
	 * @param size
	 */
	void setMaxPoolSize(int size);
	/**
	 * @return
	 */
	int getMaxReuseCount();
	/**
	 * @param cnt
	 */
	void setMaxReuseCount(int cnt);
	/**
	 * @return
	 */
	int getLifeSpan();
	/**
	 * @param time
	 */
	void setLifeSpan(int time);
	/**
	 * @return
	 */
	int getIdleTimeout();
	/**
	 * @param idleTimeout
	 */
	void setIdleTimeout(int idleTimeout);
	/**
	 * @return
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
