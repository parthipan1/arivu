/**
 * 
 */
package org.arivu.datastructure;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.locks.Lock;

import org.arivu.utils.lock.AtomicWFReentrantLock;

/**
 * @author P
 *
 */
public class Btree implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6344951761380914875L;

	private static final Comparator<Object> defaultComparator = new Comparator<Object>() {
		
		@Override
		public int compare(Object o1, Object o2) {
			if( o1 != null && o2 != null ){
				return o1.hashCode() - o2.hashCode();
			}else if( o1 == null && o2 != null ){
				return -1*o2.hashCode();
			}else if( o1 != null && o2 == null ){
				return o1.hashCode();
			}
			return 0;
		}
	}; 
	
	/**
	 * @author P
	 *
	 */
	static final class Node {
		final Comparator<Object> comparator;
		final int order;
		final Lock cas;
		
//		final DoublyLinkedList<Object> arrs = new DoublyLinkedList<Object>(CompareStrategy.EQUALS);
		final Object[] arrs;
		final Node[] refs;
		
		volatile int idx = 0;
		
		/**
		 * @param comparator
		 * @param order
		 */
		public Node(Comparator<Object> comparator, int order, Lock cas) {
			super();
			this.comparator = comparator;
			this.order = order;
			this.cas = cas;
			this.arrs = new Object[order];
			this.refs = new Node[order + 1];
		}

		void add(Object obj) {
			if (idx < order-1) {
				int sid = Arrays.binarySearch(arrs, obj, comparator);
				if (sid < 0)
					sid = -1 * (sid + 1);

				idx++;
				for( int i=idx;i>sid;i-- )
					arrs[i] = arrs[i-1];
				
				arrs[idx] = obj;
				
//				arrs[idx++] = obj;
				//Arrays.sort(arrs, comparator);
			} else if (idx==arrs.length){
				// re arrange
			}else {
				int sid = Arrays.binarySearch(arrs, obj, comparator);
				if (sid < 0){
					sid = -1 * (sid + 1);
				}else{
					Node node = refs[sid];
					if(node==null){
						// re arrange
					}else{
						node.add(obj);
					}
				}
			}
		}

		Object find(final Object obj){
			int sid = Arrays.binarySearch(arrs, obj, comparator);
			if (sid < 0){
				sid = -1 * (sid + 1);
				Node node = refs[sid];
				if(node==null){
					return null;
				}else{
					return node.find(obj);
				}
			}else{
				return arrs[sid];
			}
		}

		void remove(Object obj) {
			int sid = Arrays.binarySearch(arrs, obj, comparator);
			if (sid < 0){
				sid = -1 * (sid + 1);
				Node node = refs[sid];
				if(node==null){
					return;
				}else{
					node.remove(obj);
				}
			}else{
				System.arraycopy(arrs,sid+1,arrs,sid,arrs.length-1-sid);
			}
		}
	}

	final Node root;
	
	/**
	 * @param order
	 * @param comparator
	 */
	public Btree(int order, Comparator<Object> comparator) {
		this(order , comparator, new AtomicWFReentrantLock());
	}

	public Btree(int order) {
		this(order,defaultComparator);
	}
	
	Btree(int order, Comparator<Object> comparator, Lock lock) {
		super();
		this.root = new Node(comparator, order, lock);
	}
	
	public void add(final Object obj){
		root.add(obj);
	}
	
	public void remove(final Object obj){
		root.remove(obj);
	}
	
	public Object get(final Object obj){
		return root.find(obj);
	}

	
	
	public static void main(String[] args) {
//		List<Integer> list = new ArrayList<Integer>();
//		list.add(0);
//		list.add(1);
//		list.add(4);
//		list.add(8);
//		list.add(9);
//
//		Collections.sort(list);
//
//		for (Integer i : list)
//			System.out.print(i);
//		System.out.println();
//
//		System.out.println("search 0 " + Collections.binarySearch(list, 0));
//		System.out.println("search 1 " + Collections.binarySearch(list, 1));
//		System.out.println("search 4 " + Collections.binarySearch(list, 4));
//		int idx = Collections.binarySearch(list, 3);
//		idx = -1 * (idx + 1);
//		System.out.println("search 3 " + idx);

		Object[] a = new Object[7];
		
		a[0] = 3;
		a[1] = 1;
		a[2] = 2;
		a[3] = 0;

		for(Object o:a){
			if(o!=null)
				System.out.print(o.toString());
		}
		System.out.println();
		Arrays.sort(a, defaultComparator);
		
		for(Object o:a){
			if(o!=null)
				System.out.print(o.toString());
		}
		System.out.println();
	}
	//
}
