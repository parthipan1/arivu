/**
 * 
 */
package org.arivu.utils.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import sun.misc.Unsafe;

/**
 * @author P
 *
 */
@SuppressWarnings("restriction")
public final class AtomicReentrantLock implements Lock {

//	final AtomicBoolean cas = new AtomicBoolean(false);
	private volatile long cas = 0l;
	private long offset;
	Unsafe unsafe;

	volatile Reentrant reentrant = null;

	/**
	 * 
	 */
	public AtomicReentrantLock() {
		super();
		try {
			unsafe = AtomicWFReentrantLock.getUnsafe();
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
		try {
			offset = unsafe.objectFieldOffset(AtomicReentrantLock.class.getDeclaredField("cas"));
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
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
		while (!unsafe.compareAndSwapLong(this, offset, 0l, 1l)) {}
		reentrant = new Reentrant();
	}

	@Override
	public void unlock() {
		if (reentrant != null) {
			try {
				if (reentrant.release()) {
					reentrant = null;
//					cas.set(false);
					cas = 0l;
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
//		return !cas.get();
		return cas == 0l;
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
		return new ACondition();
	}
}
