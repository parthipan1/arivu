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

	String getFileName(String fileName) {
		return fileName+"_"+ new SimpleDateFormat(FILE_EXT_FORMAT).format(new Date()) + ".log";
	}

	@Override
	public void append(String log) {
//		try {
//		fileSize += log.getBytes("UTF-8").length;
////			fileSize += log.length();//getBytes("UTF-8").length;
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
		if(FILE_THRESHOLD_LIMIT<=fileSize){
			lock.lock();
			try {
				if(FILE_THRESHOLD_LIMIT<=fileSize){
					fileSize=0;
					oWriter.close();
//					outchannel.close();
					file = new File(getFileName(fileName));
					oWriter = new PrintWriter(new java.io.FileWriter(file, true), true);
//					outchannel = Channels.newChannel(new FileOutputStream(file,true));
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
