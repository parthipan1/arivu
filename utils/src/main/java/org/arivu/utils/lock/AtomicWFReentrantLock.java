/**
 * 
 */
package org.arivu.utils.lock;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import sun.misc.Unsafe;

/**
 * @author P
 *
 */
public final class AtomicWFReentrantLock implements Lock {
	static Unsafe unsafe;

	static Unsafe getUnsafe() throws Exception {
		if (unsafe == null) {
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			unsafe = (Unsafe) f.get(null);
		}
		return unsafe;
	}

	final LinkedReference<CountDownLatch> waits = new LinkedReference<CountDownLatch>();

	// final AtomicBoolean cas = new AtomicBoolean(false);
	private volatile long cas = 0l;

	private long offset;

	volatile Reentrant reentrant = null;

	/**
	 * 
	 */
	public AtomicWFReentrantLock() {
		super();
		try {
			getUnsafe();
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
		try {
			offset = unsafe.objectFieldOffset(AtomicWFReentrantLock.class.getDeclaredField("cas"));
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
		while (!unsafe.compareAndSwapLong(this, offset, 0l, 1l)) {
			waitForSignal();
        }
//		while (!cas.compareAndSet(false, true)) {
//			waitForSignal();
//		}
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
					cas = 0l;
//					cas.set(false);
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

final class Reentrant {
	final int id = getId();

	static int getId() {
		return Thread.currentThread().hashCode();
	}

	volatile long cnt = 1l;

	/**
	 * 
	 */
	Reentrant() {
		super();
	}

	boolean acquire() {
		if (isSame()) {
			cnt++;
			return true;
		} else
			return false;
	}

	boolean release() {
		return --cnt == 0l;
	}

	boolean isSame() {
		return id == getId();
	}
}

final class ACondition implements Condition {
	final LinkedReference<CountDownLatch> waits = new LinkedReference<CountDownLatch>();

	@Override
	public void await() throws InterruptedException {
		CountDownLatch wait = new CountDownLatch(1);
		waits.add(wait, Direction.left);
		wait.await();
		wait = null;
	}

	@Override
	public void awaitUninterruptibly() {
		try {
			await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public long awaitNanos(long nanosTimeout) throws InterruptedException {
		await(nanosTimeout, TimeUnit.NANOSECONDS);
		return 0;
	}

	@Override
	public boolean await(long time, TimeUnit unit) throws InterruptedException {
		CountDownLatch wait = new CountDownLatch(1);
		waits.add(wait, Direction.left);
		wait.await(time, unit);
		wait = null;
		return false;
	}

	@Override
	public boolean awaitUntil(Date deadline) throws InterruptedException {
		if (deadline != null) {
			long ms = deadline.getTime() - System.currentTimeMillis();
			if (ms > 0) {
				return await(ms, TimeUnit.MILLISECONDS);
			}
		}
		return false;
	}

	@Override
	public void signal() {
		CountDownLatch poll = waits.poll(Direction.right);
		if (poll != null) {
			poll.countDown();
		}
	}

	@Override
	public void signalAll() {
		CountDownLatch poll = null;
		while ((poll = waits.poll(Direction.right)) != null) {
			poll.countDown();
		}
	}

}