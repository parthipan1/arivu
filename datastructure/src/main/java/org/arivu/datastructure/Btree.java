/**
 * 
 */
package org.arivu.datastructure;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import org.arivu.utils.NullCheck;
import org.arivu.utils.lock.AtomicWFReentrantLock;
import org.arivu.utils.lock.NoLock;

/**
 * B tree implementation for faster concurrent read/write Objects. Heap memory
 * segmented into buckets and each bucket will have Object[] storing the object.
 * Hashcode is used to generate object addresses.
 * 
 * @author P
 *
 */
public final class Btree implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6344951761380914875L;

	static final int DEFAULT_BASEPOWER = 2;

	final Object[] root;
	private final int base;
	private final int height;
	private final int baseMask;
	final int order;
//	final Lock cas;
	 final Lock[] locks;
	private final CompareStrategy compareStrategy;

	// volatile int size = 0;
	final AtomicInteger size = new AtomicInteger(0);

	/**
	 * Create B tree based on power value , higher the power faster the
	 * performance. Power value can be with in 1-4. 
	 * 
	 * @param order
	 */
	Btree(int basePower) {
		this(basePower, new AtomicWFReentrantLock(), CompareStrategy.EQUALS);
	}

	/**
	 * Create B tree with default power 2 , with height 8.
	 */
	public Btree() {
		this(DEFAULT_BASEPOWER);
	}

	/**
	 * Create B tree with default power 2 , with height 8.
	 * 
	 * @param lock
	 */
	Btree(Lock lock) {
		this(DEFAULT_BASEPOWER, lock, CompareStrategy.EQUALS);
	}

	/**
	 * Create B tree with default power 2 , with height 8.
	 * 
	 * @param lock
	 * @param compareStrategy
	 */
	Btree(Lock lock, CompareStrategy compareStrategy) {
		this(DEFAULT_BASEPOWER, lock, compareStrategy);
	}

	/**
	 * Create B tree with default power 2 , with height 8.
	 * 
	 * @param basePower
	 * @param lock
	 * @param compareStrategy
	 */
	Btree(int basePower, Lock lock, CompareStrategy compareStrategy) {
		super();
		if (basePower <= 0 || basePower > 4) {
			throw new IllegalArgumentException("Invalid basePower " + basePower + " (between 1-4) specified!");
		}
		this.base = 1 << basePower;
		this.height = 32 / base;
		this.order = 1 << base;
		this.baseMask = (order - 1);
		this.compareStrategy = compareStrategy;
		this.root = new Object[order];
//		this.cas = lock;
		this.locks = new Lock[order];
		for (int i = 0; i < order; i++) {
			this.locks[i] = new AtomicWFReentrantLock();
		}
	}

	int[] getPathObj(final Object obj) {
		return getPath(obj.hashCode());
	}

	private int[] getPath(int hashCode) {
		if (height == 8)
			return getPath8(hashCode);
		else if (height == 16)
			return getPath16(hashCode);
		else {
			int[] ret = new int[height];
			for (int i = height - 1; i >= 0; i--) {
				ret[i] = (int) (hashCode & baseMask);
				
				if (hashCode != 0)
					hashCode = hashCode >>> base;
			}
			// //System.out.println(" getPath act hashCode "+hashCode2+" ret
			// "+con(ret));
			return ret;
		}
	}
	
	private int[] getPath8(int hashCode) {
		int[] ret = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };

		if (hashCode == 0)
			return ret;
		ret[7] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[6] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[5] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[4] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[3] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[2] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[1] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[0] = (int) (hashCode & baseMask);

		// for (int i = height - 1; i >= 0; i--) {
		// ret[i] = (int) (hashCode & baseMask);
		//
		// if (hashCode != 0)
		// hashCode = hashCode >>> base;
		// }
		// //System.out.println(" getPath act hashCode "+hashCode2+" ret
		// "+con(ret));
		return ret;
	}

	private int[] getPath16(int hashCode) {
		int[] ret = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

		if (hashCode == 0)
			return ret;
		ret[15] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[14] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[13] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[12] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[11] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[10] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[9] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[8] = (int) (hashCode & baseMask);
		if (hashCode == 0)
			return ret;
		ret[7] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[6] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[5] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[4] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[3] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[2] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[1] = (int) (hashCode & baseMask);
		hashCode = hashCode >>> base;
		if (hashCode == 0)
			return ret;
		ret[0] = (int) (hashCode & baseMask);

		// for (int i = height - 1; i >= 0; i--) {
		// ret[i] = (int) (hashCode & baseMask);
		//
		// if (hashCode != 0)
		// hashCode = hashCode >>> base;
		// }
		// //System.out.println(" getPath act hashCode "+hashCode2+" ret
		// "+con(ret));
		return ret;
	}
	
	public int getHeight() {
		return height;
	}

	public int size() {
		return size.get();
	}

	public boolean add(final Object obj) {
		if (obj == null)
			return false;
		return addObj(obj, getPathObj(obj));
		// System.out.println(this+" bt add "+obj+" path "+con(path));
	}

	public Object remove(final Object obj) {
		if (obj == null)
			return null;
		return removeObj(obj, getPathObj(obj));
	}

	public Object get(final Object obj) {
		if (obj == null)
			return null;
		return findObj(obj, getPathObj(obj));
	}

	public void clear() {
		clear(root);
	}

	public Collection<Object> getAll() {
		return getAll(root);
	}

	void clear(final Object[] node) {
		// cas.lock();
		resetNodes(node);
		size.set(0);// = 0;
		// cas.unlock();
	}

	void resetNodes(final Object[] node) {
		if (node != null) {
			for (int i = 0; i < node.length; i++)
				node[i] = null;
		}
	}

	Object searchArr(final Object[] arr, final Object obj) {
		if (!NullCheck.isNullOrEmpty(arr)) {
			for (int i = 0; i < arr.length; i++) {
				Object object = arr[i];
				if (object != null && this.compareStrategy.compare(obj, object)) {
					return object;
				}
			}
		}
		return null;
	}

	static int getSize(final Object[] arr) {
		if (!NullCheck.isNullOrEmpty(arr)) {
			int c = 0;
			for (int i = 0; i < arr.length; i++) {
				if (arr[i] != null)
					c++;
			}
			return c;
		}
		return 0;
	}

	Object removeArr(final Object[] arr, final Object obj) {
		if (!NullCheck.isNullOrEmpty(arr)) {
			for (int i = 0; i < arr.length; i++) {
				Object object = arr[i];
				if (object != null && this.compareStrategy.compare(obj, object)) {
					arr[i] = null;
					return object;
				}
			}
		}
		return null;
	}

	boolean addArr(final Object[] arr, final Object obj, final Object[] node, final int[] idxArr) {
		int idx = -1;
		for (int i = 0; i < arr.length; i++) {
			Object object = arr[i];
			if (object != null && this.compareStrategy.compare(obj, object)) {
				return false;
			} else {
				if (object == null && idx == -1)
					idx = i;
			}
		}
		if (idx == -1) {
			Object[] narr = new Object[arr.length + 1];
			System.arraycopy(arr, 0, narr, 0, arr.length);
			narr[arr.length] = obj;
			node[idxArr[idxArr.length - 1]] = narr;
		} else {
			arr[idx] = obj;
		}
		return true;
	}

	boolean addObj(final Object obj, final int[] arr) {
		 final Lock l = this.locks[arr[0]];//cas;//
		 l.lock();
//		final LinkedRef nodes = new LinkedRef(compareStrategy);
		
		Object[] n = root;
//		nodes.addObj(n);
		for (int i = 0; i <= arr.length - 2; i++) {
			Object[] n1 = (Object[]) n[arr[i]];
			if (n1 == null) {
				n1 = new Object[order];
				n[arr[i]] = n1;
			}
			n = n1;
//			nodes.addObj(n);
		}
		l.unlock();
		Object[] ref = (Object[]) n[arr[arr.length - 1]];
		if (ref == null) {
			ref = new Object[1];
			n[arr[arr.length - 1]] = ref;
			ref[0] = obj;
			// size++;
			size.incrementAndGet();
			// l.unlock();
			return true;
		} else {
			final boolean add = addArr(ref, obj, n, arr);
			if (add)
				size.incrementAndGet();
			// size++;

			// l.unlock();
			return add;
		}
	}

	Object[] findLeaf(final Object obj, final int[] arr, final LinkedRef path) {
		Object[] n = root;
		if (path != null)
			path.addObj(n);
		for (int i = 0; i <= arr.length - 2; i++) {
			if (n == null)
				return null;
			else {
				n = (Object[]) n[arr[i]];
				if (path != null && n != null)
					path.addObj(n);
			}
		}
		if (n == null)
			return null;
		else
			return (Object[]) n[arr[arr.length - 1]];
	}

	Object findObj(final Object obj, final int[] arr) {
		return searchArr(findLeaf(obj, arr, null), obj);
	}

	Object removeObj(final Object obj, final int[] arr) {
		final LinkedRef nodes = new LinkedRef(compareStrategy);
		final Object[] ref = findLeaf(obj, arr, nodes);
		if (ref == null) {
			return null;
		}

		final Object search = removeArr(ref, obj);
		if (search == null) {
			return null;
		} else {
			// try{
			if (getSize(ref) == 0) {
				final Lock l = this.locks[arr[0]];//cas;//
				l.lock();
				try{
				int c = 0;
				LinkedRef cref = nodes.left;
				while (cref != null && cref.obj != null && cref != nodes) {
					final Object[] obj2 = (Object[]) cref.obj;

					if (getSize(obj2) == 1) {
						obj2[arr[c++]] = null;
					} else {
						break;
					}

					cref = cref.left;
				}
				}finally{
					l.unlock();
				}
			}
			// size--;
			size.decrementAndGet();
			// }finally{
			// }
		}

		return search;
	}

	static Collection<Object> getAll(final Object[] node) {
		Collection<Object> list = new DoublyLinkedList<Object>();
		if (node == null) {
			return list;
		} else {
			for (final Object n : node) {
				if (n != null) {
					if (n instanceof Object[])
						list.addAll(getAll((Object[]) n));
					else
						list.add(n);
				}
			}
		}
		return list;
	}
}

