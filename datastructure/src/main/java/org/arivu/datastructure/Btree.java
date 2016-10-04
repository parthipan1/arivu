/**
 * 
 */
package org.arivu.datastructure;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.locks.Lock;

import org.arivu.utils.NullCheck;
import org.arivu.utils.lock.AtomicWFReentrantLock;
import org.arivu.utils.lock.NoLock;

/**
 * @author P
 *
 */
public final class Btree implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6344951761380914875L;

	private static final int DEFAULT_BASEPOWER = 2;

	final Object[] root;
	private final int base;
	private final int height;
	private final int baseMask;
	private final int order;
	final Lock cas;
	private final CompareStrategy compareStrategy;

	volatile int size = 0;

	/**
	 * @param order
	 */
	public Btree(int basePower) {
		this(basePower, new AtomicWFReentrantLock(), CompareStrategy.EQUALS);
	}

	/**
	 * 
	 */
	public Btree() {
		this(DEFAULT_BASEPOWER);
	}

	/**
	 * @param lock
	 */
	Btree(Lock lock) {
		this(DEFAULT_BASEPOWER, lock, CompareStrategy.EQUALS);
	}

	/**
	 * @param lock
	 * @param compareStrategy
	 */
	Btree(Lock lock, CompareStrategy compareStrategy) {
		this(DEFAULT_BASEPOWER, lock, compareStrategy);
	}

	/**
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
		this.cas = lock;
	}

	int[] getPathObj(final Object obj) {
		return getPath(obj.hashCode());
	}

	private int[] getPath(int hashCode) {
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

	public int getHeight() {
		return height;
	}

	public int size() {
		return size;
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
		cas.lock();
		resetNodes(node);
		size = 0;
		cas.lock();
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
			Object[] narr = new Object[arr.length + order];
			System.arraycopy(arr, 0, narr, 0, arr.length);
			narr[arr.length] = obj;
			node[idxArr[idxArr.length - 1]] = narr;
		} else {
			arr[idx] = obj;
		}
		return true;
	}

	boolean addObj(final Object obj, final int[] arr) {
		final Lock l = cas;
		l.lock();
		final LinkedReference nodes = new LinkedReference(compareStrategy);
		Object[] n = root;
		nodes.add(n);
		for (int i = 0; i <= arr.length - 2; i++) {
			Object[] n1 = (Object[]) n[arr[i]];
			if (n1 == null) {
				n1 = new Object[order];
				n[arr[i]] = n1;
			}
			n = n1;
			nodes.add(n);
		}

		Object[] ref = (Object[]) n[arr[arr.length - 1]];
		if (ref == null) {
			ref = new Object[order];
			n[arr[arr.length - 1]] = ref;
			ref[0] = obj;
			size++;
			l.unlock();
			return true;
		}else{
			final boolean add = addArr(ref, obj, n, arr);
			if (add) 
				size++;
			
			l.unlock();
			return add;
		}
	}

	Object[] findLeaf(final Object obj, final int[] arr, final LinkedReference path) {
		Object[] n = root;
		if (path != null)
			path.add(n);
		for (int i = 0; i <= arr.length - 2; i++) {
			if (n == null)
				return null;
			else {
				n = (Object[]) n[arr[i]];
				if (path != null && n != null)
					path.add(n);
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
		final LinkedReference nodes = new LinkedReference(compareStrategy);
		final Object[] ref = findLeaf(obj, arr, nodes);
		if (ref == null) {
			return null;
		}

		final Object search = removeArr(ref, obj);
		if (search == null) {
			return null;
		} else {
			final Lock l = cas;
			l.lock();

			if (getSize(ref) == 0) {
				int c = 0;
				LinkedReference cref = nodes.left;
				while (cref != null && cref.obj != null && cref != nodes) {
					final Object[] obj2 = (Object[]) cref.obj;

					if (getSize(obj2) == 1) {
						obj2[arr[c++]] = null;
					} else {
						break;
					}

					cref = cref.left;
				}
			}
			size--;
			l.unlock();
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
final class LinkedReference {
	/**
	 * 
	 */
	Object obj;

	/**
	 * 
	 */
	volatile LinkedReference left = this, right = this;

	Lock lock = null;
	CompareStrategy compareStrategy;
	String id;

	/**
	 * @param compareStrategy
	 *            TODO
	 * 
	 */
	LinkedReference(CompareStrategy compareStrategy) {
		this(null, compareStrategy, new NoLock());
	}

	/**
	 * @param compareStrategy
	 *            TODO
	 * 
	 */
	LinkedReference(CompareStrategy compareStrategy, Lock lock) {
		this(null, compareStrategy, lock);
	}

	/**
	 * @param compareStrategy
	 *            TODO
	 * @param lock
	 *            TODO
	 * @param obj
	 */
	private LinkedReference(Object t, CompareStrategy compareStrategy, Lock lock) {
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
	LinkedReference remove(final Direction dir) {
		final LinkedReference r = dir.get(this);
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
	LinkedReference search(final Object o) {

		LinkedReference ref = this.right;
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
	boolean add(final Object t) {
		if (t != null) {
			add(new LinkedReference(t, compareStrategy, lock), Direction.left);
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
	private LinkedReference add(final LinkedReference l, final Direction direction) {
		if (l != null) {
			Lock lk = lock;
			lk.lock();
			direction.getOther().set(l, this);
			final LinkedReference tl = direction.get(this);
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

}

enum Direction {
	left {

		@Override
		LinkedReference get(final LinkedReference ref) {
			if (ref == null) {
				return null;
			} else {
				return ref.left;
			}
		}

		@Override
		void set(final LinkedReference ref, final LinkedReference next) {
			if (ref != null && next != null) {
				ref.left = next;
			}
		}

	},
	right;

	LinkedReference get(final LinkedReference ref) {
		if (ref == null) {
			return null;
		} else {
			return ref.right;
		}
	}

	void set(final LinkedReference ref, final LinkedReference next) {
		if (ref != null && next != null) {
			ref.right = next;
		}
	}

	LinkedReference remove(final LinkedReference ref) {
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