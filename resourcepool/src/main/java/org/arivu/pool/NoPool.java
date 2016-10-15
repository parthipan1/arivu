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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.pool.AbstractPool#get()
	 */
	@Override
	public T get(final Map<String, Object> params) {
		return getProxyLinked(createNew(params, false));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.pool.AbstractPool#release(java.lang.AutoCloseable)
	 */
	@Override
	public void put(T t) {
		if (t != null) {
			logger.debug("close " + t.hashCode());
			factory.close(t);
//			@SuppressWarnings("unchecked")
//			DoublyLinkedList<State<T>> dll = (DoublyLinkedList<State<T>>) list.getBinaryTree()
//					.get(DoublyLinkedList.get(new State<T>(t)));
//			if (dll != null) {
//				closeExpConn(dll.element());
//			 }
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.arivu.pool.AbstractPool#releaseLink(org.arivu.pool.LinkedReference)
	 */
	@Override
	void releaseLink(final State<T> state) {
		if (state != null) {
			logger.debug("close " + state.t.hashCode());
			factory.close(state.t);
			nonBlockingRemove(state);
		}
	}

}
