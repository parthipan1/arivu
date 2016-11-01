package org.arivu.nioserver;

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
}
