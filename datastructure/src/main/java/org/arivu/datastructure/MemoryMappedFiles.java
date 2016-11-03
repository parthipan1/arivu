/**
 * 
 */
package org.arivu.datastructure;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.arivu.utils.lock.AtomicWFReentrantLock;

/**
 * @author P
 *
 */
public final class MemoryMappedFiles {

	private final Lock cas = new AtomicWFReentrantLock();
	private final Map<String, MappedByteBuffer> bufferMap = new Amap<String, MappedByteBuffer>(cas);

	/**
	 * 
	 */
	public MemoryMappedFiles() {
		super();
	}

	public String add(final String file) throws IOException {
		return convert(addBytes(file));
	}

	public ByteBuffer addBytes(final String file) throws IOException {
		cas.lock();
		MappedByteBuffer mappedByteBuffer = bufferMap.get(file);
		try {
			if (mappedByteBuffer == null) {
				mappedByteBuffer = create(file);
				bufferMap.put(file, mappedByteBuffer);
			}
		} finally {
			cas.unlock();
		}
		return copy(mappedByteBuffer);
	}

	private MappedByteBuffer create(final String file) throws IOException {
		RandomAccessFile randomAccessFile = null;
		try {
			randomAccessFile = new RandomAccessFile(new File(file), "r");
			final FileChannel fileChannel = randomAccessFile.getChannel();
			return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
		} finally {
			closeFile(randomAccessFile);
		}
	}

	void closeFile(RandomAccessFile randomAccessFile) throws IOException {
		if (randomAccessFile != null) {
			randomAccessFile.close();
		}
	}

	public ByteBuffer getBytes(final String file) {
		return copy(bufferMap.get(file));
	}

	public ByteBuffer getOriginalBytes(final String file) {
		return bufferMap.get(file);
	}
	
	public String get(final String file) {
		return convert(getBytes(file));
	}

	public ByteBuffer removeBytes(final String file) {
		return copy(bufferMap.remove(file));
	}

	public String remove(final String file) {
		return convert(removeBytes(file));
	}

	public void clear() {
		bufferMap.clear();
	}

	public int size() {
		return bufferMap.size();
	}

	private String convert(final ByteBuffer buffer) {
		if (buffer == null)
			return null;

		final StringBuffer b = new StringBuffer();

		cas.lock();

		for (int i = 0; i < buffer.limit(); i++)
			b.append((char) buffer.get());

		buffer.position(0);

		cas.unlock();

		return b.toString();
	}

	static ByteBuffer copy(final ByteBuffer original) {
		if (original == null)
			return null;
		// final ByteBuffer clone = (original.isDirect()) ?
		// ByteBuffer.allocateDirect(original.capacity())
		// : ByteBuffer.allocate(original.capacity());

		final ByteBuffer clone = ByteBuffer.allocateDirect(original.capacity());
		original.rewind();// copy from the beginning
		clone.put(original.asReadOnlyBuffer());
		original.rewind();
		clone.flip();
		return clone;
	}

}
