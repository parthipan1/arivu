package org.arivu.nioserver;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestHttpsMethodsMultiParts  {
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestApis.runAsync = false;
		TestHttpMethodsMultiThreaded.init(true, true);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		TestHttpMethodsMultiThreaded.tearDownAfterClass();
	}
	
	TestHttpMethodsMultiThreaded instance = new TestHttpMethodsMultiThreaded();
	
	@Before
	public void setUp() throws Exception {
		Thread.sleep(200);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testPostMultipart1() throws IOException {
		instance.testPostMultipart1();
	}

	@Test
	public void testPostMultipart2() throws IOException {
		instance.testPostMultipart2();
	}

	@Test
	public void testPostMultipart3() throws IOException, InterruptedException {
		instance.testPostMultipart3();
	}

	@Test
	public void testPostMultipart4() throws IOException {
		instance.testPostMultipart4();
	}

	@Test
	public void testPostMultipart5() throws IOException {
		instance.testPostMultipart5();
	}
	
	@Test
	public void testPostMultipart6() throws IOException {
		instance.testPostMultipart6();
	}

	

}
