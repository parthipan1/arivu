/**
 * 
 */
package org.arivu.datastructure;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author P
 *
 */
public final class Amap<K, V> implements Map<K, V>, Serializable {// ,
																	// Set<Map.Entry<K,V>>

	/**
	 * 
	 */
	private static final long serialVersionUID = -997810275912377568L;

	final Btree binaryTree = new Btree();

	@Override
	public int size() {
		return binaryTree.counter.get();// set.size();
	}

	@Override
	public boolean isEmpty() {
		return binaryTree.counter.get() == 0;
	}

	@SuppressWarnings("unchecked")
	AnEntry<K, V> getKeyWrap(Object key) {
		return new AnEntry<K, V>((K) key, null);
	}

	@Override
	public boolean containsKey(Object key) {
		// return set.contains(getKeyWrap(key));
		return binaryTree.get(getKeyWrap(key)) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		// DoublyLinkedSet<Entry<K, V>> ref = set.right;
		// while (ref != null) {
		// if (ref == set) {
		// break;
		// }
		// if( value instanceof String ){
		// if(ref.obj!=null &&
		// CompareStrategy.EQUALS.compare(ref.obj.getValue(), value)){
		// return true;
		// }
		// }else{
		// if(ref.obj!=null && CompareStrategy.REF.compare(ref.obj.getValue(),
		// value)){
		// return true;
		// }
		// }
		// ref = ref.right;
		// }
		for (Object e : binaryTree.getAll()) {
			@SuppressWarnings("unchecked")
			Entry<K, V> e1 = (Entry<K, V>)e; 
//			keys.add(e1.getKey());
			if( value!=null &&  value.equals(e1.getValue()) )
				return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		final Object object = binaryTree.get(getKeyWrap(key));
		if (object != null) {
			java.util.Map.Entry<K, V> e = (java.util.Map.Entry<K, V>) object;
			return e.getValue();
		} else {
			return null;
		}
		// cas.lock();
		// DoublyLinkedSet<java.util.Map.Entry<K, V>> search =
		// set.search(getKeyWrap(key));
		// cas.unlock();
		// if( search==null )
		// return null;
		// else
		// return search.obj.getValue();
	}

	@Override
	public V put(K key, V value) {
		// cas.lock();
		AnEntry<K, V> e = new AnEntry<K, V>(key, value);
		if (binaryTree.get(e) != null) {
			return null;
		} else {
			binaryTree.add(e);
			return value;
		}
		// set.add(e);
		// cas.unlock();
		// return value;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
		final Object object = binaryTree.remove(getKeyWrap(key));
		if (object != null) {
			java.util.Map.Entry<K, V> e = (java.util.Map.Entry<K, V>) object;
			return e.getValue();
		} else {
			return null;
		}
		// cas.lock();
		// final DoublyLinkedSet<java.util.Map.Entry<K, V>> search =
		// set.search(getKeyWrap(key));
		// if( search==null ){
		// cas.unlock();
		// return null;
		// }else{
		// search.removeRef();
		// cas.unlock();
		// return search.obj.getValue();
		// }
	}

	@SuppressWarnings("unchecked")
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		if (m != null) {
			@SuppressWarnings("rawtypes")
			Set entrySet = m.entrySet();
//			cas.lock();
			for (Object o : entrySet) {
				Entry<? extends K, ? extends V> e = (Entry<? extends K, ? extends V>) o;
				put(e.getKey(), e.getValue());
			}
//			cas.unlock();
		}
	}

	@Override
	public void clear() {
		// cas.lock();
		// set.clear();
		// cas.unlock();
		binaryTree.clear();
	}

	@Override
	public Set<K> keySet() {
		DoublyLinkedSet<K> keys = new DoublyLinkedSet<K>();
		for (Object e : binaryTree.getAll()) {
			@SuppressWarnings("unchecked")
			Entry<K, V> e1 = (Entry<K, V>)e; 
			keys.add(e1.getKey());
		}
		return keys;
	}

	@Override
	public Collection<V> values() {
		DoublyLinkedList<V> keys = new DoublyLinkedList<V>();
		for (Object e : binaryTree.getAll()) {
			@SuppressWarnings("unchecked")
			Entry<K, V> e1 = (Entry<K, V>)e; 
			keys.add(e1.getValue());
		}
		return keys;
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		DoublyLinkedSet<Entry<K, V>> entries = new DoublyLinkedSet<Entry<K, V>>();
		for (Object e : binaryTree.getAll()) {
			@SuppressWarnings("unchecked")
			Entry<K, V> e1 = (Entry<K, V>)e; 
			entries.add(e1);
		}
		return entries;
	}

	static class AnEntry<K, V> implements Entry<K, V>, Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = -5326227739123537044L;

		final K k;
		V v;

		AnEntry(K k, V v) {
			super();
			this.k = k;
			this.v = v;
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
			return v1;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((k == null) ? 0 : k.hashCode());
			return result;
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
