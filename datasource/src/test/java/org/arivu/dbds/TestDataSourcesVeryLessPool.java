package org.arivu.dbds;

import java.sql.SQLException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

//@Ignore
public class TestDataSourcesVeryLessPool {

	static final TestDataSourcesHelper helper = new TestDataSourcesHelper();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		helper.maxThreadsCnt = 100;
		helper.nThreads = 100;
		helper.poolSize = 2;
		helper.reuseCount = -1;
		helper.lifeSpan = -1;
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
		helper.testDataSource(new DefaultDataSource(helper.factory), helper.poolSize, false);
	}

	@Test
	public void testRequestScope() throws SQLException, InterruptedException {
		helper.testDataSource(new RequestDataSource(helper.factory), helper.nThreads, false);
	}

	@Test
	public void testThreadLocal() throws SQLException, InterruptedException {
		helper.testDataSource(new ThreadlocalDataSource(helper.factory), helper.nThreads, false);
	}

}
