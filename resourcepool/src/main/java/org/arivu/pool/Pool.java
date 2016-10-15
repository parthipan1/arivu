package org.arivu.pool;

import java.util.Map;

/**
 * @author P
 *
 * @param <T>
 */
public interface Pool<T> extends AutoCloseable{
	/**
	 * @param params
	 * @return tObject
	 */
	T get(Map<String, Object> params);
	
	/**
	 * @param t
	 */
	void put(T t);
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
	 * 
	 */
	void clear();
	
}
