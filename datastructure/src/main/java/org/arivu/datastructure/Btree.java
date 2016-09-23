/**
 * 
 */
package org.arivu.datastructure;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.arivu.utils.lock.AtomicWFReentrantLock;

/**
 * @author P
 *
 */
public final class Btree implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6344951761380914875L;

	private static final int DEFAULT_BASEPOWER = 2;
	/**
	 * @author P
	 *
	 */
	private static final class Node {
		final int order;
		Lock cas;
		final boolean leaf;
		final LinkedReference[] refs;
		final Node[] nodes;
		CompareStrategy compareStrategy;
		final Counter counter = new Counter();
		Node parent;
		
		/**
		 * @param order
		 * @param cas
		 * @param leaf
		 * @param compareStrategy
		 * @param parent
		 */
		Node(int order, Lock cas, boolean leaf, CompareStrategy compareStrategy, Node parent) {
			super();
			this.compareStrategy = compareStrategy;
			this.leaf = leaf;
			this.order = order;
			this.cas = cas;
			this.parent = parent;
			if (leaf) {
				this.refs = new LinkedReference[order];
				this.nodes = null;
			} else {
				this.refs = null;
				this.nodes = new Node[order];
			}
		}

		void clear() {
			cas.lock();
			resetNodes();
			counter.set(0);
			cas.lock();
		}

		private void resetNodes() {
			for (int i = 0; i < this.nodes.length; i++){
				if(this.nodes[i]!=null){
					Node n = this.nodes[i];
					this.nodes[i] = null;
					n.parent = null;
					n.cas = null;
					n.compareStrategy = null;
				}
			}
		}
		
		private void resetLeaves() {
			for (int i = 0; i < this.refs.length; i++){
				this.refs[i] = null;
			}
		}
		
//		@SuppressWarnings("unchecked")
		boolean add(final Object obj, final int level, final int[] arr) {
			boolean add = false;
			cas.lock();
			if (this.leaf) {
				LinkedReference ref = refs[arr[level]];
				if (ref == null) {
					ref = new LinkedReference(compareStrategy, this.cas);//DoublyLinkedList<Object>(compareStrategy, dummyLock);
					refs[arr[level]] = ref;
				}
				add = ((LinkedReference) ref).add(obj,null);
			} else {
				Node n = nodes[arr[level]];
				if (n == null) {
					n = new Node(order, cas, level == arr.length - 2, compareStrategy, Node.this);
					nodes[arr[level]] = n;
				}
				add = n.add(obj, level + 1, arr);
			}

			if (add)
				this.counter.incrementAndGet();

			cas.unlock();

			return add;
		}

		Object find(final Object obj, final int level, final int[] arr) {
			if (this.leaf) {
//				@SuppressWarnings("unchecked")
				final LinkedReference ref = (LinkedReference) refs[arr[level]];
				if (ref == null) {
					return null;
				}
				final LinkedReference search = ref.search(obj,Direction.left);
				// System.out.println(" find ref "+ref+" obj "+obj+" search
				// "+search);
				if (search == null)
					return null;
				else
					return search.obj;
			} else {
				final Node n = nodes[arr[level]];
				if (n == null) {
					return null;
				}
				return n.find(obj, level + 1, arr);
			}
		}

		Object remove(final Object obj, final int level, final int[] arr,List<Node> rns) {
			if (this.leaf) {
//				@SuppressWarnings("unchecked")
				final LinkedReference ref = (LinkedReference) refs[arr[level]];
				if (ref == null) {
					return null;
				}
				cas.lock();
				final LinkedReference search = ref.search(obj,Direction.left);
				if (search == null) {
					cas.unlock();
					return null;
				} else {
					final Object removeRef = search.remove();
					int cnt = this.counter.decrementAndGet();
					if(cnt==0){
						if(level>0 && parent!=null){
							rns.add(parent.nodes[arr[level-1]]);
							parent.nodes[arr[level-1]] = null;
						}else{
							resetLeaves();
						}
					}else{
						if (ref.size(false, Direction.right) == 0) {
							refs[arr[level]] = null;
						}
					}
					cas.unlock();
					return removeRef;
				}
			} else {
				final Node n = nodes[arr[level]];
				if (n == null) {
					return null;
				}
				cas.lock();
				Object remove = n.remove(obj, level + 1, arr, rns);
				if (remove != null && this.counter.decrementAndGet() == 0) {
					if(level>0 && parent!=null){
						rns.add(parent.nodes[arr[level-1]]);
						parent.nodes[arr[level-1]] = null;
					}else{
						resetNodes();
					}
//					nodes[arr[level]] = null;
				}
				cas.unlock();
				return remove;
			}
		}

//		@SuppressWarnings("unchecked")
		Collection<Object> getAll() {
			Collection<Object> list = new DoublyLinkedList<Object>();
			if (this.nodes == null) {
				for (final LinkedReference n : this.refs) {
					if (n != null){
						LinkedReference ref = n;
						while (ref != null) {
							ref = Direction.left.get(ref);
							if (ref == n) {
								break;
							}
							list.add(ref.obj);
						}
					}
//						list.addAll((DoublyLinkedList<Object>) n);
				}

			} else {
				for (Node n : this.nodes) {
					if (n != null)
						list.addAll(n.getAll());
				}
			}
			return list;
		}
	}

	final Node root;
	private final int base;
	private final int height;
	private final long baseMask;
	final Lock cas;
	/**
	 * @param order
	 */
	public Btree(int basePower) {
		this(basePower, new AtomicWFReentrantLock(), CompareStrategy.EQUALS);
	}

	/**
	 * 
	 */
	public Btree() {
		this(DEFAULT_BASEPOWER);
	}

	/**
	 * @param lock
	 */
	Btree(Lock lock) {
		this(DEFAULT_BASEPOWER, lock, CompareStrategy.EQUALS);
	}

	/**
	 * @param lock
	 * @param compareStrategy
	 */
	Btree(Lock lock, CompareStrategy compareStrategy) {
		this(DEFAULT_BASEPOWER, lock, compareStrategy);
	}

	/**
	 * @param basePower
	 * @param lock
	 * @param compareStrategy
	 */
	Btree(int basePower, Lock lock, CompareStrategy compareStrategy) {
		super();
		if(basePower<=0||basePower>4){
			throw new IllegalArgumentException("Invalid basePower "+basePower+" (between 1-4) specified!");
		}
		this.base = (int) Math.pow(2, basePower);
		this.height = 32/base;
		this.baseMask = ((long) Math.pow(2, base) - 1);
		this.root = new Node((int)this.baseMask+1, lock, false, compareStrategy, null);
		this.cas = lock;
	}

	private int[] getPath(Object obj) {
		return getPath(obj.hashCode());
	}

	private int[] getPath(int hashCode) {
		int[] ret = new int[height];
		for (int i = height-1; i >= 0; i--) {
			ret[i] = (int) (hashCode & baseMask);

			if (i > 0)
				hashCode = hashCode >>> base;
		}
		// //System.out.println(" getPath act hashCode "+hashCode2+" ret
		// "+con(ret));
		return ret;
	}
	
	public int getHeight() {
		return height;
	}

	public int size() {
		return root.counter.get();
	}

	public void add(final Object obj) {
		if( obj== null ) return;
		int[] path = getPath(obj);
		root.add(obj, 0, path);
		// System.out.println(this+" bt add "+obj+" path "+con(path));
	}

	public Object remove(final Object obj) {
		if( obj== null ) return null;
		// System.out.println(this+" bt remove "+obj);
		List<Node> rns = new DoublyLinkedList<Node>();
		Object remove = root.remove(obj, 0, getPath(obj), rns);
		for(Node n:rns){
			n.parent = null;
			n.cas = null;
			n.compareStrategy = null;
		}
		rns.clear();
		return remove;
	}

	public Object get(final Object obj) {
		if( obj== null ) return null;
		int[] path = getPath(obj);
		Object find = root.find(obj, 0, path);
		// System.out.println(this+" bt search "+obj+" find "+find+" path
		// "+con(path));
		return find;
	}

	public void clear() {
		root.clear();
	}

	public Collection<Object> getAll() {
		return root.getAll();
	}

	// private static final String con(int[] a) {
	// StringBuffer b = new StringBuffer();
	//
	// for (int i : a) {
	// if (b.length() == 0)
	// b.append(i);
	// else
	// b.append(",").append(i);
	// }
	//
	// return b.toString();
	// }
	//
	// public static void main(String[] args) {
	//
	// //System.out.println(con(getPath(Integer.MIN_VALUE)));
	//
	//// List<Integer> list = new ArrayList<Integer>();
	//// list.add(0);
	//// list.add(1);
	//// list.add(4);
	//// list.add(8);
	//// list.add(9);
	////
	//// Collections.sort(list);
	////
	//// for (Integer i : list)
	//// System.out.print(i);
	//// //System.out.println();
	////
	//// //System.out.println("search 0 " + Collections.binarySearch(list, 0));
	//// //System.out.println("search 1 " + Collections.binarySearch(list, 1));
	//// //System.out.println("search 4 " + Collections.binarySearch(list, 4));
	//// int idx = Collections.binarySearch(list, 3);
	//// idx = -1 * (idx + 1);
	//// //System.out.println("search 3 " + idx);
	//
	//// Object[] a = new Object[7];
	////
	//// a[0] = 3;
	//// a[1] = 1;
	//// a[2] = 2;
	//// a[3] = 0;
	////
	//// for(Object o:a){
	//// if(o!=null)
	//// System.out.print(o.toString());
	//// }
	//// //System.out.println();
	//// Arrays.sort(a, defaultComparator);
	////
	//// for(Object o:a){
	//// if(o!=null)
	//// System.out.print(o.toString());
	//// }
	//// //System.out.println();
	// }
	//
}
/**
 * Circular buffer to store all the logs and consumers.
 * 
 * @author P
 *
 */
