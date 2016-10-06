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
 * A hash table supporting full concurrency of retrievals and adjustable expected concurrency for updates. This class obeys the same functional specification as java.util.Hashtable, and includes versions of methods corresponding to each method of Hashtable. However, even though all operations are thread-safe, retrieval operations do not entail locking, and there is not any support for locking the entire table in a way that prevents all access. This class is fully interoperable with Hashtable in programs that rely on its thread safety but not on its synchronization details.
 * Retrieval operations (including get) generally do not block. Retrievals reflect the results of the most recently completed update operations holding upon their onset. For aggregate operations such as putAll and clear, concurrent retrievals may reflect insertion or removal of all entries. Similarly, Iterators and Enumerations return elements reflecting the state of the hash table at the point of the iterator/enumeration. They do not throw java.util.ConcurrentModificationException. However, iterators are designed to be thread safe.
 * This class and its views and iterators implement all of the optional methods of the java.util.Map and java.util.Iterator interfaces.
 * This class does allow null to be used as a key or value.<br>
 * 
 *  <img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAmwAAAE2CAYAAADcaM4pAAAABmJLR0QA/wD/AP+gvaeTAAAAB3RJTUUH2QgECwwCCBn6EAAAIABJREFUeJzs3Xd4FNX+x/H3BkhIQhdBUQTpUkSKl6uCinIFpIh6gaBcFaUoRXpTQAVFEZGiSBexUEWRKwYRBRW45lpQQSBAwk/aNRQBgYQEkv39Mbub3WSTLZnNbpbP63n2SWbOzJmzQzb5cuac7wERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERkSKnA5AIZAJWNy8RERERCbIjGIHZP4AIp/0K2ERERERCRF6BmQK2EBLh+RAREREx2T3At8B5IBXYYttn9xNGsPRgjvP+adv/k9O+lsAXwF/ABeAHoGuO8+zBVwwwCzhG9iPQnMd4CtI8tR0gyVZPE6d939hedk1txyR5uJ6IiIhIobsHI1jaDFwNVAY22fbZA58BGMHMv3Oc+6ltf3/b9p3ARVtdVYHSwELbMY84nWcPxIYB9QCLm7Kc3O33pu0Ac2znDrRtlwcu2V7lbfueth0zx821RURERIJqC0agcrPTvptt+761bZcD0jCCscq2fVfZtlNt5QBf285r5FRXBdu+RKd99uCroZv2+BKwedN2gPtt+5bZth9yqu8h274Vtu373VxbREREJKhSMQKVKKd9UbZ9qU773ie7VwxguG37PadjzuN+ZqcVozfLzr7P3VAoXwI2b9teFiO4PGjbXorx6DPZ9j3AYdsxZd1cW0RERCSovA167rTt+8W2/att+06nY+wBm6egJ7+xaYEI2AC22vZfD5wEXgWmAn8CtWxlWz20W0RERCQo7I8Vmzvtc/dY0QLss+3vY/u6D9fxZ5ts+zt5uKZZAZu3bQd4zrb/LdvXFsDfbd/Pt319zkO7RURERILCPnD/K4xxaZWAL8k9cB9gLNm9V1bbtrPbgHRgL9AYKA5cBzyKEVzZmRWw+dL2W2znZwCHMAJNC9mPQq22Y0RERERCkj01RqrttRVo6+a4Khhj0exj0q52c0xz4GPgBEYgdAT4ALjd6RizAjZf2l4MOGWrY6bT/lm2fadsx4iIiIiIiIiIiIiI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" />
 * 
 * Parameters:
 * <K> the type of keys maintained by this map
 * <V> the type of mapped values
 * Author(s):
 * Mr P
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
		this();
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
		
		final Lock lock = binaryTree.cas;
		lock.lock();
//		try{
			if (nc == 1) {
				nc = 0;
				nullValue = null;
			}
			final Collection<Object> all = binaryTree.getAll();
			binaryTree.clear();
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
//		}catch(Throwable  e){
//			e.printStackTrace();
//		}finally{
			lock.unlock();
//		}
	}
	
	void cancelSubmit() {
		if (submitClear != null) {
			submitClear.cancel(true);
			submitClear = null;
		}
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
