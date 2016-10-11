/**
 * 
 */
package org.arivu.datastructure;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.concurrent.locks.Lock;

import org.arivu.utils.NullCheck;
import org.arivu.utils.lock.AtomicWFReentrantLock;
import org.arivu.utils.lock.NoLock;

/**
 * @author P
 *
 */
public final class DoublyLinkedList<T> implements List<T>, Queue<T> {
	/**
	 * 
	 */
	T obj;

	/**
	 * 
	 */
	DoublyLinkedList<T> left = this, right = this;

	/**
	 * 
	 */
	Counter size;
	/**
	 * 
	 */
	CompareStrategy compareStrategy;
	/**
	 * 
	 */
	Lock cas;
	/**
	 * 
	 */
	final Btree binaryTree;

	Btree dupTree;

	/**
	 * @param col
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

	/**
	 * @param compareStrategy
	 */
	DoublyLinkedList(CompareStrategy compareStrategy) {
		this(compareStrategy, new AtomicWFReentrantLock());
	}

	/**
	 * @param compareStrategy
	 * @param lock
	 */
	DoublyLinkedList(CompareStrategy compareStrategy, Lock lock) {
		this(null, new Counter(), compareStrategy, lock, new Btree(lock, CompareStrategy.EQUALS), new Btree(new NoLock(), CompareStrategy.EQUALS));
	}

