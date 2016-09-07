/**
 * 
 */
package org.arivu.datastructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.arivu.utils.lock.AtomicWFLock;


/**
 * @author P
 *
 */
public final class DoublyLinkedSet<T> implements Set<T>,Queue<T> {
	/**
	 * 
	 */
	T obj;
	
	/**
	 * 
	 */
	DoublyLinkedSet<T> left = this, right = this;
	
	
	AtomicInteger size;
	
	final CompareStrategy compareStrategy;
	
	/**
	 * @param strategy
	 */
	DoublyLinkedSet(CompareStrategy strategy) {
		this(null,new AtomicInteger(0), strategy);
	}
	
	/**
	 * 
	 */
	public DoublyLinkedSet() {
		this(CompareStrategy.REF);
	}
	
	/**
	 * @param t
	 * @param size 
	 * @param strategy 
	 */
	private DoublyLinkedSet(T t, AtomicInteger size, CompareStrategy strategy) {
		super();
		this.obj = t;
		this.size = size;
		this.compareStrategy = strategy;
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
		
		if(size!=null)
			size.set(0);
		
		cas.unlock();
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
		DoublyLinkedSet<T> removeRight = removeRight();
		if(removeRight!=null)
			return removeRight.obj;
		return null;
	}
	
	static final AtomicWFLock cas = new AtomicWFLock();
	
