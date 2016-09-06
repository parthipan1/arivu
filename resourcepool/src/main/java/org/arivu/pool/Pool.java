package org.arivu.pool;

import java.util.Map;

/**
 * @author P
 *
 * @param <T>
 */
public interface Pool<T> extends AutoCloseable{
	/**
	 * @param params TODO
	 * @return
	 */
	T get(Map<String, Object> params);
	
	/**
	 * @param t
	 */
	void put(T t);
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
	 * 
	 */
	void clear();
	
}
