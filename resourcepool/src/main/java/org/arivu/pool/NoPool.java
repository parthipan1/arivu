package org.arivu.pool;

import java.util.Collection;
import java.util.Map;

import org.arivu.datastructure.Btree;

/**
 * @author P
 *
 * @param <T>
 */
public final class NoPool<T> extends AbstractPool<T> {
	
	final Btree nolist = new Btree();
	
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.pool.AbstractPool#get()
	 */
	@Override
	public T get(final Map<String, Object> params) {
		T create = factory.create(params);
		nolist.add(create);
		return (create);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.pool.AbstractPool#release(java.lang.AutoCloseable)
	 */
	@Override
	public void put(T t) {
		if (t != null) {
			logger.debug("close {}", t.hashCode());
			factory.close(t);
			nolist.remove(t);
		}
	}
	
	@Override
	public int getMaxPoolSize() {
		return nolist.size();
	}
	
	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void clear() {
		Collection<Object> all = nolist.getAll();
		nolist.clear();
		
		for(Object o:all){
			logger.debug("close {}", o.hashCode());
			factory.close((T)o);
		}
	}
}
