/**
 * 
 */
package org.arivu.log;

/**
 * @author P
 *
 */
public interface LogMXBean {
	int getBufferSize();
//	int getRingSize();
	void flush() throws Exception;
	void addConsumer();
	void removeConsumer() throws Exception;
	void evictConsumer() throws Exception;
	void close() throws Exception;
	int getBatchSize();
	void setBatchSize(int size);
	void addAppender(String customAppender, String fileName);
	String[] getAppenders();
	void removeAppender(String name);
}
