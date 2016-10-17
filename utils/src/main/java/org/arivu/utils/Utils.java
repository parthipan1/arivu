package org.arivu.utils;

import java.util.Collections;
import java.util.Map;

public class Utils {

	public static <K,V> Map<K,V> unmodifiableMap(Map<K,V> map){
		if( !NullCheck.isNullOrEmpty(map) )
			return Collections.unmodifiableMap(map);
		else
			return null;
	}
}
