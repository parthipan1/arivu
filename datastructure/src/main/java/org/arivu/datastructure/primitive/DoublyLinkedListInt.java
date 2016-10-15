/**
 * 
 */
package org.arivu.datastructure.primitive;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import org.arivu.utils.lock.AtomicWFReentrantLock;


/**
 * @author P
 *
 */
public final class DoublyLinkedListInt  {
//	static final AtomicLock cas = new AtomicLock();
//	static final AtomicWFLock cas = new AtomicWFLock();
	/**
	 * 
	 */
	int obj;
	
	/**
	 * 
	 */
	DoublyLinkedListInt left = this, right = this;
	
	
	AtomicInteger size;
	
	Lock cas;
	/**
	 * 
	 */
	public DoublyLinkedListInt() {
		this(new AtomicWFReentrantLock());
	}
	
	DoublyLinkedListInt(Lock cas) {
		this(Integer.MIN_VALUE,new AtomicInteger(0), cas);
	}
	/**
	 * @param size TODO
	 * @param cas TODO
	 * @param cas TODO
	 * @param obj
	 */
	private DoublyLinkedListInt(int t, AtomicInteger size, Lock cas) {
		super();
		this.obj = t;
		this.size = size;
	}

	/**
	 * 
	 */
	/* (non-Javadoc)
	 * @see java.util.List#clear()
	 */
	
	public void clear(){
		left = this;
		right = this;
		size.set(0);// = 0;
	}
	
	/* (non-Javadoc)
	 * @see java.util.List#isEmpty()
	 */
	
	public boolean isEmpty(){
		return this == left;
	}
	
	/**
	 * @return vInt
	 */
	public int poll(){
		DoublyLinkedListInt removeRight = removeRight();
		if(removeRight!=null)
			return removeRight.obj;
		return Integer.MIN_VALUE;
	}
	
	/**
	 * @return
	 */
	DoublyLinkedListInt removeRight(){
		final DoublyLinkedListInt r = this.right;
		if( r == this || r == null ){
			return null;
		}else{
			cas.lock();
			try {
				r.removeRef();
				size.decrementAndGet();//--;
				return r;
			} finally {
				cas.unlock();
			}
		}
	}
	
	/**
	 * @param random
	 * @return
	 */
//	DoublyLinkedListInt addRight(final DoublyLinkedListInt r) {
//		if (r != null) {
//			r.left = this;
//			DoublyLinkedListInt tr = right;
//			this.right = r;
//			r.right = tr;
//			tr.left = r;
//		}
//		return r;
//	}

//	
//	public boolean add(final T t){
//		if(t!=null){
//			addLeft(new DoublyLinkedListInt(t, cas));
//			return true;
//		}else{
//			return false;
//		}
////		return t;
//	}
	
	/**
	 * @param l
	 * @return
	 */
	DoublyLinkedListInt addLeft(final DoublyLinkedListInt l) {
		if (l != null) {
			cas.lock();
			try {
				size.incrementAndGet();//++;
				l.right = this;
				DoublyLinkedListInt tl = left;
				this.left = l;
				l.left = tl;
				tl.right = l;
			} finally {
				cas.unlock();
			}
		}
		return l;
	}
	
	/**
	 * @param obj
	 * @return
	 */
	DoublyLinkedListInt search(final int o){
		DoublyLinkedListInt ref = this.right;
		while (ref != null) {
			if (ref == this) {
				break;
			}
//			if( o instanceof String){
//				if(o.equals(ref.obj)){
//					return ref;
//				}
//			}else{
				if(ref.obj==o){
					return ref;
				}
//			}
			ref = ref.right;
		}
		return null;
	}
	
	
	public int remove() {
		return poll();
	}
	