final class LinkedReference {
	/**
	 * 
	 */
	Object obj;

	/**
	 * 
	 */
	volatile LinkedReference left = this, right = this;
	
	volatile LinkedReference write = null;
	volatile LinkedReference read = null;
	
	Lock lock = null;
	CompareStrategy compareStrategy;
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
//			LinkedReference l = new LinkedReference();
//			l.id = String.valueOf(i);
//			if(addLock)
//				l.lock = new AtomicWFReentrantLock();
//			add(l,Direction.left);
//		}
//		
//	}

	boolean isEmptyRing(){
		LinkedReference ref = this;
		while (ref != null) {
			if(ref.obj!=null){
				return false;
			}
			ref = ref.right;
		}
		return true;
	}
	
	/**
	 * @param compareStrategy TODO
	 * 
	 */
	LinkedReference(CompareStrategy compareStrategy, Lock lock) {
		this(null, compareStrategy, lock);
	}

	/**
	 * @param compareStrategy TODO
	 * @param lock TODO
	 * @param obj
	 */
	private LinkedReference(Object t, CompareStrategy compareStrategy, Lock lock) {
		super();
		this.obj = t;
		this.lock = lock;
		this.compareStrategy = compareStrategy;
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
	Object poll(final LinkedReference garbage,final Direction dir) {
		LinkedReference removeRight = remove(dir);
		if (removeRight != null) {
			Object obj2 = removeRight.obj;
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
	LinkedReference remove(final Direction dir) {
		final LinkedReference r = dir.get(this);//this.right;
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
	LinkedReference search(final Object o, final Direction direction){
		LinkedReference ref = this;
		while (ref != null) {
//			if( o instanceof String){
//				if(o.equals(ref.obj)){
//					return ref;
//				}
//			}else{
				if(this.compareStrategy.compare(ref.obj, o) ){
					return ref;
				}
//			}
			
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
	boolean add(final Object t,final LinkedReference garbage) {
		if (t != null){
			if (garbage!=null) {
				LinkedReference newref = garbage.remove(Direction.right);
				if (newref == null) {
					newref = new LinkedReference(t, compareStrategy, lock);
				} else {
					newref.obj = t;
				}
				add(newref, Direction.left);
				return true;
			}else{
				add(new LinkedReference(t, compareStrategy, lock), Direction.left);
				return true;
			}
		}
		return false;
	}

	/**
	 * @param l
	 * @param direction TODO
	 * @return
	 */
	private LinkedReference add(final LinkedReference l,final Direction direction) {
		if (l != null) {
			Lock lk = lock;
			lk.lock();
			direction.getOther().set(l, this);
			final LinkedReference tl = direction.get(this);
			direction.set(this, l);
			direction.set(l, tl);
			direction.getOther().set(tl, l);
			lk.unlock();
		}
		return l;
	}

	/**
	 * @param direction TODO
	 * @return
	 */
	Object[] toArray(final Direction direction) {
		List<Object> subl = new DoublyLinkedList<Object>();
		LinkedReference ref = direction.get(this);//this.right;
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
	Object remove() {
		Object o = obj;
		Lock l = lock;
		l.lock();
		Direction.right.set(left, right);
		Direction.left.set(right, left);
		Direction.right.set(this, null);
		Direction.left.set(this, null);
		lock = null;
		compareStrategy = null;
		obj = null;
		l.unlock();
		return o;
	}

	/**
	 * @param includeNull TODO
	 * @param direction TODO
	 * @return
	 */
	int size(final boolean includeNull, final Direction direction) {
		int cnt = 0;
		LinkedReference ref = direction.get(this);
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
			LinkedReference tref = ref;
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
		 LinkedReference get(final LinkedReference ref) {
			if(ref==null){
				return null;
			}else{
				return ref.left;
			}
		}
		
		@Override
		 void set(final LinkedReference ref,final LinkedReference next){
			if(ref!=null && next!=null ){
				ref.left = next;
			}
		}
		
	},right;
	
	 LinkedReference get(final LinkedReference ref){
		if(ref==null){
			return null;
		}else{
			return ref.right;
		}
	}
	
	 void set(final LinkedReference ref,final LinkedReference next){
		if(ref!=null && next!=null ){
			ref.right = next;
		}
	}
	
	 LinkedReference remove(final LinkedReference ref){
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