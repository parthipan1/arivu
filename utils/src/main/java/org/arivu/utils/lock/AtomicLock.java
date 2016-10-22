/**
 * 
 */
package org.arivu.utils.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import sun.misc.Unsafe;

/**
 * @author P
 *
 */
public final class AtomicLock implements Lock{
	
//	final AtomicBoolean cas = new AtomicBoolean(false);
	private volatile long cas = 0l;
	private long offset;
	Unsafe unsafe;
	
	/**
	 * 
	 */
	AtomicLock() {
		super();
		try {
			unsafe = AtomicWFReentrantLock.getUnsafe();
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
		try {
			offset = unsafe.objectFieldOffset(AtomicLock.class.getDeclaredField("cas"));
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void lock(){
		while (!unsafe.compareAndSwapLong(this, offset, 0l, 1l)) {}
	}
	
	@Override
	public void unlock(){
//		cas.set(false);
		cas = 0l;
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
		if (unit!=null) {
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
