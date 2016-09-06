/**
 * 
 */
package org.arivu.pool;

/**
 * @author P
 *
 */
final class State {
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
	 * @param type
	 *            TODO
	 * 
	 */
	final void inc(IncType type) {
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
		return ((maxPoolSize > 0 && size > maxPoolSize || maxReuseCount > 0 && reuse >= maxReuseCount
				|| lifeSpan > 0 && (currentTimeMillis - createTime) >= lifeSpan
				|| idleTimeout > 0 && (currentTimeMillis - lastTime) >= idleTimeout));
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
}