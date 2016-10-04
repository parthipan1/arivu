/**
 * 
 */
package org.arivu.utils.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @author P
 *
 */
public final class AtomicWFReentrantLock implements Lock {

	final LinkedReference<CountDownLatch> waits = new LinkedReference<CountDownLatch>();

	final AtomicBoolean cas = new AtomicBoolean(false);

	volatile Reentrant reentrant = null;

	/**
	 * 
	 */
	public AtomicWFReentrantLock() {
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
			waitForSignal();
		}
		reentrant = new Reentrant();
	}

	private void waitForSignal() {
		final CountDownLatch wait = new CountDownLatch(1);
		waits.add(wait, Direction.left);

		try {
			wait.await();
		} catch (InterruptedException e) {
			// System.err.println(e.getMessage());
		}
	}

	@Override
	public void unlock() {
		if (reentrant != null) {
			try {
				if (reentrant.release()) {
					reentrant = null;
					cas.set(false);
					releaseAWait();
				} else {
				}
			} catch (NullPointerException e) {
			}
		}
	}

	private void releaseAWait() {
		CountDownLatch poll = waits.poll(Direction.right);
		if (poll != null) {
			poll.countDown();
		}
	}

//	void releaseAllWait() {
//		CountDownLatch poll = null;
//		while ((poll = waits.poll(Direction.right)) != null) {
//			poll.countDown();
//		}
//	}

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

final class Reentrant {
	final int id = getId();

	static int getId() {
		return Thread.currentThread().hashCode();
	}

//	final AtomicLong cnt = new AtomicLong(1);
	volatile long cnt = 1l;//new AtomicLong(1);

	/**
	 * 
	 */
	Reentrant() {
		super();
	}

	boolean acquire() {
		if (isSame()) {
//			cnt.incrementAndGet();
			cnt++;
			return true;
		} else
			return false;
	}

	boolean release() {
//		return cnt.decrementAndGet() == 0l;
		return --cnt == 0l;
	}

	boolean isSame() {
		return id == getId();
	}
}