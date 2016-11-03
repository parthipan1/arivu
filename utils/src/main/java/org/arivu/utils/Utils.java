package org.arivu.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public final class Utils {

	public static <K, V> Map<K, V> unmodifiableMap(Map<K, V> map) {
		if (!NullCheck.isNullOrEmpty(map))
			return Collections.unmodifiableMap(map);
		else
			return null;
	}

	public static <T> Collection<T> unmodifiableCollection(Collection<T> col) {
		if (!NullCheck.isNullOrEmpty(col))
			return Collections.unmodifiableCollection(col);
		else
			return null;
	}

	public static <T> List<T> unmodifiableList(List<T> col) {
		if (!NullCheck.isNullOrEmpty(col))
			return Collections.unmodifiableList(col);
		else
			return null;
	}
	
	public static String replaceAll(String txt, String key, String value) {
		int index = txt.indexOf(key);
		while (index >= 0) {
			txt = txt.substring(0, index) + value + txt.substring(index + key.length(), txt.length());
			index = txt.indexOf(key);
		}
		return txt;
	}
	

	public static <K, V> String toString(Map<K, V> map) {
		if (map==null)
			return "null";
		
		Set<Entry<K, V>> entrySet = map.entrySet();
		StringBuffer buf = new StringBuffer("{");
		for (Entry<K, V> e : entrySet) {
			V value = e.getValue();
			if( buf.length()>1 )
				buf.append(",");
			
			if(value instanceof Collection<?>){
				buf.append(e.getKey()).append("=").append(toString((Collection<?>)value));
			}else if(value instanceof Map<?,?>){
				buf.append(e.getKey()).append("=").append(toString((Map<?,?>)value));
			}else{
				buf.append(e.getKey()).append("=").append(value);
			}
		}
		buf.append("}");
		return buf.toString();
	}

	public static String toString(Collection<?> collection) {
		if (collection==null)
			return "null";
		
		StringBuffer buf = new StringBuffer("[");
		for (Object e : collection) {
			Object value = e;
			if( buf.length()>1 )
				buf.append(",");
			
			if(value instanceof Collection<?>){
				buf.append(toString((Collection<?>)value));
			}else if(value instanceof Map<?,?>){
				buf.append(toString((Map<?,?>)value));
			}else{
				buf.append(value);
			}
		}
		buf.append("]");
		return buf.toString();
	}
}
