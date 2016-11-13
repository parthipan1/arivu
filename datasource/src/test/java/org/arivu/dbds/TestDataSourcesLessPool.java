package org.arivu.dbds;

import java.sql.SQLException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

//@Ignore
public class TestDataSourcesLessPool {

	static final TestDataSourcesHelper helper = new TestDataSourcesHelper();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		helper.maxThreadsCnt = 500;
		helper.nThreads = 1000000;
		helper.poolSize = 375;
		helper.reuseCount = 2500;
		helper.lifeSpan = 30000;
		helper.setUpBeforeClass();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		helper.tearDownAfterClass();
	}

	@Before
	public void setUp() throws Exception {
		helper.setUp();
	}

	@After
	public void tearDown() throws Exception {
		helper.tearDown();
	}

	@Test
	public void testDefault() throws SQLException, InterruptedException {
		helper.testDataSource(new DefaultDataSource(helper.factory), helper.poolSize, true, true);
	}

	@Test
	@Ignore
	public void testRequestScope() throws SQLException, InterruptedException {
		helper.testDataSource(new RequestDataSource(helper.factory), helper.nThreads, true, true);
	}

	@Test
	public void testThreadLocal() throws SQLException, InterruptedException {
		helper.testDataSource(new ThreadlocalDataSource(helper.factory), helper.nThreads, true, true);
	}

}
