package org.arivu.nioserver;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestHttpsMethods extends TestHttpMethodsMultiThreaded {
//	private static final Logger logger = LoggerFactory.getLogger(TestHttpMethods.class);
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestApis.runAsync = false;
		init("true", true);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		TestHttpMethodsMultiThreaded.tearDownAfterClass();
	}

	@Before
	public void setUp() throws Exception {
//		Thread.sleep(100);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Override @Test
	public void testGet1() {
		
		super.testGet1();
	}

	@Override @Test
	public void testGet2() {
		
		super.testGet2();
	}

	@Override @Test
	public void testGet3() {
		
		super.testGet3();
	}

	@Override @Test
	public void testGet4() throws IOException {
		
		super.testGet4();
	}

	@Override @Test
	public void testGet5() throws IOException {
		
		super.testGet5();
	}

	@Override @Test
	public void testGet6() throws IOException {
		
		super.testGet6();
	}

	@Override @Test
	public void testGet7() throws IOException {
		
		super.testGet7();
	}

	@Override @Test
	public void testGet8() throws IOException {
		
		super.testGet8();
	}

	@Override @Test
	public void testGet9() throws IOException {
		
		super.testGet9();
	}

	@Override @Test
	public void testGetProxyDir1() throws IOException {
		
		super.testGetProxyDir1();
	}

	@Override @Test
	public void testGetProxyDir2() throws IOException {
		
		super.testGetProxyDir2();
	}

	@Override @Test
	public void testGetProxyDir3() throws IOException {
		
		super.testGetProxyDir3();
	}

	@Override @Test @Ignore
	public void testGetProxy1() throws IOException {
		
		super.testGetProxy1();
	}

	@Override @Test
	public void testGetAdminProxy1() throws IOException {
		
		super.testGetAdminProxy1();
	}

	@Override @Test
	public void testGetAdminProxy2() throws IOException {
		
		super.testGetAdminProxy2();
	}

	@Override @Test
	public void testGetAdminProxy3() throws IOException {
		
		super.testGetAdminProxy3();
	}

	@Override @Test 
	public void testPostMultipart1() throws IOException {
		
		super.testPostMultipart1();
	}

	@Override @Test 
	public void testPostMultipart2() throws IOException {
		
		super.testPostMultipart2();
	}

	@Override @Test
	public void testPostMultipart3() throws IOException, InterruptedException {
		
		super.testPostMultipart3();
	}

	@Override @Test
	public void testPostMultipart4() throws IOException {
		
		super.testPostMultipart4();
	}

	@Override @Test
	public void testPostMultipart5() throws IOException {
		
		super.testPostMultipart5();
	}

	@Override @Test
	public void testPostMultipart6() throws IOException {
		
		super.testPostMultipart6();
	}
	
	@Override @Test
	public void testPost1() throws IOException {
		
		super.testPost1();
	}

	@Override @Test
	public void testPost2() throws IOException {
		
		super.testPost2();
	}

	@Override @Test
	public void testPost3() throws IOException {
		
		super.testPost3();
	}

	@Override @Test
	public void testPost4() throws IOException {
		
		super.testPost4();
	}

	@Override @Test
	public void testPost5() throws IOException {
		
		super.testPost5();
	}

	@Override @Test
	public void testPost6() throws IOException {
		
		super.testPost6();
	}

	@Override @Test
	public void testPost7() throws IOException {
		
		super.testPost7();
	}

	@Override @Test
	public void testPost8() throws IOException {
		
		super.testPost8();
	}

	@Override @Test
	public void testPut1() throws IOException {
		
		super.testPut1();
	}

	@Override @Test
	public void testPut2() throws IOException {
		
		super.testPut2();
	}

	@Override @Test
	public void testPut3() throws IOException {
		
		super.testPut3();
	}

	@Override @Test
	public void testPut4() throws IOException {
		
		super.testPut4();
	}

	@Override @Test
	public void testPut5() throws IOException {
		
		super.testPut5();
	}

	@Override @Test
	public void testPut6() throws IOException {
		
		super.testPut6();
	}

	@Override @Test
	public void testPut7() throws IOException {
		
		super.testPut7();
	}

	@Override @Test
	public void testPut8() throws IOException {
		
		super.testPut8();
	}

	@Override @Test
	public void testHead1() {
		
		super.testHead1();
	}

	@Override @Test
	public void testHead2() throws IOException {
		
		super.testHead2();
	}

	@Override @Test
	public void testHead3() throws IOException {
		
		super.testHead3();
	}

	@Override @Test
	public void testHead4() throws IOException {
		
		super.testHead4();
	}

	@Override @Test
	public void testHead5() throws IOException {
		
		super.testHead5();
	}

	@Override @Test
	public void testHead6() throws IOException {
		
		super.testHead6();
	}

	@Override @Test
	public void testDelete() throws IOException {
		
		super.testDelete();
	}

}
