package org.arivu.nioserver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.arivu.utils.Env;
import org.arivu.utils.Utils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import io.restassured.RestAssured;
import io.restassured.http.Headers;

public class TestHttpMethodsMultiThreaded {
	private static final String TEST_RESOURCE_BASE = "."+File.separator+"src"+File.separator+"test"+File.separator+"resources"+File.separator;
	private static final String TEST_RESOURCE_ADMIN_BASE = TEST_RESOURCE_BASE+"admin";

	static final String ARIVU_NIOSERVER_ZIP = TEST_RESOURCE_BASE+"arivu.nioserver.zip";

	static final String README_MD = TEST_RESOURCE_BASE+"README.md";

	static final String DOWNLOAD_ZIP = TEST_RESOURCE_BASE+"download.zip";

	static final String SRC_TEST_RESOURCES_ADMIN_ADMIN_CSS = TEST_RESOURCE_ADMIN_BASE+File.separator+"Admin.css";

	static final String SRC_TEST_RESOURCES_ADMIN_ADMIN_HTML = TEST_RESOURCE_ADMIN_BASE+File.separator+"Admin.html";

	static final String SRC_TEST_RESOURCES_LIGHTNINGLOG_JSON = TEST_RESOURCE_BASE+"lightninglog.json";

	static final String SRC_TEST_RESOURCES_MULTI_BYTE_TXT = TEST_RESOURCE_BASE+"multiByte.txt";

	// private static final Logger logger =
	// LoggerFactory.getLogger(TestHttpMethods.class);
	static final String port = "8188";

