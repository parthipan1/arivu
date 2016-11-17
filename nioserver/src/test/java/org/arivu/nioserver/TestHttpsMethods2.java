package org.arivu.nioserver;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestHttpsMethods2 {
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestHttpMethodsMultiThreaded.init("true", true);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		TestHttpMethodsMultiThreaded.tearDownAfterClass();
	}

	@Before
	public void setUp() throws Exception {
//		Thread.sleep(200);
	}
	
	static TestHttpMethodsMultiThreaded instance = new TestHttpMethodsMultiThreaded();
	
	@After
	public void tearDown() throws Exception {
	}
	

//	@Test
//	public void testGet1() {
//		
//		instance.testGet1();
//	}
//
//	@Test
//	public void testGet2() {
//		
//		instance.testGet2();
//	}
//
//	@Test
//	public void testGet3() {
//		
//		instance.testGet3();
//	}
//
//	@Test
//	public void testGet4() throws IOException {
//		
//		instance.testGet4();
//	}
//
//	@Test
//	public void testGet5() throws IOException {
//		
//		instance.testGet5();
//	}
//
//	@Test
//	public void testGet6() throws IOException {
//		
//		instance.testGet6();
//	}
//
//	@Test
//	public void testGet7() throws IOException {
//		
//		instance.testGet7();
//	}
//
//	@Test
//	public void testGet8() throws IOException {
//		
//		instance.testGet8();
//	}
//
//	@Test
//	public void testGet9() throws IOException {
//		
//		instance.testGet9();
//	}
//
//	@Test
//	public void testGetProxyDir1() throws IOException {
//		
//		instance.testGetProxyDir1();
//	}
//
//	@Test
//	public void testGetProxyDir2() throws IOException {
//		
//		instance.testGetProxyDir2();
//	}
//
//	@Test
//	public void testGetProxyDir3() throws IOException {
//		
//		instance.testGetProxyDir3();
//	}
//
//	@Test @Ignore
//	public void testGetProxy1() throws IOException {
//		
//		instance.testGetProxy1();
//	}
//
//	@Test
//	public void testGetAdminProxy1() throws IOException {
//		
//		instance.testGetAdminProxy1();
//	}
//
//	@Test
//	public void testGetAdminProxy2() throws IOException {
//		
//		instance.testGetAdminProxy2();
//	}
//
//	@Test
//	public void testGetAdminProxy3() throws IOException {
//		
//		instance.testGetAdminProxy3();
//	}
	
	@Test
	public void testPost1() throws IOException {
		
		instance.testPost1();
	}

	@Test
	public void testPost2() throws IOException {
		
		instance.testPost2();
	}

	@Test
	public void testPost3() throws IOException {
		
		instance.testPost3();
	}

	@Test
	public void testPost4() throws IOException {
		
		instance.testPost4();
	}

	@Test
	public void testPost5() throws IOException {
		
		instance.testPost5();
	}

	@Test
	public void testPost6() throws IOException {
		
		instance.testPost6();
	}

	@Test
	public void testPost7() throws IOException {
		
		instance.testPost7();
	}

	@Test
	public void testPost8() throws IOException {
		
		instance.testPost8();
	}

	@Test
	public void testPut1() throws IOException {
		
		instance.testPut1();
	}

	@Test
	public void testPut2() throws IOException {
		
		instance.testPut2();
	}

	@Test
	public void testPut3() throws IOException {
		
		instance.testPut3();
	}

	@Test
	public void testPut4() throws IOException {
		
		instance.testPut4();
	}

	@Test
	public void testPut5() throws IOException {
		
		instance.testPut5();
	}

	@Test
	public void testPut6() throws IOException {
		
		instance.testPut6();
	}

	@Test
	public void testPut7() throws IOException {
		
		instance.testPut7();
	}

	@Test
	public void testPut8() throws IOException {
		
		instance.testPut8();
	}

	@Test
	public void testHead1() {
		
		instance.testHead1();
	}

	@Test
	public void testHead2() throws IOException {
		
		instance.testHead2();
	}

	@Test
	public void testHead3() throws IOException {
		
		instance.testHead3();
	}

	@Test
	public void testHead4() throws IOException {
		
		instance.testHead4();
	}

	@Test
	public void testHead5() throws IOException {
		
		instance.testHead5();
	}

	@Test
	public void testHead6() throws IOException {
		
		instance.testHead6();
	}

	@Test
	public void testDelete() throws IOException {
		
		instance.testDelete();
	}

}
