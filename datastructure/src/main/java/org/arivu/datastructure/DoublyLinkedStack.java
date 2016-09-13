/**
 * 
 */
package org.arivu.datastructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import org.arivu.utils.lock.AtomicWFLock;


/**
 * @author P
 *
 */
public final class DoublyLinkedStack<T> implements Iterable<T> , Queue<T>{
	/**
	 * 
	 */
	T obj;
	
	/**
	 * 
	 */
	DoublyLinkedStack<T> left = this, right = this;
	
	DoublyLinkedStack<T> top = this;
	
	AtomicInteger size;
	Lock cas;
	final CompareStrategy compareStrategy;
	final boolean set;
	/**
	 * 
	 */
	public DoublyLinkedStack() {
		this(null,new AtomicInteger(0), false, CompareStrategy.REF, new AtomicWFLock());
	}
	
	DoublyLinkedStack(boolean set,CompareStrategy compareStrategy) {
		this(null,new AtomicInteger(0), set, compareStrategy, new AtomicWFLock());
	}
	/**
	 * @param size TODO
	 * @param set TODO
	 * @param compareStrategy TODO
	 * @param cas TODO
	 * @param obj
	 */
	private DoublyLinkedStack(T t, AtomicInteger size, boolean set, CompareStrategy compareStrategy, Lock cas) {
		super();
		this.obj = t;
		this.size = size;
		this.set = set;
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
		cas.lock();
		left = this;
		right = this;
		size.set(0);
		cas.unlock();
	}
	
	public int search(final Object o){
		int idx = 0;
		DoublyLinkedStack<T> ref = this.right;
		while (ref != null) {
			if (ref == this) {
				break;
			}
			if(o instanceof String){
				if(CompareStrategy.EQUALS.compare(ref.obj, o)){
					return idx;
				}
			}else{
				if(compareStrategy.compare(ref.obj, o)){
					return idx;
				}
			}
			ref = ref.right;
			idx++;
		}
		return -1;
	}
	
//	@Override
	public T pop(){
		return poll();
	}
	
	/**
	 * @return T
	 */
	@Override
	public T poll(){
		if(isEmpty()){
			return null;
		}else{
			DoublyLinkedStack<T> ref = top;
			top = ref.left;
			
			if(ref!=this)
				ref.remove();
			
			return ref.obj;
		}
	}

//	@Override
	public T push(T e) {
		if(set && contains(e) ){
			return e;
		}
		DoublyLinkedStack<T> ref = top;
		DoublyLinkedStack<T> ref1 = new DoublyLinkedStack<T>(e, size, false, compareStrategy, cas);
		top.addRight(ref1);
		top = ref1;
		return ref.obj;
	}

	@Override
	public T element() {
		return obj;
	}

	@Override
	public T peek() {
		if( isEmpty() )
			return null;
		else
			return top.obj;
	}
	
	/**
	 * @return
	 */
	DoublyLinkedStack<T> removeRight(){
		final DoublyLinkedStack<T> r = this.right;
		if( r == this || r == null ){
			return null;
		}else{
			cas.lock();
			r.remove();
			size.decrementAndGet();
			cas.unlock();
			return r;
		}
	}
	
	/**
	 * @param random
	 * @return
	 */
	DoublyLinkedStack<T> addRight(final DoublyLinkedStack<T> r) {
		if (r != null) {
			cas.lock();
			size.incrementAndGet();
			r.left = this;
			DoublyLinkedStack<T> tr = right;
			this.right = r;
			r.right = tr;
			tr.left = r;
			cas.unlock();
		}
		return r;
	}
	
	/**
	 * @param l
	 * @return
	 */
	DoublyLinkedStack<T> addLeft(final DoublyLinkedStack<T> l) {
		if (l != null) {
			cas.lock();
			size.incrementAndGet();
			l.right = this;
			DoublyLinkedStack<T> tl = left;
			this.left = l;
			l.left = tl;
			tl.right = l;
			cas.unlock();
		}
		return l;
	}
	
//	/**
//	 * @param obj
//	 * @return
//	 */
//	DoublyLinkedStack<T> searchObj(final Object o){
//		DoublyLinkedStack<T> ref = this.right;
//		while (ref != null) {
//			if (ref == this) {
//				break;
//			}
//			if(o instanceof String){
//				if(o.equals(ref.obj)){
//					return ref;
//				}
//			}else{
//				if(ref.obj==o){
//					return ref;
//				}
//			}
//			ref = ref.right;
//		}
//		return null;
//	}
	
	/**
	 * 
	 */
	@Override
	public T remove() {
		cas.lock();
		DoublyLinkedStack<T> tleft = left, tright = right;
		if (tleft != null)
			tleft.right = tright;
		if (tright != null)
			tright.left = tleft;
		size.decrementAndGet();
		left = null;
		right = null;
		size = null;
		cas.unlock();
		return obj;
	}

	@Override
	public int size() {
		return size.get();
	}

	@Override
	public boolean contains(Object o) {
		DoublyLinkedStack<T> search = searchRef(o);
		if( search == null )
			return false;
		else
			return true;
	}

