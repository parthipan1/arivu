package org.arivu.datastructure;

import java.util.Map.Entry;

/**
 * @author P
 *
 */
public final class LruCache<K,V> {

	/**
	 * @author P
	 *
	 */
	enum CacheStrategy {
		TIME_MOST_RECENT {

			@Override
			<T> Tracker<T> access(Tracker<T> t) {
				t.tracker = System.currentTimeMillis();
				return t;
			}

		},
		TIME_LEAST_RECENT {

			@Override
			<T> Tracker<T> access(Tracker<T> t) {
				t.tracker = System.currentTimeMillis();
				return t;
			}

			@Override
			<T> boolean compare(Tracker<T> t1, Tracker<T> t2) {
				return !super.compare(t1, t2);
			}

		},
		COUNT_LEAST {

			@Override
			<T> boolean compare(Tracker<T> t1, Tracker<T> t2) {
				return !super.compare(t1, t2);
			}

		},
		COUNT_MOST;

		<T> Tracker<T> access(Tracker<T> t) {
			t.tracker++;
			return t;
		}

		<T> Tracker<T> create(T t) {
			Tracker<T> tracker = new Tracker<T>();
			tracker.t = t;
			return this.access(tracker);
		}

		<T> boolean compare(Tracker<T> t1, Tracker<T> t2) {
			return t1.tracker < t2.tracker;
		}
		
		CacheStrategy other(){
			
			switch (this) {
			case TIME_MOST_RECENT:
				return TIME_LEAST_RECENT;
			case TIME_LEAST_RECENT:
				return TIME_MOST_RECENT;
			case COUNT_MOST:
				return COUNT_LEAST;
			case COUNT_LEAST:
				return COUNT_MOST;
			default:
				break;
			}
			
			return CacheStrategy.COUNT_MOST;
		}
	}

	/**
	 * 
	 */
	private final CacheStrategy cacheStrategy;

	/**
	 * 
	 */
	private final Amap<K,Tracker<V>> cache = new Amap<K,Tracker<V>>();

	private final int maxSize; 
	
	/**
	 * Constructor for default Object creation.
	 * 
	 * @param cacheStrategy
	 * @param maxSize
	 */
	public LruCache(CacheStrategy cacheStrategy,int maxSize) {
		super();
		if (cacheStrategy == null)
			this.cacheStrategy = CacheStrategy.TIME_MOST_RECENT;
		else
			this.cacheStrategy = cacheStrategy;
		
		if( maxSize<1 )
			throw new IllegalArgumentException("Invalid maxSize "+maxSize);
		
		this.maxSize = maxSize; 
	}

	
	/**
	 * put to cache with key.
	 * 
	 * @param key
	 * @param t
	 */
	public void put(K key,V t) {
		if (t != null) {
			if( maxSize == cache.size() ){
				remove();
			}
			cache.put(key,cacheStrategy.create(t));
		}
	}

	/**
	 * get from cache.
	 * 
	 * @param Key
	 * @return V
	 */
	public V get(K key) {
		Tracker<V> tracker = cache.get(key);
		if( tracker!=null ){
			return cacheStrategy.access(tracker).t;
		}
		return null;
	}

	private Entry<K, Tracker<V>> get(CacheStrategy cs) {
		Entry<K, Tracker<V>> e0 = null;
		for( Entry<K, Tracker<V>> e:cache.entrySet() ){
			if(e0==null)
				e0=e;
			else{
				if (cs.compare(e0.getValue(), e.getValue())) {
					e0 = e;
				}
			}
		}
		return e0;
	}
	
	public int size() {
		return cache.size();
	}

	public boolean isEmpty() {
		return cache.isEmpty();
	}

	public V remove(){
		Entry<K, Tracker<V>> e = get(cacheStrategy.other());
		if( e!=null){
			cache.remove(e.getKey());
			return e.getValue().t;
		}else{
			return null;
		}
	}
	
	/**
	 * clears cache.
	 */
	public void clear() {
		cache.clear();
	}

	/**
	 * @param Key
	 * @return boolean
	 */
	public boolean remove(K key) {
		if (key != null) {
			Tracker<V> search = cache.remove(key);
			if(search!=null){
				return true;
			}
		}
		return false;
	}

	/**
	 * @author P
	 *
	 * @param <T>
	 */
	private static class Tracker<T> {
		T t;
		long tracker = 0;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((t == null) ? 0 : t.hashCode());
			return result;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Tracker other = (Tracker) obj;
			if (t == null) {
				if (other.t != null)
					return false;
			} else if (!t.equals(other.t))
				return false;
			return true;
		}

	}

}
