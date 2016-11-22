/**
 * 
 */
package org.arivu.nioserver;

/**
 * Exception Handler interface to handle un handled Exception. 
 * Any class implements this interface should handle exception.
 * To make this part of the app , config "exceptionHandler" in 
 * "arivu.nioserver.json" file. The implementation class should 
 * have a public default constructor.
 * 
 * {
 * 		"exceptionHandler": "xxx.xxx.ExceptionHandlerImpl"
 * }
 * 
 * @author Mr P
 *
 */
public interface ExceptionHandler {
	void handle(Throwable t);
}
