/**
 * 
 */
package org.arivu.log.appender;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.locks.Lock;

import org.arivu.log.Appender;
import org.arivu.utils.lock.AtomicWFLock;

/**
 * @author P
 *
 */
class FileAppender implements Appender {
	
	public static String FILE_EXT_FORMAT = AppenderProperties.FILE_EXT_FORMAT;

	String fileName;
	
	File file;
	
	PrintWriter oWriter;
	
	volatile long fileSize = 0;
	
	Date lastUpdated = null;
	
	final Lock lock = new AtomicWFLock();//new ReentrantLock(true);
	
	public FileAppender(String fileName) throws IOException {
		super();
		this.fileName = fileName;

		file = new File(fileName);
		if (!file.exists()) {
			file.createNewFile();
		}
		this.fileSize = file.length();
		if (file.canWrite()){
			oWriter = new PrintWriter(new java.io.FileWriter(file, true), true);
		}
		else
			throw new IOException("Unable to write to file "+fileName);
	}
	
	@Override
	public void append(String log) {
		Date date = new Date();
		if( lastUpdated!=null && checkDay(date)){
			lock.lock();
			try {
				if (checkDay(date)) {
					fileSize = 0;
					file.renameTo(new File(getFileName(fileName)
							+ new SimpleDateFormat(FILE_EXT_FORMAT).format(lastUpdated) + ".log"));
					oWriter.close();
					file = new File(getFileName(fileName));
					oWriter = new PrintWriter(new java.io.FileWriter(file, true), true);
					lastUpdated = date;
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}finally {
				lock.unlock();
			}
		}
		lock.lock();
		try {
			oWriter.println(log);
			fileSize = file.length();
		}finally {
			lock.unlock();
		}
	}

	private boolean checkDay(Date date) {
		Calendar calendar1 = Calendar.getInstance();
	    calendar1.setTime(date);
	    Calendar calendar2 = Calendar.getInstance();
	    calendar2.setTime(lastUpdated);
	    boolean sameYear = calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR);
	    boolean sameMonth = calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH);
	    boolean sameDay = calendar1.get(Calendar.DAY_OF_MONTH) == calendar2.get(Calendar.DAY_OF_MONTH);
	    return sameDay && sameMonth && sameYear;
	}

	@Override
	public void close() throws Exception {
//		outchannel.close();
		oWriter.close();
	}
	
	String getFileName(final String f) {
		return f;
	}
}
