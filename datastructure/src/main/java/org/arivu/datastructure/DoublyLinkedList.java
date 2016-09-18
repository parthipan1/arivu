/**
 * 
 */
package org.arivu.datastructure;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.concurrent.locks.Lock;

import org.arivu.utils.lock.AtomicWFReentrantLock;


/**
 * @author P
 *
 */
public final class DoublyLinkedList<T> implements List<T>,Queue<T> {
	/**
	 * 
	 */
	T obj;
	
	/**
	 * 
	 */
	DoublyLinkedList<T> left = this, right = this;
	
	Counter size;
	CompareStrategy compareStrategy;
	Lock cas;
	
	/**
	 * 
	 */
	public DoublyLinkedList(Collection<T> col) {
		this();
		addAll(col);
	}
	
	/**
	 * 
	 */
	public DoublyLinkedList() {
		this(CompareStrategy.REF);
	}
	
	DoublyLinkedList(CompareStrategy compareStrategy) {
		this(compareStrategy, new AtomicWFReentrantLock());
	}
	
	DoublyLinkedList(CompareStrategy compareStrategy,Lock lock) {
		this(null, new Counter(),compareStrategy, lock);
	}
	
	/**
	 * @param t
	 * @param size 
	 * @param cas 
	 */
	private DoublyLinkedList(T t, Counter size,CompareStrategy compareStrategy, Lock cas) {
		super();
		this.obj = t;
		this.size = size;
		this.compareStrategy = compareStrategy;
		this.cas = cas;
	}

	/**
	 * 
	 */
	/* (non-Javadoc)
	 * @see java.util.List#clear()
	 */
	@Override
	public void clear(){
		Lock l = this.cas;
		l.lock();
		left = this;
		right = this;
		size.set(0);
		l.unlock();
	}
	
	/* (non-Javadoc)
	 * @see java.util.List#isEmpty()
	 */
	@Override
	public boolean isEmpty(){
		return this == left;
	}
	
	/**
	 * @return T
	 */
	@Override
	public T poll(){
		DoublyLinkedList<T> removeRight = removeRight();
		if(removeRight!=null)
			return removeRight.obj;
		return null;
	}
	
	/**
	 * @return
	 */
	DoublyLinkedList<T> removeRight(){
		final DoublyLinkedList<T> r = this.right;
		if( r == this || r == null ){
			return null;
		}else{
			r.removeRef();
			return r;
		}
	}
	
	/**
	 * @param l
	 * @return
	 */
	DoublyLinkedList<T> addLeft(final DoublyLinkedList<T> l) {
		if (l != null) {
			Lock lo = this.cas;
			lo.lock();
			if(size!=null)
				size.incrementAndGet();
			l.right = this;
			DoublyLinkedList<T> tl = left;
			this.left = l;
			l.left = tl;
			tl.right = l;
			if(right==this){
				right = l;
			}
			lo.unlock();
		}
		return l;
	}
	
	/**
	 * @param obj
	 * @return
	 */
	DoublyLinkedList<T> search(final Object o){
		DoublyLinkedList<T> ref = this.right;
		while (ref != null) {
			if (ref == this) {
				break;
			}
			if( o instanceof String){
				if(o.equals(ref.obj)){
					return ref;
				}
			}else{
//				if(ref.obj==o){
				if(this.compareStrategy.compare(ref.obj, o) ){
					return ref;
				}
			}
			ref = ref.right;
		}
		return null;
	}
	
	@Override
	public T remove() {
		return poll();
	}
	
	/**
	 * 
	 */
	T removeRef() {
		Lock l = this.cas;
		l.lock();
		DoublyLinkedList<T> tleft = left, tright = right;
		
		if (tleft != null)
			tleft.right = tright;

		if (tright != null)
			tright.left = tleft;

		if (size!=null) 
			size.decrementAndGet();
		
		left = null;
		right = null;
		size = null;
		compareStrategy = null;
		l.unlock();
		return obj;
	}

	@Override
	public int size() {
		return size.get();
	}

	@Override
	public boolean contains(Object o) {
		DoublyLinkedList<T> search = search(o);
		if( search == null )
			return false;
		else
			return true;
	}

	@Override
	public Iterator<T> iterator() {
		final DoublyLinkedList<T> ref = this;
		return new Iterator<T>() {
			DoublyLinkedList<T> cursor = ref.right; 
			@Override
			public boolean hasNext() {
				return !(cursor == ref);
			}

			@Override
			public T next() {
				T t = cursor.obj;
				cursor = cursor.right; 
				return t;
			}
		};
	}

	@Override
	public Object[] toArray() {
		List<T> subl = new DoublyLinkedList<T>(this);
		Object[] arr = new Object[subl.size()];
		int i=0;
		for(T t:subl){
			arr[i++] = t;
		}
		return arr;
	}

	@SuppressWarnings({ "hiding", "unchecked", "rawtypes" })
	@Override
	public <T> T[] toArray(T[] a) {
		List<?> subl = new DoublyLinkedList(this);
		T[] arr = (T[])Array.newInstance(a.getClass(), subl.size());//new Object[subl.size()];
		int i=0;
		for(Object t:subl){
			arr[i++] = (T)t;
		}
		return arr;
	}

	@Override
	public boolean add(T e) {
		if(e!=null){
			addLeft(new DoublyLinkedList<T>(e, size, compareStrategy, cas));
			return true;
		}else{
			return false;
		}
	}

