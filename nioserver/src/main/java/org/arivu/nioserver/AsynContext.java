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
	 * @return
	 */
	boolean isAsynchronousFinish();
	/**
	 * 
	 */
	void finish();
	/**
	 * @return
	 */
	Response getResponse();
	/**
	 * @return
	 */
	Request getRequest();
	
	/**
	 * @return
	 */
	SelectionKey getKey();
}
