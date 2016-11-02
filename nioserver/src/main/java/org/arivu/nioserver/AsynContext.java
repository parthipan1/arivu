package org.arivu.nioserver;

import java.nio.channels.SelectionKey;

/**
 * @author Mr P
 *
 */
public interface AsynContext {
	/**
	 * @param flag
	 */
	void setAsynchronousFinish(boolean flag);
	/**
	 * @return asyncFlag
	 */
	boolean isAsynchronousFinish();
	/**
	 * 
	 */
	void finish();
	/**
	 * @return response
	 */
	Response getResponse();
	/**
	 * @return request
	 */
	Request getRequest();
	
	/**
	 * @return selectionKey
	 */
	SelectionKey getKey();
}
