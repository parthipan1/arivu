/**
 * 
 */
package org.arivu.datastructure;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

import org.arivu.utils.NullCheck;
import org.arivu.utils.lock.AtomicWFReentrantLock;

/**
 * @author P
 *
 */
public final class Amap<K, V> implements Map<K, V>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -997810275912377568L;

	final Btree binaryTree;

	/**
	 */
	public Amap(Map<? extends K, ? extends V> m) {
		this(new AtomicWFReentrantLock());
		putAll(m);
	}
	
	/**
	 */
	public Amap() {
		this(new AtomicWFReentrantLock());
	}
	
	/**
	 * @param lock
	 */
	Amap(Lock lock) {
		this.binaryTree = new Btree(lock);
	}
	
	V nullValue;
	volatile int nc = 0;

	@Override
	public int size() {
		return binaryTree.size() + nc;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@SuppressWarnings("unchecked")
	AnEntry<K, V> getKeyWrap(Object key) {
		return new AnEntry<K, V>((K) key, null, null);
	}

	@Override
	public boolean containsKey(Object key) {
		return binaryTree.get(getKeyWrap(key)) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		for (Object e : binaryTree.getAll()) {
			@SuppressWarnings("unchecked")
			Entry<K, V> e1 = (Entry<K, V>) e;
			if (value != null && value.equals(e1.getValue()))
				return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {

		if (key == null)
			return nullValue;

		final Object object = binaryTree.get(getKeyWrap(key));
		if (object != null) {
			java.util.Map.Entry<K, V> e = (java.util.Map.Entry<K, V>) object;
			return e.getValue();
		} else {
			return null;
		}
	}

	@Override
	public V put(final K key, final V value) {

		if (key == null) {
			binaryTree.cas.lock();
			if (value == null) {
				nc = 0;
				this.nullValue = value;
			} else {
				nc = 1;
				this.nullValue = value;
			}
			binaryTree.cas.unlock();
			return nullValue;
		} else if (value == null) {
			binaryTree.remove(getKeyWrap(key));
			return value;
		}

		AnEntry<K, V> e = new AnEntry<K, V>(key, value, binaryTree);
		Object object = binaryTree.get(e);
		if (object != null) {
			@SuppressWarnings("unchecked")
			java.util.Map.Entry<K, V> e1 = (java.util.Map.Entry<K, V>) object;
			e1.setValue(value);
			return value;
		} else {
			binaryTree.add(e);
			return value;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
		if (key == null) {
			if (nullValue == null) {
				return null;
			} else {
				binaryTree.cas.lock();
				nc = 0;
				V value = this.nullValue;
				this.nullValue = null;
				binaryTree.cas.unlock();
				return value;
			}
		}

		final Object object = binaryTree.remove(getKeyWrap(key));
		if (object != null) {
			java.util.Map.Entry<K, V> e = (java.util.Map.Entry<K, V>) object;
			return e.getValue();
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		if (m != null) {
			@SuppressWarnings("rawtypes")
			Set entrySet = m.entrySet();
			for (Object o : entrySet) {
				Entry<? extends K, ? extends V> e = (Entry<? extends K, ? extends V>) o;
				put(e.getKey(), e.getValue());
			}
		}
	}

	Future<?> submitClear;
	@Override
	public void clear() {
		if(isEmpty()) return;
		
		binaryTree.cas.lock();
//		try{
			if (nc == 1) {
				nc = 0;
				nullValue = null;
			}
			final Collection<Object> all = binaryTree.getAll();
			binaryTree.clear();
			binaryTree.cas.unlock();
			if (!NullCheck.isNullOrEmpty(all)) {
				final ExecutorService exe = Executors.newFixedThreadPool(1);
				submitClear = exe.submit(new Runnable() {

					@Override
					public void run() {
						for (Object e : all) {
							AnEntry<?, ?> e1 = (AnEntry<?, ?>) e;
							e1.tree = null;
						}
						cancelSubmit();
						exe.shutdownNow();
					}
				});
			}
//		}finally{
			
//		}
	}

	@Override
	public Set<K> keySet() {
		DoublyLinkedSet<K> keys = new DoublyLinkedSet<K>();
		for (Object e : binaryTree.getAll()) {
			@SuppressWarnings("unchecked")
			Entry<K, V> e1 = (Entry<K, V>) e;
			keys.add(e1.getKey());
		}
		return keys;
	}

	@Override
	public Collection<V> values() {
		DoublyLinkedList<V> keys = new DoublyLinkedList<V>();
		for (Object e : binaryTree.getAll()) {
			@SuppressWarnings("unchecked")
			Entry<K, V> e1 = (Entry<K, V>) e;
			keys.add(e1.getValue());
		}
		return keys;
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		DoublyLinkedSet<Entry<K, V>> entries = new DoublyLinkedSet<Entry<K, V>>();
		for (Object e : binaryTree.getAll()) {
			@SuppressWarnings("unchecked")
			Entry<K, V> e1 = (Entry<K, V>) e;
			entries.add(e1);
		}
		return entries;
	}

	void cancelSubmit() {
		if (submitClear != null) {
			submitClear.cancel(true);
			submitClear = null;
		}
	}

	/**
	 * @author P
	 *
	 * @param <K>
	 * @param <V>
	 */
	static final class AnEntry<K, V> implements Entry<K, V>, Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = -5326227739123537044L;

		final K k;
		V v;
		Btree tree;

		/**
		 * @param k
		 * @param v
		 * @param tree
		 */
		AnEntry(K k, V v, Btree tree) {
			super();
			this.k = k;
			this.v = v;
			this.tree = tree;
		}

		@Override
		public K getKey() {
			return this.k;
		}

		@Override
		public V getValue() {
			return this.v;
		}

		@Override
		public V setValue(V value) {
			V v1 = this.v;
			this.v = value;
			if (value == null && tree != null) {
				tree.remove(AnEntry.this);
				tree=null;
			}
			return v1;
		}

		@Override
		public int hashCode() {
//			final int prime = 31;
//			int result = 1;
//			result = prime * result + ((k == null) ? 0 : k.hashCode());
			return ((k == null) ? 0 : k.hashCode());
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
			AnEntry other = (AnEntry) obj;
			if (k == null) {
				if (other.k != null)
					return false;
			} else if (!k.equals(other.k))
				return false;
			return true;
		}

	}

	// @Override
	// public boolean contains(Object o) {
	// return set.contains(o);
	// }
	//
	// @Override
	// public Iterator<Entry<K, V>> iterator() {
	// return set.iterator();
	// }
	//
	// @Override
	// public Object[] toArray() {
	// return set.toArray();
	// }
	//
	// @Override
	// public <T> T[] toArray(T[] a) {
	// return set.toArray(a);
	// }
	//
	// @Override
	// public boolean add(Entry<K, V> e) {
	// return set.add(e);
	// }
	//
	// @Override
	// public boolean containsAll(Collection<?> c) {
	// return set.containsAll(c);
	// }
	//
	// @Override
	// public boolean retainAll(Collection<?> c) {
	// return set.retainAll(c);
	// }
	//
	// @Override
	// public boolean removeAll(Collection<?> c) {
	// return set.removeAll(c);
	// }
	//
	// @Override
	// public boolean addAll(Collection<? extends Entry<K, V>> c) {
	// return set.addAll(c);
	// }
}
