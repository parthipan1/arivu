package org.arivu.pool;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestPoolsNoExp {

	static final TestHelper helper = new TestHelper();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		helper.maxThreadCnt = 500;
		helper.nThreads = 1000000;
		helper.poolSize = 375;
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
	public void test4ConcurrentPool() throws Exception {
		final Pool<Resource> pool = new ConcurrentPool<Resource>(helper.factory , Resource.class);
		helper.testPool(pool, helper.poolSize, true);
	}
		
	@Test
//	@Ignore
	public void test2NonBlockingPool() throws Exception {
		final Pool<Resource> pool = new NonBlockingPool<Resource>(helper.factory, Resource.class);//
		helper.testPool(pool, helper.poolSize, true);
	}
	
	@Test
	@Ignore
	public void test6NoPool() throws Exception {
		final Pool<Resource> pool = new NoPool<Resource>(helper.factory, Resource.class);//
		helper.testPool(pool, helper.nThreads, true);
	}
	
	@Test
	public void test5ThreadLocalPool() throws Exception {
		final Pool<Resource> pool = new ThreadLocalPool<Resource>(helper.factory, Resource.class);//
		helper.testPool(pool, helper.nThreads, true);
	}

}
