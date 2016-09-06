/**
 * 
 */
package org.arivu.dbds;

import org.arivu.pool.NoPool;
import org.arivu.pool.Pool;
import org.arivu.pool.PoolFactory;

/**
 * Not Actually a connection pool , Creates and closes connection when required.
 * No Connection is maintained in a connection pool(NoPool).
 * 
 * @author 
 *
 */
public final class RequestDataSource extends AbstractDataSource {

	/**
	 * 
	 */
	public RequestDataSource() {
		this(null);
	}

	public RequestDataSource(final ConnectionFactory cfin) {
		super(cfin,new UsePool(){
			@Override
			public Pool<DelegatingConnection> create(PoolFactory<DelegatingConnection> factory) {
				return new NoPool<DelegatingConnection>(factory,DelegatingConnection.class);
			}
		});
	}
}
