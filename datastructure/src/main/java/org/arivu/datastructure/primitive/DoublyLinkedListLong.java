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
public final class DoublyLinkedListLong  {
//	static final AtomicLock cas = new AtomicLock();
//	static final AtomicWFLock cas = new AtomicWFLock();
	/**
	 * 
	 */
	long obj;
	
	/**
	 * 
	 */
	DoublyLinkedListLong left = this, right = this;
	
	
	AtomicInteger size;
	Lock cas;
	/**
	 * @param cas TODO
	 * 
	 */
	public DoublyLinkedListLong() {
		this(new AtomicWFReentrantLock());
	}
	
	DoublyLinkedListLong(Lock cas) {
		this(Long.MIN_VALUE,new AtomicInteger(0), cas);
	}
	
	/**
	 * @param size TODO
	 * @param cas TODO
	 * @param obj
	 */
	private DoublyLinkedListLong(long t, AtomicInteger size, Lock cas) {
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
	 * @return
	 */
	
	public long poll(){
		DoublyLinkedListLong removeRight = removeRight();
		if(removeRight!=null)
			return removeRight.obj;
		return Long.MIN_VALUE;
	}
	
	/**
	 * @return
	 */
	DoublyLinkedListLong removeRight(){
		final DoublyLinkedListLong r = this.right;
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
	DoublyLinkedListLong addLeft(final DoublyLinkedListLong l) {
		if (l != null) {
			cas.lock();
			try {
				size.incrementAndGet();//++;
				l.right = this;
				DoublyLinkedListLong tl = left;
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
	DoublyLinkedListLong search(final long o){
		DoublyLinkedListLong ref = this.right;
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
	
	
	public long remove() {
		return poll();
	}
	
	/**
	 * 
	 */
	private long removeRef() {
		DoublyLinkedListLong tleft = left, tright = right;

		if (tleft != null)
			tleft.right = tright;

		if (tright != null)
			tright.left = tleft;

		left = null;
		right = null;
		size = null;
		return obj;
	}

	
	public long size() {
		return size.get();
	}

	
	public boolean contains(long o) {
		DoublyLinkedListLong search = search(o);
		if( search == null )
			return false;
		else
			return true;
	}

	
//	public Iterator<Integer> iterator() {
//		final DoublyLinkedListLong ref = this.right;
//		return new Iterator<Integer>() {
//			DoublyLinkedListLong cursor = ref; 
//			
//			public boolean hasNext() {
//				return !(cursor == ref.left);
//			}
//
//			
//			public Integer next() {
//				long t = cursor.obj;
//				cursor = cursor.right; 
//				return t;
//			}
//		};
//	}

	
	public long[] toArray() {
//		List<Integer> subl = new ArrayList<Integer>();
		int idx = 0;
		long[] arr = new long[size.get()];
		DoublyLinkedListLong ref = this.right;
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

	
	public boolean add(long e) {
		if(e!=Long.MIN_VALUE){
			addLeft(new DoublyLinkedListLong(e, size, cas));
			return true;
		}else{
			return false;
		}
	}

	
	public boolean removeObj(long o) {
		DoublyLinkedListLong search = search(o);
		if( search!=null ){
			search.removeRef();
			size.decrementAndGet();
			return true;
		}else 
			return false;
	}

	
	public boolean containsAll(long[] c) {
		if( c!=null && c.length>0 ){
			boolean ret = true;
			for( long t:c )
				ret = ret && contains(t);
			return ret;
		}else{
			return false;
		}
	}

	
	public boolean addAll(long[] c) {
		if( c!=null && c.length>0 ){
			for( long t:c )
				add(t);

			return true;
		}else{
			return false;
		}
	}

	
	public boolean addAll(int index, long[] c) {
		if( c!=null && c.length>0 ){
			validateIndex(index);
			DoublyLinkedListLong linked = getLinked(index);
			if( linked != null ){
				for( long t:c )
					linked.add(t);
			}
			return true;
		}else{
			return false;
		}
	}

	
	public boolean removeAll(long[] c) {
		if( c!=null && c.length>0 ){
			boolean ret = true;
			for( long t:c )
				ret = ret && removeObj(t);
			return ret;
		}else{
			return false;
		}
	}

	
	public boolean retainAll(long[] c) {
		boolean ret = false;
		if( c!=null && c.length>0 ){
			DoublyLinkedListLong ref = this.right;
			while (ref != null) {
				if (ref == this) {
					break;
				}
				
				boolean ccon = false;
				for( long lc:c ){
					if(lc==ref.obj){
						ccon = true;
						break;
					}
				}
				
				if( !ccon ){
					DoublyLinkedListLong ll = ref;
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

	
	public long get(int index) {
		validateIndex(index);
		DoublyLinkedListLong ref = getLinked(index);
		if(ref!=null)
			return ref.obj;
		else
			return Long.MIN_VALUE;
	}

	private DoublyLinkedListLong getLinked(int index) {
		int idx = 0;
		DoublyLinkedListLong ref = this.right;
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

	  void validateIndex(int index) {
		if( index >= size.get() || index < 0 ) throw new ArrayIndexOutOfBoundsException(index);
	}

	
	public long set(int index, long element) {
		validateIndex(index);
		DoublyLinkedListLong ref = getLinked(index);
		if(ref!=null){
			long obj2 = ref.obj;
			ref.obj = element;
			return obj2;
		}else{
			return Long.MIN_VALUE;
		}
	}

	
	public void add(int index, long element) {
		validateIndex(index);
		DoublyLinkedListLong ref = getLinked(index);
		if(ref!=null){
			ref.add(element);
		}
	}

	
	public long remove(int index) {
		DoublyLinkedListLong ref = getLinked(index);
		if(ref!=null){
			long obj2 = ref.obj;
			ref.removeRef();
			size.decrementAndGet();
			return obj2;
		}else{
			return Long.MIN_VALUE;
		}
	}

	
	public int indexOf(long o) {
		int idx = 0;
		DoublyLinkedListLong ref = this.right;
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

	
	public int lastIndexOf(long o) {
		DoublyLinkedListLong ref = this.left;
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

	
	public boolean offer(long e) {
		return add(e);
	}

	
	public long element() {
		return obj;
	}

	
	public long peek() {
		return right.obj;
	}
}
