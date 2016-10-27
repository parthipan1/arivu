package org.arivu.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Utils {

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
}