	/**
	 * 
	 */
	private int removeRef() {
		DoublyLinkedListInt tleft = left, tright = right;

		if (tleft != null)
			tleft.right = tright;

		if (tright != null)
			tright.left = tleft;

		left = null;
		right = null;
		size = null;
		return obj;
	}

	
	public int size() {
		return size.get();
	}

	
	public boolean contains(int o) {
		DoublyLinkedListInt search = search(o);
		if( search == null )
			return false;
		else
			return true;
	}

//	
//	public Iterator<Integer> iterator() {
//		final DoublyLinkedListInt ref = this.right;
//		return new Iterator<Integer>() {
//			DoublyLinkedListInt cursor = ref; 
//			
//			public boolean hasNext() {
//				return !(cursor == ref.left);
//			}
//
//			
//			public Integer next() {
//				int t = cursor.obj;
//				cursor = cursor.right; 
//				return t;
//			}
//		};
//	}

	
	public int[] toArray() {
//		List<Integer> subl = new ArrayList<Integer>();
		int idx = 0;
		int[] arr = new int[size.get()];
		DoublyLinkedListInt ref = this.right;
		while (ref != null) {
			if (ref == this) {
				break;
			}
//			subl.add(ref.obj);
			arr[idx++] = ref.obj;
			ref = ref.right;
		}
		return arr;
	}

//	
//	public  T[] toArray(T[] a) {
//		List<Object> subl = new ArrayList<Object>();
//		DoublyLinkedListInt<?> ref = this.right;
//		while (ref != null) {
//			if (ref == this) {
//				break;
//			}
//			subl.add(ref.obj);
//			ref = ref.right;
//		}
//		return subl.toArray(a);
//	}

	
	public boolean add(int e) {
		if(e!=Integer.MIN_VALUE){
			addLeft(new DoublyLinkedListInt(e, size, cas));
			return true;
		}else{
			return false;
		}
	}

	
	public boolean removeObj(int o) {
		DoublyLinkedListInt search = search(o);
		if( search!=null ){
			search.removeRef();
			size.decrementAndGet();
			return true;
		}else 
			return false;
	}

	
	public boolean containsAll(int[] c) {
		if( c!=null && c.length>0 ){
			boolean ret = true;
			for( int t:c )
				ret = ret && contains(t);
			return ret;
		}else{
			return false;
		}
	}

	
	public boolean addAll(int[] c) {
		if( c!=null && c.length>0 ){
			for( int t:c )
				add(t);

			return true;
		}else{
			return false;
		}
	}

	
	public boolean addAll(int index, int[] c) {
		if( c!=null && c.length>0 ){
			validateIndex(index);
			DoublyLinkedListInt linked = getLinked(index);
			if( linked != null ){
				for( int t:c )
					linked.add(t);
			}
			return true;
		}else{
			return false;
		}
	}

	
	public boolean removeAll(int[] c) {
		if( c!=null && c.length>0 ){
			boolean ret = true;
			for( int t:c )
				ret = ret && removeObj(t);
			return ret;
		}else{
			return false;
		}
	}

	
	public boolean retainAll(int[] c) {
		boolean ret = false;
		if( c!=null && c.length>0 ){
			DoublyLinkedListInt ref = this.right;
			while (ref != null) {
				if (ref == this) {
					break;
				}
				
				boolean ccon = false;
				for( int lc:c ){
					if(lc==ref.obj){
						ccon = true;
						break;
					}
				}
				
				if( !ccon ){
					DoublyLinkedListInt ll = ref;
					ref = ref.left;
					ll.removeRef();
					size.decrementAndGet();
					ret = true;
				}
				ref = ref.right;
			}
		}
		return ret;
	}

	
	public int get(int index) {
		validateIndex(index);
		DoublyLinkedListInt ref = getLinked(index);
		if(ref!=null)
			return ref.obj;
		else
			return Integer.MIN_VALUE;
	}

	private DoublyLinkedListInt getLinked(int index) {
		int idx = 0;
		DoublyLinkedListInt ref = this.right;
		while (ref != null) {
			if (ref == this) {
				break;
			}
			if(idx==index){
				return ref;
			}
			ref = ref.right;
			idx++;
		}
		return null;
	}

