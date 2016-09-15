/**
 * 
 */
package org.arivu.utils.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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
	
	@Override
	public void lock() {
		if (reentrant!=null && reentrant.isSame() ) {
			reentrant.acquire();
			System.out.println("Rentrant acquire!");
		}else{
			while (!cas.compareAndSet(false, true)) {
				waitForSignal();
			}
//			if( reentrant == null ){
//				synchronized (this) {
//					if( reentrant == null ){
						reentrant = new Reentrant();
						System.out.println("Rentrant created!");
//					}
//				}
//			}
		}
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
		if (reentrant!=null && reentrant.release()){
			reentrant = null;
			cas.set(false);
			releaseAWait();
		}else{
//			throw new RuntimeException("Lock unlock called with out lock");
			System.err.println("Lock unlock called with out lock,"+Reentrant.getId());
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
		throw new RuntimeException("Unsupported function!");
	}
}
final class Reentrant{
	final String id = getId();

	static String getId() {
		return String.valueOf(Thread.currentThread().hashCode());
	}
	final AtomicLong cnt = new AtomicLong(1);
	
	void acquire(){
		cnt.incrementAndGet();
	}

	boolean release(){
		boolean b = cnt.decrementAndGet() == 0l;
		System.out.println("Rentrant released! "+b);
		return b;
	}
	
	boolean isSame(){
		return id.equalsIgnoreCase(getId());
	}
}