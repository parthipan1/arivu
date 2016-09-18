/**
 * 
 */
package org.arivu.log.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.arivu.datastructure.DoublyLinkedList;

/**
 * Circular buffer to store all the logs and consumers.
 * 
 * @author P
 *
 */
final class LinkedReference<T> {
	/**
	 * 
	 */
	T obj;

	/**
	 * 
	 */
	volatile LinkedReference<T> left = this, right = this;
	
	volatile LinkedReference<T> write = null;
	volatile LinkedReference<T> read = null;
	
	Lock lock = null;
	
	String id;
	
//	public LinkedReference(int size, boolean addLock) {
//		super();
//		this.obj = null;
//		this.write = this;
//		this.read = this;
//		this.id = "0";
//		
//		if(addLock)
//			this.lock = new AtomicWFReentrantLock();//new ReentrantLock(true);
//		
//		for(int i=1;i<size;i++){
//			LinkedReference<T> l = new LinkedReference<T>();
//			l.id = String.valueOf(i);
//			if(addLock)
//				l.lock = new AtomicWFReentrantLock();
//			add(l,Direction.left);
//		}
//		
//	}

	boolean isEmptyRing(){
		LinkedReference<T> ref = this;
		while (ref != null) {
			if(ref.obj!=null){
				return false;
			}
			ref = ref.right;
		}
		return true;
	}
	
	/**
	 * 
	 */
	public LinkedReference() {
		this(null);
	}

	/**
	 * @param obj
	 */
	public LinkedReference(T t) {
		super();
		this.obj = t;
	}

	/**
	 * 
	 */
	void clear() {
		left = this;
		right = this;
	}

	/**
	 * @return
	 */
	boolean isEmpty() {
		return size(false, Direction.left)==0;
	}

	/**
	 * @param rwLock TODO
	 * @return
	 */
	T poll(final LinkedReference<T> garbage,final Direction dir) {
		LinkedReference<T> removeRight = remove(dir);
		if (removeRight != null) {
			T obj2 = removeRight.obj;
			removeRight.obj = null;
			if(garbage!=null){
				garbage.add(removeRight, Direction.left);
			}
			return obj2;
		}
		return null;
	}

	/**
	 * @return
	 */
	LinkedReference<T> remove(final Direction dir) {
		final LinkedReference<T> r = dir.get(this);//this.right;
		if (r == this || r == null) {
			return null;
		} else {
			r.remove();
			return r;
		}
	}
	
	/**
	 * @param direction TODO
	 * @param obj
	 * @return
	 */
	LinkedReference<T> search(final T o, final Direction direction){
		LinkedReference<T> ref = this;
		while (ref != null) {
			if(ref.obj!=null && ref.obj==o){
				return ref;
			}
			ref = direction.get(ref);
			if (ref == this) {
				break;
			}
		}
		return null;
	}
	
	/**
	 * @param t
	 * @param rwLock TODO
	 * @return
	 */
	T add(final T t,final LinkedReference<T> garbage) {
		if (t != null){
			if (garbage!=null) {
				LinkedReference<T> newref = garbage.remove(Direction.right);
				if (newref == null) {
					newref = new LinkedReference<T>(t);
				} else {
					newref.obj = t;
				}
				add(newref, Direction.left);
			}else{
				add(new LinkedReference<T>(t), Direction.left);
			}
		}
		return t;
	}

	/**
	 * @param l
	 * @param direction TODO
	 * @return
	 */
	private LinkedReference<T> add(final LinkedReference<T> l,final Direction direction) {
		if (l != null) {
			direction.getOther().set(l, this);
			final LinkedReference<T> tl = direction.get(this);
			direction.set(this, l);
			direction.set(l, tl);
			direction.getOther().set(tl, l);
		}
		return l;
	}

	/**
	 * @param direction TODO
	 * @return
	 */
	Object[] toArray(final Direction direction) {
		List<T> subl = new DoublyLinkedList<T>();
		LinkedReference<T> ref = direction.get(this);//this.right;
		while (ref != null) {
			if (ref == this) {
				break;
			}
			if (ref.obj!=null) {
				subl.add(ref.obj);
			}
			ref = direction.get(ref);//ref.right;
		}
		return subl.toArray();
	}

	/**
	 * 
	 */
	void remove() {
		Direction.right.set(left, right);
		Direction.left.set(right, left);
		Direction.right.set(this, null);
		Direction.left.set(this, null);
	}

	/**
	 * @param includeNull TODO
	 * @param direction TODO
	 * @return
	 */
	int size(final boolean includeNull, final Direction direction) {
		int cnt = 0;
		LinkedReference<T> ref = direction.get(this);
		while (ref != null) {
			if (ref == this) {
				return cnt;
			}
			if (includeNull) {
				cnt++;
			}else{
				if (ref.obj != null) {
					cnt++;
				} 
			}
			LinkedReference<T> tref = ref;
			ref = direction.get(ref);
			if( tref == ref ) break;
//			System.out.println(" size ref "+ref+" left "+ref.left+" this "+this+" cnt "+cnt);
		}
		return cnt;
	}
}
enum Direction{
	left{

		@Override
		<T> LinkedReference<T> get(final LinkedReference<T> ref) {
			if(ref==null){
				return null;
			}else{
				return ref.left;
			}
		}
		
		@Override
		<T> void set(final LinkedReference<T> ref,final LinkedReference<T> next){
			if(ref!=null && next!=null ){
				ref.left = next;
			}
		}
		
	},right;
	
	<T> LinkedReference<T> get(final LinkedReference<T> ref){
		if(ref==null){
			return null;
		}else{
			return ref.right;
		}
	}
	
	<T> void set(final LinkedReference<T> ref,final LinkedReference<T> next){
		if(ref!=null && next!=null ){
			ref.right = next;
		}
	}
	
	<T> LinkedReference<T> remove(final LinkedReference<T> ref){
		if(ref==null){
			return null;
		}else{
			return ref.remove(this);
		}
	}
	
	Direction getOther(){
		if( this==Direction.right )
			return left;
		else
			return right;
	}
}
