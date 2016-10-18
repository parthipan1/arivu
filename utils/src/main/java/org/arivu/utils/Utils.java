package org.arivu.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class Utils {

	public static <K,V> Map<K,V> unmodifiableMap(Map<K,V> map){
		if( !NullCheck.isNullOrEmpty(map) )
			return Collections.unmodifiableMap(map);
		else
			return null;
	}
	
	public static <T> Collection<T> unmodifiableCollection(Collection<T> col){
		if( !NullCheck.isNullOrEmpty(col) )
			return Collections.unmodifiableCollection(col);
		else
			return null;
	}
}
