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
	void flush() throws Exception;
	int getConsumerCount();
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
