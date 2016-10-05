/**
 * 
 */
package org.arivu.datastructure;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.arivu.utils.lock.AtomicWFReentrantLock;

/**
 * @author P
 *
 */
public final class DoublyLinkedSet<T> implements Set<T>, Queue<T> {
	/**
	 * 
	 */
	T obj;

	/**
	 * 
	 */
	DoublyLinkedSet<T> left = this, right = this;

	Counter size;
	CompareStrategy compareStrategy;
	Lock cas;

	final Btree binaryTree;

	/**
	 * @param strategy
	 */
	DoublyLinkedSet(CompareStrategy strategy) {
		this(strategy, new AtomicWFReentrantLock());
	}

	/**
	 * @param strategy
	 * @param lock
	 */
	DoublyLinkedSet(CompareStrategy strategy, Lock lock) {
		this(null, new Counter(), strategy, lock, new Btree(lock, CompareStrategy.EQUALS));
	}

	/**
	 * @param col
	 */
	public DoublyLinkedSet(Collection<T> col) {
		this();
		addAll(col);
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
	 * @param cas
	 */
	DoublyLinkedSet(T t, Counter size, CompareStrategy strategy, Lock cas, Btree binaryTree) {
		super();
		this.obj = t;
		this.size = size;
		this.compareStrategy = strategy;
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

		if (size != null)
			size.set(0);

		this.binaryTree.clear();
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
		Lock l = cas;
		l.lock();
		DoublyLinkedSet<T> removeRight = removeRight();
		if (removeRight != null){
			l.unlock();
			return removeRight.obj;
		}else{
			l.unlock();
			return null;
		}
	}

	/**
	 * @return
	 */
	DoublyLinkedSet<T> removeRight() {
		final DoublyLinkedSet<T> r = this.right;
		if (r == this || r == null) {
			return null;
		} else {
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
			Lock lo = this.cas;
			lo.lock();
			if (size != null) {
				size.incrementAndGet();
			}
			this.binaryTree.add(new Ref(l));
			// if(null==right && left == null ){
			// left = l;
			// right = l;
			// l.left = this;
			// l.right = this;
			// }else
			if (left == right && left == this) {
				left = l;
				right = l;
				l.left = this;
				l.right = this;
			} else {
				l.right = this;
				DoublyLinkedSet<T> tl = left;
				this.left = l;
				l.left = tl;

				if (tl != null)
					tl.right = l;

//				TODO: check to see if we don't get any null pointer exp
//				if (right == this) {
//					right = l;
//				}
			}

			lo.unlock();
		}
		return l;
	}

	/**
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("unchecked")
	DoublyLinkedSet<T> search(final Object o) {
		Object object = this.binaryTree.get(new Ref(o));
		if (object == null)
			return null;
		else
			return (DoublyLinkedSet<T>) ((Ref) object).set;
		// DoublyLinkedSet<T> ref = this.right;
		// while (ref != null) {
		// if (ref == this) {
		// break;
		// }
		// if( o instanceof String ){
		// if(ref.obj!=null && CompareStrategy.EQUALS.compare(ref.obj, o)){
		// return ref;
		// }
		// }else{
		// if(ref.obj!=null && compareStrategy.compare(ref.obj, o)){
		// return ref;
		// }
		// }
		// ref = ref.right;
		// }
		// return null;
	}

	/**
	 * 
	 */
	@Override
	public T remove() {
		Lock l = cas;
		l.lock();
		DoublyLinkedSet<T> removeRight = removeRight();
		if (removeRight != null){
			l.unlock();
			return removeRight.obj;
		}else{
			l.unlock();
			return null;
		}
	}

	T removeRef() {
		Lock l = this.cas;
		l.lock();
		DoublyLinkedSet<T> tleft = left, tright = right;

		if (left == right) {
			left.right = left;
			right.left = right;
		} else {
			if (tleft != null)
				tleft.right = tright;

			if (tright != null)
				tright.left = tleft;
		}

		if (size != null) {
			size.decrementAndGet();
		}
		left = null;
		right = null;
		size = null;
		cas = null;
		compareStrategy = null;
		this.binaryTree.remove(new Ref(obj));
		l.unlock();
		return obj;
	}

	@Override
	public int size() {
		if (size == null)
			return 0;
		else
			return size.get();
	}

	@Override
	public boolean contains(Object o) {
		DoublyLinkedSet<T> search = search(o);
		if (search == null)
			return false;
		else
			return true;
	}

	@Override
	public Iterator<T> iterator() {
		final DoublyLinkedSet<T> ref = this;
		return new Iterator<T>() {
			DoublyLinkedSet<T> cursor = ref.right;

			@Override
			public boolean hasNext() {
				// if(ref==null)
				// return false;
				// else
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
			DoublyLinkedSet<T> search = null;
			Lock l = this.cas;
			l.lock();
			search = search(e);
			if (search == null) {
				addLeft(new DoublyLinkedSet<T>(e, size, compareStrategy, cas, binaryTree));
				l.unlock();
				return true;
			} else {
				l.unlock();
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public boolean remove(Object o) {
		DoublyLinkedSet<T> search = null;
		Lock l = this.cas;
		if (l == null)
			return false;
		l.lock();
		search = search(o);
		if (search != null) {
			// System.out.println("remove "+this+" Object "+o+" search
			// "+search.obj);
			search.removeRef();
			l.unlock();
			return true;
		} else {
			// System.out.println("remove "+this+" Object "+o+" search null");
			l.unlock();
			return false;
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if (c != null && c.size() > 0) {
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
		if (c != null && c.size() > 0) {
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

	// @Override
	// public boolean addAll(int index, Collection<? extends T> c) {
	// if( c!=null && c.size()>0 ){
	// validateIndex(index);
	// DoublyLinkedSetInt<T> linked = getLinked(index);
	// if( linked != null ){
	// for( T t:c )
	// add(t);
	// }
	// return true;
	// }else{
	// return false;
	// }
	// }

	@Override
	public boolean removeAll(Collection<?> c) {
		if (c != null && c.size() > 0) {
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
		if (c != null && c.size() > 0) {
			DoublyLinkedSet<T> ref = this.right;
			while (ref != null) {
				if (ref == this) {
					break;
				} else if (!c.contains(ref.obj)) {
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

	// @Override
	// public T get(int index) {
	// validateIndex(index);
	// DoublyLinkedSetInt<T> ref = getLinked(index);
	// if(ref!=null)
	// return ref.obj;
	// else
	// return null;
	// }

	// private final DoublyLinkedSetInt<T> getLinked(int index) {
	// int idx = 0;
	// DoublyLinkedSetInt<T> ref = this;
	// while (ref != null) {
	// if(idx==index){
	// return ref;
	// }
	// ref = ref.right;
	// if (ref == this) {
	// break;
	// }
	// }
	// return null;
	// }

	// private final void validateIndex(int index) {
	// if( index >= size || index < 0 ) throw new
	// ArrayIndexOutOfBoundsException(index);
	// }

	// @Override
	// public T set(int index, T element) {
	// validateIndex(index);
	// DoublyLinkedSetInt<T> ref = getLinked(index);
	// if(ref!=null){
	// T obj2 = ref.obj;
	// ref.obj = element;
	// return obj2;
	// }else{
	// return null;
	// }
	// }

	// @Override
	// public void add(int index, T element) {
	// validateIndex(index);
	// DoublyLinkedSetInt<T> ref = getLinked(index);
	// if(ref!=null){
	// ref.add(element);
	// }
	// }
	//
	// @Override
	// public T remove(int index) {
	// DoublyLinkedSetInt<T> ref = getLinked(index);
	// if(ref!=null){
	// T obj2 = ref.obj;
	// ref.remove();
	// return obj2;
	// }else{
	// return null;
	// }
	// }
	//
	// @Override
	// public int indexOf(Object o) {
	// int idx = 0;
	// DoublyLinkedSetInt<T> ref = this;
	// while (ref != null) {
	// if(ref.obj==o){
	// return idx;
	// }
	// ref = ref.right;
	// idx++;
	// if(ref == null){
	// ref = this.right;
	// }else if (ref.obj == null) {
	// break;
	// }else if (ref == this) {
	// break;
	// }
	// }
	// return -1;
	// }
	//
	// @Override
	// public int lastIndexOf(Object o) {
	// DoublyLinkedSetInt<T> ref = this.left;
	// int idx = size;
	// while (ref != null) {
	// ref = ref.left;
	// idx--;
	// if (ref.obj == o) {
	// break;
	// }
	// }
	// return idx;
	// }
	//
	// @Override
	// public ListIterator<T> listIterator() {
	// final DoublyLinkedSetInt<T> ref = this;
	// return new ListIterator<T>() {
	// DoublyLinkedSetInt<T> cursor = ref;
	// int idx = 0;
	// @Override
	// public boolean hasNext() {
	// return !(cursor == ref.left);
	// }
	//
	// @Override
	// public T next() {
	// T t = cursor.obj;
	// cursor = cursor.left;
	// idx++;
	// return t;
	// }
	//
	// @Override
	// public boolean hasPrevious() {
	// return !(cursor == ref.right);
	// }
	//
	// @Override
	// public T previous() {
	// T t = cursor.obj;
	// cursor = cursor.right;
	// idx--;
	// return t;
	// }
	//
	// @Override
	// public int nextIndex() {
	// return idx+1;
	// }
	//
	// @Override
	// public int previousIndex() {
	// return idx-1;
	// }
	//
	// @Override
	// public void remove() {
	// DoublyLinkedSetInt<T> tref = cursor.right;
	// cursor.remove();
	// cursor = tref;
	// }
	//
	// @Override
	// public void set(T e) {
	// cursor.obj = e;
	// }
	//
	// @Override
	// public void add(T e) {
	// cursor.add(e);
	// }
	// };
	// }
	//
	// @Override
	// public ListIterator<T> listIterator(int index) {
	// validateIndex(index);
	// final DoublyLinkedSetInt<T> ref = getLinked(index);
	// return new ListIterator<T>() {
	// DoublyLinkedSetInt<T> cursor = ref;
	// int idx = 0;
	// @Override
	// public boolean hasNext() {
	// return !(cursor == ref.left);
	// }
	//
	// @Override
	// public T next() {
	// T t = cursor.obj;
	// cursor = cursor.left;
	// idx++;
	// return t;
	// }
	//
	// @Override
	// public boolean hasPrevious() {
	// return !(cursor == ref.right);
	// }
	//
	// @Override
	// public T previous() {
	// T t = cursor.obj;
	// cursor = cursor.right;
	// idx--;
	// return t;
	// }
	//
	// @Override
	// public int nextIndex() {
	// return idx+1;
	// }
	//
	// @Override
	// public int previousIndex() {
	// return idx-1;
	// }
	//
	// @Override
	// public void remove() {
	// DoublyLinkedSetInt<T> tref = cursor.right;
	// cursor.remove();
	// cursor = tref;
	// }
	//
	// @Override
	// public void set(T e) {
	// cursor.obj = e;
	// }
	//
	// @Override
	// public void add(T e) {
	// cursor.add(e);
	// }
	// };
	// }
	//
	// @Override
	// public List<T> subList(int fromIndex, int toIndex) {
	// validateIndex(toIndex);
	// validateIndex(fromIndex);
	// List<T> subl = new ArrayList<T>();
	// DoublyLinkedSetInt<T> ref = getLinked(fromIndex);
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

	// @Override
	// public int hashCode() {
	// final int prime = 31;
	// int result = 1;
	// result = prime * result + ((obj == null) ? 0 : obj.hashCode());
	// return result;
	// }
	//
	// @Override
	// public boolean equals(Object obj) {
	// if (this == obj)
	// return true;
	// if (obj == null)
	// return false;
	// if (getClass() != obj.getClass())
	// return false;
	// DoublyLinkedSet other = (DoublyLinkedSet) obj;
	// if (this.obj == null) {
	// if (other.obj != null)
	// return false;
	// } else if (!this.obj.equals(other.obj))
	// return false;
	// return true;
	// }
}

/**
 * @author P
 *
 */
enum CompareStrategy {
	REF, EQUALS {

		@Override
		boolean compare(Object o1, Object o2) {
			if (o1 != null)
				return o1.equals(o2);
			else if (o1 == null && o2 == null)
				return true;
			else
				return false;
		}

	};

	boolean compare(Object o1, Object o2) {
		return o1 == o2;
	}

}

/**
 * @author P
 *
 */
final class Counter {
	volatile int cnt = 0;

	void set(int v) {
		cnt = v;
	}

	int incrementAndGet() {
		return ++cnt;
	}

	int decrementAndGet() {
		return --cnt;
	}

	int get() {
		return cnt;
	}
}
/**
 * @author P
 *
 */
final class Ref {
	final DoublyLinkedSet<?> set;
	final DoublyLinkedStack<?> st;
	final DoublyLinkedList<?> lst;
	final Object obj;
	final int hashcode;
	int cnt = 1;
	
	/**
	 * @param set
	 */
	Ref(DoublyLinkedSet<?> set) {
		super();
		this.set = set;
		this.obj = set.obj;
		this.st = null;
		this.lst = null;
		if(this.obj==null)
			this.hashcode = 0;
		else
			this.hashcode = this.obj.hashCode();
	}
	
	/**
	 * @param stack
	 */
	Ref(DoublyLinkedList<?> lst) {
		super();
		this.set = null;
		this.obj = lst.obj;
		this.st = null;
		this.lst = lst;
		if(this.obj==null)
			this.hashcode = 0;
		else
			this.hashcode = this.obj.hashCode();
	}

	/**
	 * @param stack
	 */
	Ref(DoublyLinkedStack<?> stack) {
		super();
		this.set = null;
		this.obj = stack.obj;
		this.st = stack;
		this.lst = null;
		if(this.obj==null)
			this.hashcode = 0;
		else
			this.hashcode = this.obj.hashCode();
	}
	
	/**
	 * @param obj
	 */
	public Ref(Object obj) {
		super();
		this.set = null;
		this.obj = obj;
		this.st = null;
		this.lst = null;
		if(this.obj==null)
			this.hashcode = 0;
		else
			this.hashcode = this.obj.hashCode();
	}

	@Override
	public int hashCode() {
		return this.hashcode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Ref other = (Ref) obj;
		if (this.obj == null) {
			if (other.obj != null)
				return false;
		} else if (!this.obj.equals(other.obj))
			return false;
		return true;
	}

}