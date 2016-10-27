/**
 * 
 */
package org.arivu.nioserver;

/**
 * @author P
 *
 */
public final class ByteData{
	private final byte[] data;

	public ByteData(byte[] array) {
		super();
		this.data = array;
	}

	public byte[] array() {
		return data;
	}
	
	public static ByteData wrap(byte[] array){
		return new ByteData(array);
	}
}
