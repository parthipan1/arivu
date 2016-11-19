package org.arivu.nioserver;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestHttpProxy {
//	private static final Logger logger = LoggerFactory.getLogger(TestHttpMethods.class);
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestHttpMethodsMultiThreaded.init(true, false);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		TestHttpMethodsMultiThreaded.tearDownAfterClass();
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void testGet() {
		
	}

	@Test
	public void testPost() {
		
	}

	@Test
	public void testPut() {
	}

	@Test
	public void testHead() {
	}

	@Test
	public void testDelete() {
	}

}

