/**
 * 
 */
package org.arivu.utils.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @author P
 *
 */
public final class AtomicReentrantLock implements Lock {

	final AtomicBoolean cas = new AtomicBoolean(false);

	volatile Reentrant reentrant = null;

	/**
	 * 
	 */
	public AtomicReentrantLock() {
		super();
	}

	@Override
	public void lock() {
		if (reentrant != null) {
			try {
				if (!reentrant.acquire())
					internalLock();
			} catch (NullPointerException e) {
				internalLock();
			}
		} else {
			internalLock();
		}
	}

	private void internalLock() {
		while (!cas.compareAndSet(false, true)) {
		}
		reentrant = new Reentrant();
	}

	@Override
	public void unlock() {
		if (reentrant != null) {
			try {
				if (reentrant.release()) {
					reentrant = null;
					cas.set(false);
				} else {
				}
			} catch (NullPointerException e) {
			}
		}
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {

	}

	@Override
	public boolean tryLock() {
		return !cas.get();
	}

	private static final int delta = 100;

	@Override
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		if (unit != null) {
			long nanos = unit.toNanos(time);
			do {
				if (tryLock())
					return true;
				Thread.sleep(0, delta);
				nanos -= delta;
			} while (nanos > 0);
		}
		return tryLock();
	}

	@Override
	public Condition newCondition() {
		throw new RuntimeException("Unsupported function!");
	}
}
