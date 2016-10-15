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
public final class DoublyLinkedSetInt {
//	static final AtomicLock cas = new AtomicLock();
//	static final AtomicWFLock cas = new AtomicWFLock();
	/**
	 * 
	 */
	int obj;
	
	/**
	 * 
	 */
	DoublyLinkedSetInt left = this, right = this;
	
	
//	volatile int size = 0;
	AtomicInteger size;
	Lock cas;
	/**
	 * 
	 */
	public DoublyLinkedSetInt() {
		this(new AtomicWFReentrantLock());
	}
	
	public DoublyLinkedSetInt(Lock cas) {
		this(Integer.MIN_VALUE,new AtomicInteger(0), cas);
	}
	/**
	 * @param size TODO
	 * @param cas TODO
	 * @param obj
	 */
	private DoublyLinkedSetInt(int t, AtomicInteger size, Lock cas) {
		super();
		this.obj = t;
		this.size = size;
		this.cas = cas;
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
		DoublyLinkedSetInt removeRight = removeRight();
		if(removeRight!=null)
			return removeRight.obj;
		return Integer.MIN_VALUE;
	}
	
	/**
	 * @return
	 */
	DoublyLinkedSetInt removeRight(){
		final DoublyLinkedSetInt r = this.right;
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
//	public boolean add(final int t){
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
	DoublyLinkedSetInt addLeft(final DoublyLinkedSetInt l) {
		if (l != null) {
			cas.lock();
			try {
				size.incrementAndGet();//++;
				l.right = this;
				DoublyLinkedSetInt tl = left;
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
	DoublyLinkedSetInt search(final int o){
		DoublyLinkedSetInt ref = this.right;
		while (ref != null) {
			if (ref == this) {
				break;
			}
			if(ref.obj==o){
				return ref;
			}
			ref = ref.right;
		}
		return null;
	}
	
	/**
	 * 
	 */
	
	public int remove() {
		DoublyLinkedSetInt removeRight = removeRight();
		if(removeRight!=null)
			return removeRight.obj;
		return Integer.MIN_VALUE;
	}

	private int removeRef(){
		DoublyLinkedSetInt tleft = left, tright = right;
		
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
		DoublyLinkedSetInt search = search(o);
		if( search == null )
			return false;
		else
			return true;
	}

	
//	public Iterator iterator() {
//		final DoublyLinkedSetInt ref = this.right;
//		return new Iterator() {
//			DoublyLinkedSetInt cursor = ref; 
//			
//			public boolean hasNext() {
//				return !(cursor == ref.left);
//			}
//
//			
//			public T next() {
//				T t = cursor.obj;
//				cursor = cursor.right; 
//				return t;
//			}
//		};
//	}

	
	public int[] toArray() {
//		List subl = new ArrayList();
		int idx = 0;
		int[] arr = new int[size.get()];
		DoublyLinkedSetInt ref = this.right;
		while (ref != null) {
			if (ref == this) {
				break;
			}
//			subl.add(ref.obj);
			arr[idx++] = ref.obj;
			ref = ref.right;
		}
//		return subl.toArray();
		return arr;
	}

//	@SuppressWarnings("hiding")
//	
//	public  T[] toArray(T[] a) {
//		List<Object> subl = new ArrayList<Object>();
//		DoublyLinkedSetInt<?> ref = this.right;
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
//		if(e!=null){
			DoublyLinkedSetInt search = search(e);
			if( search == null ){
				addLeft(new DoublyLinkedSetInt(e, size, cas));
				return true;
			}else{
				return false;
			}
//		}else{
//			return false;
//		}
	}

	
	public boolean remove(int o) {
		DoublyLinkedSetInt search = search(o);
//		System.out.println("remove "+this+" Object "+o+" search "+search.obj);
		if( search!=null ){
			search.removeRef();
			size.decrementAndGet();//--;
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

//	
//	public boolean addAll(int index, Collection<? extends T> c) {
//		if( c!=null && c.size()>0 ){
//			validateIndex(index);
//			DoublyLinkedSetInt linked = getLinked(index);
//			if( linked != null ){
//				for( T t:c )
//					add(t);
//			}
//			return true;
//		}else{
//			return false;
//		}
//	}

	
	public boolean removeAll(int[] c) {
		if( c!=null && c.length>0 ){
			boolean ret = true;
			for( int t:c )
				ret = ret && remove(t);
			return ret;
		}else{
			return false;
		}
	}

	
	public boolean retainAll(int[] c) {
		boolean ret = false;
		if( c!=null && c.length>0 ){
			DoublyLinkedSetInt ref = this.right;
			while (ref != null) {
				if (ref == this) {
					break;
				} 
				
				boolean ccon = false;
				for(int lc:c){
					if(ref.obj==lc){
						ccon = true;
						break;
					}
				}
				
				
				if( !ccon ){
					DoublyLinkedSetInt ll = ref;
					ref = ref.left;
					ll.removeRef();
					size.decrementAndGet();//--;
					ret = true;
				}
				ref = ref.right;
				
			}
		}
		return ret;
	}

//	
//	public T get(int index) {
//		validateIndex(index);
//		DoublyLinkedSetInt ref = getLinked(index);
//		if(ref!=null)
//			return ref.obj;
//		else
//			return null;
//	}

//	private DoublyLinkedSetInt getLinked(int index) {
//		int idx = 0;
//		DoublyLinkedSetInt ref = this;
//		while (ref != null) {
//			if(idx==index){
//				return ref;
//			}
//			ref = ref.right;
//			if (ref == this) {
//				break;
//			}
//		}
//		return null;
//	}

//	private void validateIndex(int index) {
//		if( index >= size || index < 0 ) throw new ArrayIndexOutOfBoundsException(index);
//	}

//	
//	public T set(int index, T element) {
//		validateIndex(index);
//		DoublyLinkedSetInt ref = getLinked(index);
//		if(ref!=null){
//			T obj2 = ref.obj;
//			ref.obj = element;
//			return obj2;
//		}else{
//			return null;
//		}
//	}

//	
//	public void add(int index, T element) {
//		validateIndex(index);
//		DoublyLinkedSetInt ref = getLinked(index);
//		if(ref!=null){
//			ref.add(element);
//		}
//	}
//
//	
//	public T remove(int index) {
//		DoublyLinkedSetInt ref = getLinked(index);
//		if(ref!=null){
//			T obj2 = ref.obj;
//			ref.remove();
//			return obj2;
//		}else{
//			return null;
//		}
//	}
//
//	
//	public int indexOf(Object o) {
//		int idx = 0;
//		DoublyLinkedSetInt ref = this;
//		while (ref != null) {
//			if(ref.obj==o){
//				return idx;
//			}
//			ref = ref.right;
//			idx++;
//			if(ref == null){
//				ref = this.right;
//			}else if (ref.obj == null) {
//				break;
//			}else if (ref == this) {
//				break;
//			}
//		}
//		return -1;
//	}
//
//	
//	public int lastIndexOf(Object o) {
//		DoublyLinkedSetInt ref = this.left;
//		int idx = size;
//		while (ref != null) {
//			ref = ref.left;
//			idx--;
//			if (ref.obj == o) {
//				break;
//			}
//		}
//		return idx;
//	}
//
//	
//	public ListIterator listIterator() {
//		final DoublyLinkedSetInt ref = this;
//		return new ListIterator() {
//			DoublyLinkedSetInt cursor = ref; 
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
//				DoublyLinkedSetInt tref = cursor.right;
//				cursor.remove();
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
//		final DoublyLinkedSetInt ref = getLinked(index);
//		return new ListIterator() {
//			DoublyLinkedSetInt cursor = ref; 
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
//				DoublyLinkedSetInt tref = cursor.right;
//				cursor.remove();
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
//		DoublyLinkedSetInt ref = getLinked(fromIndex);
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
		return this.right.obj;
	}
}