	static ExecutorService exe = Executors.newFixedThreadPool(1);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestApis.runAsync = false;
		init(false, false);
	}

	static void init(boolean singleThread, boolean ssl) throws Exception {
		if(ssl){
			RestAssured.baseURI = "https://localhost:" + port;
			String keyStorePath = Env.getEnv("ssl.ksfile", "nioserver.jks");
			String keyStorePassword = Env.getEnv("ssl.pass", "nioserver");
			
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());
			RestAssured.trustStore(keyStore);
			RestAssured.useRelaxedHTTPSValidation();
			RestAssured.config.getSSLConfig().allowAllHostnames();
			
			System.setProperty("ssl", "true");
			System.setProperty("ssl.protocol", "TLSv1.2");
		}else{
			RestAssured.baseURI = "http://localhost:" + port;
		}
		
		System.setProperty("access.log", "."+File.separator+"logs"+File.separator+"access.test.log");
		System.setProperty("singleThread", ""+singleThread);
		System.setProperty("port", port);

		System.setProperty("adminMod", "true");
		System.setProperty("adminLoc", TEST_RESOURCE_ADMIN_BASE);

		exe.execute(new Runnable() {

			@Override
			public void run() {
				try {
					Server.main(null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		Thread.sleep(1000);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		Server.stop();//.main(new String[] { "stop" });
		exe.shutdownNow();
	}

	@Before
	public void setUp() throws Exception {
//		Thread.sleep(200);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGet1() {
		io.restassured.response.Response response = RestAssured.given().when().get("/test/get1");
		response.then().statusCode(201);
		byte[] responseBodyAsByteArray = response.asByteArray();
		assertTrue(responseBodyAsByteArray.length == Configuration.defaultChunkSize);
		final byte v = (byte) 48;

		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != v) {
				fail("Failed on Get Content!");
			}
		}

	}

	@Test
	public void testGet2() {
		io.restassured.response.Response response = RestAssured.given().when().get("/test/get2");
		response.then().statusCode(201);
		byte[] responseBodyAsByteArray = response.asByteArray();
		assertTrue(responseBodyAsByteArray.length == Configuration.defaultChunkSize + 2);
		final byte v = (byte) 48;

		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != v) {
				fail("Failed on Get Content!");
			}
		}

	}

	@Test
	public void testGet3() {
		io.restassured.response.Response response = RestAssured.given().when().get("/test/get3");
		response.then().statusCode(201);
		byte[] responseBodyAsByteArray = response.asByteArray();
		assertTrue(responseBodyAsByteArray.length == Configuration.defaultChunkSize / 2);
		final byte v = (byte) 48;

		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != v) {
				fail("Failed on Get Content!");
			}
		}

	}

	@Test
	public void testGet4() throws IOException {
		io.restassured.response.Response response = RestAssured.given().when().get("/test/get4");
		response.then().statusCode(200);
		byte[] responseBodyAsByteArray = response.asByteArray();
		File expectedFile = new File(README_MD);
		byte[] read = Utils.read(expectedFile);
		assertTrue(responseBodyAsByteArray.length == read.length);
		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != read[i]) {
				fail("Failed on Get Content!");
			}
		}
	}

	@Test
	public void testGet5() throws IOException {
		io.restassured.response.Response response = RestAssured.given().when().get("/test/get5");
		response.then().statusCode(200);
		byte[] responseBodyAsByteArray = response.asByteArray();
		File expectedFile = new File(SRC_TEST_RESOURCES_MULTI_BYTE_TXT);
		byte[] read = Utils.read(expectedFile);
		assertTrue(responseBodyAsByteArray.length == read.length);
		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != read[i]) {
				fail("Failed on Get Content!");
			}
		}
	}

	@Test
	public void testGet6() throws IOException {
		io.restassured.response.Response response = RestAssured.given().when().get("/test/get6");
		response.then().statusCode(200);
		byte[] responseBodyAsByteArray = response.asByteArray();
		File expectedFile = new File(DOWNLOAD_ZIP);
		byte[] read = Utils.read(expectedFile);
		assertTrue(responseBodyAsByteArray.length == read.length);
		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != read[i]) {
				fail("Failed on Get Content!");
			}
		}
	}

	@Test
	public void testGet7() throws IOException {
		io.restassured.response.Response response = RestAssured.given().param("param1", "value1")
				.param("param2", "value2").when().get("/test/get7");
		response.then().statusCode(200);
		String asString = response.asString();
		// byte[] responseBodyAsByteArray = response.asByteArray();
		String expBodyAsString = "{param1=[value1],param2=[value2]},/test/get7?param1=value1&param2=value2";
		// byte[] read = expBodyAsString.getBytes();
		assertTrue(asString.length() == expBodyAsString.length());
		assertTrue(asString.equals(expBodyAsString));
	}

	@Test
	public void testGet8() throws IOException {
		io.restassured.response.Response response = RestAssured.given().param("param1", "value1")
				.param("param2", "value2").when().get("/test/get101");
		response.then().statusCode(404);
	}

	@Test
	public void testGet9() throws IOException {
		io.restassured.response.Response response = RestAssured.given().param("param1", "value1")
				.param("param2", "value2").when().get("/test/getexp");
		response.then().statusCode(400);
	}

	@Test
	public void testGetProxyDir1() throws IOException {
		io.restassured.response.Response response = RestAssured.given().when().get("/testproxydir");
		response.then().statusCode(200);
		String asString = response.asString();

		io.restassured.response.Response response1 = RestAssured.given().when().get("/testproxydir/");
		response1.then().statusCode(200);
		String asString1 = response1.asString();

		assertFalse(asString.equals(asString1));
	}

	@Test
	public void testGetProxyDir2() throws IOException {
		io.restassured.response.Response response = RestAssured.given().when().get("/testproxydir/test");
		response.then().statusCode(404);
	}

	@Test
	public void testGetProxyDir3() throws IOException {
		// response :: 369 read :: 374
		io.restassured.response.Response response = RestAssured.given().when().get("/testproxydir/lightninglog.json");
		response.then().statusCode(200);
		byte[] responseBodyAsByteArray = response.asByteArray();
		File expectedFile = new File(SRC_TEST_RESOURCES_LIGHTNINGLOG_JSON);
		byte[] read = Utils.read(expectedFile);
		// System.out.println("******** response ::
		// "+responseBodyAsByteArray.length+" read :: "+read.length);
		assertTrue(responseBodyAsByteArray.length == read.length);
		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != read[i]) {
				fail("Failed on Get Content!");
			}
		}
		Headers headers = response.getHeaders();
		assertTrue("Failed on header Expires! got :: " + headers.getValue("Expires"),
				headers.getValue("Expires").equals("-1"));
		assertTrue("Failed on header Cache-Control! got :: " + headers.getValue("Cache-Control"),
				headers.getValue("Cache-Control").equals("private, max-age=0"));
		assertTrue("Failed on header Server! got :: " + headers.getValue("Server"),
				headers.getValue("Server").equals("clowntestserver"));
		assertTrue("Failed on header X-XSS-Protection! got :: " + headers.getValue("X-XSS-Protection"),
				headers.getValue("X-XSS-Protection").equals("1; mode=block"));
		assertTrue("Failed on header Connection! got :: " + headers.getValue("Connection"),
				headers.getValue("Connection").equals("keepalive"));
		assertTrue("Failed on header Content-Disposition! got :: " + headers.getValue("Content-Disposition"),
				headers.getValue("Content-Disposition").equals("inline; filename=\"" + expectedFile.getName() + "\""));
		assertTrue("Failed on header Content-Type! got :: " + headers.getValue("Content-Type"),
				headers.getValue("Content-Type").equals("application/json"));
		assertTrue("Failed on header Content-Length! got :: " + headers.getValue("Content-Length"),
				headers.getValue("Content-Length").equals("" + expectedFile.length()));

		String cookie = response.cookie("theme");
		assertTrue("Failed on cookie theme! got:: " + cookie, cookie.equals("light"));

	}

	@Test
	@Ignore
	public void testGetProxy1() throws IOException {
		try {
			io.restassured.response.Response response = RestAssured.given().when().get("/search");
			response.then().statusCode(200);
			// String asString = response.asString();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetAdminProxy1() throws IOException {
		io.restassured.response.Response response = RestAssured.given().when().get("/admin");
		response.then().statusCode(200);
		byte[] responseBodyAsByteArray = response.asByteArray();
		File expectedFile = new File(SRC_TEST_RESOURCES_ADMIN_ADMIN_HTML);
		byte[] read = Utils.read(expectedFile);
		assertTrue(responseBodyAsByteArray.length == read.length);
		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != read[i]) {
				fail("Failed on Get Content!");
			}
		}
	}

	@Test
	public void testGetAdminProxy2() throws IOException {
		io.restassured.response.Response response = RestAssured.given().when().get("/admin/Admin.html");
		response.then().statusCode(200);
		byte[] responseBodyAsByteArray = response.asByteArray();
		File expectedFile = new File(SRC_TEST_RESOURCES_ADMIN_ADMIN_HTML);
		byte[] read = Utils.read(expectedFile);
		assertTrue(responseBodyAsByteArray.length == read.length);
		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != read[i]) {
				fail("Failed on Get Content!");
			}
		}
	}

	@Test
	public void testGetAdminProxy3() throws IOException {
		io.restassured.response.Response response = RestAssured.given().when().get("/admin/Admin.css");
		response.then().statusCode(200);
		byte[] responseBodyAsByteArray = response.asByteArray();
		File expectedFile = new File(SRC_TEST_RESOURCES_ADMIN_ADMIN_CSS);
		byte[] read = Utils.read(expectedFile);
		assertTrue(responseBodyAsByteArray.length == read.length);
		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != read[i]) {
				fail("Failed on Get Content!");
			}
		}
	}

	@Test
	public void testPostMultipart1() throws IOException {
		File inputFile = new File(ARIVU_NIOSERVER_ZIP);
		File expectedFile = new File("1_arivu.nioserver.zip");
		if (expectedFile.exists())
			expectedFile.delete();
		
		io.restassured.response.Response response = RestAssured.given().multiPart(inputFile).when()
				.post("/test/multipart");

		response.then().statusCode(200);
		byte[] responseBodyAsByteArray = Utils.read(inputFile);
		byte[] read = Utils.read(expectedFile);
		assertTrue(responseBodyAsByteArray.length == read.length);
		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != read[i]) {
				fail("Failed on Get Content!");
			}
		}
		expectedFile.delete();
	}

	@Test
	public void testPostMultipart2() throws IOException {
		File inputFile = new File(ARIVU_NIOSERVER_ZIP);
		io.restassured.response.Response response = RestAssured.given().multiPart(inputFile).when()
				.post("/test/multipart1");

		response.then().statusCode(404);
	}

	@Test
