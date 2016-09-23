/**
 * 
 */
package org.arivu.datastructure;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
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
	private final Map<String,MappedByteBuffer> bufferMap = new Amap<String,MappedByteBuffer>(cas);
	/**
	 * 
	 */
	public MemoryMappedFiles() {
		super();
	}

	public String add(final String file) throws IOException{
		cas.lock();
        MappedByteBuffer mappedByteBuffer = bufferMap.get(file);
        
        if(mappedByteBuffer==null){
        	final RandomAccessFile randomAccessFile = new RandomAccessFile(new File(file), "r");
        	FileChannel fileChannel = randomAccessFile.getChannel();
        	
        	mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
        	randomAccessFile.close();
        	
        	bufferMap.put(file, mappedByteBuffer);
        }
        cas.unlock();
		return convert(mappedByteBuffer);
	}
	
	public String get(final String file) throws IOException{
		return convert(bufferMap.get(file));
	}

	public String remove(final String file) throws IOException{
		return convert(bufferMap.remove(file));
	}

	public void clear(){
		bufferMap.clear();
	}
	
	public int size(){
		return bufferMap.size();
	}
	
	private String convert(final MappedByteBuffer buffer){
		if(buffer==null) return null;
		
		final StringBuffer b = new StringBuffer();
		
		cas.lock();
		
		for (int i = 0; i < buffer.limit(); i++)
			b.append((char) buffer.get());

		buffer.position(0);
		
		cas.unlock();
		
		return b.toString();
	}
}
