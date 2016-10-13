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
public final class AtomicLock implements Lock{
	
	final AtomicBoolean cas = new AtomicBoolean(false);
	
	
	@Override
	public void lock(){
		while(!cas.compareAndSet(false, true)){}
	}
	
	@Override
	public void unlock(){
		cas.set(false);
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
