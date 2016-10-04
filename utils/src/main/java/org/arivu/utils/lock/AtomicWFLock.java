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
public final class AtomicWFLock implements Lock {

	final LinkedReference<CountDownLatch> waits = new LinkedReference<CountDownLatch>();

	final AtomicBoolean cas = new AtomicBoolean(false);
	
	/**
	 * 
	 */
	public AtomicWFLock() {
		super();
	}

	@Override
	public void lock() {
		while (!cas.compareAndSet(false, true)) {
			waitForSignal();
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
		cas.set(false);
		releaseAWait();
	}

	private void releaseAWait() {
		CountDownLatch poll = waits.poll(Direction.right);
		if (poll != null) {
			poll.countDown();
		}
	}

	void releaseAllWait() {
		CountDownLatch poll = null;
		while ((poll = waits.poll(Direction.right)) != null) {
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

/**
 * @author P
 *
 * @param <T>
 */
final class LinkedReference<T> {
	Lock lock;

	/**
	 * 
	 */
	T obj;

	/**
	 * 
	 */
	volatile LinkedReference<T> left = this, right = this;

	/**
	 * 
	 */
	public LinkedReference() {
		this(null, new AtomicLock());
	}

	/**
	 * @param lock TODO
	 * @param obj
	 */
	private LinkedReference(T t, Lock lock) {
		super();
		this.obj = t;
		this.lock = new AtomicLock();
	}

	/**
	 * 
	 */
	void clear() {
		left = this;
		right = this;
	}

	/**
	 * @return
	 */
	boolean isEmpty() {
		return size(false, Direction.left) == 0;
	}

	/**
	 * @param rwLock
	 *            TODO
	 * @return
	 */
	T poll(final Direction dir) {
		LinkedReference<T> removeRef = null;
		Lock l = lock;
		l.lock();
		removeRef = remove(dir);
		l.unlock();
		if (removeRef != null) {
			T obj2 = removeRef.obj;
			removeRef.obj = null;
			return obj2;
		}
		return null;
	}

	/**
	 * @return
	 */
	LinkedReference<T> remove(final Direction dir) {
		final LinkedReference<T> r = dir.get(this);// this.right;
		if (r == this || r == null) {
			return null;
		} else {
			r.remove();
			return r;
		}
	}

	// /**
	// * @param direction TODO
	// * @param obj
	// * @return
	// */
	// LinkedReference<T> search(final T o, final Direction direction){
	// LinkedReference<T> ref = this;
	// while (ref != null) {
	// if(ref.obj!=null && ref.obj==o){
	// return ref;
	// }
	// ref = direction.get(ref);
	// if (ref == this) {
	// break;
	// }
	// }
	// return null;
	// }

	/**
	 * @param t
	 * @param direction
	 *            TODO
	 * @param rwLock
	 *            TODO
	 * @return
	 */
	T add(final T t, Direction direction) {
		if (t != null) {
			Lock l = lock;
			l.lock();
			add(new LinkedReference<T>(t, l), direction);
			l.unlock();
		}
		return t;
	}

	/**
	 * @param l
	 * @param direction
	 *            TODO
	 * @return
	 */
	private LinkedReference<T> add(final LinkedReference<T> l, final Direction direction) {
		if (l != null) {
			direction.getOther().set(l, this);
			final LinkedReference<T> tl = direction.get(this);
			direction.set(this, l);
			direction.set(l, tl);
			direction.getOther().set(tl, l);
		}
		return l;
	}

	// /**
	// * @param direction TODO
	// * @return
	// */
	// Object[] toArray(final Direction direction) {
	// List<T> subl = new ArrayList<T>();
	// LinkedReference<T> ref = direction.get(this);//this.right;
	// while (ref != null) {
	// if (ref == this) {
	// break;
	// }
	// if (ref.obj!=null) {
	// subl.add(ref.obj);
	// }
	// ref = direction.get(ref);//ref.right;
	// }
	// return subl.toArray();
	// }

	/**
	 * 
	 */
	void remove() {
		Direction.right.set(left, right);
		Direction.left.set(right, left);
		Direction.right.set(this, null);
		Direction.left.set(this, null);
		lock = null;
	}

	/**
	 * @param includeNull
	 *            TODO
	 * @param direction
	 *            TODO
	 * @return
	 */
	int size(final boolean includeNull, final Direction direction) {
		int cnt = 0;
		LinkedReference<T> ref = direction.get(this);
		while (ref != null) {
			if (ref == this) {
				return cnt;
			}
			if (includeNull) {
				cnt++;
			} else {
				if (ref.obj != null) {
					cnt++;
				}
			}
			LinkedReference<T> tref = ref;
			ref = direction.get(ref);
			if (tref == ref)
				break;
			// System.out.println(" size ref "+ref+" left "+ref.left+" this
			// "+this+" cnt "+cnt);
		}
		return cnt;
	}
}

/**
 * Direction of linked reference traversal
 * 
 * @author P
 *
 */
enum Direction {
	left {

		@Override
		<T> LinkedReference<T> get(final LinkedReference<T> ref) {
			if (ref == null) {
				return null;
			} else {
				return ref.left;
			}
		}

		@Override
		<T> void set(final LinkedReference<T> ref, final LinkedReference<T> next) {
			if (ref != null && next != null) {
				ref.left = next;
			}
		}

	},
	right;

	<T> LinkedReference<T> get(final LinkedReference<T> ref) {
		if (ref == null) {
			return null;
		} else {
			return ref.right;
		}
	}

	<T> void set(final LinkedReference<T> ref, final LinkedReference<T> next) {
		if (ref != null && next != null) {
			ref.right = next;
		}
	}

	<T> LinkedReference<T> remove(final LinkedReference<T> ref) {
		if (ref == null) {
			return null;
		} else {
			return ref.remove(this);
		}
	}

	Direction getOther() {
		if (this == Direction.right)
			return left;
		else
			return right;
	}
}