	private void validateIndex(int index) {
		if( index >= size.get() || index < 0 ) throw new ArrayIndexOutOfBoundsException(index);
	}

	
	public int set(int index, int element) {
		validateIndex(index);
		DoublyLinkedListInt ref = getLinked(index);
		if(ref!=null){
			int obj2 = ref.obj;
			ref.obj = element;
			return obj2;
		}else{
			return Integer.MIN_VALUE;
		}
	}

	
	public void add(int index, int element) {
		validateIndex(index);
		DoublyLinkedListInt ref = getLinked(index);
		if(ref!=null){
			ref.add(element);
		}
	}

	
	public int remove(int index) {
		DoublyLinkedListInt ref = getLinked(index);
		if(ref!=null){
			int obj2 = ref.obj;
			ref.removeRef();
			size.decrementAndGet();
			return obj2;
		}else{
			return Integer.MIN_VALUE;
		}
	}

	
	public int indexOf(int o) {
		int idx = 0;
		DoublyLinkedListInt ref = this.right;
		while (ref != null) {
			if (ref == this) {
				break;
			}
			if(ref.obj==o){
				return idx;
			}
			ref = ref.right;
			idx++;
//			if(ref == null){
//				ref = this.right;
//			}else if (ref.obj == null) {
//				break;
//			}else 
		}
		return -1;
	}

	
	public int lastIndexOf(int o) {
		DoublyLinkedListInt ref = this.left;
		int idx = size.get()-1;
		while (ref != null) {
			if(ref==this)
				break;
			if (ref.obj == o) {
				return idx;
			}
			ref = ref.left;
			idx--;
		}
		return -1;
	}

//	
//	public ListIterator<Integer> listIterator() {
//		final DoublyLinkedListInt ref = this.right;
//		return new ListIterator() {
//			DoublyLinkedListInt cursor = ref; 
//			int idx = 0;
//			
//			public boolean hasNext() {
//				return !(cursor == ref.left);
//			}
//
//			
//			public T next() {
//				T t = cursor.obj;
//				cursor = cursor.right; 
//				idx++;
//				return t;
//			}
//
//			
//			public boolean hasPrevious() {
//				return !(cursor == ref);
//			}
//
//			
//			public T previous() {
//				T t = cursor.obj;
//				cursor = cursor.left; 
//				idx--;
//				return t;
//			}
//
//			
//			public int nextIndex() {
//				return idx+1;
//			}
//
//			
//			public int previousIndex() {
//				return idx-1;
//			}
//
//			
//			public void remove() {
//				DoublyLinkedListInt tref = cursor.right;
//				cursor.removeRef();
//				cursor = tref;
//			}
//
//			
//			public void set(T e) {
//				cursor.obj = e;
//			}
//
//			
//			public void add(T e) {
//				cursor.add(e);
//			}
//		};
//	}
//
//	
//	public ListIterator listIterator(int index) {
//		validateIndex(index);
//		final DoublyLinkedListInt ref = getLinked(index);
//		return new ListIterator() {
//			DoublyLinkedListInt cursor = ref; 
//			int idx = 0;
//			
//			public boolean hasNext() {
//				return !(cursor == ref.left);
//			}
//
//			
//			public T next() {
//				T t = cursor.obj;
//				cursor = cursor.left; 
//				idx++;
//				return t;
//			}
//
//			
//			public boolean hasPrevious() {
//				return !(cursor == ref.right);
//			}
//
//			
//			public T previous() {
//				T t = cursor.obj;
//				cursor = cursor.right; 
//				idx--;
//				return t;
//			}
//
//			
//			public int nextIndex() {
//				return idx+1;
//			}
//
//			
//			public int previousIndex() {
//				return idx-1;
//			}
//
//			
//			public void remove() {
//				DoublyLinkedListInt tref = cursor.right;
//				cursor.removeRef();
//				cursor = tref;
//			}
//
//			
//			public void set(T e) {
//				cursor.obj = e;
//			}
//
//			
//			public void add(T e) {
//				cursor.add(e);
//			}
//		};
//	}
//
//	
//	public List subList(int fromIndex, int toIndex) {
//		validateIndex(toIndex);
//		validateIndex(fromIndex);
//		List subl = new ArrayList();
//		DoublyLinkedListInt ref = getLinked(fromIndex);
//		int idx = fromIndex;
//		while (ref != null) {
//			subl.add(ref.obj);
//			ref = ref.right;
//			idx++;
//			if (idx == toIndex) {
//				break;
//			}
//		}
//		return Collections.unmodifiableList(subl);
//	}

	
	public boolean offer(int e) {
		return add(e);
	}

	
	public int element() {
		return obj;
	}

	
	public int peek() {
		return right.obj;
	}
}
