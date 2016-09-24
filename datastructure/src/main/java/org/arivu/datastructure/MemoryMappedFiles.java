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
		return mappedByteBuffer;
	}
	
	private MappedByteBuffer create(final String file) throws IOException {
		final RandomAccessFile randomAccessFile = new RandomAccessFile(new File(file), "r");
		try {
			final FileChannel fileChannel = randomAccessFile.getChannel();
			return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
		} finally {
			randomAccessFile.close();
		}
	}

	public ByteBuffer getBytes(final String file) {
		return bufferMap.get(file);
	}
	
	public String get(final String file) {
		return convert(getBytes(file));
	}

	public ByteBuffer removeBytes(final String file) {
		return bufferMap.remove(file);
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
}
