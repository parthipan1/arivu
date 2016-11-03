/**
 * 
 */
package org.arivu.dbds;

import org.arivu.pool.ConcurrentPool;
import org.arivu.pool.Pool;
import org.arivu.pool.PoolFactory;

/**
 * Concurrent Connection Pool, creates a connection when 
 * required otherwise uses available connections from the pool.
 * Connections are recycled once on 30 secs ( default Configured value). 
 * 
 * @author Mr P
 *
 */
public final class DefaultDataSource extends AbstractDataSource {

	
	/**
	 * @throws MalformedObjectNameException 
	 * 
	 */
	public DefaultDataSource(){
		this(null);
	}

	public DefaultDataSource(ConnectionFactory cfin) {
		super(cfin,new UsePool(){
			@Override
			public Pool<DelegatingConnection> create(PoolFactory<DelegatingConnection> factory) {
				return new ConcurrentPool<DelegatingConnection>(factory,DelegatingConnection.class);
			}
		});
	}
	
}