/**
 * Circular buffer to store all the logs and consumers.
 * 
 * @author P
 *
 */
final class LinkedRef {
	/**
	 * 
	 */
	Object obj;

	/**
	 * 
	 */
	volatile LinkedRef left = this, right = this;

	Lock lock = null;
	CompareStrategy compareStrategy;
	String id;

	/**
	 * 
	 */
	LinkedRef(String id) {
		this(CompareStrategy.REF);
		this.id = id;
	}

	/**
	 * @param compareStrategy
	 *            TODO
	 * 
	 */
	LinkedRef(CompareStrategy compareStrategy) {
		this(null, compareStrategy, new NoLock());
	}

	/**
	 * @param compareStrategy
	 *            TODO
	 * 
	 */
	LinkedRef(CompareStrategy compareStrategy, Lock lock) {
		this(null, compareStrategy, lock);
	}

	/**
	 * @param compareStrategy
	 *            TODO
	 * @param lock
	 *            TODO
	 * @param obj
	 */
	private LinkedRef(Object t, CompareStrategy compareStrategy, Lock lock) {
		super();
		this.obj = t;
		this.lock = lock;
		this.compareStrategy = compareStrategy;
	}

	/**
	 * @return
	 */
	boolean isEmpty() {
		return this == left;
	}

