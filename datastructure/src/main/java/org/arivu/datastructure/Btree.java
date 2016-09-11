/**
 * 
 */
package org.arivu.datastructure;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author P
 *
 */
public class Btree implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6344951761380914875L;

	/**
	 * @author P
	 *
	 */
	static class Node {
		final Comparator<Object> comparator;
		final int blen;

		final DoublyLinkedList<Object> arrs = new DoublyLinkedList<Object>(CompareStrategy.EQUALS);
		final Node[] refs;
		
		/**
		 * @param comparator
		 * @param blen
		 */
		public Node(Comparator<Object> comparator, int blen) {
			super();
			this.comparator = comparator;
			this.blen = blen;
			this.refs = new Node[blen+1];
		}

		void add(Object obj){
			if(arrs.size()<blen){
				arrs.add(obj);
				Collections.sort(arrs,comparator);
			}else{
				int idx = Collections.binarySearch(arrs, obj, comparator);
				if( idx>=0 && idx<blen ){}
				else{
					idx = -1*(idx+1);
				}
				
			}
		}
		
	}

	final Node root;

	/**
	 * @param blen
	 * @param comparator
	 */
	public Btree(int blen, Comparator<Object> comparator) {
		super();
		this.root = new Node(comparator, blen);
	}

//	public static void main(String[] args){
//		List<Integer> list = new ArrayList<Integer>();
//		list.add(0);
//		list.add(1);
//		list.add(4);
//		list.add(8);
//		list.add(9);
//		
//		Collections.sort(list);
//		
//		for(Integer i:list)
//			System.out.print(i);
//		System.out.println();
//		
//		System.out.println("search 0 "+Collections.binarySearch(list, 0));
//		System.out.println("search 1 "+Collections.binarySearch(list, 1));
//		System.out.println("search 4 "+Collections.binarySearch(list, 4));
//		int idx = Collections.binarySearch(list, 3);
//		idx = -1*(idx+1);
//		System.out.println("search 3 "+idx);
//		
//	}
//	
}
