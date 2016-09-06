/**
 * 
 */
package org.arivu.log.appender;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author P
 *
 */
final class RollingZipFileAppender extends ZipFileAppender {

	
	public RollingZipFileAppender(String fileName) throws IOException {
		super(fileName);
		
	}

	String getFileName(String fileName) {
		return fileName+"_"+ new SimpleDateFormat(FileAppender.FILE_EXT_FORMAT).format(new Date()) + ".zip";
	}

	@Override
	public void append(String log) {
		if(RollingFileAppender.FILE_THRESHOLD_LIMIT<=fileSize){
			lock.lock();
			try {
				if(RollingFileAppender.FILE_THRESHOLD_LIMIT<=fileSize){
					fileSize=0;
					out.close();
					file = new File(getFileName(fileName));
					out = new ZipOutputStream(new FileOutputStream(file));
					ZipEntry e = new ZipEntry(fileName);
					out.putNextEntry(e);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}finally {
				lock.unlock();
			}
		}
		super.append(log);
	}

}
