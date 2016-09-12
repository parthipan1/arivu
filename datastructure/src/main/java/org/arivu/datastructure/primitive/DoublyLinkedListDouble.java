/**
 * 
 */
package org.arivu.datastructure.primitive;

import java.util.concurrent.atomic.AtomicInteger;

import org.arivu.utils.lock.AtomicWFLock;


/**
 * @author P
 *
 */
public final class DoublyLinkedListDouble  {
	/**
	 * 
	 */
	double obj;
	
	/**
	 * 
	 */
	DoublyLinkedListDouble left = this, right = this;
	
	
	AtomicInteger size;
	
//	static final AtomicLock cas = new AtomicLock();
	static final AtomicWFLock cas = new AtomicWFLock();
	/**
	 * @param writeLock TODO
	 * 
	 */
	public DoublyLinkedListDouble() {
		this(Double.MIN_VALUE,new AtomicInteger(0));
	}
	
	/**
	 * @param size TODO
	 * @param obj
	 */
	private DoublyLinkedListDouble(double t, AtomicInteger size) {
		super();
		this.obj = t;
		this.size = size;
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
	
	public double poll(){
		DoublyLinkedListDouble removeRight = removeRight();
		if(removeRight!=null)
			return removeRight.obj;
		return Double.MIN_VALUE;
	}
	
	/**
	 * @return
	 */
	DoublyLinkedListDouble removeRight(){
		final DoublyLinkedListDouble r = this.right;
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
	DoublyLinkedListDouble addLeft(final DoublyLinkedListDouble l) {
		if (l != null) {
			cas.lock();
			size.incrementAndGet();//++;
			l.right = this;
			DoublyLinkedListDouble tl = left;
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
	DoublyLinkedListDouble search(final double o){
		DoublyLinkedListDouble ref = this.right;
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
	
	
	public double remove() {
		return poll();
	}
	
	/**
	 * 
	 */
	private double removeRef() {
		DoublyLinkedListDouble tleft = left, tright = right;

		if (tleft != null)
			tleft.right = tright;

		if (tright != null)
			tright.left = tleft;

		left = null;
		right = null;
		size = null;
		return obj;
	}

	
	public double size() {
		return size.get();
	}

	
	public boolean contains(double o) {
		DoublyLinkedListDouble search = search(o);
		if( search == null )
			return false;
		else
			return true;
	}

	public double[] toArray() {
		int idx = 0;
		double[] arr = new double[size.get()];
		DoublyLinkedListDouble ref = this.right;
		while (ref != null) {
			if (ref == this) {
				break;
			}
			arr[idx++] = ref.obj;
			ref = ref.right;
		}
		return arr;
	}

	public boolean add(double e) {
		if(e!=Double.MIN_VALUE){
			addLeft(new DoublyLinkedListDouble(e, size));
			return true;
		}else{
			return false;
		}
	}

	
	public boolean removeObj(double o) {
		DoublyLinkedListDouble search = search(o);
		if( search!=null ){
			search.removeRef();
			size.decrementAndGet();
			return true;
		}else 
			return false;
	}

	
	public boolean containsAll(double[] c) {
		if( c!=null && c.length>0 ){
			boolean ret = true;
			for( double t:c )
				ret = ret && contains(t);
			return ret;
		}else{
			return false;
		}
	}

	
	public boolean addAll(double[] c) {
		if( c!=null && c.length>0 ){
			for( double t:c )
				add(t);

			return true;
		}else{
			return false;
		}
	}

	
	public boolean addAll(int index, double[] c) {
		if( c!=null && c.length>0 ){
			validateIndex(index);
			DoublyLinkedListDouble linked = getLinked(index);
			if( linked != null ){
				for( double t:c )
					linked.add(t);
			}
			return true;
		}else{
			return false;
		}
	}

	
	public boolean removeAll(double[] c) {
		if( c!=null && c.length>0 ){
			boolean ret = true;
			for( double t:c )
				ret = ret && removeObj(t);
			return ret;
		}else{
			return false;
		}
	}

	
	public boolean retainAll(double[] c) {
		boolean ret = false;
		if( c!=null && c.length>0 ){
			DoublyLinkedListDouble ref = this.right;
			while (ref != null) {
				if (ref == this) {
					break;
				}
				
				boolean ccon = false;
				for( double lc:c ){
					if(lc==ref.obj){
						ccon = true;
						break;
					}
				}
				
				if( !ccon ){
					DoublyLinkedListDouble ll = ref;
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

	
	public double get(int index) {
		validateIndex(index);
		DoublyLinkedListDouble ref = getLinked(index);
		if(ref!=null)
			return ref.obj;
		else
			return Double.MIN_VALUE;
	}

	private DoublyLinkedListDouble getLinked(int index) {
		int idx = 0;
		DoublyLinkedListDouble ref = this.right;
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

	
	public double set(int index, double element) {
		validateIndex(index);
		DoublyLinkedListDouble ref = getLinked(index);
		if(ref!=null){
			double obj2 = ref.obj;
			ref.obj = element;
			return obj2;
		}else{
			return Double.MIN_VALUE;
		}
	}

	
	public void add(int index, double element) {
		validateIndex(index);
		DoublyLinkedListDouble ref = getLinked(index);
		if(ref!=null){
			ref.add(element);
		}
	}

	
	public double remove(int index) {
		DoublyLinkedListDouble ref = getLinked(index);
		if(ref!=null){
			double obj2 = ref.obj;
			ref.removeRef();
			size.decrementAndGet();
			return obj2;
		}else{
			return Double.MIN_VALUE;
		}
	}

	
	public int indexOf(double o) {
		int idx = 0;
		DoublyLinkedListDouble ref = this.right;
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

	
	public int lastIndexOf(double o) {
		DoublyLinkedListDouble ref = this.left;
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
	
	public boolean offer(double e) {
		return add(e);
	}

	
	public double element() {
		return obj;
	}

	
	public double peek() {
		return right.obj;
	}
}
