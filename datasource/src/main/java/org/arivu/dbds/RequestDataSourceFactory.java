/**
 * 
 */
package org.arivu.dbds;

/**
 * Not Actually a connection pool , Creates and closes connection when required.
 * No Connection is maintained in a connectionpool.
 * 
 * @author P
 *
 */
public final class RequestDataSourceFactory extends BaseDataSourceFactory {
	/**
	 * 
	 */
	public RequestDataSourceFactory() {
		super(new RequestDataSource());
	}

}
