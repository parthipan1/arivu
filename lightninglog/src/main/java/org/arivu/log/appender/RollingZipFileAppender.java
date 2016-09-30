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

	@Override
	public void append(String log) {
		if (RollingFileAppender.FILE_THRESHOLD_LIMIT <= fileSize) {
			lock.lock();
			try {
				if (RollingFileAppender.FILE_THRESHOLD_LIMIT <= fileSize) {
					fileSize = 0;
					file.renameTo(new File(
							getFileName(fileName, false, ".zip") +"_" + new SimpleDateFormat(FileAppender.FILE_EXT_FORMAT).format(new Date()) + "_" 
									+ (sizeFiles++) + ".zip"));
					out.close();
					file = new File(getFileName(fileName, true, ".zip"));
					out = new ZipOutputStream(new FileOutputStream(file,true));
					ZipEntry e = new ZipEntry(getFileName(fileName, false, ".zip")
							 +"_"+ new SimpleDateFormat(FileAppender.FILE_EXT_FORMAT).format(new Date()) + ".log");
					out.putNextEntry(e);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				lock.unlock();
			}
		}
		super.append(log);
	}

}
