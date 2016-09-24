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
import org.arivu.utils.lock.AtomicWFReentrantLock;

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
	
	volatile int sizeFiles = 1;
	
	Calendar lastUpdated = null;
	
	final Lock lock = new AtomicWFReentrantLock();//new ReentrantLock(true);
	
	public FileAppender(String fileName) throws IOException {
		super();
		this.fileName = getFileName(fileName, true);

		file = new File(this.fileName);
		if (!file.exists()) {
			file.createNewFile();
		}
		this.fileSize = file.length();
		if (file.canWrite()){
			oWriter = new PrintWriter(new java.io.FileWriter(file, true), true);
		}
		else
			throw new IOException("Unable to write to file "+this.fileName);
	}
	
	@Override
	public void append(String log) {
		dayRollover();
		lock.lock();
		try {
			oWriter.println(log);
			fileSize = file.length();
		}finally {
			lock.unlock();
		}
	}

	private final void dayRollover() {
		final Calendar date = Calendar.getInstance();
		date.setTime(new Date());
		if( lastUpdated!=null && checkDay(date)){
			lock.lock();
			try {
				if (checkDay(date)) {
					fileSize = 0;
					file.renameTo(new File(getFileName(fileName, false)
							+ new SimpleDateFormat(FILE_EXT_FORMAT).format(lastUpdated.getTime()) + ".log"));
					oWriter.close();
					file = new File(getFileName(fileName, true));
					oWriter = new PrintWriter(new java.io.FileWriter(file, true), true);
					lastUpdated = date;
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}finally {
				lock.unlock();
			}
		}
	}

	private final boolean checkDay(final Calendar date) {
	    return date.get(Calendar.HOUR_OF_DAY) < lastUpdated.get(Calendar.HOUR_OF_DAY) ; 
	}

	@Override
	public void close() throws Exception {
		oWriter.close();
	}
	
	String getFileName(final String f, boolean add) {
		if (add) {
			if (f.endsWith(".log"))
				return f;
			else
				return f + ".log";
		}else{
			return f.replace(".log", "");
		}
	}
}
