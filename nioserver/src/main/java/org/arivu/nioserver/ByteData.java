/**
 * 
 */
package org.arivu.nioserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.Threadlocal;
import org.arivu.datastructure.Threadlocal.Factory;
import org.arivu.utils.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Byte data which can be read and written from request and response Objects.
 * 
 * @author P
 *
 */
public final class ByteData {
	private static final String CHUNK_DATA_KEY = "chunk";
	private static final Logger logger = LoggerFactory.getLogger(ByteData.class);

	static{
		Server.registerShutdownHook(new Runnable() {
			@Override
			public void run() {
				clean(true, null);
				mdc.close();
			}
		});
	}
	
	static final Threadlocal<Map<String, RandomAccessFileHelper>> mdc = new Threadlocal<Map<String, RandomAccessFileHelper>>(
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
		Set<Entry<Object, Map<String, RandomAccessFileHelper>>> all2 = mdc.getAll();
		
		for(Entry<Object, Map<String, RandomAccessFileHelper>> e1:all2){
				Map<String, RandomAccessFileHelper> value2 = e1.getValue();
				for (Entry<String, RandomAccessFileHelper> e : value2.entrySet()) {
					RandomAccessFileHelper value = e.getValue();
					if (force || (name != null && value.isExpired() && !e.getKey().equals(name))) {
						try {
							final RandomAccessFile randomAccessFile = value.get();
							if (randomAccessFile != null) {
								if( value.file != null ){
									e.setValue(null);
//									value2.remove(value.file.getAbsolutePath());
//									System.out.println(" clearing "+e1.getKey()+"("+ Utils.toString(value2.keySet()) +") = "+value.file.getAbsolutePath());
									randomAccessFile.close();
									value.chunkData = null;
									logger.debug("Closing file {}", e.getKey());
								}
							}
						} catch (IOException e3) {
							logger.error("Error closing file " + e.getKey(), e3);
						}
					}
				}
				if( force && !NullCheck.isNullOrEmpty(value2) && value2.size()==1 && value2.containsKey(CHUNK_DATA_KEY) ){
					RandomAccessFileHelper randomAccessFileHelper = value2.get(CHUNK_DATA_KEY);
					mdc.remove(e1.getKey());
					if(randomAccessFileHelper!=null)
						randomAccessFileHelper.chunkData = null;
				}
		}
	}

	private static RandomAccessFileHelper getRAF(File f) throws IOException {
		final String name = f.getAbsolutePath();
		Map<String, RandomAccessFileHelper> map = mdc.get(null);
		RandomAccessFileHelper randomAccessFileHelper = map.get(name);
		if (randomAccessFileHelper == null) {
			randomAccessFileHelper = new RandomAccessFileHelper(f);
			map.put(name, randomAccessFileHelper);
		}
		return randomAccessFileHelper;
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
		this.file = f;
		this.fileLen = this.file.length();
		this.offset = 0l;
	}

