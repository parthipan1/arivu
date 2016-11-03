/**
 * 
 */
package org.arivu.nioserver;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 * @author P
 *
 */
public final class ByteData {
	private final byte[] data;
	private final RandomAccessFile file;
	private final long fileLen;

	public ByteData(byte[] array) {
		super();
		this.data = array;
		this.file = null;
		this.fileLen = 0l;
	}

	public ByteData(File f) throws IOException {
		super();
		this.data = null;
		this.file = new RandomAccessFile(f, "r");
		this.fileLen = this.file.length();
	}
	
	public byte[] array() {
		return data;
	}

	public long length() {
		if( file==null ){
			return data.length;
		}else{
			return fileLen;
		}
	}

	public byte[] copyOfRange(int from, int to) throws IOException {
		if (file == null) {
			return Arrays.copyOfRange(data, (int)from, (int)to);
		}else{
			final int len = to-from;
			byte[] arr = new byte[len];
			file.seek(from);
			file.readFully(arr);
			return arr;
		}
	}

	public static ByteData wrap(byte[] array) {
		return new ByteData(array);
	}
}
