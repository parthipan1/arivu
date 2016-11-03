/**
 * 
 */
package org.arivu.dbds;

import org.arivu.pool.Pool;
import org.arivu.pool.PoolFactory;
import org.arivu.pool.ThreadLocalPool;

/**
 * ThreadLocal datasource creates a connection per thread and keeps 
 * it until the thread is active or the connection expired by config.
 * 
 * @author Mr P
 *
 */
public final class ThreadlocalDataSource extends AbstractDataSource  {

	/**
	 * 
	 */
	public ThreadlocalDataSource() {
		this(null);
	}

	public ThreadlocalDataSource(final ConnectionFactory cfin) {
		super(cfin,new UsePool(){
			@Override
			public Pool<DelegatingConnection> create(PoolFactory<DelegatingConnection> factory) {
				return new ThreadLocalPool<DelegatingConnection>(factory,DelegatingConnection.class);
			}
		});
	}

}