	@Override
	public Iterator<T> iterator() {
		final DoublyLinkedStack<T> ref = this;
		return new Iterator<T>() {
			DoublyLinkedStack<T> cursor = ref.right; 
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
		List<T> subl = new ArrayList<T>();
		DoublyLinkedStack<T> ref = this.right;
		while (ref != null) {
			if (ref == this) {
				break;
			}
			subl.add(ref.obj);
			ref = ref.right;
		}
		return subl.toArray();
	}

	@SuppressWarnings("hiding")
	@Override
	public <T> T[] toArray(T[] a) {
		List<Object> subl = new ArrayList<Object>();
		DoublyLinkedStack<?> ref = this.right;
		while (ref != null) {
			if (ref == this) {
				break;
			}
			subl.add(ref.obj);
			ref = ref.right;
		}
		return subl.toArray(a);
	}

	@Override
	public boolean add(T e) {
		push(e);
		return true;
	}

	@Override
	public boolean remove(Object o) {
		DoublyLinkedStack<T> search = searchRef(obj);
		if( search!=null ){
			search.remove();
			return true;
		}else 
			return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if( c!=null && c.size()>0 ){
			boolean ret = true;
			for( Object t:c )
				ret = ret && contains(t);
			return ret;
		}else{
			return false;
		}
	}

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
//			DoublyLinkedStackInt<T> linked = getLinked(index);
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
	@Override
	public boolean removeAll(Collection<?> c) {
		if( c!=null && c.size()>0 ){
			boolean ret = true;
			for( Object t:c )
				ret = ret && remove(t);
			return ret;
		}else{
			return false;
		}
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean ret = false;
		if( c!=null && c.size()>0 ){
			DoublyLinkedStack<T> ref = this;
			while (ref != null) {
				if( !c.contains(ref.obj) ){
					DoublyLinkedStack<T> ll = ref;
					ref = ref.left;
					ll.remove();
					ret = true;
				}
				ref = ref.right;
				if (ref == this) {
					break;
				}
			}
		}
		return ret;
	}

//	@Override
	public T get(int index) {
		validateIndex(index);
		DoublyLinkedStack<T> ref = getLinked(index);
		if(ref!=null)
			return ref.obj;
		else
			return null;
	}

	private DoublyLinkedStack<T> getLinked(int index) {
		int idx = 0;
		DoublyLinkedStack<T> ref = this.right;
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
	/**
	 * @param obj
	 * @return
	 */
	DoublyLinkedStack<T> searchRef(final Object o){
		DoublyLinkedStack<T> ref = this.right;
		while (ref != null) {
			if (ref == this) {
				break;
			}
			if( o instanceof String ){
				if(ref.obj!=null && CompareStrategy.EQUALS.compare(ref.obj, o)){
					return ref;
				}
			}else{
				if(ref.obj!=null && compareStrategy.compare(ref.obj, o)){
					return ref;
				}
			}
			ref = ref.right;
		}
		return null;
	}
	//@Override
//	public T set(int index, T element) {
//		validateIndex(index);
//		DoublyLinkedStackInt<T> ref = getLinked(index);
//		if(ref!=null){
//			T obj2 = ref.obj;
//			ref.obj = element;
//			return obj2;
//		}else{
//			return null;
//		}
//	}
//
//	//@Override
//	public void add(int index, T element) {
//		validateIndex(index);
//		DoublyLinkedStackInt<T> ref = getLinked(index);
//		if(ref!=null){
//			ref.add(element);
//		}
//	}
//
//	//@Override
//	public T remove(int index) {
//		DoublyLinkedStackInt<T> ref = getLinked(index);
//		if(ref!=null){
//			T obj2 = ref.obj;
//			ref.remove();
//			return obj2;
//		}else{
//			return null;
//		}
//	}

//	@Override
	public int indexOf(final Object o) {
		int idx = 0;
		DoublyLinkedStack<T> ref = this.right;
		while (ref != null) {
			if (ref == this) {
				break;
			}
			if( o instanceof String ){
				if(o.equals(ref.obj)){
					return idx;
				}
			}else{
				if(ref.obj==o){
					return idx;
				}
			}
			ref = ref.right;
			idx++;
		}
		return -1;
	}

	//@Override
	public int lastIndexOf(Object o) {
		DoublyLinkedStack<T> ref = this.left;
		int idx = size.get()-1;
		while (ref != null) {
			if (ref == this) {
				break;
			}
			if( o instanceof String ){
				if(o.equals(ref.obj)){
					break;
				}
			}else{
				if(ref.obj==o){
					break;
				}
			}
			ref = ref.left;
			idx--;
		}
		return idx;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean addAll(Collection<? extends T> list) {
		Object[] array = list.toArray();
		for(Object obj:array){
			push((T)obj);
		}
		return true;
	}

	@Override
	public boolean isEmpty() {
		return this==left;
	}

	@Override
	public boolean offer(T e) {
		return add(e);
	}

	//@Override
//	public ListIterator<T> listIterator() {
//		final DoublyLinkedStackInt<T> ref = this;
//		return new ListIterator<T>() {
//			DoublyLinkedStackInt<T> cursor = ref; 
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
//				DoublyLinkedStackInt<T> tref = cursor.right;
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
//	public ListIterator<T> listIterator(int index) {
//		validateIndex(index);
//		final DoublyLinkedStackInt<T> ref = getLinked(index);
//		return new ListIterator<T>() {
//			DoublyLinkedStackInt<T> cursor = ref; 
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
//				DoublyLinkedStackInt<T> tref = cursor.right;
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
//	public List<T> subList(int fromIndex, int toIndex) {
//		validateIndex(toIndex);
//		validateIndex(fromIndex);
//		List<T> subl = new ArrayList<T>();
//		DoublyLinkedStackInt<T> ref = getLinked(fromIndex);
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
