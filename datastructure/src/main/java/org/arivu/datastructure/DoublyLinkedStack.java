/**
 * 
 */
package org.arivu.datastructure;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Lock;

import org.arivu.utils.NullCheck;
import org.arivu.utils.lock.AtomicWFReentrantLock;

/**
 * @author P
 *
 */
public final class DoublyLinkedStack<T> implements Iterable<T>, Queue<T> {
	/**
	 * 
	 */
	T obj;

	/**
	 * 
	 */
	DoublyLinkedStack<T> left = this, right = this;

	DoublyLinkedStack<T> top = this;

	Counter size;
	Lock cas;
	CompareStrategy compareStrategy;
	final boolean set;

	final Btree binaryTree;
	/**
	 * @param col
	 */
	public DoublyLinkedStack(Collection<T> col) {
		this();
		addAll(col);
	}

	/**
	 * 
	 */
	public DoublyLinkedStack() {
		this( false, CompareStrategy.REF);
	}

	/**
	 * @param set
	 * @param compareStrategy
	 */
	DoublyLinkedStack(boolean set, CompareStrategy compareStrategy) {
		this(set, compareStrategy, new AtomicWFReentrantLock());
	}

	/**
	 * @param set
	 * @param compareStrategy
	 */
	DoublyLinkedStack(boolean set, CompareStrategy compareStrategy, Lock cas) {
		this(null, new Counter(), set, compareStrategy, cas, new Btree(cas, CompareStrategy.EQUALS));
	}
	
	/**
	 * @param size
	 *            TODO
	 * @param set
	 *            TODO
	 * @param compareStrategy
	 *            TODO
	 * @param cas
	 *            TODO
	 * @param binaryTree TODO
	 * @param obj
	 */
	DoublyLinkedStack(T t, Counter size, boolean set, CompareStrategy compareStrategy, Lock cas, Btree binaryTree) {
		super();
		this.obj = t;
		this.size = size;
		this.set = set;
		this.compareStrategy = compareStrategy;
		this.cas = cas;
		this.binaryTree = binaryTree;
	}

	/**
	 * 
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.List#clear()
	 */
	@Override
	public void clear() {
		Lock l = this.cas;
		l.lock();
		left = this;
		right = this;
		this.binaryTree.clear();
		size.set(0);
		l.unlock();
	}

	public int search(final Object o) {
		int idx = 0;
		DoublyLinkedStack<T> ref = this.right;
		while (ref != null && ref != this) {
			if (compareStrategy.compare(ref.obj, o)) {
				return idx;
			}
			ref = ref.right;
			idx++;
		}
		return -1;
	}

	// @Override
	public T pop() {
		return poll();
	}

	/**
	 * @return T
	 */
	@Override
	public T poll() {
		if (isEmpty()) {
			return null;
		} else {
			Lock l = this.cas;
			l.lock();
			DoublyLinkedStack<T> ref = top;
			top = ref.left;

			if (ref != this)
				ref.remove();

			T obj2 = ref.obj;
			
			l.unlock();
			
			return obj2;
		}
	}

	// @Override
	public T push(T e) {
		if (set && contains(e)) {
			return e;
		}
		Lock l = this.cas;
		l.lock();
		DoublyLinkedStack<T> ref1 = new DoublyLinkedStack<T>(e, size, false, compareStrategy, cas, binaryTree);
		DoublyLinkedStack<T> ref = top;
//		do{
		ref = top;
		ref.addRight(ref1, l);
//		}while(ref.addRight(ref1, l)==null);
		
		top = ref1;
		l.unlock();
		return ref.obj;
	}

	@Override
	public T element() {
		return peek();
	}

	@Override
	public T peek() {
		if (isEmpty())
			return null;
		else
			return top.obj;
	}

//	/**
//	 * @return
//	 */
//	DoublyLinkedStack<T> removeRight() {
//		final DoublyLinkedStack<T> r = this.right;
//		if (r == this || r == null) {
//			return null;
//		} else {
//			r.remove();
//			return r;
//		}
//	}

	/**
	 * @param l TODO
	 * @param random
	 * @return
	 */
	DoublyLinkedStack<T> addRight(final DoublyLinkedStack<T> r, Lock l) {
		if (r != null) {
			Lock ll = this.cas;
			if(ll==null) return null;
			ll.lock();
			
			final Ref obj2 = new Ref(r);
			final int[] pathObj = this.binaryTree.getPathObj(obj2);
			final Object object = this.binaryTree.findObj(obj2, pathObj);
			if (object == null)
				this.binaryTree.addObj(obj2, pathObj);
			else
				((Ref) object).cnt++;
			
			if (size!=null) {
				size.incrementAndGet();
			}
			r.left = this;
			DoublyLinkedStack<T> tr = right;
			this.right = r;
			r.right = tr;
			
			if(tr!=null)
				tr.left = r;
			
			ll.unlock();
		}
		return r;
	}

//	/**
//	 * @param l
//	 * @return
//	 */
//	DoublyLinkedStack<T> addLeft(final DoublyLinkedStack<T> l) {
//		if (l != null) {
//			Lock lo = this.cas;
//			lo.lock();
//			size.incrementAndGet();
//			l.right = this;
//			DoublyLinkedStack<T> tl = left;
//			this.left = l;
//			l.left = tl;
//			tl.right = l;
//			lo.unlock();
//		}
//		return l;
//	}

