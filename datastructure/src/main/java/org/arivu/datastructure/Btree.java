/**
 * 
 */
package org.arivu.datastructure;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.locks.Lock;

import org.arivu.utils.lock.AtomicWFReentrantLock;
import org.arivu.utils.lock.NoLock;

/**
 * @author P
 *
 */
public class Btree implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6344951761380914875L;

	private static final Lock dummyLock = new NoLock();
	
	private static final long MAX_RANGE = (long) (Integer.MAX_VALUE) + 1l;
	
	private static final int DEFAULT_BASEPOWER = 2;
	/**
	 * @author P
	 *
	 */
	static final class Node {
		final int order;
		final Lock cas;
		final boolean leaf;
		final Object[] refs;
		final Node[] nodes;
		final CompareStrategy compareStrategy;
		volatile int idx = 0;
		final Counter counter = new Counter();

		/**
		 * @param order
		 * @param leaf
		 *            TODO
		 */
		public Node(int order, Lock cas, boolean leaf, CompareStrategy compareStrategy) {
			super();
			this.compareStrategy = compareStrategy;
			this.leaf = leaf;
			this.order = order;
			this.cas = cas;

			if (leaf) {
				this.refs = new Object[order];
				this.nodes = null;
			} else {
				this.refs = null;
				this.nodes = new Node[order];
			}
		}

		void clear() {
			cas.lock();
			for (int i = 0; i < this.nodes.length; i++)
				this.nodes[i] = null;

			counter.set(0);
			cas.lock();
		}

		@SuppressWarnings("unchecked")
		boolean add(final Object obj, final int level, final int[] arr) {
			boolean add = false;
			cas.lock();
			if (this.leaf) {
				Object ref = refs[arr[level]];
				if (ref == null) {
					ref = new DoublyLinkedList<Object>(compareStrategy, dummyLock);
					refs[arr[level]] = ref;
				}
				add = ((DoublyLinkedList<Object>) ref).add(obj);
			} else {
				Node n = nodes[arr[level]];
				if (n == null) {
					n = new Node(order, cas, level == arr.length - 2, compareStrategy);
					nodes[arr[level]] = n;
				}
				add = n.add(obj, level + 1, arr);
			}

			if (add)
				this.counter.incrementAndGet();

			cas.unlock();

			return add;
		}

		Object find(final Object obj, final int level, final int[] arr) {
			if (this.leaf) {
				@SuppressWarnings("unchecked")
				final DoublyLinkedList<Object> ref = (DoublyLinkedList<Object>) refs[arr[level]];
				if (ref == null) {
					return null;
				}
				final DoublyLinkedList<Object> search = ref.search(obj);
				// System.out.println(" find ref "+ref+" obj "+obj+" search
				// "+search);
				if (search == null)
					return null;
				else
					return search.obj;
			} else {
				final Node n = nodes[arr[level]];
				if (n == null) {
					return null;
				}
				return n.find(obj, level + 1, arr);
			}
		}

		Object remove(final Object obj, final int level, final int[] arr) {
			if (this.leaf) {
				@SuppressWarnings("unchecked")
				final DoublyLinkedList<Object> ref = (DoublyLinkedList<Object>) refs[arr[level]];
				if (ref == null) {
					return null;
				}
				cas.lock();
				final DoublyLinkedList<Object> search = ref.search(obj);
				if (search == null) {
					cas.unlock();
					return null;
				} else {
					final Object removeRef = search.removeRef();
					this.counter.decrementAndGet();
					if (ref.size() == 0) {
						refs[arr[level]] = null;
					}
					cas.unlock();
					return removeRef;
				}
			} else {
				final Node n = nodes[arr[level]];
				if (n == null) {
					return null;
				}
				cas.lock();
				Object remove = n.remove(obj, level + 1, arr);
				if (remove != null && this.counter.decrementAndGet() == 0) {
					nodes[arr[level]] = null;
				}
				cas.unlock();
				return remove;
			}
		}

		@SuppressWarnings("unchecked")
		Collection<Object> getAll() {
			Collection<Object> list = new DoublyLinkedList<Object>();
			if (this.nodes == null) {
				for (Object n : this.refs) {
					if (n != null)
						list.addAll((DoublyLinkedList<Object>) n);
				}

			} else {
				for (Node n : this.nodes) {
					if (n != null)
						list.addAll(n.getAll());
				}
			}
			return list;
		}
	}

	final Node root;
	private final int base;
	private final int depth;
	private final long baseValue;
	
	/**
	 * @param order
	 */
	public Btree(int basePower) {
		this(basePower, new AtomicWFReentrantLock(), CompareStrategy.EQUALS);
	}

	public Btree() {
		this(DEFAULT_BASEPOWER);
	}

	Btree(Lock lock) {
		this(DEFAULT_BASEPOWER, lock, CompareStrategy.EQUALS);
	}

	Btree(Lock lock, CompareStrategy compareStrategy) {
		this(DEFAULT_BASEPOWER, lock, compareStrategy);
	}

	Btree(int basePower, Lock lock, CompareStrategy compareStrategy) {
		super();
		if(basePower<=0||basePower>4){
			throw new IllegalArgumentException("Invalid basePower "+basePower+" (between 1-4) specified!");
		}
		this.base = (int) Math.pow(2, basePower);
		this.depth = 32/base;
		this.baseValue = ((long) Math.pow(2, base) - 1);
		this.root = new Node((int)this.baseValue+1, lock, false, compareStrategy);
	}

	private int[] getPath(Object obj) {
		int hashCode2 = obj.hashCode();
		return getPath(hashCode2);
	}

	private int[] getPath(final int hashCode2) {
		int[] ret = new int[depth];
		long hashCode = (long) hashCode2 + MAX_RANGE;

		for (int i = depth-1; i >= 0; i--) {
			ret[i] = (int) (hashCode & baseValue);

			if (i > 0)
				hashCode = hashCode >>> base;
		}
		// //System.out.println(" getPath act hashCode "+hashCode2+" ret
		// "+con(ret));
		return ret;
	}
	
	public int size() {
		return root.counter.get();
	}

	public void add(final Object obj) {
		int[] path = getPath(obj);
		root.add(obj, 0, path);
		// System.out.println(this+" bt add "+obj+" path "+con(path));
	}

	public Object remove(final Object obj) {
		// System.out.println(this+" bt remove "+obj);
		return root.remove(obj, 0, getPath(obj));
	}

	public Object get(final Object obj) {
		int[] path = getPath(obj);
		Object find = root.find(obj, 0, path);
		// System.out.println(this+" bt search "+obj+" find "+find+" path
		// "+con(path));
		return find;
	}

	public void clear() {
		root.clear();
	}

	public Collection<Object> getAll() {
		return root.getAll();
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
