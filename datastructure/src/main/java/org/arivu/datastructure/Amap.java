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
public final class Amap<K,V> implements Map<K, V> ,Serializable {//, Set<Map.Entry<K,V>>

	/**
	 * 
	 */
	private static final long serialVersionUID = -997810275912377568L;
	
	final DoublyLinkedSet<Entry<K, V>> set = new DoublyLinkedSet<Map.Entry<K,V>>(CompareStrategy.EQUALS);

	@Override
	public int size() {
		return set.size();
	}

	@Override
	public boolean isEmpty() {
		return set.isEmpty();
	}

	@SuppressWarnings("unchecked")
	AnEntry<K, V> getKeyWrap(Object key){
		return new AnEntry<K, V>((K) key, null);
	}
	
	@Override
	public boolean containsKey(Object key) {
		return set.contains(getKeyWrap(key));
	}

	@Override
	public boolean containsValue(Object value) {
		DoublyLinkedSet<Entry<K, V>> ref = set.right;
		while (ref != null) {
			if (ref == set) {
				break;
			}
			if( value instanceof String ){
				if(ref.obj!=null && CompareStrategy.EQUALS.compare(ref.obj.getValue(), value)){
					return true;
				}
			}else{
				if(ref.obj!=null && CompareStrategy.REF.compare(ref.obj.getValue(), value)){
					return true;
				}
			}
			ref = ref.right;
		}
		return false;
	}

	@Override
	public V get(Object key) {
		DoublyLinkedSet<java.util.Map.Entry<K, V>> search = set.search(getKeyWrap(key));
		if( search==null )
			return null;
		else
			return search.obj.getValue();
	}

	@Override
	public V put(K key, V value) {
		AnEntry<K, V> e = new AnEntry<K, V>(key, value);
		set.add(e);
		return value;
	}

	@Override
	public V remove(Object key) {
		DoublyLinkedSet<java.util.Map.Entry<K, V>> search = set.search(getKeyWrap(key));
		if( search==null ){
			return null;
		}else{
			search.removeRef();
			return search.obj.getValue();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		if( m!=null ){
			@SuppressWarnings("rawtypes")
			Set entrySet = m.entrySet();
			for(Object o:entrySet){
				Entry<? extends K, ? extends V> e = (Entry<? extends K, ? extends V>)o;
				put(e.getKey(), e.getValue());
			}
		}
	}

	@Override
	public void clear() {
		set.clear();
	}

	@Override
	public Set<K> keySet() {
		DoublyLinkedSet<K> keys = new DoublyLinkedSet<K>();
		for(Entry<K, V> e:set){
			keys.add(e.getKey());
		}
		return keys;
	}

	@Override
	public Collection<V> values() {
		DoublyLinkedList<V> keys = new DoublyLinkedList<V>();
		for(Entry<K, V> e:set){
			keys.add(e.getValue());
		}
		return keys;
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		DoublyLinkedSet<Entry<K, V>> entries = new DoublyLinkedSet<Entry<K, V>>();
		for( Entry<K, V> e:set ){
			entries.add(e);
		}
		return entries;
	}
	
	static class AnEntry<K, V> implements Entry<K, V>,Serializable{

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

//	@Override
//	public boolean contains(Object o) {
//		return set.contains(o);
//	}
//
//	@Override
//	public Iterator<Entry<K, V>> iterator() {
//		return set.iterator();
//	}
//
//	@Override
//	public Object[] toArray() {
//		return set.toArray();
//	}
//
//	@Override
//	public <T> T[] toArray(T[] a) {
//		return set.toArray(a);
//	}
//
//	@Override
//	public boolean add(Entry<K, V> e) {
//		return set.add(e);
//	}
//
//	@Override
//	public boolean containsAll(Collection<?> c) {
//		return set.containsAll(c);
//	}
//
//	@Override
//	public boolean retainAll(Collection<?> c) {
//		return set.retainAll(c);
//	}
//
//	@Override
//	public boolean removeAll(Collection<?> c) {
//		return set.removeAll(c);
//	}
//
//	@Override
//	public boolean addAll(Collection<? extends Entry<K, V>> c) {
//		return set.addAll(c);
//	}
}
