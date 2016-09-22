/**
 * 
 */
package org.arivu.log.appender;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author P
 *
 */
final class RollingFileAppender extends FileAppender {

	public static long FILE_THRESHOLD_LIMIT = AppenderProperties.FILE_THRESHOLD_LIMIT;
	
	public RollingFileAppender(String fileName) throws IOException {
		super(fileName);
	}
	
	@Override
	public void append(String log) {
		if(FILE_THRESHOLD_LIMIT<=fileSize){
			lock.lock();
			try {
				if(FILE_THRESHOLD_LIMIT<=fileSize){
					fileSize=0;
					file.renameTo(new File(
							getFileName(fileName, false) +"_" + new SimpleDateFormat(FileAppender.FILE_EXT_FORMAT).format(new Date())+ "_"
									+ (sizeFiles++) + ".log"));
					oWriter.close();
					file = new File(getFileName(fileName, true));
					oWriter = new PrintWriter(new java.io.FileWriter(file, true), true);
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
