/**
 * 
 */
package org.arivu.log.appender;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.arivu.log.Appender;
import org.arivu.utils.lock.AtomicWFReentrantLock;

/**
 * @author P
 *
 */
class ZipFileAppender implements Appender {

	String fileName;

	File file;

	ZipOutputStream out;

	volatile long fileSize = 0;

	volatile int sizeFiles = 1;

	Calendar lastUpdated = null;

	final Lock lock = new AtomicWFReentrantLock();

	public ZipFileAppender(String fileName) throws IOException {
		super();
		String formatedDate = new SimpleDateFormat(FileAppender.FILE_EXT_FORMAT).format(new Date());
		
		this.fileName = getFileName(fileName, true, ".zip");

		file = new File(this.fileName);
		if (!file.exists()) {
			file.createNewFile();
		}else{
			file.renameTo(new File(getFileName(fileName, false, ".zip")
					+ "_" + formatedDate
					+ ".zip"));
			file.createNewFile();
		}
		this.fileSize = file.length();
		if (file.canWrite()) {
			out = new ZipOutputStream(new FileOutputStream(file, true));
			ZipEntry e = new ZipEntry(getFileName(fileName, false, ".zip") + "_"
					+ formatedDate + ".log");
			out.putNextEntry(e);
		} else
			throw new IOException("Unable to write to file " + this.fileName);
	}

	@Override
	public void append(final String log) {
		dayRollover();
		lock.lock();
		try {
			final byte[] data = log.getBytes();

			out.write(data, 0, data.length);
			out.flush();

			fileSize += data.length;

		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			lock.unlock();
		}
	}

	private final void dayRollover() {
		final Calendar date = Calendar.getInstance();
		date.setTime(new Date());
		if (lastUpdated != null && checkDay(date)) {
			lock.lock();
			try {
				if (checkDay(date)) {
					fileSize = 0;
					file.renameTo(new File(getFileName(fileName, false, ".zip")
							+ "_" + new SimpleDateFormat(FileAppender.FILE_EXT_FORMAT).format(lastUpdated.getTime())
							+ ".zip"));
					out.close();
					file = new File(getFileName(fileName, true, ".zip"));
					out = new ZipOutputStream(new FileOutputStream(file, true));
					ZipEntry e = new ZipEntry(getFileName(fileName, false, ".zip") + "_"
							+ new SimpleDateFormat(FileAppender.FILE_EXT_FORMAT).format(new Date()) + ".log");
					out.putNextEntry(e);
					lastUpdated = date;
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				lock.unlock();
			}
		}
	}

	private final boolean checkDay(final Calendar date) {

		// Calendar calendar1 = Calendar.getInstance();
		// calendar1.setTime(date);
		// Calendar calendar2 = Calendar.getInstance();
		// calendar2.setTime(lastUpdated);
		// boolean sameYear = calendar1.get(Calendar.YEAR) ==
		// calendar2.get(Calendar.YEAR);
		// boolean sameMonth = calendar1.get(Calendar.MONTH) ==
		// calendar2.get(Calendar.MONTH);
		// boolean sameDay = calendar1.get(Calendar.DAY_OF_MONTH) ==
		// calendar2.get(Calendar.DAY_OF_MONTH);
		// return sameDay && sameMonth && sameYear;
		return date.get(Calendar.HOUR_OF_DAY) < lastUpdated.get(Calendar.HOUR_OF_DAY);
	}

	@Override
	public void close() throws Exception {
		out.close();
	}

	String getFileName(final String f, boolean add, String ext) {
		if (add) {
			if (f.endsWith(ext))
				return f;
			else
				return f + ext;
		} else {
			return f.replace(ext, "");
		}
	}
}
