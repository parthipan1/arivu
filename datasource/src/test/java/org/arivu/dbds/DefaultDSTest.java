package org.arivu.dbds;

import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Test;
import org.mockito.Mockito;

//@Ignore
public class DefaultDSTest {

	@Test
	public void testCreate() throws SQLException {

		final DefaultDataSource ds = new DefaultDataSource(new ConnectionFactory() {

			@Override
			public DelegatingConnection create(String user, String password, String driver, String url) {
				return Mockito.mock(DelegatingConnection.class);
			}
		});
		ds.setName("test");
		ds.setMaxPoolSize(2);//maxPoolSize = 2;
		ds.setInterval(-1);
		ds.registerMXBean();
		
		Connection connection1 = ds.getConnection();
		// @SuppressWarnings("unused")
		// Connection connection2 =
		ds.getConnection();

//		assertTrue("Failed in allconnections!", ds.allConnections.size() == 2);
//		assertTrue("Failed in allconnections!", ds.connectionQueue.size() == 0);

		connection1.close();
		ds.close();
		assertTrue("Failed in allconnections! expected=" + ds.getMaxPoolSize(), ds.getMaxPoolSize() == 0);

		// connection1 = ds.getConnection();
		// assertTrue("Failed in allconnections!", ds.allConnections.size()==2);
		// assertTrue("Failed in allconnections!",
		// ds.connectionQueue.size()==0);

		// @SuppressWarnings("unused")
		// Connection connection3 =
		ds.getConnection();
	}

}
