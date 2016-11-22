package org.arivu.nioserver;

/**
 * AsynContext class enabled app to run Async API calls.
 * AsynContext is available thru {@link StaticRef#getAsynContext()} method. 
 * This method should be invoked concurrently on the main thread and 
 * to make the call asynchronous, method {@link #setAsynchronousFinish(true)} should 
 * be called on the same thread and once the Async task is done 
 * {@link #finish()} method should be called to complete the request.
 * 
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
	
}
