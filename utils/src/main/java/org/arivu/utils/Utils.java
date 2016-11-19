package org.arivu.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
		if( key.equals(value) ) return txt;
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
	
	public static MappedByteBuffer readBB(File file) throws IOException {
        if (file == null)
            return null;
        else if (!file.exists())
            return null;
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
            final FileChannel fileChannel = randomAccessFile.getChannel();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
        } finally {
            if (randomAccessFile != null) {
                randomAccessFile.close();
            }
        }
    }
	
	public static byte[] read(File file) throws IOException {
        if (file == null)
            return null;
        else if (!file.exists())
            return null;

        ByteBuffer bb = readBB(file);
        byte[] data = new byte[bb.remaining()];
        bb.get(data, 0, data.length);
        return data;
    }
}