	/**
	 * @return
	 */
	DoublyLinkedSet<T> removeRight(){
		final DoublyLinkedSet<T> r = this.right;
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
	DoublyLinkedSet<T> addLeft(final DoublyLinkedSet<T> l) {
		if (l != null) {
			cas.lock();
			if (size!=null) {
				size.incrementAndGet();
			}

			if(null==right && left == null ){
				left = l;
				right = l;
				l.left = this;
				l.right = this;
			}else if(left==right && left == this ){
				left = l;
				right = l;
				l.left = this;
				l.right = this;
			}else{
				l.right = this;
				DoublyLinkedSet<T> tl = left;
				this.left = l;
				l.left = tl;
				
				if(tl!=null)
					tl.right = l;
				
				if(right==this){
					right = l;
				}
			}
			
			cas.unlock();
		}
		return l;
	}
	
	/**
	 * @param obj
	 * @return
	 */
	DoublyLinkedSet<T> search(final Object o){
		DoublyLinkedSet<T> ref = this.right;
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
	
	/**
	 * 
	 */
	@Override
	public T remove() {
		DoublyLinkedSet<T> removeRight = removeRight();
		if(removeRight!=null)
			return removeRight.obj;
		return null;
	}

	final T removeRef(){
		cas.lock();
		DoublyLinkedSet<T> tleft = left, tright = right;
		
		if(left==right){
			left.right = left;
			right.left = right;
		}else{
			if (tleft != null)
				tleft.right = tright;
			
			if (tright != null)
				tright.left = tleft;
		}
		
		if (size!=null) {
			size.decrementAndGet();
		}
		left = null;
		right = null;
		size = null;
		cas.unlock();
		return obj;
	}
	
	@Override
	public int size() {
		if(size==null)
			return 0;
		else 
			return size.get();
	}

	@Override
	public boolean contains(Object o) {
		DoublyLinkedSet<T> search = search(o);
		if( search == null )
			return false;
		else
			return true;
	}

	@Override
	public Iterator<T> iterator() {
		final DoublyLinkedSet<T> ref = this.right;
		return new Iterator<T>() {
			DoublyLinkedSet<T> cursor = ref; 
			@Override
			public boolean hasNext() {
				if(ref==null)
					return false;
				else 
					return !(cursor == ref.left);
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
		DoublyLinkedSet<T> ref = this.right;
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
		DoublyLinkedSet<?> ref = this.right;
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
		if(e!=null){
			DoublyLinkedSet<T> search = search(e);
			if( search == null ){
				addLeft(new DoublyLinkedSet<T>(e, size, compareStrategy));
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}

	@Override
	public boolean remove(Object o) {
		DoublyLinkedSet<T> search = search(o);
//		System.out.println("remove "+this+" Object "+o+" search "+search.obj);
		if( search!=null ){
			search.removeRef();
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

	@Override
	public boolean addAll(Collection<? extends T> c) {
		if( c!=null && c.size()>0 ){
			for( T t:c )
				add(t);

			return true;
		}else{
			return false;
		}
	}

//	@Override
//	public boolean addAll(int index, Collection<? extends T> c) {
//		if( c!=null && c.size()>0 ){
//			validateIndex(index);
//			DoublyLinkedSetInt<T> linked = getLinked(index);
//			if( linked != null ){
//				for( T t:c )
//					add(t);
//			}
//			return true;
//		}else{
//			return false;
//		}
//	}

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
			DoublyLinkedSet<T> ref = this.right;
			while (ref != null) {
				if (ref == this) {
					break;
				}else if( !c.contains(ref.obj) ){
					DoublyLinkedSet<T> ll = ref;
					ref = ref.left;
					ll.removeRef();
					ret = true;
				}
				ref = ref.right;
				
			}
		}
		return ret;
	}

//	@Override
//	public T get(int index) {
//		validateIndex(index);
//		DoublyLinkedSetInt<T> ref = getLinked(index);
//		if(ref!=null)
//			return ref.obj;
//		else
//			return null;
//	}

//	private final DoublyLinkedSetInt<T> getLinked(int index) {
//		int idx = 0;
//		DoublyLinkedSetInt<T> ref = this;
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

//	private final void validateIndex(int index) {
//		if( index >= size || index < 0 ) throw new ArrayIndexOutOfBoundsException(index);
//	}

//	@Override
//	public T set(int index, T element) {
//		validateIndex(index);
//		DoublyLinkedSetInt<T> ref = getLinked(index);
//		if(ref!=null){
//			T obj2 = ref.obj;
//			ref.obj = element;
//			return obj2;
//		}else{
//			return null;
//		}
//	}

//	@Override
//	public void add(int index, T element) {
//		validateIndex(index);
//		DoublyLinkedSetInt<T> ref = getLinked(index);
//		if(ref!=null){
//			ref.add(element);
//		}
//	}
//
//	@Override
//	public T remove(int index) {
//		DoublyLinkedSetInt<T> ref = getLinked(index);
//		if(ref!=null){
//			T obj2 = ref.obj;
//			ref.remove();
//			return obj2;
//		}else{
//			return null;
//		}
//	}
//
//	@Override
//	public int indexOf(Object o) {
//		int idx = 0;
//		DoublyLinkedSetInt<T> ref = this;
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
//	@Override
//	public int lastIndexOf(Object o) {
//		DoublyLinkedSetInt<T> ref = this.left;
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
//	@Override
//	public ListIterator<T> listIterator() {
//		final DoublyLinkedSetInt<T> ref = this;
//		return new ListIterator<T>() {
//			DoublyLinkedSetInt<T> cursor = ref; 
//			int idx = 0;
//			@Override
//			public boolean hasNext() {
//				return !(cursor == ref.left);
//			}
//
//			@Override
//			public T next() {
//				T t = cursor.obj;
//				cursor = cursor.left; 
//				idx++;
//				return t;
//			}
//
//			@Override
//			public boolean hasPrevious() {
//				return !(cursor == ref.right);
//			}
//
//			@Override
//			public T previous() {
//				T t = cursor.obj;
//				cursor = cursor.right; 
//				idx--;
//				return t;
//			}
//
//			@Override
//			public int nextIndex() {
//				return idx+1;
//			}
//
//			@Override
//			public int previousIndex() {
//				return idx-1;
//			}
//
//			@Override
//			public void remove() {
//				DoublyLinkedSetInt<T> tref = cursor.right;
//				cursor.remove();
//				cursor = tref;
//			}
//
//			@Override
//			public void set(T e) {
//				cursor.obj = e;
//			}
//
//			@Override
//			public void add(T e) {
//				cursor.add(e);
//			}
//		};
//	}
//
//	@Override
//	public ListIterator<T> listIterator(int index) {
//		validateIndex(index);
//		final DoublyLinkedSetInt<T> ref = getLinked(index);
//		return new ListIterator<T>() {
//			DoublyLinkedSetInt<T> cursor = ref; 
//			int idx = 0;
//			@Override
//			public boolean hasNext() {
//				return !(cursor == ref.left);
//			}
//
//			@Override
//			public T next() {
//				T t = cursor.obj;
//				cursor = cursor.left; 
//				idx++;
//				return t;
//			}
//
//			@Override
//			public boolean hasPrevious() {
//				return !(cursor == ref.right);
//			}
//
//			@Override
//			public T previous() {
//				T t = cursor.obj;
//				cursor = cursor.right; 
//				idx--;
//				return t;
//			}
//
//			@Override
//			public int nextIndex() {
//				return idx+1;
//			}
//
//			@Override
//			public int previousIndex() {
//				return idx-1;
//			}
//
//			@Override
//			public void remove() {
//				DoublyLinkedSetInt<T> tref = cursor.right;
//				cursor.remove();
//				cursor = tref;
//			}
//
//			@Override
//			public void set(T e) {
//				cursor.obj = e;
//			}
//
//			@Override
//			public void add(T e) {
//				cursor.add(e);
//			}
//		};
//	}
//
//	@Override
//	public List<T> subList(int fromIndex, int toIndex) {
//		validateIndex(toIndex);
//		validateIndex(fromIndex);
//		List<T> subl = new ArrayList<T>();
//		DoublyLinkedSetInt<T> ref = getLinked(fromIndex);
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
		return this.right.obj;
	}
}
enum CompareStrategy{
	REF,EQUALS{

		@Override
		boolean compare(Object o1, Object o2) {
			if(o1!=null) return o1.equals(o2);
			else if(o1==null&&o2==null) return true;
			else return false;
		}
		
	};
	
	boolean compare(Object o1,Object o2){
		return o1==o2;
	}
	
}