	/**
	 * 
	 */
	@Override
	public T remove() {
		return removeRef();
	}

	private T removeRef() {
		Lock l = this.cas;
		l.lock();
		
		final Ref obj2 = new Ref(obj);
		final int[] pathObj = this.binaryTree.getPathObj(obj2);
		final Object object = this.binaryTree.findObj(obj2, pathObj);
		if (object != null && --((Ref) object).cnt == 0)
			this.binaryTree.removeObj(obj2, pathObj);
		
		DoublyLinkedStack<T> tleft = left, tright = right;
		if (tleft != null)
			tleft.right = tright;
		if (tright != null)
			tright.left = tleft;
		if (size != null)
			size.decrementAndGet();
		left = null;
		right = null;
		size = null;
		cas = null;
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
		DoublyLinkedStack<T> search = searchRef(o);
		if (search == null)
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
		List<T> subl = new DoublyLinkedList<T>(this);
		Object[] arr = new Object[subl.size()];
		int i=0;
		for(T t:subl){
			arr[i++] = t;
		}
		return arr;
	}

	@SuppressWarnings({ "hiding", "unchecked" })
	@Override
	public <T> T[] toArray(T[] a) {
		Object[] elementData = toArray();
		if (a.length < size())
			return (T[]) Arrays.copyOf(elementData, size(), a.getClass());
		
		System.arraycopy(elementData, 0, a, 0, size());
		if (a.length > size())
			a[size()] = null;
		return a;
	}

	@Override
	public boolean add(T e) {
		push(e);
		return true;
	}

