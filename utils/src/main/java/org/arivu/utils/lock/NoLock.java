/**
 * 
 */
package org.arivu.utils.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @author P
 *
 */
public final class NoLock implements Lock {

	/**
	 * 
	 */
	public NoLock() {
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.locks.Lock#lock()
	 */
	@Override
	public void lock() {
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.locks.Lock#lockInterruptibly()
	 */
	@Override
	public void lockInterruptibly() throws InterruptedException {
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.locks.Lock#tryLock()
	 */
	@Override
	public boolean tryLock() {
		return true;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.locks.Lock#tryLock(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		return true;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.locks.Lock#unlock()
	 */
	@Override
	public void unlock() {

	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.locks.Lock#newCondition()
	 */
	@Override
	public Condition newCondition() {
		return null;
	}

}
