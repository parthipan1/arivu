/**
 * 
 */
package org.arivu.log;

/**
 * @author P
 *
 */
public interface LoggerMXBean {
	
	boolean getShowThreadName();
	void setShowThreadName(boolean flag);
	
	boolean getShowLogName();
	void setShowLogName(boolean flag);
	
	boolean getShowLogShortName();
	void setShowLogShortName(boolean flag);

	boolean getShowDate();
	void setShowDate(boolean flag);

	String getDateFormat();
	void setDateFormat(String format);

	String getLogFile();
	void setLogFile(String file);

	long getLogFileSize();
	void setLogFileSize(long size);
	
	String getLogLevel(String logger);
	void setLogLevel(String logger,String level);
	
	void flush() throws Exception;
	void close() throws Exception;
}
