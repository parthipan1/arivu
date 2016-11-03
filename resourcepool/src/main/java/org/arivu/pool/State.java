/**
 * 
 */
package org.arivu.pool;

import java.util.concurrent.atomic.AtomicBoolean;

import org.arivu.datastructure.DoublyLinkedList;

/**
 * @author P
 *
 */
final class State<T> {
	/**
	 * 
	 */
	final long createTime = System.currentTimeMillis();
	/**
	 * 
	 */
	int reuse = 0;
	/**
	 * 
	 */
	long lastTime = System.currentTimeMillis();

	/**
	 * 
	 */
	IncType type = IncType.RELEASE;

	/**
	 * 
	 */
	final AtomicBoolean available = new AtomicBoolean(false);
	
	/**
	 * 
	 */
	final AtomicBoolean released = new AtomicBoolean(false);
	
	/**
	 * 
	 */
	final T t;
	
	/**
	 * 
	 */
	T proxy;
	
	DoublyLinkedList<State<T>> dll = null;
	public State(T t) {
		super();
		this.t = t;
//		this.released = false;
	}

	/**
	 * @param type
	 *            TODO
	 * 
	 */
	void inc(IncType type) {
		reuse++;
		lastTime = System.currentTimeMillis();
		this.type = type;
	}

	/**
	 * @param size
	 * @param pool TODO
	 * @return
	 */
	boolean checkExp(final int size, final AbstractPool<?> pool) {
		final long currentTimeMillis = System.currentTimeMillis();
		return pool.maxPoolSize > 0 && size > pool.maxPoolSize || pool.maxReuseCount > 0 && reuse >= pool.maxReuseCount
				|| pool.lifeSpan > 0 && (currentTimeMillis - createTime) >= pool.lifeSpan
				|| pool.idleTimeout > 0 && (currentTimeMillis - lastTime) >= pool.idleTimeout;
	}

//	/**
//	 * @param currentTimeMillis
//	 * @return
//	 */
//	boolean isOrphaned(final long currentTimeMillis, final long timeThreshold) {
//		return type == IncType.GET && (currentTimeMillis - lastTime) > timeThreshold;
//	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "State [createTime=" + createTime + ", reuse=" + reuse + "]";
	}

	@Override
	public int hashCode() {
		return ((t == null) ? 0 : t.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("rawtypes")
		State other = (State) obj;
		if (t == null) {
			if (other.t != null)
				return false;
		} else if (!t.equals(other.t))
			return false;
		return true;
	}
	
	
}