	/**
	 * @return
	 */
	LinkedRef remove(final Direction dir) {
		final LinkedRef r = dir.get(this);
		if (r == this || r == null) {
			return null;
		} else {
			r.remove();
			return r;
		}
	}

	/**
	 * @param obj
	 * @return
	 */
	LinkedRef search(final Object o) {

		LinkedRef ref = this.right;
		while (ref != null && ref.obj != null && ref != this) {
			if (this.compareStrategy.compare(ref.obj, o)) {
				return ref;
			} else {
				ref = ref.right;
			}
			// System.out.println(" search ref "+ref+" compareStrategy
			// "+compareStrategy+" ref.obj "+ref.obj+" o "+o+" this "+this);
		}
		return null;
	}

	/**
	 * @param t
	 * @param rwLock
	 *            TODO
	 * @return
	 */
	boolean addObj(final Object t) {
		if (t != null) {
			add(new LinkedRef(t, compareStrategy, lock), Direction.left);
			return true;
		}
		return false;
	}

	/**
	 * @param l
	 * @param direction
	 *            TODO
	 * @return
	 */
	private LinkedRef add(final LinkedRef l, final Direction direction) {
		if (l != null) {
			Lock lk = lock;
			lk.lock();
			direction.getOther().set(l, this);
			final LinkedRef tl = direction.get(this);
			direction.set(this, l);
			direction.set(l, tl);
			direction.getOther().set(tl, l);
			lk.unlock();
		}
		return l;
	}

	/**
	 * 
	 */
	Object remove() {
		Object o = obj;
		Lock l = lock;
		l.lock();
		Direction.right.set(left, right);
		Direction.left.set(right, left);
		Direction.right.set(this, null);
		Direction.left.set(this, null);
		lock = null;
		compareStrategy = null;
		obj = null;
		l.unlock();
		return o;
	}

	@Override
	public int hashCode() {
		return ((id == null) ? 0 : id.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LinkedRef other = (LinkedRef) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}

enum Direction {
	left {

		@Override
		LinkedRef get(final LinkedRef ref) {
			if (ref == null) {
				return null;
			} else {
				return ref.left;
			}
		}

		@Override
		void set(final LinkedRef ref, final LinkedRef next) {
			if (ref != null && next != null) {
				ref.left = next;
			}
		}

	},
	right;

	LinkedRef get(final LinkedRef ref) {
		if (ref == null) {
			return null;
		} else {
			return ref.right;
		}
	}

	void set(final LinkedRef ref, final LinkedRef next) {
		if (ref != null && next != null) {
			ref.right = next;
		}
	}

	LinkedRef remove(final LinkedRef ref) {
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