	/**
	 * @param t
	 * @param size
	 * @param cas
	 * @param binaryTree
	 *            TODO
	 * @param dupTree TODO
	 */
	DoublyLinkedList(T t, Counter size, CompareStrategy compareStrategy, Lock cas, Btree binaryTree, Btree dupTree) {
		super();
		this.obj = t;
		this.size = size;
		this.compareStrategy = compareStrategy;
		this.cas = cas;
		this.binaryTree = binaryTree;
		this.dupTree = dupTree;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.List#clear()
	 */
	@Override
	public void clear() {
		Lock l = this.cas;
		l.lock();
		this.binaryTree.clear();
		this.dupTree.clear();
		left = this;
		right = this;
		size.set(0);
		l.unlock();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.List#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return this == left;
	}

	/**
	 * @return T
	 */
	@Override
	public T poll() {
		Lock l = this.cas;
		l.lock();
		DoublyLinkedList<T> removeRight = removeRight();
		if (removeRight != null) {
			l.unlock();
			return removeRight.obj;
		} else {
			l.unlock();
			return null;
		}
	}

	/**
	 * @return
	 */
	DoublyLinkedList<T> removeRight() {
		final DoublyLinkedList<T> r = this.right;
		if (r == this ) {//|| r == null
			return null;
		} else {
			r.removeRef();
			return r;
		}
	}

	volatile int cnt = 1;

	/**
	 * @param l
	 * @return
	 */
	@SuppressWarnings("unchecked")
	DoublyLinkedList<T> addLeft(final DoublyLinkedList<T> l) {
//		if (l != null) {
			Lock lo = this.cas;
			lo.lock();
//			if (size != null)
				size.incrementAndGet();
			l.right = this;
			DoublyLinkedList<T> tl = left;
			this.left = l;
			l.left = tl;
			tl.right = l;

			// final Ref obj2 = new Ref(l);
			final int[] pathObj = this.binaryTree.getPathObj(l);
			final Object object = this.binaryTree.findObj(l, pathObj);
			if (object == null)
				this.binaryTree.addObj(l, pathObj);
			else {
				((DoublyLinkedList<T>) object).cnt++;
				LinkedRef lref = new LinkedRef(String.valueOf(l.hashCode()));
				Object object2 = this.dupTree.get(lref);
				if (object2 == null) {
					lref.addObj(l);
					lref.addObj(object);
					this.dupTree.add(lref);
				} else {
					lref = (LinkedRef) object2;
					lref.addObj(l);
				}
			}

			lo.unlock();
//		}
		return l;
	}

	/**
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("unchecked")
	DoublyLinkedList<T> search(final Object o) {
		Object object = this.binaryTree.get(new DoublyLinkedList<Object>(o, null, null, null, null, this.dupTree));
		if (object == null)
			return null;
		else
			return (DoublyLinkedList<T>) (object);
	}

	@Override
	public T remove() {
		return poll();
	}

	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	T removeRef() {
		Lock l = this.cas;
		l.lock();
		DoublyLinkedList<T> tleft = left, tright = right;

//		if (tleft != null)
			tleft.right = tright;

//		if (tright != null)
			tright.left = tleft;

//		if (size != null)
			size.decrementAndGet();

		left = null;
		right = null;
		size = null;
		compareStrategy = null;

		final int[] pathObj = this.binaryTree.getPathObj(this);
		final Object object = this.binaryTree.findObj(this, pathObj);
		if (object != null) {

			DoublyLinkedList<T> object3 = (DoublyLinkedList<T>) object;
			int dupCnt = --object3.cnt;
			if (dupCnt == 0) {
				this.binaryTree.removeObj(this, pathObj);
			} else {
				LinkedRef lref = (LinkedRef) this.dupTree
						.get(new LinkedRef(String.valueOf(this.hashCode())));
				LinkedRef search = lref.search(this);
				search.remove();
//				if (dupCnt >= 1) {
					LinkedRef ref = lref.right;
					while ( ref.obj != null && ref != lref) { //ref != null &&
						((DoublyLinkedList<T>) ref.obj).cnt = dupCnt;
						ref = ref.right;
					}
//					if( object3 == this ){
						this.binaryTree.removeObj(this, pathObj);
						this.binaryTree.addObj(lref.right.obj, pathObj);
//					}
					if(dupCnt==1)
						this.dupTree.remove(new LinkedRef(String.valueOf(this.hashCode())));
//				}
			}

		}

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
		if (search == null)
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
		if (e != null) {
			addLeft(new DoublyLinkedList<T>(e, size, compareStrategy, cas, binaryTree, this.dupTree));
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean remove(Object o) {
		DoublyLinkedList<T> search = null;
		Lock l = this.cas;
		l.lock();
		search = search(o);
		if (search != null) {
			search.removeRef();
			l.unlock();
			return true;
		} else {
			l.unlock();
			return false;
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if (!NullCheck.isNullOrEmpty(c)) {
			boolean ret = true;

			Lock l = this.cas;
			l.lock();

			for (Object t : c)
				ret = ret & contains(t);

			l.unlock();
			return ret;
		} else {
			return false;
		}
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		if (!NullCheck.isNullOrEmpty(c)) {
			boolean ret = true;

			Lock l = this.cas;
			l.lock();

			for (T t : c)
				ret = ret & add(t);

			l.unlock();
			return ret;
		} else {
			return false;
		}
	}

	@Override
	public boolean addAll(final int index, final Collection<? extends T> c) {
		if (!NullCheck.isNullOrEmpty(c)) {
			if (index == 0 && size() == 0) {
				return addAll(c);
			}
			validateIndex(index);
			boolean ret = true;

			Lock l = this.cas;
			l.lock();

			DoublyLinkedList<T> linked = getLinked(index);
//			if (linked != null) {
				for (T t : c)
					ret = ret & linked.add(t);
//			}

			l.unlock();
			return ret;
		} else {
			return false;
		}
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (!NullCheck.isNullOrEmpty(c)) {
			boolean ret = true;

			Lock l = this.cas;
			l.lock();

			for (Object t : c)
				ret = ret & remove(t);

			l.unlock();
			return ret;
		} else {
			return false;
		}
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean ret = false;
		if (!NullCheck.isNullOrEmpty(c)) {
			Lock l = this.cas;
			l.lock();

			DoublyLinkedList<T> ref = this.right;
			while (ref != this) {
				if (!c.contains(ref.obj)) {
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
		return ref.obj;
	}

	DoublyLinkedList<T> getLinked(final int index) {
		int idx = 0;
		DoublyLinkedList<T> ref = this.right;
		while (ref != this) {
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

	@Override
	public T set(int index, T element) {
		validateIndex(index);
		DoublyLinkedList<T> ref = getLinked(index);
		T obj2 = ref.obj;
		ref.obj = element;
		return obj2;
	}

	@Override
	public void add(int index, T element) {
		validateIndex(index);
		Lock l = this.cas;
		l.lock();
//		DoublyLinkedList<T> ref = 
		getLinked(index).add(element);
//		if (ref != null) {
//			ref.add(element);
//		}
		l.unlock();
	}

	@Override
	public T remove(int index) {
		DoublyLinkedList<T> ref = getLinked(index);
		if (ref != null) {
			T obj2 = ref.obj;
			ref.removeRef();
			return obj2;
		} else {
			return null;
		}
	}

	@Override
	public int indexOf(Object o) {
		int idx = 0;
		DoublyLinkedList<T> ref = this.right;
		while (ref != this) {
			if (compareStrategy.compare(ref.obj, o)) {
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
		int idx = size.get() - 1;
		while (ref != this) {
			if (compareStrategy.compare(o, ref.obj)) {
				return idx;
			}
			ref = ref.left;
			idx--;
		}
		return -1;
	}

	@Override
	public ListIterator<T> listIterator() {
		final DoublyLinkedList<T> that = this;
		return getListIterator(that, that.right, 0);
	}

	ListIterator<T> getListIterator(final DoublyLinkedList<T> that, final DoublyLinkedList<T> cur, final int index) {
		return new ListIterator<T>() {
			// Direction dir = Direction.right;
			// DoublyLinkedList<T> cursor = cur;
			int idx = index;

			@Override
			public boolean hasNext() {
				// boolean b = cursor != that;
				// if (!b)
				// previous();
				// return b;
				return idx >= 0 && idx < that.size();
			}

			@Override
			public T next() {
				// T t = cursor.obj;
				// cursor = cursor.right;
				// idx = nextIndex();
				// dir = Direction.right;
				return that.get(nextIndex());
			}

			@Override
			public boolean hasPrevious() {
				// boolean b = cursor != that;
				// if (!b)
				// next();
				// return b;
				return idx > 0 && idx <= that.size();
			}

			@Override
			public T previous() {
				// T t = cursor.obj;
				// cursor = cursor.left;
				// idx = previousIndex();
				//// dir = Direction.left;
				// return t;
				return that.get(previousIndex());
			}

			@Override
			public int nextIndex() {
				return idx++;
			}

			@Override
			public int previousIndex() {
				return --idx;
			}

			@Override
			public void remove() {
				// DoublyLinkedList<T> tref = null;
				// if (dir == Direction.right)
				// tref = cursor.left;
				// else
				// tref = cursor.right;
				// if( tref == that) tref = tref.right;
				// tref.removeRef();
				that.remove(idx);
				if (idx > 0 && idx == size()) {
					previousIndex();
				}
			}

			@Override
			public void set(T e) {
				// cursor.obj = e;
				that.set(idx, e);
			}

			@Override
			public void add(T e) {
				// cursor.add(e);
				that.add(idx, e);
			}
		};
	}

	@Override
	public ListIterator<T> listIterator(final int index) {
		validateIndex(index);
		final DoublyLinkedList<T> that = this;
		return getListIterator(that, getLinked(index), index);
	}

	@Override
	public List<T> subList(final int fromIndex, final int toIndex) {
		validateIndex(toIndex);
		validateIndex(fromIndex);

		if (fromIndex > toIndex)
			throw new ArrayIndexOutOfBoundsException(fromIndex);

		List<T> subl = new DoublyLinkedList<T>();
		DoublyLinkedList<T> ref = getLinked(fromIndex);
		int idx = fromIndex;
		while (ref != null) {
			subl.add(ref.obj);
			if (idx == toIndex) {
				break;
			}
			ref = ref.right;
			idx++;
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
		DoublyLinkedList other = (DoublyLinkedList) obj;
		if (this.obj == null) {
			if (other.obj != null)
				return false;
		} else if (!this.obj.equals(other.obj))
			return false;
		return true;
	}

}
