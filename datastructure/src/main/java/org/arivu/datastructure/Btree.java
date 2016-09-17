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
		final Counter counter;

		/**
		 * @param order
		 * @param leaf
		 *            TODO
		 * @param cnt
		 *            TODO
		 */
		public Node(int order, Lock cas, boolean leaf, Counter cnt, CompareStrategy compareStrategy) {
			super();
			this.compareStrategy = compareStrategy;
			this.leaf = leaf;
			this.counter = cnt;
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
		void add(final Object obj, final int level, final int[] arr) {
			cas.lock();
			if (this.leaf) {
				Object ref = refs[arr[level]];
				if (ref == null) {
					ref = new DoublyLinkedList<Object>(compareStrategy, dummyLock);
					refs[arr[level]] = ref;
				}
//				boolean add = 
				((DoublyLinkedList<Object>) ref).add(obj);
				//System.out.println(" add ref "+ref+" obj "+obj+" addflag "+add+" compareStrategy "+compareStrategy);
				this.counter.incrementAndGet();
			} else {
				Node n = nodes[arr[level]];
				if (n == null) {
					n = new Node(order, cas, level == arr.length - 2, this.counter, compareStrategy);
					nodes[arr[level]] = n;
				}
				n.add(obj, level + 1, arr);
			}
			cas.unlock();
		}

		Object find(final Object obj, final int level, final int[] arr) {
			if (this.leaf) {
				@SuppressWarnings("unchecked")
				final DoublyLinkedList<Object> ref = (DoublyLinkedList<Object>) refs[arr[level]];
				if (ref == null) {
					return null;
				}
				final DoublyLinkedList<Object> search = ref.search(obj);
				//System.out.println(" find ref "+ref+" obj "+obj+" search "+search);
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
					if(ref.size()==0){
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
				return n.remove(obj, level + 1, arr);
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
	final Counter counter = new Counter();

	/**
	 * @param order
	 */
	public Btree(int order) {
		this(order, new AtomicWFReentrantLock(), CompareStrategy.EQUALS);
	}

	public Btree() {
		this((int) baseValue + 1);
	}

	Btree(Lock lock) {
		this((int) baseValue + 1, lock, CompareStrategy.EQUALS);
	}
	
	Btree(Lock lock, CompareStrategy compareStrategy) {
		this((int) baseValue + 1, lock, compareStrategy);
	}
	
	Btree(int order, Lock lock, CompareStrategy compareStrategy) {
		super();
		this.root = new Node(order, lock, false, counter, compareStrategy);
	}

	public void add(final Object obj) {
		int[] path = getPath(obj);
		root.add(obj, 0, path);
		//System.out.println(this+" bt add "+obj+" path "+con(path));
	}

	public Object remove(final Object obj) {
		//System.out.println(this+" bt remove "+obj);
		return root.remove(obj, 0, getPath(obj));
	}

	public Object get(final Object obj) {
		int[] path = getPath(obj);
		Object find = root.find(obj, 0, path);
		//System.out.println(this+" bt search "+obj+" find "+find+" path "+con(path));
		return find;
	}

	public void clear() {
		root.clear();
	}

	public Collection<Object> getAll() {
		return root.getAll();
	}

	private static final int basePower = 3;
	private static final int base = (int) Math.pow(2, basePower);
	private static final long baseValue = ((long) Math.pow(2, base) - 1);
	private static final long MAX_RANGE = (long) (Integer.MAX_VALUE) + 1l;

	private static final int[] getPath(Object obj) {
		int hashCode2 = obj.hashCode();
		return getPath(hashCode2);
	}

	private static final int[] getPath(final int hashCode2) {
		int[] ret = new int[basePower + 1];
		long hashCode = (long) hashCode2 + MAX_RANGE;

		for (int i = basePower; i >= 0; i--) {
			ret[i] = (int) (hashCode & baseValue);
			
			if( i>0 )
				hashCode = hashCode >>> base;
		}
//		//System.out.println(" getPath act hashCode "+hashCode2+" ret "+con(ret));
		return ret;
	}

//	private static final String con(int[] a) {
//		StringBuffer b = new StringBuffer();
//
//		for (int i : a) {
//			if (b.length() == 0)
//				b.append(i);
//			else
//				b.append(",").append(i);
//		}
//
//		return b.toString();
//	}
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
