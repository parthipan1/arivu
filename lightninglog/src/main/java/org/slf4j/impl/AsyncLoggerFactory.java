/**
 * 
 */
package org.slf4j.impl;

import java.util.Map;
import org.arivu.datastructure.Amap;
import org.arivu.log.AsyncLogger;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * @author P
 *
 */
final class AsyncLoggerFactory implements ILoggerFactory {

	private final Map<String, Logger> loggerMap = new Amap<String, Logger>();

	public AsyncLoggerFactory() {
	}

	public Logger getLogger(String name) {
		Logger simpleLogger = loggerMap.get(name);
		if (simpleLogger != null) {
			return simpleLogger;
		} else {
			Logger newInstance = new AsyncLogger(name);
			Logger oldInstance = loggerMap.put(name, newInstance);
			return oldInstance == null ? newInstance : oldInstance;
		}
	}

	void reset() {
		loggerMap.clear();
	}
}