//	@Ignore
	public void testPostMultipart3() throws IOException, InterruptedException {
		int oldValue = Configuration.defaultRequestBuffer;
		Configuration.defaultRequestBuffer = 150;
		File inputFile = new File(SRC_TEST_RESOURCES_LIGHTNINGLOG_JSON);
		File expectedFile = new File("1_lightninglog.json");
		if (expectedFile.exists())
			expectedFile.delete();
		io.restassured.response.Response response = RestAssured.given()
				.multiPart(inputFile)
				.multiPart("name","test")
				.multiPart("scanpackages","com.rjil")
				.header("Expires", "-1")
				.header("Cache-Control", "private, max-age=0").header("Server", "clownhggashgasserver")
				.header("X-XSS-Protection", "1; mode=block").header("Connection", "close").when()
				.post("/test/multipart");

		response.then().statusCode(200);
		byte[] responseBodyAsByteArray = Utils.read(inputFile);
		byte[] read = Utils.read(expectedFile);
		assertTrue(responseBodyAsByteArray.length == read.length);
		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != read[i]) {
				fail("Failed on Get Content!");
			}
		}
		expectedFile.delete();
		Configuration.defaultRequestBuffer = oldValue;
	}


	@Test
	public void testPostMultipart4() throws IOException {
		int oldValue = Configuration.defaultRequestBuffer;
		Configuration.defaultRequestBuffer = 150;
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		File inputFile = new File(SRC_TEST_RESOURCES_MULTI_BYTE_TXT);
		File expectedFile = new File("1_multiByte.txt");
		if (expectedFile.exists())
			expectedFile.delete();
		io.restassured.response.Response response = RestAssured.given().multiPart(inputFile).header("Expires", "-1")
				.header("Cache-Control", "private, max-age=0").header("Server", "clownhggashgasserver")
				.header("X-XSS-Protection", "1; mode=block").header("Connection", "close").when()
				.post("/test/multipart");

		response.then().statusCode(200);
		byte[] responseBodyAsByteArray = Utils.read(inputFile);
		byte[] read = Utils.read(expectedFile);
		assertTrue(responseBodyAsByteArray.length == read.length);
		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != read[i]) {
				fail("Failed on Get Content!");
			}
		}
		expectedFile.delete();
		Configuration.defaultRequestBuffer = oldValue;
	}

	@Test
	public void testPostMultipart5() throws IOException {
		int oldValue = Configuration.defaultRequestBuffer;
		Configuration.defaultRequestBuffer = 1024;
		File inputFile = new File(ARIVU_NIOSERVER_ZIP);
		File expectedFile = new File("1_arivu.nioserver.zip");
		if (expectedFile.exists())
			expectedFile.delete();
		io.restassured.response.Response response = RestAssured.given().multiPart(inputFile).when()
				.post("/test/multipart");

		response.then().statusCode(200);
		byte[] responseBodyAsByteArray = Utils.read(inputFile);
		byte[] read = Utils.read(expectedFile);
		assertTrue("Exp :: "+read.length+" but got :: "+responseBodyAsByteArray.length,responseBodyAsByteArray.length == read.length);
		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != read[i]) {
				fail("Failed on Get Content!");
			}
		}
		expectedFile.delete();
		Configuration.defaultRequestBuffer = oldValue;
	}
	
	@Test
	public void testPostMultipart6() throws IOException {
		File inputFile = new File(SRC_TEST_RESOURCES_MULTI_BYTE_TXT);
		File expectedFile = new File("1_multiByte.txt");
		if (expectedFile.exists())
			expectedFile.delete();
		io.restassured.response.Response response = RestAssured.given().multiPart(inputFile).header("Expires", "-1")
				.header("Cache-Control", "private, max-age=0").header("Server", "clownhggashgasserver")
				.header("X-XSS-Protection", "1; mode=block").header("Connection", "close").when()
				.post("/test/multipart");

		response.then().statusCode(200);
		byte[] responseBodyAsByteArray = Utils.read(inputFile);
		byte[] read = Utils.read(expectedFile);
		assertTrue(responseBodyAsByteArray.length == read.length);
		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != read[i]) {
				fail("Failed on Get Content!");
			}
		}
		expectedFile.delete();
	}

	
	@Test
	public void testPost1() throws IOException {
		final String body = "Test POST";
		io.restassured.response.Response response = RestAssured.given().body(body).header("Expires", "-1")
				.header("Cache-Control", "private, max-age=0").header("Server", "clownhggashgasserver")
				.header("X-XSS-Protection", "1; mode=block").header("Connection", "close").when().post("/test/post1");

		assertTrue("failed on post body check!", body.equals(response.asString()));

		Headers headers = response.getHeaders();
		assertTrue("Failed on header Expires! got :: " + headers.getValue("Expires"),
				headers.getValue("Expires").equals("-1"));
		assertTrue("Failed on header Cache-Control! got :: " + headers.getValue("Cache-Control"),
				headers.getValue("Cache-Control").equals("private, max-age=1"));
		assertTrue("Failed on header Server! got :: " + headers.getValue("Server"),
				headers.getValue("Server").equals("clownresserver"));
		assertTrue("Failed on header X-XSS-Protection! got :: " + headers.getValue("X-XSS-Protection"),
				headers.getValue("X-XSS-Protection").equals("1; mode=block"));
		// assertTrue("Failed on header Connection! got ::
		// "+headers.getValue("Connection"),headers.getValue("Connection").equals("keepalive"));
		assertTrue("Failed on header Content-Length! got :: " + headers.getValue("Content-Length"),
				headers.getValue("Content-Length").equals("" + body.length()));

		String cookie = response.cookie("rest");
		assertTrue("Failed on cookie theme! got:: " + cookie, cookie.equals("back"));

	}

	@Test
	public void testPost2() throws IOException {
		final String body = "username=user1&password=password1";
		io.restassured.response.Response response = RestAssured.given().
		// body(body).
				formParam("username", "user1").formParam("password", "password1")
				.header("Content-Type", "application/x-www-form-urlencoded").header("Expires", "-1")
				.header("Cache-Control", "private, max-age=0").header("Server", "clownhggashgasserver")
				.header("X-XSS-Protection", "1; mode=block").header("Connection", "close").when().post("/test/post1");

		// System.out.println("_____******** "+response.asString());

		assertTrue("failed on post body check!", body.equals(response.asString()));

		Headers headers = response.getHeaders();
		assertTrue("Failed on header Expires! got :: " + headers.getValue("Expires"),
				headers.getValue("Expires").equals("-1"));
		assertTrue("Failed on header Cache-Control! got :: " + headers.getValue("Cache-Control"),
				headers.getValue("Cache-Control").equals("private, max-age=1"));
		assertTrue("Failed on header Server! got :: " + headers.getValue("Server"),
				headers.getValue("Server").equals("clownresserver"));
		assertTrue("Failed on header X-XSS-Protection! got :: " + headers.getValue("X-XSS-Protection"),
				headers.getValue("X-XSS-Protection").equals("1; mode=block"));
		// assertTrue("Failed on header Connection! got ::
		// "+headers.getValue("Connection"),headers.getValue("Connection").equals("keepalive"));
		assertTrue("Failed on header Content-Length! got :: " + headers.getValue("Content-Length"),
				headers.getValue("Content-Length").equals("" + body.length()));

		String cookie = response.cookie("rest");
		assertTrue("Failed on cookie theme! got:: " + cookie, cookie.equals("back"));

	}

	@Test
	public void testPost3() throws IOException {
		int oldValue = Configuration.defaultRequestBuffer;
		Configuration.defaultRequestBuffer = 150;
		final byte[] body = Utils.read(new File(SRC_TEST_RESOURCES_LIGHTNINGLOG_JSON));
		io.restassured.response.Response response = RestAssured.given().header("Expires", "-1")
				.header("Cache-Control", "private, max-age=0").header("Server", "clownhggashgasserver")
				.header("X-XSS-Protection", "1; mode=block").
				// header("Content-Length", body.length).
				header("Connection", "close").body(body).when().post("/test/post1");

		byte[] responseBodyAsByteArray = response.asByteArray();
		// System.out.println("\n\n*****************************%\n"+new
		// String(responseBodyAsByteArray)+"\n%*****************************\n\n");
		assertTrue(
				"failed in multibyte contentlen check exp::" + body.length + " got::" + responseBodyAsByteArray.length,
				responseBodyAsByteArray.length == body.length);
		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != body[i]) {
				fail("Failed on Get Content!");
			}
		}

		Headers headers = response.getHeaders();
		assertTrue("Failed on header Expires! got :: " + headers.getValue("Expires"),
				headers.getValue("Expires").equals("-1"));
		assertTrue("Failed on header Cache-Control! got :: " + headers.getValue("Cache-Control"),
				headers.getValue("Cache-Control").equals("private, max-age=1"));
		assertTrue("Failed on header Server! got :: " + headers.getValue("Server"),
				headers.getValue("Server").equals("clownresserver"));
		assertTrue("Failed on header X-XSS-Protection! got :: " + headers.getValue("X-XSS-Protection"),
				headers.getValue("X-XSS-Protection").equals("1; mode=block"));
		// assertTrue("Failed on header Connection! got ::
		// "+headers.getValue("Connection"),headers.getValue("Connection").equals("keepalive"));
		assertTrue("Failed on header Content-Length! got :: " + headers.getValue("Content-Length"),
				headers.getValue("Content-Length").equals("" + body.length));

		String cookie = response.cookie("rest");
		assertTrue("Failed on cookie theme! got:: " + cookie, cookie.equals("back"));
		Configuration.defaultRequestBuffer = oldValue;
	}

	@Test
	public void testPost4() throws IOException {
		int oldValue = Configuration.defaultRequestBuffer;
		Configuration.defaultRequestBuffer = 150;
		final byte[] body = Utils.read(new File(SRC_TEST_RESOURCES_LIGHTNINGLOG_JSON));
		io.restassured.response.Response response = RestAssured.given().header("Expires", "-1")
				.header("Cache-Control", "private, max-age=0").header("Server", "clownhggashgasserver")
				.header("X-XSS-Protection", "1; mode=block").header("Connection", "close").body(body).when()
				.post("/test/post101");

		response.then().statusCode(404);
		Configuration.defaultRequestBuffer = oldValue;
	}

	@Test
	public void testPost5() throws IOException {
		int oldValue = Configuration.defaultRequestBuffer;
		Configuration.defaultRequestBuffer = 150;
		final byte[] body = Utils.read(new File(SRC_TEST_RESOURCES_MULTI_BYTE_TXT));
		io.restassured.response.Response response = RestAssured.given().header("Expires", "-1")
				.header("Cache-Control", "private, max-age=0").header("Server", "clownhggashgasserver")
				.header("X-XSS-Protection", "1; mode=block").
				// header("Content-Length", body.length).
				header("Connection", "close").body(body).when().post("/test/post1");

		byte[] responseBodyAsByteArray = response.asByteArray();
		// System.out.println("\n\n*****************************%\n"+new
		// String(responseBodyAsByteArray)+"\n%*****************************\n\n");
		assertTrue(
				"failed in multibyte contentlen check exp::" + body.length + " got::" + responseBodyAsByteArray.length,
				responseBodyAsByteArray.length == body.length);
		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != body[i]) {
				fail("Failed on Get Content!");
			}
		}

		Headers headers = response.getHeaders();
		assertTrue("Failed on header Expires! got :: " + headers.getValue("Expires"),
				headers.getValue("Expires").equals("-1"));
		assertTrue("Failed on header Cache-Control! got :: " + headers.getValue("Cache-Control"),
				headers.getValue("Cache-Control").equals("private, max-age=1"));
		assertTrue("Failed on header Server! got :: " + headers.getValue("Server"),
				headers.getValue("Server").equals("clownresserver"));
		assertTrue("Failed on header X-XSS-Protection! got :: " + headers.getValue("X-XSS-Protection"),
				headers.getValue("X-XSS-Protection").equals("1; mode=block"));
		// assertTrue("Failed on header Connection! got ::
		// "+headers.getValue("Connection"),headers.getValue("Connection").equals("keepalive"));
		assertTrue("Failed on header Content-Length! got :: " + headers.getValue("Content-Length"),
				headers.getValue("Content-Length").equals("" + body.length));

		String cookie = response.cookie("rest");
		assertTrue("Failed on cookie theme! got:: " + cookie, cookie.equals("back"));
		Configuration.defaultRequestBuffer = oldValue;
	}

	@Test
	public void testPost6() throws IOException {
		int oldValue = Configuration.defaultRequestBuffer;
		Configuration.defaultRequestBuffer = 150;
		final byte[] body = Utils.read(new File(SRC_TEST_RESOURCES_MULTI_BYTE_TXT));
		io.restassured.response.Response response = RestAssured.given().body(body).header("Expires", "-1")
				.header("Cache-Control", "private, max-age=0").header("Server", "clownhggashgasserver")
				.header("X-XSS-Protection", "1; mode=block").header("Connection", "close").when().post("/test/post101");

		response.then().statusCode(404);
		Configuration.defaultRequestBuffer = oldValue;
	}

	@Test
	public void testPost7() throws IOException {
		int oldValue = Configuration.defaultRequestBuffer;
		Configuration.defaultRequestBuffer = 150;
		final byte[] body = "".getBytes();// Utils.read(new
											// File("multiByte.txt"));
		io.restassured.response.Response response = RestAssured.given().body(body).header("Expires", "-1")
				.header("Cache-Control", "private, max-age=0").header("Server", "clownhggashgasserver")
				.header("X-XSS-Protection", "1; mode=block").header("Connection", "close").when().post("/test/post1");

		response.then().statusCode(200);

		byte[] responseBodyAsByteArray = response.asByteArray();
		// System.out.println("\n\n*****************************%\n"+new
		// String(responseBodyAsByteArray)+"\n%*****************************\n\n");
		assertTrue(
				"failed in multibyte contentlen check exp::" + body.length + " got::" + responseBodyAsByteArray.length,
				responseBodyAsByteArray.length == body.length);
		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != body[i]) {
				fail("Failed on Get Content!");
			}
		}

		Configuration.defaultRequestBuffer = oldValue;
	}

	@Test
	public void testPost8() throws IOException {
		int oldValue = Configuration.defaultRequestBuffer;
		Configuration.defaultRequestBuffer = 150;
		final byte[] body = Utils.read(new File(SRC_TEST_RESOURCES_MULTI_BYTE_TXT));
		io.restassured.response.Response response = RestAssured.given().body(body).header("Expires", "-1")
				.header("Cache-Control", "private, max-age=0").header("Server", "clownhggashgasserver")
				.header("X-XSS-Protection", "1; mode=block").header("Connection", "close").when().post("/test/postexp");

		response.then().statusCode(400);
		Configuration.defaultRequestBuffer = oldValue;
	}

	@Test
	public void testPut1() throws IOException {
		final String body = "Test PUT";
		io.restassured.response.Response response = RestAssured.given().body(body).header("Expires", "-1")
				.header("Cache-Control", "private, max-age=0").header("Server", "clownhggashgasserver")
				.header("X-XSS-Protection", "1; mode=block").header("Connection", "close").when().put("/test/put1");

		assertTrue("failed on post body check!", body.equals(response.asString()));

		Headers headers = response.getHeaders();
		assertTrue("Failed on header Expires! got :: " + headers.getValue("Expires"),
				headers.getValue("Expires").equals("-1"));
		assertTrue("Failed on header Cache-Control! got :: " + headers.getValue("Cache-Control"),
				headers.getValue("Cache-Control").equals("private, max-age=1"));
		assertTrue("Failed on header Server! got :: " + headers.getValue("Server"),
				headers.getValue("Server").equals("clownresserver"));
		assertTrue("Failed on header X-XSS-Protection! got :: " + headers.getValue("X-XSS-Protection"),
				headers.getValue("X-XSS-Protection").equals("1; mode=block"));
		// assertTrue("Failed on header Connection! got ::
		// "+headers.getValue("Connection"),headers.getValue("Connection").equals("keepalive"));
		assertTrue("Failed on header Content-Length! got :: " + headers.getValue("Content-Length"),
				headers.getValue("Content-Length").equals("" + body.length()));

		String cookie = response.cookie("rest");
		assertTrue("Failed on cookie theme! got:: " + cookie, cookie.equals("back"));

	}

	@Test
	public void testPut2() throws IOException {
		final String body = "username=user1&password=password1";
		io.restassured.response.Response response = RestAssured.given().
		// body(body).
				formParam("username", "user1").formParam("password", "password1")
				.header("Content-Type", "application/x-www-form-urlencoded").header("Expires", "-1")
				.header("Cache-Control", "private, max-age=0").header("Server", "clownhggashgasserver")
				.header("X-XSS-Protection", "1; mode=block").header("Connection", "close").when().put("/test/put1");

		// System.out.println("_____******** "+response.asString());

		assertTrue("failed on post body check!", body.equals(response.asString()));

		Headers headers = response.getHeaders();
		assertTrue("Failed on header Expires! got :: " + headers.getValue("Expires"),
				headers.getValue("Expires").equals("-1"));
		assertTrue("Failed on header Cache-Control! got :: " + headers.getValue("Cache-Control"),
				headers.getValue("Cache-Control").equals("private, max-age=1"));
		assertTrue("Failed on header Server! got :: " + headers.getValue("Server"),
				headers.getValue("Server").equals("clownresserver"));
		assertTrue("Failed on header X-XSS-Protection! got :: " + headers.getValue("X-XSS-Protection"),
				headers.getValue("X-XSS-Protection").equals("1; mode=block"));
		// assertTrue("Failed on header Connection! got ::
		// "+headers.getValue("Connection"),headers.getValue("Connection").equals("keepalive"));
		assertTrue("Failed on header Content-Length! got :: " + headers.getValue("Content-Length"),
				headers.getValue("Content-Length").equals("" + body.length()));

		String cookie = response.cookie("rest");
		assertTrue("Failed on cookie theme! got:: " + cookie, cookie.equals("back"));

	}

	@Test
	public void testPut3() throws IOException {
		int oldValue = Configuration.defaultRequestBuffer;
		Configuration.defaultRequestBuffer = 150;
		final byte[] body = Utils.read(new File(SRC_TEST_RESOURCES_LIGHTNINGLOG_JSON));
		io.restassured.response.Response response = RestAssured.given().header("Expires", "-1")
				.header("Cache-Control", "private, max-age=0").header("Server", "clownhggashgasserver")
				.header("X-XSS-Protection", "1; mode=block").
				// header("Content-Length", body.length).
				header("Connection", "close").body(body).when().put("/test/put1");

		byte[] responseBodyAsByteArray = response.asByteArray();
		// System.out.println("\n\n*****************************%\n"+new
		// String(responseBodyAsByteArray)+"\n%*****************************\n\n");
		assertTrue(
				"failed in multibyte contentlen check exp::" + body.length + " got::" + responseBodyAsByteArray.length,
				responseBodyAsByteArray.length == body.length);
		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != body[i]) {
				fail("Failed on Get Content!");
			}
		}

		Headers headers = response.getHeaders();
		assertTrue("Failed on header Expires! got :: " + headers.getValue("Expires"),
				headers.getValue("Expires").equals("-1"));
		assertTrue("Failed on header Cache-Control! got :: " + headers.getValue("Cache-Control"),
				headers.getValue("Cache-Control").equals("private, max-age=1"));
		assertTrue("Failed on header Server! got :: " + headers.getValue("Server"),
				headers.getValue("Server").equals("clownresserver"));
		assertTrue("Failed on header X-XSS-Protection! got :: " + headers.getValue("X-XSS-Protection"),
				headers.getValue("X-XSS-Protection").equals("1; mode=block"));
		// assertTrue("Failed on header Connection! got ::
		// "+headers.getValue("Connection"),headers.getValue("Connection").equals("keepalive"));
		assertTrue("Failed on header Content-Length! got :: " + headers.getValue("Content-Length"),
				headers.getValue("Content-Length").equals("" + body.length));

		String cookie = response.cookie("rest");
		assertTrue("Failed on cookie theme! got:: " + cookie, cookie.equals("back"));
		Configuration.defaultRequestBuffer = oldValue;
	}

	@Test
	public void testPut4() throws IOException {
		int oldValue = Configuration.defaultRequestBuffer;
		Configuration.defaultRequestBuffer = 150;
		final byte[] body = Utils.read(new File(SRC_TEST_RESOURCES_LIGHTNINGLOG_JSON));
		io.restassured.response.Response response = RestAssured.given().header("Expires", "-1")
				.header("Cache-Control", "private, max-age=0").header("Server", "clownhggashgasserver")
				.header("X-XSS-Protection", "1; mode=block").header("Connection", "close").body(body).when()
				.put("/test/put101");

		response.then().statusCode(404);
		Configuration.defaultRequestBuffer = oldValue;
	}

	@Test
	public void testPut5() throws IOException {
		int oldValue = Configuration.defaultRequestBuffer;
		Configuration.defaultRequestBuffer = 150;
		final byte[] body = Utils.read(new File(SRC_TEST_RESOURCES_MULTI_BYTE_TXT));
		io.restassured.response.Response response = RestAssured.given().header("Expires", "-1")
				.header("Cache-Control", "private, max-age=0").header("Server", "clownhggashgasserver")
				.header("X-XSS-Protection", "1; mode=block").
				// header("Content-Length", body.length).
				header("Connection", "close").body(body).when().put("/test/put1");

		byte[] responseBodyAsByteArray = response.asByteArray();
		// System.out.println("\n\n*****************************%\n"+new
		// String(responseBodyAsByteArray)+"\n%*****************************\n\n");
		assertTrue(
				"failed in multibyte contentlen check exp::" + body.length + " got::" + responseBodyAsByteArray.length,
				responseBodyAsByteArray.length == body.length);
		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != body[i]) {
				fail("Failed on Get Content!");
			}
		}

		Headers headers = response.getHeaders();
		assertTrue("Failed on header Expires! got :: " + headers.getValue("Expires"),
				headers.getValue("Expires").equals("-1"));
		assertTrue("Failed on header Cache-Control! got :: " + headers.getValue("Cache-Control"),
				headers.getValue("Cache-Control").equals("private, max-age=1"));
		assertTrue("Failed on header Server! got :: " + headers.getValue("Server"),
				headers.getValue("Server").equals("clownresserver"));
		assertTrue("Failed on header X-XSS-Protection! got :: " + headers.getValue("X-XSS-Protection"),
				headers.getValue("X-XSS-Protection").equals("1; mode=block"));
		// assertTrue("Failed on header Connection! got ::
		// "+headers.getValue("Connection"),headers.getValue("Connection").equals("keepalive"));
		assertTrue("Failed on header Content-Length! got :: " + headers.getValue("Content-Length"),
				headers.getValue("Content-Length").equals("" + body.length));

		String cookie = response.cookie("rest");
		assertTrue("Failed on cookie theme! got:: " + cookie, cookie.equals("back"));
		Configuration.defaultRequestBuffer = oldValue;
	}

	@Test
	public void testPut6() throws IOException {
		int oldValue = Configuration.defaultRequestBuffer;
		Configuration.defaultRequestBuffer = 150;
		final byte[] body = Utils.read(new File(SRC_TEST_RESOURCES_MULTI_BYTE_TXT));
		io.restassured.response.Response response = RestAssured.given().body(body).header("Expires", "-1")
				.header("Cache-Control", "private, max-age=0").header("Server", "clownhggashgasserver")
				.header("X-XSS-Protection", "1; mode=block").header("Connection", "close").when().put("/test/put101");

		response.then().statusCode(404);
		Configuration.defaultRequestBuffer = oldValue;
	}

	@Test
	public void testPut7() throws IOException {
		int oldValue = Configuration.defaultRequestBuffer;
		Configuration.defaultRequestBuffer = 150;
		final byte[] body = "".getBytes();
		io.restassured.response.Response response = RestAssured.given().body(body).header("Expires", "-1")
				.header("Cache-Control", "private, max-age=0").header("Server", "clownhggashgasserver")
				.header("X-XSS-Protection", "1; mode=block").header("Connection", "close").when().put("/test/put1");

		response.then().statusCode(200);

		byte[] responseBodyAsByteArray = response.asByteArray();
		assertTrue(
				"failed in multibyte contentlen check exp::" + body.length + " got::" + responseBodyAsByteArray.length,
				responseBodyAsByteArray.length == body.length);
		for (int i = 0; i < responseBodyAsByteArray.length; i++) {
			if (responseBodyAsByteArray[i] != body[i]) {
				fail("Failed on Get Content!");
			}
		}

		Configuration.defaultRequestBuffer = oldValue;
	}

	@Test
	public void testPut8() throws IOException {
		int oldValue = Configuration.defaultRequestBuffer;
		Configuration.defaultRequestBuffer = 150;
		final byte[] body = Utils.read(new File(SRC_TEST_RESOURCES_MULTI_BYTE_TXT));
		io.restassured.response.Response response = RestAssured.given().body(body).header("Expires", "-1")
				.header("Cache-Control", "private, max-age=0").header("Server", "clownhggashgasserver")
				.header("X-XSS-Protection", "1; mode=block").header("Connection", "close").when().put("/test/putexp");

		response.then().statusCode(400);
		Configuration.defaultRequestBuffer = oldValue;
	}

	@Test
	public void testHead1() {
		io.restassured.response.Response response = RestAssured.given().when().head("/test/get1");
		response.then().statusCode(201);
		byte[] responseBodyAsByteArray = response.asByteArray();
		assertTrue(responseBodyAsByteArray.length == 0);

		Headers headers = response.getHeaders();
		assertTrue("Failed on header Expires! got :: " + headers.getValue("Expires"),
				headers.getValue("Expires").equals("-1"));
		assertTrue("Failed on header Cache-Control! got :: " + headers.getValue("Cache-Control"),
				headers.getValue("Cache-Control").equals("private, max-age=1"));
		assertTrue("Failed on header Server! got :: " + headers.getValue("Server"),
				headers.getValue("Server").equals("clownresserver"));
		assertTrue("Failed on header X-XSS-Protection! got :: " + headers.getValue("X-XSS-Protection"),
				headers.getValue("X-XSS-Protection").equals("1; mode=block"));

		String cookie = response.cookie("rest");
		assertTrue("Failed on cookie theme! got:: " + cookie, cookie.equals("back"));
	}

	@Test
	public void testHead2() throws IOException {
		io.restassured.response.Response response = RestAssured.given().when().head("/test/get4");
		response.then().statusCode(200);
		byte[] responseBodyAsByteArray = response.asByteArray();
		assertTrue(responseBodyAsByteArray.length == 0);

		Headers headers = response.getHeaders();
		assertTrue("Failed on header Expires! got :: " + headers.getValue("Expires"),
				headers.getValue("Expires").equals("-1"));
		assertTrue("Failed on header Cache-Control! got :: " + headers.getValue("Cache-Control"),
				headers.getValue("Cache-Control").equals("private, max-age=1"));
		assertTrue("Failed on header Server! got :: " + headers.getValue("Server"),
				headers.getValue("Server").equals("clownresserver"));
		assertTrue("Failed on header X-XSS-Protection! got :: " + headers.getValue("X-XSS-Protection"),
				headers.getValue("X-XSS-Protection").equals("1; mode=block"));

		String cookie = response.cookie("rest");
		assertTrue("Failed on cookie theme! got:: " + cookie, cookie.equals("back"));
	}

	@Test
	public void testHead3() throws IOException {
		io.restassured.response.Response response = RestAssured.given().when().head("/test/get5");
		response.then().statusCode(200);
		byte[] responseBodyAsByteArray = response.asByteArray();
		assertTrue(responseBodyAsByteArray.length == 0);

		Headers headers = response.getHeaders();
		assertTrue("Failed on header Expires! got :: " + headers.getValue("Expires"),
				headers.getValue("Expires").equals("-1"));
		assertTrue("Failed on header Cache-Control! got :: " + headers.getValue("Cache-Control"),
				headers.getValue("Cache-Control").equals("private, max-age=1"));
		assertTrue("Failed on header Server! got :: " + headers.getValue("Server"),
				headers.getValue("Server").equals("clownresserver"));
		assertTrue("Failed on header X-XSS-Protection! got :: " + headers.getValue("X-XSS-Protection"),
				headers.getValue("X-XSS-Protection").equals("1; mode=block"));

		String cookie = response.cookie("rest");
		assertTrue("Failed on cookie theme! got:: " + cookie, cookie.equals("back"));

	}

	@Test
	public void testHead4() throws IOException {
		io.restassured.response.Response response = RestAssured.given().when().head("/test/get6");
		response.then().statusCode(200);
		byte[] responseBodyAsByteArray = response.asByteArray();
		assertTrue(responseBodyAsByteArray.length == 0);

		Headers headers = response.getHeaders();
		assertTrue("Failed on header Expires! got :: " + headers.getValue("Expires"),
				headers.getValue("Expires").equals("-1"));
		assertTrue("Failed on header Cache-Control! got :: " + headers.getValue("Cache-Control"),
				headers.getValue("Cache-Control").equals("private, max-age=1"));
		assertTrue("Failed on header Server! got :: " + headers.getValue("Server"),
				headers.getValue("Server").equals("clownresserver"));
		assertTrue("Failed on header X-XSS-Protection! got :: " + headers.getValue("X-XSS-Protection"),
				headers.getValue("X-XSS-Protection").equals("1; mode=block"));

		String cookie = response.cookie("rest");
		assertTrue("Failed on cookie theme! got:: " + cookie, cookie.equals("back"));
	}

	@Test
	public void testHead5() throws IOException {
		io.restassured.response.Response response = RestAssured.given().param("param1", "value1")
				.param("param2", "value2").when().head("/test/get101");
		response.then().statusCode(404);
	}

	@Test
	public void testHead6() throws IOException {
		io.restassured.response.Response response = RestAssured.given().param("param1", "value1")
				.param("param2", "value2").when().head("/test/getexp");
		response.then().statusCode(400);
	}

	@Test
	public void testDelete() throws IOException {
		io.restassured.response.Response response = RestAssured.given().when().delete("/test/del1");
		response.then().statusCode(ResponseCodes.Accepted);
		byte[] responseBodyAsByteArray = response.asByteArray();
		assertTrue(responseBodyAsByteArray.length == 0);

		Headers headers = response.getHeaders();
		assertTrue("Failed on header Expires! got :: " + headers.getValue("Expires"),
				headers.getValue("Expires").equals("-1"));
		assertTrue("Failed on header Cache-Control! got :: " + headers.getValue("Cache-Control"),
				headers.getValue("Cache-Control").equals("private, max-age=1"));
		assertTrue("Failed on header Server! got :: " + headers.getValue("Server"),
				headers.getValue("Server").equals("clownresserver"));
		assertTrue("Failed on header X-XSS-Protection! got :: " + headers.getValue("X-XSS-Protection"),
				headers.getValue("X-XSS-Protection").equals("1; mode=block"));

		String cookie = response.cookie("rest");
		assertTrue("Failed on cookie theme! got:: " + cookie, cookie.equals("back"));
	}

}
