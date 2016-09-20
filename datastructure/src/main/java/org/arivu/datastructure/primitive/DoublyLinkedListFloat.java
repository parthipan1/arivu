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
public final class DoublyLinkedListFloat  {
	/**
	 * 
	 */
	float obj;
	
	/**
	 * 
	 */
	DoublyLinkedListFloat left = this, right = this;
	
	
	AtomicInteger size;
	Lock cas;
//	static final AtomicLock cas = new AtomicLock();
//	static final AtomicWFLock cas = new AtomicWFLock();
	
	/**
	 * @param writeLock TODO
	 * 
	 */
	public DoublyLinkedListFloat() {
		this(new AtomicWFReentrantLock());
	}
	
	DoublyLinkedListFloat(Lock cas) {
		this(Float.MIN_VALUE,new AtomicInteger(0), cas);
	}
	
	/**
	 * @param size TODO
	 * @param cas TODO
	 * @param obj
	 */
	private DoublyLinkedListFloat(float t, AtomicInteger size, Lock cas) {
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
	
	public void clear(){
		left = this;
		right = this;
		size.set(0);// = 0;
	}
	
	/* (non-Javadoc)
	 * @see java.util.List#isEmpty()
	 */
	
	public boolean isEmpty(){
		return this == left;
	}
	
	/**
	 * @return
	 */
	
	public float poll(){
		DoublyLinkedListFloat removeRight = removeRight();
		if(removeRight!=null)
			return removeRight.obj;
		return Float.MIN_VALUE;
	}
	
	/**
	 * @return
	 */
	DoublyLinkedListFloat removeRight(){
		final DoublyLinkedListFloat r = this.right;
		if( r == this || r == null ){
			return null;
		}else{
			cas.lock();
			r.removeRef();
			size.decrementAndGet();
			cas.unlock();
			return r;
		}
	}
	
	/**
	 * @param l
	 * @return
	 */
	DoublyLinkedListFloat addLeft(final DoublyLinkedListFloat l) {
		if (l != null) {
			cas.lock();
			size.incrementAndGet();//++;
			l.right = this;
			DoublyLinkedListFloat tl = left;
			this.left = l;
			l.left = tl;
			tl.right = l;
			cas.unlock();
		}
		return l;
	}
	
	/**
	 * @param obj
	 * @return
	 */
	DoublyLinkedListFloat search(final float o){
		DoublyLinkedListFloat ref = this.right;
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
	
	
	public float remove() {
		return poll();
	}
	
	/**
	 * 
	 */
	private float removeRef() {
		DoublyLinkedListFloat tleft = left, tright = right;

		if (tleft != null)
			tleft.right = tright;

		if (tright != null)
			tright.left = tleft;

		left = null;
		right = null;
		size = null;
		return obj;
	}

	
	public float size() {
		return size.get();
	}

	
	public boolean contains(float o) {
		DoublyLinkedListFloat search = search(o);
		if( search == null )
			return false;
		else
			return true;
	}
	
	public float[] toArray() {
		int idx = 0;
		float[] arr = new float[size.get()];
		DoublyLinkedListFloat ref = this.right;
		while (ref != null) {
			if (ref == this) {
				break;
			}
			arr[idx++] = ref.obj;
			ref = ref.right;
		}
		return arr;
	}

	public boolean add(float e) {
		if(e!=Float.MIN_VALUE){
			addLeft(new DoublyLinkedListFloat(e, size, cas));
			return true;
		}else{
			return false;
		}
	}

	
	public boolean removeObj(float o) {
		DoublyLinkedListFloat search = search(o);
		if( search!=null ){
			search.removeRef();
			size.decrementAndGet();
			return true;
		}else 
			return false;
	}

	
	public boolean containsAll(float[] c) {
		if( c!=null && c.length>0 ){
			boolean ret = true;
			for( float t:c )
				ret = ret && contains(t);
			return ret;
		}else{
			return false;
		}
	}

	
	public boolean addAll(float[] c) {
		if( c!=null && c.length>0 ){
			for( float t:c )
				add(t);

			return true;
		}else{
			return false;
		}
	}

	
	public boolean addAll(int index, float[] c) {
		if( c!=null && c.length>0 ){
			validateIndex(index);
			DoublyLinkedListFloat linked = getLinked(index);
			if( linked != null ){
				for( float t:c )
					linked.add(t);
			}
			return true;
		}else{
			return false;
		}
	}

	
	public boolean removeAll(float[] c) {
		if( c!=null && c.length>0 ){
			boolean ret = true;
			for( float t:c )
				ret = ret && removeObj(t);
			return ret;
		}else{
			return false;
		}
	}

	
	public boolean retainAll(float[] c) {
		boolean ret = false;
		if( c!=null && c.length>0 ){
			DoublyLinkedListFloat ref = this.right;
			while (ref != null) {
				if (ref == this) {
					break;
				}
				
				boolean ccon = false;
				for( float lc:c ){
					if(lc==ref.obj){
						ccon = true;
						break;
					}
				}
				
				if( !ccon ){
					DoublyLinkedListFloat ll = ref;
					ref = ref.left;
					ll.removeRef();
					size.decrementAndGet();
					ret = true;
				}
				ref = ref.right;
			}
		}
		return ret;
	}

	
	public float get(int index) {
		validateIndex(index);
		DoublyLinkedListFloat ref = getLinked(index);
		if(ref!=null)
			return ref.obj;
		else
			return Float.MIN_VALUE;
	}

	private DoublyLinkedListFloat getLinked(int index) {
		int idx = 0;
		DoublyLinkedListFloat ref = this.right;
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

	
	public float set(int index, float element) {
		validateIndex(index);
		DoublyLinkedListFloat ref = getLinked(index);
		if(ref!=null){
			float obj2 = ref.obj;
			ref.obj = element;
			return obj2;
		}else{
			return Float.MIN_VALUE;
		}
	}

	
	public void add(int index, float element) {
		validateIndex(index);
		DoublyLinkedListFloat ref = getLinked(index);
		if(ref!=null){
			ref.add(element);
		}
	}

	
	public float remove(int index) {
		DoublyLinkedListFloat ref = getLinked(index);
		if(ref!=null){
			float obj2 = ref.obj;
			ref.removeRef();
			size.decrementAndGet();
			return obj2;
		}else{
			return Float.MIN_VALUE;
		}
	}

	
	public int indexOf(float o) {
		int idx = 0;
		DoublyLinkedListFloat ref = this.right;
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

	
	public int lastIndexOf(float o) {
		DoublyLinkedListFloat ref = this.left;
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

	public boolean offer(float e) {
		return add(e);
	}

	
	public float element() {
		return obj;
	}

	
	public float peek() {
		return right.obj;
	}
}