	@Override
	public boolean remove(Object o) {
		DoublyLinkedList<T> search = null;
		Lock l = this.cas;
		l.lock();
		search = search(o);
		if( search!=null ){
			search.removeRef();
			l.unlock();
			return true;
		}else{
			l.unlock();
			return false;
		} 
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if( c!=null && c.size()>0 ){
			boolean ret = true;
			
			Lock l = this.cas;
			l.lock();
			
			for( Object t:c )
				ret = ret & contains(t);
			
			l.unlock();
			return ret;
		}else{
			return false;
		}
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		if( c!=null && c.size()>0 ){
			boolean ret = true;
			
			Lock l = this.cas;
			l.lock();
			
			for( T t:c )
				ret = ret & add(t);

			l.unlock();
			return ret;
		}else{
			return false;
		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		if( c!=null && c.size()>0 ){
			validateIndex(index);
			boolean ret = true;
			
			Lock l = this.cas;
			l.lock();
			
			DoublyLinkedList<T> linked = getLinked(index);
			if( linked != null ){
				for( T t:c )
					ret = ret &linked.add(t);
			}
			
			l.unlock();
			return ret;
		}else{
			return false;
		}
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if( c!=null && c.size()>0 ){
			boolean ret = true;
			
			Lock l = this.cas;
			l.lock();
			
			for( Object t:c )
				ret = ret & remove(t);
			
			l.unlock();
			return ret;
		}else{
			return false;
		}
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean ret = false;
		if( c!=null && c.size()>0 ){
			Lock l = this.cas;
			l.lock();
			
			DoublyLinkedList<T> ref = this.right;
			while (ref != null) {
				if (ref == this) {
					break;
				}
				if( !c.contains(ref.obj) ){
					DoublyLinkedList<T> ll = ref;
					ref = ref.left;
					ll.removeRef();
					ret = true;
				}
				ref = ref.right;
			}
			
			l.unlock();
		}
		return ret;
	}

	@Override
	public T get(int index) {
		validateIndex(index);
		DoublyLinkedList<T> ref = getLinked(index);
		if(ref!=null)
			return ref.obj;
		else
			return null;
	}

	private DoublyLinkedList<T> getLinked(int index) {
		int idx = 0;
		DoublyLinkedList<T> ref = this.right;
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

	@Override
	public T set(int index, T element) {
		validateIndex(index);
		DoublyLinkedList<T> ref = getLinked(index);
		if(ref!=null){
			T obj2 = ref.obj;
			ref.obj = element;
			return obj2;
		}else{
			return null;
		}
	}

	@Override
	public void add(int index, T element) {
		validateIndex(index);
		Lock l = this.cas;
		l.lock();
		DoublyLinkedList<T> ref = getLinked(index);
		if(ref!=null){
			ref.add(element);
		}
		l.unlock();
	}

	@Override
	public T remove(int index) {
		DoublyLinkedList<T> ref = getLinked(index);
		if(ref!=null){
			T obj2 = ref.obj;
			ref.removeRef();
			return obj2;
		}else{
			return null;
		}
	}

	@Override
	public int indexOf(Object o) {
		int idx = 0;
		DoublyLinkedList<T> ref = this.right;
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

	@Override
	public int lastIndexOf(Object o) {
		DoublyLinkedList<T> ref = this.left;
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

	@Override
	public ListIterator<T> listIterator() {
		final DoublyLinkedList<T> ref = this.right;
		return new ListIterator<T>() {
			DoublyLinkedList<T> cursor = ref; 
			int idx = 0;
			@Override
			public boolean hasNext() {
				return !(cursor == ref.left);
			}

			@Override
			public T next() {
				T t = cursor.obj;
				cursor = cursor.right; 
				idx++;
				return t;
			}

			@Override
			public boolean hasPrevious() {
				return !(cursor == ref);
			}

			@Override
			public T previous() {
				T t = cursor.obj;
				cursor = cursor.left; 
				idx--;
				return t;
			}

			@Override
			public int nextIndex() {
				return idx+1;
			}

			@Override
			public int previousIndex() {
				return idx-1;
			}

			@Override
			public void remove() {
				DoublyLinkedList<T> tref = cursor.right;
				cursor.removeRef();
				cursor = tref;
			}

			@Override
			public void set(T e) {
				cursor.obj = e;
			}

			@Override
			public void add(T e) {
				cursor.add(e);
			}
		};
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		validateIndex(index);
		final DoublyLinkedList<T> ref = getLinked(index);
		return new ListIterator<T>() {
			DoublyLinkedList<T> cursor = ref; 
			int idx = 0;
			@Override
			public boolean hasNext() {
				return !(cursor == ref.left);
			}

			@Override
			public T next() {
				T t = cursor.obj;
				cursor = cursor.left; 
				idx++;
				return t;
			}

			@Override
			public boolean hasPrevious() {
				return !(cursor == ref.right);
			}

			@Override
			public T previous() {
				T t = cursor.obj;
				cursor = cursor.right; 
				idx--;
				return t;
			}

			@Override
			public int nextIndex() {
				return idx+1;
			}

			@Override
			public int previousIndex() {
				return idx-1;
			}

			@Override
			public void remove() {
				DoublyLinkedList<T> tref = cursor.right;
				cursor.removeRef();
				cursor = tref;
			}

			@Override
			public void set(T e) {
				cursor.obj = e;
			}

			@Override
			public void add(T e) {
				cursor.add(e);
			}
		};
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		validateIndex(toIndex);
		validateIndex(fromIndex);
		List<T> subl = new DoublyLinkedList<T>();
		DoublyLinkedList<T> ref = getLinked(fromIndex);
		int idx = fromIndex;
		while (ref != null) {
			subl.add(ref.obj);
			ref = ref.right;
			idx++;
			if (idx == toIndex) {
				break;
			}
		}
		return Collections.unmodifiableList(subl);
	}

	@Override
	public boolean offer(T e) {
		return add(e);
	}

	@Override
	public T element() {
		return obj;
	}

	@Override
	public T peek() {
		return right.obj;
	}
}
