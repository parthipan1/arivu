/**
 * 
 */
package org.arivu.datastructure.primitive;

import java.util.concurrent.atomic.AtomicInteger;

import org.arivu.utils.lock.AtomicWFLock;


/**
 * @author P
 *
 */
public final class DoublyLinkedSetFloat {
//	static final AtomicLock cas = new AtomicLock();
	static final AtomicWFLock cas = new AtomicWFLock();
	/**
	 * 
	 */
	float obj;
	
	/**
	 * 
	 */
	DoublyLinkedSetFloat left = this, right = this;
	
	
//	volatile int size = 0;
	AtomicInteger size;
	
	/**
	 * @param cas TODO
	 * 
	 */
	public DoublyLinkedSetFloat() {
		this(Float.MIN_VALUE,new AtomicInteger(0));
	}
	
	/**
	 * @param size TODO
	 * @param obj
	 */
	private DoublyLinkedSetFloat(float t, AtomicInteger size) {
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
	 * @return
	 */
	
	public float poll(){
		DoublyLinkedSetFloat removeRight = removeRight();
		if(removeRight!=null)
			return removeRight.obj;
		return Float.MIN_VALUE;
	}
	
	/**
	 * @return
	 */
	DoublyLinkedSetFloat removeRight(){
		final DoublyLinkedSetFloat r = this.right;
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
//	public boolean add(final float t){
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
	DoublyLinkedSetFloat addLeft(final DoublyLinkedSetFloat l) {
		if (l != null) {
			cas.lock();
			try {
				size.incrementAndGet();//++;
				l.right = this;
				DoublyLinkedSetFloat tl = left;
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
	DoublyLinkedSetFloat search(final float o){
		DoublyLinkedSetFloat ref = this.right;
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
	
	public float remove() {
		DoublyLinkedSetFloat removeRight = removeRight();
		if(removeRight!=null)
			return removeRight.obj;
		return Float.MIN_VALUE;
	}

	private final float removeRef(){
		DoublyLinkedSetFloat tleft = left, tright = right;
		
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

	
	public boolean contains(float o) {
		DoublyLinkedSetFloat search = search(o);
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

	
	public float[] toArray() {
//		List subl = new ArrayList();
		int idx = 0;
		float[] arr = new float[size.get()];
		DoublyLinkedSetFloat ref = this.right;
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

	
	public boolean add(float e) {
//		if(e!=null){
			DoublyLinkedSetFloat search = search(e);
			if( search == null ){
				addLeft(new DoublyLinkedSetFloat(e, size));
				return true;
			}else{
				return false;
			}
//		}else{
//			return false;
//		}
	}

	
	public boolean remove(float o) {
		DoublyLinkedSetFloat search = search(o);
//		System.out.println("remove "+this+" Object "+o+" search "+search.obj);
		if( search!=null ){
			search.removeRef();
			size.decrementAndGet();//--;
			return true;
		}else 
			return false;
	}

	
	public boolean containsAll(float[] c) {
		if( c!=null && c.length>0 ){
			boolean ret = true;
			for( float t:c )
				ret = ret && contains(t);
			return ret;
		}else{
			return false;
		}
	}

	
	public boolean addAll(float[] c) {
		if( c!=null && c.length>0 ){
			for( float t:c )
				add(t);

			return true;
		}else{
			return false;
		}
	}

//	
//	public boolean addAll(float index, Collection<? extends T> c) {
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

	
	public boolean removeAll(float[] c) {
		if( c!=null && c.length>0 ){
			boolean ret = true;
			for( float t:c )
				ret = ret && remove(t);
			return ret;
		}else{
			return false;
		}
	}

	
	public boolean retainAll(float[] c) {
		boolean ret = false;
		if( c!=null && c.length>0 ){
			DoublyLinkedSetFloat ref = this.right;
			while (ref != null) {
				if (ref == this) {
					break;
				} 
				
				boolean ccon = false;
				for(float lc:c){
					if(ref.obj==lc){
						ccon = true;
						break;
					}
				}
				
				
				if( !ccon ){
					DoublyLinkedSetFloat ll = ref;
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
//	public T get(float index) {
//		validateIndex(index);
//		DoublyLinkedSetInt ref = getLinked(index);
//		if(ref!=null)
//			return ref.obj;
//		else
//			return null;
//	}

//	private final DoublyLinkedSetInt getLinked(float index) {
//		float idx = 0;
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

//	private final void validateIndex(float index) {
//		if( index >= size || index < 0 ) throw new ArrayIndexOutOfBoundsException(index);
//	}

//	
//	public T set(float index, T element) {
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

	
	public boolean offer(float e) {
		return add(e);
	}

	
	public float element() {
		return obj;
	}

	
	public float peek() {
		return this.right.obj;
	}
}
