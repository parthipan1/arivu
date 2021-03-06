package org.arivu.pool;

import java.util.Map;

/**
 * @author P
 *
 * @param <T>
 */
public interface PoolFactory<T> {
	/**
	 * @param params
	 * @return tObject
	 */
	T create(Map<String, Object> params);
	
	/**
	 * @param t
	 */
	void close(T t);
	
	/**
	 * @param t
	 */
	void clear(T t);
}
