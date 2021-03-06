/**
 * 
 */
package org.arivu.datastructure;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.arivu.utils.NullCheck;
import org.arivu.utils.lock.AtomicWFReentrantLock;
import org.arivu.utils.lock.NoLock;

/**
 * @author P
 *
 */
public final class DoublyLinkedStack<T> implements Astack<T> {
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
	Btree dupTree;

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
		this(false, CompareStrategy.REF);
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
		this(null, new Counter(), set, compareStrategy, cas, new Btree(cas, CompareStrategy.EQUALS),
				new Btree(new NoLock(), CompareStrategy.EQUALS));
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
	 * @param binaryTree
	 *            TODO
	 * @param dupTree
	 *            TODO
	 * @param obj
	 */
	DoublyLinkedStack(T t, Counter size, boolean set, CompareStrategy compareStrategy, Lock cas, Btree binaryTree,
			Btree dupTree) {
		super();
		this.obj = t;
		this.size = size;
		this.set = set;
		this.compareStrategy = compareStrategy;
		this.cas = cas;
		this.binaryTree = binaryTree;
		this.dupTree = dupTree;
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
		this.dupTree.clear();
		size.set(0);
		l.unlock();
	}

	public int search(final Object o) {
		int idx = 0;
		DoublyLinkedStack<T> ref = this.right;
		while ( ref != this) {//ref != null &&
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
		if (l == null)
			return null;
		l.lock();
		DoublyLinkedStack<T> ref1 = new DoublyLinkedStack<T>(e, size, false, compareStrategy, cas, binaryTree,
				this.dupTree);
		DoublyLinkedStack<T> ref = top;
		// do{
		ref = top;
		ref.addRight(ref1, l);
		// }while(ref.addRight(ref1, l)==null);

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

	// /**
	// * @return
	// */
	// DoublyLinkedStack<T> removeRight() {
	// final DoublyLinkedStack<T> r = this.right;
	// if (r == this || r == null) {
	// return null;
	// } else {
	// r.remove();
	// return r;
	// }
	// }
	volatile int cnt = 1;

	/**
	 * @param l
	 *            TODO
	 * @param random
	 * @return
	 */
	DoublyLinkedStack<T> addRight(final DoublyLinkedStack<T> r, Lock l) {
		if (r != null) {
			Lock ll = this.cas;
			if (ll == null)
				return null;
			ll.lock();

			addBTree(r);

//			if (size != null) {
				size.incrementAndGet();
//			}
			r.left = this;
			DoublyLinkedStack<T> tr = right;
			this.right = r;
			r.right = tr;

//			if (tr != null)
				tr.left = r;

			ll.unlock();
		}
		return r;
	}

	@SuppressWarnings("unchecked")
	void addBTree(final DoublyLinkedStack<T> r) {
		final int[] pathObj = this.binaryTree.getPathObj(r);
		final Object object = this.binaryTree.findObj(r, pathObj);
		if (object == null)
			this.binaryTree.addObj(r, pathObj);
		else {
			((DoublyLinkedStack<T>) object).cnt++;
			LinkedRef lref = new LinkedRef(String.valueOf(r.hashCode()));
			Object object2 = this.dupTree.get(lref);
			if (object2 == null) {
				lref.addObj(r);
				lref.addObj(object);
				this.dupTree.add(lref);
			} else {
				lref = (LinkedRef) object2;
				lref.addObj(r);
			}
		}
	}

	// /**
	// * @param l
	// * @return
	// */
	// DoublyLinkedStack<T> addLeft(final DoublyLinkedStack<T> l) {
	// if (l != null) {
	// Lock lo = this.cas;
	// lo.lock();
	// size.incrementAndGet();
	// l.right = this;
	// DoublyLinkedStack<T> tl = left;
	// this.left = l;
	// l.left = tl;
	// tl.right = l;
	// lo.unlock();
	// }
	// return l;
	// }

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

		removeBtree();

		DoublyLinkedStack<T> tleft = left, tright = right;
//		if (tleft != null)
			tleft.right = tright;
//		if (tright != null)
			tright.left = tleft;
//		if (size != null)
			size.decrementAndGet();
		left = null;
		right = null;
		size = null;
		cas = null;
		compareStrategy = null;
		l.unlock();
		return obj;
	}

	@SuppressWarnings("unchecked")
	void removeBtree() {
		final int[] pathObj = this.binaryTree.getPathObj(this);
		final Object object = this.binaryTree.findObj(this, pathObj);
//		if (object != null) {

			DoublyLinkedStack<T> object3 = (DoublyLinkedStack<T>) object;
			int dupCnt = --object3.cnt;
			if (dupCnt == 0) {
				this.binaryTree.removeObj(this, pathObj);
			} else {
				final LinkedRef lref = (LinkedRef) this.dupTree.get(new LinkedRef(String.valueOf(this.hashCode())));
				LinkedRef search = lref.search(this);
				search.remove();
//				if (dupCnt >= 1) {
					LinkedRef ref = lref.right;
					while (  ref != lref) { //ref != null && ref.obj != null &&
						((DoublyLinkedStack<T>) ref.obj).cnt = dupCnt;
						ref = ref.right;
					}

//					if ( object3 == this ) {
						this.binaryTree.removeObj(this, pathObj);
						this.binaryTree.addObj(lref.right.obj, pathObj);
//					}
					if (dupCnt == 1){
						this.dupTree.remove(new LinkedRef(String.valueOf(this.hashCode())));
						LinkedRef.clearRef(lref);						
					}

//				}
			}

//		}
	}

	@Override
	public int size() {
		if (size == null)
			return 0;
		return (int) size.get();
	}
	
	@Override
	public long sizeL() {
		if (size == null)
			return 0l;
		else
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
		int i = 0;
		for (T t : subl) {
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
		if (l == null)
			return false;
		l.lock();
		try{
		search = searchRef(o);
		if (search != null) {
			search.remove();
//			l.unlock();
			return true;
		} else {
			return false;
		}
		}finally{
			l.unlock();
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
			while (ref != this) {//ref != null &&
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
		} else {
			return false;
		}
	}

	// @Override
	public T get(int index) {
		validateIndex(index);
		DoublyLinkedStack<T> ref = getLinked(index);
		// if (ref != null)
		return ref.obj;
		// else
		// return null;
	}

	DoublyLinkedStack<T> getLinked(int index) {
		int idx = 0;
		DoublyLinkedStack<T> ref = this.right;
		while (ref != this) {//ref != null && 
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
		Object object = this.binaryTree.get(new DoublyLinkedStack<Object>(o, null, false, null, null, null, null));
		if (object == null)
			return null;
		else
			return (DoublyLinkedStack<T>) (object);

		// DoublyLinkedStack<T> ref = this.right;
		// while (ref != null) {
		// if (ref == this) {
		// break;
		// }
		// if (o instanceof String) {
		// if (ref.obj != null && CompareStrategy.EQUALS.compare(ref.obj, o)) {
		// return ref;
		// }
		// } else {
		// if (ref.obj != null && compareStrategy.compare(ref.obj, o)) {
		// return ref;
		// }
		// }
		// ref = ref.right;
		// }
		// return null;
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
		while ( ref != this) {//ref != null &&
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
		int idx = (int) (size.get() - 1);
		while (ref != this) {//ref != null &&
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

	@Override
	public int hashCode() {
		return ((obj == null) ? 0 : obj.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("rawtypes")
		DoublyLinkedStack other = (DoublyLinkedStack) obj;
		if (this.obj == null) {
			if (other.obj != null)
				return false;
		} else if (!this.obj.equals(other.obj))
			return false;
		return true;
	}

}