	public ByteData(File f, long offset, long len) throws IOException {
		super();
		this.data = null;
		this.file = f;
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
			return fileLen-offset;
		}
	}

	public byte[] copyOfRange(long from, long to) throws IOException {
		final int len = (int) (to - from);
		if (file == null) {
			final int from2 = (int)from;
			if( len == Configuration.defaultChunkSize ){
				byte[] arr = getChunkData(true);
				
				for(int i=0;i<arr.length;i++)
					arr[i] = data[from2+i];
				
				return arr;
			}else{
				return Arrays.copyOfRange(data, from2, (int) to);
			}
		} else {
			if (to > offset + fileLen)
				throw new ArrayIndexOutOfBoundsException(
						" to " + to + " higher than the max len " + (offset + fileLen));
			
			final RandomAccessFileHelper rafh = getRAF(file);
//			System.out.println(rafh.file.getAbsolutePath()+" copyOfRange from :: "+from+" to :: "+to+"  len :: "+len+" file.len :: "+rafh.file.length()+" rafh.chunkData.length :: "+rafh.chunkData.length+" Configuration.defaultChunkSize :: "+Configuration.defaultChunkSize);
//			if( !Configuration.SINGLE_THREAD_MODE ){
//				RandomAccessFile raf = rafh.get();
//				byte[] arr = new byte[len];
//				raf.seek(offset + from);
//				raf.readFully(arr);
//				return arr;
//			}else 
			if( len == Configuration.defaultChunkSize ){
				byte[] arr = getChunkData(false);
				RandomAccessFile raf = rafh.get();
				raf.seek(offset + from);
				raf.readFully(arr);
				return arr;
			}else if(len == rafh.chunkData.length){
				RandomAccessFile raf = rafh.get();
				byte[] arr = rafh.chunkData;
				if(to == rafh.file.length()){
					if( rafh.chunkSet ) return arr;
					rafh.chunkSet = true;
				}
				reset(arr);
				raf.seek(offset + from);
				raf.readFully(arr);
				return arr;
			}else{
				RandomAccessFile raf = rafh.get();
				byte[] arr = new byte[len];
				raf.seek(offset + from);
				raf.readFully(arr);
				return arr;
			}
		}
	}
	
	private static final byte INIT_VAL = (new byte[1])[0];
	
	public static byte[] getChunkData(final boolean dirty) {
		boolean first = false;
		Map<String, RandomAccessFileHelper> map = mdc.get(null);
		RandomAccessFileHelper randomAccessFileHelper = map.get(CHUNK_DATA_KEY);
		if (randomAccessFileHelper == null) {
			randomAccessFileHelper = new RandomAccessFileHelper();
			map.put(CHUNK_DATA_KEY, randomAccessFileHelper);
			first = true;
		}
		
		final byte[] byteBuffer = randomAccessFileHelper.chunkData;
		if( !dirty && !first){
			reset(byteBuffer);
		}
		return byteBuffer;
	}

	static void reset(final byte[] byteBuffer) {
		for(int i=0;i<byteBuffer.length;i++)
			byteBuffer[i] = INIT_VAL;
	}
	
	public static ByteData wrap(byte[] array) {
		return new ByteData(array);
	}

	private static final class RandomAccessFileHelper {
		private static final int THRESHOLD_TIME_MILLISECS = 300000;
		private RandomAccessFile raf;
		private final File file;
		volatile long time = 0l;
		long lmt = 0l;
		byte[] chunkData;
		volatile boolean chunkSet = false;
		RandomAccessFileHelper(){
			this.file = null;
			this.raf = null;
			this.time = 0l;
			this.lmt = 0l;
			this.chunkData = new byte[Configuration.defaultChunkSize];
			this.chunkSet = false;
		}
		
		RandomAccessFileHelper(File file) throws IOException {
			super();
			this.file = file;
			this.raf = new RandomAccessFile(file, "r");
			this.time = System.currentTimeMillis();
			this.lmt = file.lastModified();
			this.chunkData = new byte[(int) (file.length()%Configuration.defaultChunkSize)];
			this.chunkSet = false;
		}
		
		RandomAccessFile get() throws IOException {
			if(file==null)
				return null;
			else if( !file.exists() ){
				raf.close();
				throw new FileNotFoundException(this.file.getAbsolutePath());
			}else if( file.lastModified() > lmt ){
				this.raf.close();
				this.raf = new RandomAccessFile(file, "r");;
				this.time = System.currentTimeMillis();
				this.lmt = file.lastModified();
				return this.raf;
			}else{
				long currentTimeMillis = System.currentTimeMillis();
				if (currentTimeMillis - this.time > THRESHOLD_TIME_MILLISECS)
					clean(false, file.getAbsolutePath());
				this.time = currentTimeMillis;
				return raf;
			}
		}

		boolean isExpired() {
			return System.currentTimeMillis() - this.time > THRESHOLD_TIME_MILLISECS;
		}
	}
}
