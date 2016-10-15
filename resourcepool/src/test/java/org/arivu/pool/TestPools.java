package org.arivu.pool;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPools {

	static final TestHelper helper = new TestHelper();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
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

//	@Test
////	@Ignore
//	public void test3ConcurrentLinkedPool() throws Exception {
//		final Pool<Resource> pool = new ConcurrentLinkedPool<>(helper.factory, Resource.class);//
//		helper.testPool(pool, helper.poolSize);
//	}

	@Test
	public void test4ConcurrentPool() throws Exception {
		final Pool<Resource> pool = new ConcurrentPool<Resource>(helper.factory , Resource.class);
		helper.testPool(pool, helper.poolSize, false);
	}
	
//	@Test
//	public void test1NonBlockingLinkedPool() throws Exception {
//		final Pool<Resource> pool = new NonBlockingLinkedPool<>(helper.factory , Resource.class);
//		helper.testPool(pool, helper.poolSize);
//	}
	
	@Test
//	@Ignore
	public void test2NonBlockingPool() throws Exception {
		final Pool<Resource> pool = new NonBlockingPool<Resource>(helper.factory, Resource.class);//
		helper.testPool(pool, helper.poolSize, true);
	}
	
	@Test
//	@Ignore
	public void test6NoPool() throws Exception {
		final Pool<Resource> pool = new NoPool<Resource>(helper.factory, Resource.class);//
		helper.testPool(pool, helper.nThreads, true);
	}
	
	@Test
//	@Ignore
	public void test5ThreadLocalPool() throws Exception {
		final Pool<Resource> pool = new ThreadLocalPool<Resource>(helper.factory, Resource.class);//
		helper.testPool(pool, helper.nThreads, true);
	}

}
