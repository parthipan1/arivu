/**
 * 
 */
package org.arivu.nioserver;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.Threadlocal;
import org.arivu.datastructure.Threadlocal.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author P
 *
 */
public final class ByteData {
	private static final Logger logger = LoggerFactory.getLogger(ByteData.class);

	private static final Threadlocal<Map<String, RandomAccessFileHelper>> mdc = new Threadlocal<Map<String, RandomAccessFileHelper>>(
			new Factory<Map<String, RandomAccessFileHelper>>() {

				@Override
				public Map<String, RandomAccessFileHelper> create(Map<String, Object> params) {
					return new Amap<String, RandomAccessFileHelper>();
				}

			});

	static void clean(final boolean force, final String name) {
		if (force)
			innerClose(true, null);
		else {
			final ExecutorService exe = Executors.newFixedThreadPool(1);
			exe.submit(new Runnable() {
				@Override
				public void run() {
					exe.shutdownNow();
					innerClose(false, name);
				}
			});
		}
	}

	private static void innerClose(final boolean force, final String name) {
		Collection<Map<String, RandomAccessFileHelper>> all = mdc.getAll();
		for (Map<String, RandomAccessFileHelper> threadValues : all) {
			for (Entry<String, RandomAccessFileHelper> e : threadValues.entrySet()) {
				RandomAccessFileHelper value = e.getValue();
				if (force || (name != null && value.isExpired() && !e.getKey().equals(name))) {
					final RandomAccessFile randomAccessFile = value.get();
					if (randomAccessFile != null) {
						e.setValue(null);
						try {
							randomAccessFile.close();
							logger.debug("Closing file {}", e.getKey());
						} catch (IOException e1) {
							logger.error("Error closing file " + e.getKey(), e1);
						}
					}
				}
			}

		}
	}

//	static {
//		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
//
//			@Override
//			public void run() {
//				
//			}
//		}));
//	}

	private static RandomAccessFile getRAF(File f) throws IOException {
		final String name = f.getAbsolutePath();
		Map<String, RandomAccessFileHelper> map = mdc.get(null);
		RandomAccessFileHelper randomAccessFileHelper = map.get(name);
		if (randomAccessFileHelper == null) {
			randomAccessFileHelper = new RandomAccessFileHelper(name, new RandomAccessFile(f, "r"));
			map.put(name, randomAccessFileHelper);
		}
		return randomAccessFileHelper.get();
	}

	private final byte[] data;
	private final File file;
	private final long fileLen;

	private final long offset;

	public ByteData(byte[] array) {
		super();
		this.data = array;
		this.file = null;
		this.fileLen = 0l;
		this.offset = 0l;
	}

	public ByteData(File f) throws IOException {
		super();
		this.data = null;
		this.file = f;// new RandomAccessFile(f, "r");
		this.fileLen = this.file.length();
		this.offset = 0l;
	}

	public ByteData(File f, long offset, long len) throws IOException {
		super();
		this.data = null;
		this.file = f;// new RandomAccessFile(f, "r");
		this.fileLen = len;
		this.offset = offset;
	}

	public byte[] array() {
		return data;
	}

	public long length() {
		if (file == null) {
			return data.length;
		} else {
			return fileLen;
		}
	}

	public void close() throws IOException {
		// if( file!=null )
		// file.close();
	}

	public byte[] copyOfRange(long from, long to) throws IOException {
		if (file == null) {
			return Arrays.copyOfRange(data, (int) from, (int) to);
		} else {
			if (to > offset + fileLen)
				throw new ArrayIndexOutOfBoundsException(
						" to " + to + " higher than the max len " + (offset + fileLen));
			final int len = (int) (to - from);
			byte[] arr = new byte[len];
			RandomAccessFile raf = getRAF(file);
			raf.seek(offset + from);
			raf.readFully(arr);
			return arr;
		}
	}

	public static ByteData wrap(byte[] array) {
		return new ByteData(array);
	}

	private static final class RandomAccessFileHelper {
		private static final int THRESHOLD_TIME_MILLISECS = 300000;
		private final RandomAccessFile raf;
		private final String name;
		volatile long time = 0l;

		RandomAccessFileHelper(String name, RandomAccessFile raf) {
			super();
			this.name = name;
			this.raf = raf;
			this.time = System.currentTimeMillis();
		}

		RandomAccessFile get() {
			long currentTimeMillis = System.currentTimeMillis();
			if (currentTimeMillis - this.time > THRESHOLD_TIME_MILLISECS)
				clean(false, name);
			this.time = currentTimeMillis;
			return raf;
		}

		boolean isExpired() {
			return System.currentTimeMillis() - this.time > THRESHOLD_TIME_MILLISECS;
		}
	}
}
