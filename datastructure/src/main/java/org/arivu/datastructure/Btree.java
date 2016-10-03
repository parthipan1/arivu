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

	/**
	 * @author P
	 *
	 */
	static final class Node {
		Object[] nodes;
		 volatile int cnt = 0;
	}

	final Node root;
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
		this.base = 1 << basePower;// (int) Math.pow(2, basePower);
		this.height = 32 / base;
		this.order = 1 << base;// (int) Math.pow(2, base);
		this.baseMask = (order - 1);
		this.compareStrategy = compareStrategy;
		this.root = new Node();
		this.cas = lock;
	}

	int[] getPath(Object obj) {
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

	public void add(final Object obj) {
		if (obj == null)
			return;
		addObj(obj, getPath(obj));
		// System.out.println(this+" bt add "+obj+" path "+con(path));
	}

	public Object remove(final Object obj) {
		if (obj == null)
			return null;
		Object remove = removeObj(obj, getPath(obj));
		return remove;
	}

	public Object get(final Object obj) {
		if (obj == null)
			return null;
		int[] path = getPath(obj);
		Object find = findObj(obj, path);
		// System.out.println(this+" bt search "+obj+" find "+find+" path
		// "+con(path));
		return find;
	}

	public void clear() {
		clear(root);
	}

	public Collection<Object> getAll() {
		return getAll(root);
	}

	void clear(final Node node) {
		cas.lock();
		resetNodes(node);
//		node.cnt = 0;// counter.set(0);
		size = 0;
		cas.lock();
	}

	void resetNodes(final Node node) {
		if (node.nodes != null) {
			for (int i = 0; i < node.nodes.length; i++)
				node.nodes[i] = null;
			node.nodes = null;
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

	static class Sh{
		final int cnt;
		final int[] arrIdx;
		/**
		 * @param cnt
		 * @param arrIdx
		 */
		Sh(int cnt, int[] arrIdx) {
			super();
			this.cnt = cnt;
			this.arrIdx = arrIdx;
		}
		
	}
	
	static Sh getSize(final Object[] arr) {
		if (!NullCheck.isNullOrEmpty(arr)) {
			int c = 0;
			int[] arrIdx = new int[arr.length];
			for (int i = 0; i < arr.length; i++) {
				if (arr[i] != null)
					arrIdx[c++]=i;
			}
			return new Sh(c, arrIdx);
		}
		return new Sh(0, null);
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

	boolean addArr(final Object[] arr, final Object obj, final Node node, final int[] idxArr) {
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
		// System.out.println("addArr idx::"+idx+" obj::"+obj);
		if (idx == -1) {
			Object[] narr = new Object[arr.length + order];
			System.arraycopy(arr, 0, narr, 0, arr.length);
			narr[arr.length] = obj;
			node.nodes[idxArr[idxArr.length - 1]] = narr;
		} else {
			arr[idx] = obj;
		}
		return true;
	}

	boolean addObj(final Object obj, final int[] arr) {
		final Lock l = cas;
		l.lock();
		final LinkedReference nodes = new LinkedReference(compareStrategy);// ,
																			// this.cas
		Node n = root;
		nodes.add(n);
		for (int i = 0; i <= arr.length - 2; i++) {
			if (n.nodes == null) {
				n.nodes = new Object[order];
			}
			Node n1 = (Node) n.nodes[arr[i]];
			if (n1 == null) {
				n1 = new Node();
				n.nodes[arr[i]] = n1;
			}
			n = n1;
			nodes.add(n);
		}

		if (n.nodes == null) {
			n.nodes = new Object[order];
		}

		Object[] ref = (Object[]) n.nodes[arr[arr.length - 1]];
		if (ref == null) {
			ref = new Object[order];// new LinkedReference(compareStrategy);// ,
									// this.cas
			n.nodes[arr[arr.length - 1]] = ref;
		}
		final boolean add = addArr(ref, obj, n, arr);// ((LinkedReference)
														// ref).add(obj);

		if (add) {
			LinkedReference cref = nodes.right;
//			int c = 0;
			while (cref != null && cref.obj != null && cref != nodes) {
				Node obj2 = (Node) cref.obj;
				obj2.cnt++;// counter.incrementAndGet();

//				System.out
//						.println("add Node size :: " + getSize(obj2.nodes) + " obj2 :: " + obj2 + " level :: " + (++c));
				cref = cref.right;
			}
//			System.out.println(" <----------- ----------> ");
			size++;
		}
		l.unlock();
		return add;
	}

	Object[] findLeaf(final Object obj, final int[] arr, final LinkedReference path) {
		Node n = root;
		if (path != null)
			path.add(n);
		for (int i = 0; i <= arr.length - 2; i++) {
			if (n == null)
				return null;
			else {
				final Object[] tnodes = n.nodes;
				if (tnodes == null)
					return null;
				else {
					n = (Node) tnodes[arr[i]];
					if (path != null && n != null)
						path.add(n);
				}
			}
		}
		if (n == null)
			return null;
		if (n.nodes == null)
			return null;
		else
			return (Object[]) n.nodes[arr[arr.length - 1]];
	}

	Object findObj(final Object obj, final int[] arr) {
		final Object[] ref = findLeaf(obj, arr, null);
		if (ref != null) {
			final Object search = searchArr(ref, obj);// ref.search(obj);
			// if (search != null)
			return search;
		}
		return null;
	}

	Object removeObj(final Object obj, final int[] arr) {
//		Object removeRef = null;
		final LinkedReference nodes = new LinkedReference(compareStrategy);
		Object[] ref = findLeaf(obj, arr, nodes);
		if (ref == null) {
			return null;
		}

		final Object search = removeArr(ref, obj);//ref.search(obj);
		if (search == null) {
			return null;
		} else {
			final Lock l = cas;
			l.lock();

//			if( getSize(ref) == 0 ){
//				int c = 0;
//				LinkedReference cref = nodes.right;
//				while (cref != null && cref.obj != null && cref != nodes) {
//					Node obj2 = (Node) cref.obj;
//					if(obj2.nodes[0] instanceof Node){
//						
//					}else{
//						
//						System.out.println("rem Node size :: "+getSize(obj2.nodes)+" childsize :: "+obj2.nodes[0]+"  obj2 :: "+obj2+" level :: "+(++c));
//					}
////					if (--obj2.cnt == 0)
////						resetNodes(obj2);
//
//					cref = cref.right;
//				}	
//				System.out.println(" <----------- ----------> ");
//			}
			
			LinkedReference cref = nodes.left;
			while (cref != null && cref.obj != null && cref != nodes) {
				Node obj2 = (Node) cref.obj;

				if (--obj2.cnt == 0)
					resetNodes(obj2);

				cref = cref.left;
			}
			
			size--;
			l.unlock();
		}

		return search;
	}

	static Collection<Object> getAll(final Node node) {
		Collection<Object> list = new DoublyLinkedList<Object>();
		if (node.nodes == null) {
			return list;
		} else {
			for (Object n : node.nodes) {
				if (n != null) {
					if (n instanceof Node)
						list.addAll(getAll((Node) n));
					else {
						Object[] arr = (Object[]) n;
						for (int i = 0; i < arr.length; i++) {
							if (arr[i] != null) {
								list.add(arr[i]);
							}
						}
						// while (ref != null) {
						// ref = Direction.left.get(ref);
						// if (ref == n) {
						// break;
						// }
						// list.add(ref.obj);
						// }

					}
				}
			}
		}
		return list;
	}
	// private static final String con(int[] a) {
	// StringBuffer b = new StringBuffer();
	//
	// for (int i : a) {
	// if (b.length() == 0)
	// b.append(i);
	// else
	// b.append(",").append(i);
	// }
	//
	// return b.toString();
	// }
	//
	// public static void main(String[] args) {
	//
	// //System.out.println(con(getPath(Integer.MIN_VALUE)));
	//
	//// List<Integer> list = new ArrayList<Integer>();
	//// list.add(0);
	//// list.add(1);
	//// list.add(4);
	//// list.add(8);
	//// list.add(9);
	////
	//// Collections.sort(list);
	////
	//// for (Integer i : list)
	//// System.out.print(i);
	//// //System.out.println();
	////
	//// //System.out.println("search 0 " + Collections.binarySearch(list, 0));
	//// //System.out.println("search 1 " + Collections.binarySearch(list, 1));
	//// //System.out.println("search 4 " + Collections.binarySearch(list, 4));
	//// int idx = Collections.binarySearch(list, 3);
	//// idx = -1 * (idx + 1);
	//// //System.out.println("search 3 " + idx);
	//
	//// Object[] a = new Object[7];
	////
	//// a[0] = 3;
	//// a[1] = 1;
	//// a[2] = 2;
	//// a[3] = 0;
	////
	//// for(Object o:a){
	//// if(o!=null)
	//// System.out.print(o.toString());
	//// }
	//// //System.out.println();
	//// Arrays.sort(a, defaultComparator);
	////
	//// for(Object o:a){
	//// if(o!=null)
	//// System.out.print(o.toString());
	//// }
	//// //System.out.println();
	// }
	//
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

	// volatile LinkedReference write = null;
	// volatile LinkedReference read = null;

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
		// if(isEmpty()) return null;

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
		// if(lock==null) return null;
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

	// /**
	// * @param includeNull TODO
	// * @param direction TODO
	// * @return
	// */
	// int size(final boolean includeNull, final Direction direction) {
	// int cnt = 0;
	// LinkedReference ref = direction.get(this);
	// while (ref != null) {
	// if (ref == this) {
	// return cnt;
	// }
	// if (includeNull) {
	// cnt++;
	// }else{
	// if (ref.obj != null) {
	// cnt++;
	// }
	// }
	// LinkedReference tref = ref;
	// ref = direction.get(ref);
	// if( tref == ref ) break;
	//// System.out.println(" size ref "+ref+" left "+ref.left+" this "+this+"
	// cnt "+cnt);
	// }
	// return cnt;
	// }
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