/**
 * 
 */
package org.arivu.datastructure;

import java.io.Serializable;
import java.util.Collection;
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
	static final class Ref  {
		final DoublyLinkedList<Object> linkedList = new DoublyLinkedList<Object>(CompareStrategy.EQUALS);
		final int hashCode;
		/**
		 * @param hashCode
		 */
		Ref(int hashCode) {
			super();
			this.hashCode = hashCode;
		}
		@Override
		public int hashCode() {
			return hashCode;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Ref other = (Ref) obj;
			if (hashCode != other.hashCode)
				return false;
			if (linkedList == null) {
				if (other.linkedList != null)
					return false;
			} else if (!linkedList.equals(other.linkedList))
				return false;
			return true;
		}
		
	}
	
	/**
	 * @author P
	 *
	 */
	static final class Node {
		final Comparator<Object> comparator;
		final int order;
		final Lock cas;
		
		final Ref[] refs;
		final Node[] nodes;
		
		volatile int idx = 0;
		final Counter counter;
		/**
		 * @param comparator
		 * @param order
		 * @param leaf TODO
		 * @param cnt TODO
		 */
		public Node(Comparator<Object> comparator, int order, Lock cas, boolean leaf, Counter cnt) {
			super();
			this.counter = cnt;
			this.comparator = comparator;
			this.order = order;
			this.cas = cas;
			if(leaf){
				this.refs = new Ref[order];
				this.nodes = null;
			}else{
				this.refs = null;
				this.nodes = new Node[order];
			}
		}
		
		void clear(){
			cas.lock();
			for(int i=0;i<this.nodes.length;i++)
				this.nodes[i] = null;
			
			counter.set(0);
			cas.lock();
		}

		void add(Object obj,int level,int[] arr) {
			cas.lock();
			if( level == arr.length-1 ){
				Ref ref = refs[arr[level]];
				if(ref == null){
					ref = new Ref(obj.hashCode());
					refs[arr[level]] = ref;
				}
				ref.linkedList.add(obj);
				this.counter.incrementAndGet();
			}else{
				Node n = nodes[arr[level]];
				if(n==null){
					n = new Node(comparator, order, cas, level == arr.length-2, this.counter);
					nodes[arr[level]] = n;
				}
				n.add(obj, level+1, arr);
			}
			cas.unlock();
		}

		Object find(Object obj,int level,int[] arr){
			if( level == arr.length-1 ){
				final Ref ref = refs[arr[level]];
				if(ref == null){
					return null;
				}
				final DoublyLinkedList<Object> search = ref.linkedList.search(obj);
				if( search == null )
					return null;
				else
					return search.obj;
			}else{
				final Node n = nodes[arr[level]];
				if(n==null){
					return null;
				}
				return n.find(obj, level+1, arr);
			}
		}

		Object remove(Object obj,int level,int[] arr){
			if( level == arr.length-1 ){
				final Ref ref = refs[arr[level]];
				if(ref == null){
					return null;
				}
				cas.lock();
				final DoublyLinkedList<Object> search = ref.linkedList.search(obj);
				if( search == null ){
					cas.unlock();	
					return null;
				}else{
					final Object removeRef = search.removeRef();
					this.counter.decrementAndGet();
					cas.unlock();
					return removeRef;
				}
			}else{
				final Node n = nodes[arr[level]];
				if(n==null){
					return null;
				}
				return n.remove(obj, level+1, arr);
			}
		}

		Collection<Object> getAll() {
			Collection<Object> list = new DoublyLinkedList<Object>();
			if(this.nodes==null){
				for( Ref n:this.refs ){
					if(n!=null)
						list.addAll(n.linkedList);
				}
				
			}else{
				for( Node n:this.nodes ){
					if(n!=null)
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
	 * @param comparator
	 */
	public Btree(int order, Comparator<Object> comparator) {
		this(order , comparator, new AtomicWFReentrantLock());
	}

	public Btree(int order) {
		this(order,defaultComparator);
	}
	
	public Btree() {
		this((int)baseValue+1,defaultComparator);
	}
	
	Btree(Lock lock) {
		this((int)baseValue+1,defaultComparator,lock);
	}
	
	Btree(int order, Comparator<Object> comparator, Lock lock) {
		super();
		this.root = new Node(comparator, order, lock, false, counter);
	}
	
	public void add(final Object obj){
		root.add(obj,0,getPath(obj));
	}
	
	public Object remove(final Object obj){
		return root.remove(obj,0,getPath(obj));
	}
	
	public Object get(final Object obj){
		return root.find(obj,0,getPath(obj));
	}

	public void clear(){
		root.clear();
	}
	
	public Collection<Object> getAll(){
		return root.getAll();
	}
	
	private static final int base = 8;
	private static final long baseValue = ((long) Math.pow(2, base)-1);
	private static final long MAX = (long)(Integer.MAX_VALUE)+1l;
	private static final int[] getPath(Object obj){
		int[] ret = new int[4]; 
		final long hashCode2 = (long)obj.hashCode();
		long hashCode = hashCode2+MAX;
		ret[3] = (int)(hashCode & baseValue);
		hashCode = hashCode >>> base;
		ret[2] = (int)(hashCode & baseValue);
		hashCode = hashCode >>> base;
		ret[1] = (int)(hashCode & baseValue);
		hashCode = hashCode >>> base;
		ret[0] = (int)(hashCode & baseValue);
//		System.out.println(" hashCode "+hashCode+" actual# "+hashCode2+" ret "+con(ret));
		return ret;
	}
	
	private static final String con(int[] a){
		StringBuffer b = new StringBuffer();
		
		for(int i:a)
			b.append(i).append(",");
		
		return b.toString();
	}
	
	public static void main(String[] args) {
		
		System.out.println(con(getPath(16)));
		
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

//		Object[] a = new Object[7];
//		
//		a[0] = 3;
//		a[1] = 1;
//		a[2] = 2;
//		a[3] = 0;
//
//		for(Object o:a){
//			if(o!=null)
//				System.out.print(o.toString());
//		}
//		System.out.println();
//		Arrays.sort(a, defaultComparator);
//		
//		for(Object o:a){
//			if(o!=null)
//				System.out.print(o.toString());
//		}
//		System.out.println();
	}
	//
}
