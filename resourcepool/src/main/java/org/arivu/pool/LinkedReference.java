/**
 * 
 */
package org.arivu.pool;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author P
 *
 */
final class LinkedReference<T> {
	/**
	 * 
	 */
//	final T t;
	/**
	 * 
	 */
	final State<T> state;
	/**
	 * 
	 */
//	final AtomicBoolean available = new AtomicBoolean(false);

	/**
	 * 
	 */
	LinkedReference<T> left = this, right = this;

//	private Ref size;
	final AtomicInteger size;
	
//	T proxy;
//	volatile boolean released = false;
	
	/**
	 * 
	 */
	public LinkedReference() {
		super();
//		this.t = null;
		this.state = null;
		this.size = new AtomicInteger(0);
//		this.released = false;
	}

	/**
	 * @param t
	 */
	public LinkedReference(T t,AtomicInteger size) {
		super();
//		this.t = t;
		this.state = new State<T>(t);
		this.size = size;
	}

	/**
	 * 
	 */
	void clear() {
		left = this;
		right = this;
		this.size.set(0);
//		if(size!=null)
//			size.size = 0;
	}

	int size(){
//		if(size==null) return 0;
//		else return size.size;
		return size.get();
	}
	
	void incSize(){
		size.incrementAndGet();
//		if(size!=null)
//			size.size += 1;
	}
	
	/**
	 * @return
	 */
	LinkedReference<T> poll() {
		return removeRight();
	}

	/**
	 * @return
	 */
	private LinkedReference<T> removeRight() {
		final LinkedReference<T> r = this.right;
		if (r == this || r == null) {
			return null;
		} else {
			r.remove();
			return r;
		}
	}

	LinkedReference<T> add(final LinkedReference<T> l) {
		return addLeft(l);
	}

	/**
	 * @param l
	 * @return
	 */
	private LinkedReference<T> addLeft(final LinkedReference<T> l) {
		if (l != null) {
			l.right = this;
			LinkedReference<T> tl = left;
			this.left = l;
			
			if(l!=null)
				l.left = tl;
			
			if(tl != null)
				tl.right = l;
			
		}
		return l;
	}

	/**
	 * @param t
	 * @return
	 */
	LinkedReference<T> search(final T t) {
		LinkedReference<T> ref = this.right;
		while (ref != null) {
			if (ref.state.t == t) {
				return ref;
			}
			ref = ref.right;
			if (ref == null) {
				return null;
			} else if (ref.state == null) {
				break;
			} else if (ref == this) {
				break;
			}
		}
		return null;
	}

	/**
	 * 
	 */
	void remove() {
		LinkedReference<T> tleft = left, tright = right;
		if (tleft != null)
			tleft.right = tright;
		
		if (tright != null)
			tright.left = tleft;
		
		left = null;
		right = null;
	}
}
