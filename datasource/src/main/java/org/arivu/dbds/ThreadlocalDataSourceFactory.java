/**
 * 
 */
package org.arivu.dbds;

/**
 * ThreadLocal datasource creates a connection per thread and keeps 
 * it until the thread is active.
 * 
 * @author P
 *
 */
public class ThreadlocalDataSourceFactory extends BaseDataSourceFactory {
	/**
	 * 
	 */
	public ThreadlocalDataSourceFactory() {
		super();
	}

}
