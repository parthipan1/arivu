/**
 * 
 */
package org.arivu.dbds;

/**
 * Unlimited Connection Pool, creates a connection when 
 * required otherwise uses available connections from the pool.
 * Connections are recycled once on 30 secs (Configured value). 
 * 
 * @author P
 *
 */
public final class DefaultDataSourceFactory extends BaseDataSourceFactory {
	/**
	 * 
	 */
	public DefaultDataSourceFactory() {
		super(new DefaultDataSource());
	}
}
