/**
 * 
 */
package org.arivu.log;

/**
 * @author P
 *
 */
public interface Appender extends AutoCloseable {
	void append(String log);
}