	@Override
	public boolean remove(Object o) {
		DoublyLinkedStack<T> search = null;
		Lock l = this.cas;
		l.lock();
		search = searchRef(o);
		if (search != null) {
			search.remove();
			l.unlock();
			return true;
		} else{
			l.unlock();
			return false;
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if (!NullCheck.isNullOrEmpty(c)) {
			boolean ret = true;
			for (Object t : c)
				ret = ret & contains(t);
			return ret;
		} else {
			return false;
		}
	}

	// //@Override
	// public boolean addAll(Collection<? extends T> c) {
	// if( c!=null && c.size()>0 ){
	// for( T t:c )
	// add(t);
	//
	// return true;
	// }else{
	// return false;
	// }
	// }
	//
	// //@Override
	// public boolean addAll(int index, Collection<? extends T> c) {
	// if( c!=null && c.size()>0 ){
	// validateIndex(index);
	// DoublyLinkedStackInt<T> linked = getLinked(index);
	// if( linked != null ){
	// for( T t:c )
	// add(t);
	// }
	// return true;
	// }else{
	// return false;
	// }
	// }
	//
	@Override
	public boolean removeAll(Collection<?> c) {
		if (!NullCheck.isNullOrEmpty(c)) {
			boolean ret = true;
			for (Object t : c)
				ret = ret & remove(t);
			return ret;
		} else {
			return false;
		}
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		if (!NullCheck.isNullOrEmpty(c)) {
			boolean ret = false;
			Lock l = this.cas;
			l.lock();

			DoublyLinkedStack<T> ref = this.right;
			while (ref != null && ref != this) {
				if (!c.contains(ref.obj)) {
					DoublyLinkedStack<T> ll = ref;
					ref = ref.left;
					ll.removeRef();
					ret = true;
				}
				ref = ref.right;
			}

			l.unlock();
			return ret;
		}else{
			return false;
		}
	}

	// @Override
	public T get(int index) {
		validateIndex(index);
		DoublyLinkedStack<T> ref = getLinked(index);
//		if (ref != null)
			return ref.obj;
//		else
//			return null;
	}

	DoublyLinkedStack<T> getLinked(int index) {
		int idx = 0;
		DoublyLinkedStack<T> ref = this.right;
		while (ref != null && ref != this ) {
			if (idx == index) {
				return ref;
			}
			ref = ref.right;
			idx++;
		}
		return null;
	}

	void validateIndex(int index) {
		if (index >= size.get() || index < 0)
			throw new ArrayIndexOutOfBoundsException(index);
	}

	/**
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("unchecked")
	DoublyLinkedStack<T> searchRef(final Object o) {
		Object object = this.binaryTree.get(new Ref(o));
		if (object == null)
			return null;
		else
			return (DoublyLinkedStack<T>) ((Ref) object).st;
		
//		DoublyLinkedStack<T> ref = this.right;
//		while (ref != null) {
//			if (ref == this) {
//				break;
//			}
//			if (o instanceof String) {
//				if (ref.obj != null && CompareStrategy.EQUALS.compare(ref.obj, o)) {
//					return ref;
//				}
//			} else {
//				if (ref.obj != null && compareStrategy.compare(ref.obj, o)) {
//					return ref;
//				}
//			}
//			ref = ref.right;
//		}
//		return null;
	}
	// @Override
	// public T set(int index, T element) {
	// validateIndex(index);
	// DoublyLinkedStackInt<T> ref = getLinked(index);
	// if(ref!=null){
	// T obj2 = ref.obj;
	// ref.obj = element;
	// return obj2;
	// }else{
	// return null;
	// }
	// }
	//
	// //@Override
	// public void add(int index, T element) {
	// validateIndex(index);
	// DoublyLinkedStackInt<T> ref = getLinked(index);
	// if(ref!=null){
	// ref.add(element);
	// }
	// }
	//
	// //@Override
	// public T remove(int index) {
	// DoublyLinkedStackInt<T> ref = getLinked(index);
	// if(ref!=null){
	// T obj2 = ref.obj;
	// ref.remove();
	// return obj2;
	// }else{
	// return null;
	// }
	// }

	// @Override
	public int indexOf(final Object o) {
		int idx = 0;
		DoublyLinkedStack<T> ref = this.right;
		while (ref != null && ref != this ) {
			if (compareStrategy.compare(ref.obj, o)) {
				return idx;
			}
			ref = ref.right;
			idx++;
		}
		return -1;
	}

	// @Override
	public int lastIndexOf(Object o) {
		DoublyLinkedStack<T> ref = this.left;
		int idx = size.get() - 1;
		while (ref != null && ref != this ) {
			if (compareStrategy.compare(ref.obj, o)) {
				return idx;
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
		for (Object obj : array) {
			push((T) obj);
		}
		return true;
	}

	@Override
	public boolean isEmpty() {
		return this == left;
	}

	@Override
	public boolean offer(T e) {
		return add(e);
	}

	// @Override
	// public ListIterator<T> listIterator() {
	// final DoublyLinkedStackInt<T> ref = this;
	// return new ListIterator<T>() {
	// DoublyLinkedStackInt<T> cursor = ref;
	// int idx = 0;
	// //@Override
	// public boolean hasNext() {
	// return !(cursor == ref.left);
	// }
	//
	// //@Override
	// public T next() {
	// T t = cursor.obj;
	// cursor = cursor.left;
	// idx++;
	// return t;
	// }
	//
	// //@Override
	// public boolean hasPrevious() {
	// return !(cursor == ref.right);
	// }
	//
	// //@Override
	// public T previous() {
	// T t = cursor.obj;
	// cursor = cursor.right;
	// idx--;
	// return t;
	// }
	//
	// //@Override
	// public int nextIndex() {
	// return idx+1;
	// }
	//
	// //@Override
	// public int previousIndex() {
	// return idx-1;
	// }
	//
	// //@Override
	// public void remove() {
	// DoublyLinkedStackInt<T> tref = cursor.right;
	// cursor.remove();
	// cursor = tref;
	// }
	//
	// //@Override
	// public void set(T e) {
	// cursor.obj = e;
	// }
	//
	// //@Override
	// public void add(T e) {
	// cursor.add(e);
	// }
	// };
	// }
	//
	// //@Override
	// public ListIterator<T> listIterator(int index) {
	// validateIndex(index);
	// final DoublyLinkedStackInt<T> ref = getLinked(index);
	// return new ListIterator<T>() {
	// DoublyLinkedStackInt<T> cursor = ref;
	// int idx = 0;
	// //@Override
	// public boolean hasNext() {
	// return !(cursor == ref.left);
	// }
	//
	// //@Override
	// public T next() {
	// T t = cursor.obj;
	// cursor = cursor.left;
	// idx++;
	// return t;
	// }
	//
	// //@Override
	// public boolean hasPrevious() {
	// return !(cursor == ref.right);
	// }
	//
	// //@Override
	// public T previous() {
	// T t = cursor.obj;
	// cursor = cursor.right;
	// idx--;
	// return t;
	// }
	//
	// //@Override
	// public int nextIndex() {
	// return idx+1;
	// }
	//
	// //@Override
	// public int previousIndex() {
	// return idx-1;
	// }
	//
	// //@Override
	// public void remove() {
	// DoublyLinkedStackInt<T> tref = cursor.right;
	// cursor.remove();
	// cursor = tref;
	// }
	//
	// //@Override
	// public void set(T e) {
	// cursor.obj = e;
	// }
	//
	// //@Override
	// public void add(T e) {
	// cursor.add(e);
	// }
	// };
	// }
	//
	// //@Override
	// public List<T> subList(int fromIndex, int toIndex) {
	// validateIndex(toIndex);
	// validateIndex(fromIndex);
	// List<T> subl = new ArrayList<T>();
	// DoublyLinkedStackInt<T> ref = getLinked(fromIndex);
	// int idx = fromIndex;
	// while (ref != null) {
	// subl.add(ref.obj);
	// ref = ref.right;
	// idx++;
	// if (idx == toIndex) {
	// break;
	// }
	// }
	// return Collections.unmodifiableList(subl);
	// }

}
