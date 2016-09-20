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
public final  class DoublyLinkedStackInt {
//	static final AtomicLock cas = new AtomicLock();
//	static final AtomicWFLock cas = new AtomicWFLock();
	/**
	 * 
	 */
	int obj;
	
	/**
	 * 
	 */
	DoublyLinkedStackInt left = this, right = this;
	
	DoublyLinkedStackInt top = this;
	
	AtomicInteger size;
	Lock cas;
	/**
	 * @param cas TODO
	 * 
	 */
	public DoublyLinkedStackInt() {
		this(new AtomicWFReentrantLock());
	}
	
	DoublyLinkedStackInt(Lock cas) {
		this(Integer.MIN_VALUE,new AtomicInteger(0), cas);
	}
	/**
	 * @param size TODO
	 * @param cas TODO
	 * @param obj
	 */
	private DoublyLinkedStackInt(int t, AtomicInteger size, Lock cas) {
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
	//@Override
	public void clear(){
		cas.lock();
		try {
			left = this;
			right = this;
			size.set(0);// = 0;
		} finally {
			cas.unlock();
		}
	}
	
	public int search(final int o){
		int idx = 0;
		DoublyLinkedStackInt ref = this.right;
		while (ref != null) {
			if (ref == this) {
				break;
			}
			if(ref.obj==o){
				return idx;
			}
			ref = ref.right;
			idx++;
		}
		return -1;
	}
	
	/* (non-Javadoc)
	 * @see java.util.List#isEmpty()
	 */
	//@Override
	public boolean empty(){
		return this == left;
	}
	
	public int pop(){
		return poll();
	}
	
	/**
	 * @return
	 */
	//@Override
	public int poll(){
		if(empty()){
			return Integer.MIN_VALUE;
		}else{
			DoublyLinkedStackInt ref = top;
			top = ref.left;
			ref.remove();
			return ref.obj;
		}
	}

	//@Override
	public int push(int e) {
		DoublyLinkedStackInt ref = top;
		DoublyLinkedStackInt ref1 = new DoublyLinkedStackInt(e, size, cas);
		top.addRight(ref1);
		top = ref1;
		return ref.obj;
	}

	//@Override
	public int element() {
		return obj;
	}

	//@Override
	public int peek() {
		if( empty() )
			return Integer.MIN_VALUE;
		else
			return top.obj;
	}
	
	/**
	 * @return
	 */
	DoublyLinkedStackInt removeRight(){
		final DoublyLinkedStackInt r = this.right;
		if( r == this || r == null ){
			return null;
		}else{
			cas.lock();
			try {
				r.remove();
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
	DoublyLinkedStackInt addRight(final DoublyLinkedStackInt r) {
		if (r != null) {
			cas.lock();
			try {
				size.incrementAndGet();//++;
				r.left = this;
				DoublyLinkedStackInt tr = right;
				this.right = r;
				r.right = tr;
				tr.left = r;
			} finally {
				cas.unlock();
			}
		}
		return r;
	}

//	//@Override
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
	DoublyLinkedStackInt addLeft(final DoublyLinkedStackInt l) {
		if (l != null) {
			cas.lock();
			try {
				size.incrementAndGet();//+;
				l.right = this;
				DoublyLinkedStackInt tl = left;
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
	DoublyLinkedStackInt searchObj(final int o){
		DoublyLinkedStackInt ref = this.right;
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
	//@Override
	 int remove() {
		 cas.lock();
		try {
			DoublyLinkedStackInt tleft = left, tright = right;
			if (tleft != null)
				tleft.right = tright;
			if (tright != null)
				tright.left = tleft;
			size.decrementAndGet();//--;
			left = null;
			right = null;
			size = null;
		} finally {
			cas.unlock();
		}
		return obj;
	}

	//@Override
	public int size() {
		return size.get();
	}

	//@Override
	boolean contains(int o) {
		DoublyLinkedStackInt search = searchObj(o);
		if( search == null )
			return false;
		else
			return true;
	}

//	//@Override
//	public Iterator iterator() {
//		final DoublyLinkedStackInt ref = this.right;
//		return new Iterator() {
//			DoublyLinkedStackInt cursor = ref; 
//			//@Override
//			public boolean hasNext() {
//				return !(cursor == ref.left);
//			}
//
//			//@Override
//			public int next() {
//				int t = cursor.obj;
//				cursor = cursor.right; 
//				return t;
//			}
//		};
//	}

	//@Override
	public int[] toArray() {
//		List subl = new ArrayList();
		int idx = 0;
		int[] arr = new int[size.get()];
		DoublyLinkedStackInt ref = this.right;
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

//	@SuppressWarnings("hiding")
//	//@Override
//	public  T[] toArray(T[] a) {
//		List<Object> subl = new ArrayList<Object>();
//		DoublyLinkedStackInt<?> ref = this.right;
//		while (ref != null) {
//			if (ref == this) {
//				break;
//			}
//			subl.add(ref.obj);
//			ref = ref.right;
//		}
//		return subl.toArray(a);
//	}

	//@Override
//	public boolean add(T e) {
//		if(e!=null){
//			addLeft(new DoublyLinkedStackInt(e, cas));
//			return true;
//		}else{
//			return false;
//		}
//	}
//
//	//@Override
//	public boolean remove(Object o) {
//		DoublyLinkedStackInt search = search(obj);
//		if( search!=null ){
//			search.remove();
//			return true;
//		}else 
//			return false;
//	}
//
//	//@Override
//	public boolean containsAll(Collection<?> c) {
//		if( c!=null && c.size()>0 ){
//			boolean ret = true;
//			for( Object t:c )
//				ret = ret && contains(t);
//			return ret;
//		}else{
//			return false;
//		}
//	}
//
//	//@Override
//	public boolean addAll(Collection<? extends T> c) {
//		if( c!=null && c.size()>0 ){
//			for( T t:c )
//				add(t);
//
//			return true;
//		}else{
//			return false;
//		}
//	}
//
//	//@Override
//	public boolean addAll(int index, Collection<? extends T> c) {
//		if( c!=null && c.size()>0 ){
//			validateIndex(index);
//			DoublyLinkedStackInt linked = getLinked(index);
//			if( linked != null ){
//				for( T t:c )
//					add(t);
//			}
//			return true;
//		}else{
//			return false;
//		}
//	}
//
//	//@Override
//	public boolean removeAll(Collection<?> c) {
//		if( c!=null && c.size()>0 ){
//			boolean ret = true;
//			for( Object t:c )
//				ret = ret && remove(t);
//			return ret;
//		}else{
//			return false;
//		}
//	}
//
//	//@Override
//	public boolean retainAll(Collection<?> c) {
//		boolean ret = false;
//		if( c!=null && c.size()>0 ){
//			DoublyLinkedStackInt ref = this;
//			while (ref != null) {
//				if( !c.contains(ref.obj) ){
//					DoublyLinkedStackInt ll = ref;
//					ref = ref.left;
//					ll.remove();
//					ret = true;
//				}
//				ref = ref.right;
//				if (ref == this) {
//					break;
//				}
//			}
//		}
//		return ret;
//	}

	//@Override
	public int get(int index) {
		validateIndex(index);
		DoublyLinkedStackInt ref = getLinked(index);
		if(ref!=null)
			return ref.obj;
		else
			return Integer.MIN_VALUE;
	}

	private DoublyLinkedStackInt getLinked(int index) {
		int idx = 0;
		DoublyLinkedStackInt ref = this.right;
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

	//@Override
//	public int set(int index, int element) {
//		validateIndex(index);
//		DoublyLinkedStackInt ref = getLinked(index);
//		if(ref!=null){
//			int obj2 = ref.obj;
//			ref.obj = element;
//			return obj2;
//		}else{
//			return null;
//		}
//	}
//
//	//@Override
//	public void add(int index, int element) {
//		validateIndex(index);
//		DoublyLinkedStackInt ref = getLinked(index);
//		if(ref!=null){
//			ref.add(element);
//		}
//	}
//
//	//@Override
//	public int remove(int index) {
//		DoublyLinkedStackInt ref = getLinked(index);
//		if(ref!=null){
//			int obj2 = ref.obj;
//			ref.remove();
//			return obj2;
//		}else{
//			return null;
//		}
//	}

	//@Override
	public int indexOf(final int o) {
		int idx = 0;
		DoublyLinkedStackInt ref = this.right;
		while (ref != null) {
			if (ref == this) {
				break;
			}
			if(ref.obj==o){
				return idx;
			}
			ref = ref.right;
			idx++;
		}
		return -1;
	}

	//@Override
	public int lastIndexOf(int o) {
		DoublyLinkedStackInt ref = this.left;
		int idx = size.get()-1;
		while (ref != null) {
			if (ref == this) {
				break;
			}
			if(ref.obj==o){
				break;
			}
			ref = ref.left;
			idx--;
		}
		return idx;
	}
//
//	@SuppressWarnings("unchecked")
//	public void addAll(Collection list) {
//		Object[] array = list.toArray();
//		for(Object obj:array){
//			push((T)obj);
//		}
//	}

	//@Override
//	public ListIterator listIterator() {
//		final DoublyLinkedStackInt ref = this;
//		return new ListIterator() {
//			DoublyLinkedStackInt cursor = ref; 
//			int idx = 0;
//			//@Override
//			public boolean hasNext() {
//				return !(cursor == ref.left);
//			}
//
//			//@Override
//			public T next() {
//				T t = cursor.obj;
//				cursor = cursor.left; 
//				idx++;
//				return t;
//			}
//
//			//@Override
//			public boolean hasPrevious() {
//				return !(cursor == ref.right);
//			}
//
//			//@Override
//			public T previous() {
//				T t = cursor.obj;
//				cursor = cursor.right; 
//				idx--;
//				return t;
//			}
//
//			//@Override
//			public int nextIndex() {
//				return idx+1;
//			}
//
//			//@Override
//			public int previousIndex() {
//				return idx-1;
//			}
//
//			//@Override
//			public void remove() {
//				DoublyLinkedStackInt tref = cursor.right;
//				cursor.remove();
//				cursor = tref;
//			}
//
//			//@Override
//			public void set(T e) {
//				cursor.obj = e;
//			}
//
//			//@Override
//			public void add(T e) {
//				cursor.add(e);
//			}
//		};
//	}
//
//	//@Override
//	public ListIterator listIterator(int index) {
//		validateIndex(index);
//		final DoublyLinkedStackInt ref = getLinked(index);
//		return new ListIterator() {
//			DoublyLinkedStackInt cursor = ref; 
//			int idx = 0;
//			//@Override
//			public boolean hasNext() {
//				return !(cursor == ref.left);
//			}
//
//			//@Override
//			public T next() {
//				T t = cursor.obj;
//				cursor = cursor.left; 
//				idx++;
//				return t;
//			}
//
//			//@Override
//			public boolean hasPrevious() {
//				return !(cursor == ref.right);
//			}
//
//			//@Override
//			public T previous() {
//				T t = cursor.obj;
//				cursor = cursor.right; 
//				idx--;
//				return t;
//			}
//
//			//@Override
//			public int nextIndex() {
//				return idx+1;
//			}
//
//			//@Override
//			public int previousIndex() {
//				return idx-1;
//			}
//
//			//@Override
//			public void remove() {
//				DoublyLinkedStackInt tref = cursor.right;
//				cursor.remove();
//				cursor = tref;
//			}
//
//			//@Override
//			public void set(T e) {
//				cursor.obj = e;
//			}
//
//			//@Override
//			public void add(T e) {
//				cursor.add(e);
//			}
//		};
//	}
//
//	//@Override
//	public List subList(int fromIndex, int toIndex) {
//		validateIndex(toIndex);
//		validateIndex(fromIndex);
//		List subl = new ArrayList();
//		DoublyLinkedStackInt ref = getLinked(fromIndex);
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

}
