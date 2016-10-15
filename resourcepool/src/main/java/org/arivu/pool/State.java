/**
 * 
 */
package org.arivu.pool;

import java.util.concurrent.atomic.AtomicBoolean;

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
	
//	volatile boolean released = false;
	
	/**
	 * 
	 */
	final T t;
	
	/**
	 * 
	 */
	T proxy;
	
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
	 * @param maxPoolSize
	 * @param maxReuseCount
	 * @param lifeSpan
	 * @param idleTimeout
	 * @return
	 */
	boolean checkExp(int size, int maxPoolSize, int maxReuseCount, int lifeSpan, int idleTimeout) {
		final long currentTimeMillis = System.currentTimeMillis();
		return maxPoolSize > 0 && size > maxPoolSize || maxReuseCount > 0 && reuse >= maxReuseCount
				|| lifeSpan > 0 && (currentTimeMillis - createTime) >= lifeSpan
				|| idleTimeout > 0 && (currentTimeMillis - lastTime) >= idleTimeout;
	}

	/**
	 * @param currentTimeMillis
	 * @return
	 */
	boolean isOrphaned(final long currentTimeMillis, final long timeThreshold) {
		return type == IncType.GET && (currentTimeMillis - lastTime) > timeThreshold;
	}

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
		final int prime = 31;
		int result = 1;
		result = prime * result + ((t == null) ? 0 : t.hashCode());
		return result;
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