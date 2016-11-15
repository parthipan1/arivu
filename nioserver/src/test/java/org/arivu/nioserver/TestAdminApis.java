package org.arivu.nioserver;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.script.ScriptException;

import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.utils.Ason;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.restassured.RestAssured;

public class TestAdminApis {
//	private static final Logger logger = LoggerFactory.getLogger(TestHttpMethods.class);
	static final String port = "8188";
	
	static ExecutorService exe = Executors.newFixedThreadPool(1);
	
	static void init(String singleThread) throws InterruptedException {
		RestAssured.baseURI = "http://localhost:"+port;
		System.setProperty("access.log", "./logs/access.log");
		System.setProperty("singleThread", singleThread);
		System.setProperty("port", port);
		
		System.setProperty("adminMod", "true");
		System.setProperty("adminLoc", "./src/test/resources/admin");
		System.setProperty("deployLoc", "./src/test/resources/apps");
		
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
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		init("true");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		Server.main(new String[]{"stop"});
		exe.shutdownNow();
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRoutes() throws ScriptException {
		io.restassured.response.Response response = RestAssured.given().when().get("/__admin/routes");
		response.then().statusCode(ResponseCodes.Unauthorized);
		
		io.restassured.response.Response pageRes = RestAssured.given().
				when().get("/admin/Admin.html");
		pageRes.then().statusCode(200);
		
		String hash = String.valueOf(System.currentTimeMillis());
		String refer = RestAssured.baseURI +"/admin/Admin.html";
		
		io.restassured.response.Response response2 = RestAssured.given().
				header("Referer", refer).
				header("X-HASH", hash).
				header("Expires", "-1").
				header("Cache-Control", "private, max-age=0").
				header("Server", "clownhggashgasserver").
				header("X-XSS-Protection", "1; mode=block").
				header("Connection", "close").
				when().get("/__admin/routes");
		
		response2.then().statusCode(200);
		
		String json = "{\"list\":"+response2.asString()+"}";
		Map<String, Object> fromJson = new Ason().fromJson(json);
		Collection<Map<String,Object>> array = Ason.getList(fromJson, "list", new DoublyLinkedList<Map<String,Object>>());
		final int count = array.size();
		int activeCnt = 0;
		for(Map<String,Object> obj:array){
			if( Boolean.parseBoolean(obj.get("active").toString()) )
				activeCnt++;
		}
		
		String postBody = "{\"uri\":\"/uri1\",\"name\":\"name1\",\"loc\":\"loc1\",\"type\":\"browser\"}";

		final byte[] body = postBody.getBytes();//RequestUtil.read(new File("multiByte.txt"));
		io.restassured.response.Response response3 = RestAssured.given().
				body(body).
				header("Referer", refer).
				header("X-HASH", hash).
				header("Expires", "-1").
				header("Cache-Control", "private, max-age=0").
				header("Server", "clownhggashgasserver").
				header("X-XSS-Protection", "1; mode=block").
				header("Connection", "close").
					when().post("/__admin/routes");
		
		json = "{\"list\":"+response3.asString()+"}";
		fromJson = new Ason().fromJson(json);
		array = Ason.getList(fromJson, "list", new DoublyLinkedList<Map<String,Object>>());
		int activeCnt2 = 0;
		for(Map<String,Object> obj:array){
			if( Boolean.parseBoolean(obj.get("active").toString()) )
				activeCnt2++;
		}
		assertTrue(count+1==array.size());
		assertTrue(activeCnt+1==activeCnt2);
		
		
		String putBody = "{\"uri\":\"/uri1\",\"method\":\"ALL\",\"active\":\"false\"}";
		io.restassured.response.Response response4 = RestAssured.given().
				body(putBody).
				header("Referer", refer).
				header("X-HASH", hash).
				header("Expires", "-1").
				header("Cache-Control", "private, max-age=0").
				header("Server", "clownhggashgasserver").
				header("X-XSS-Protection", "1; mode=block").
				header("Connection", "close").
					when().put("/__admin/routes");
		
		json = "{\"list\":"+response4.asString()+"}";
		fromJson = new Ason().fromJson(json);
		array = Ason.getList(fromJson, "list", new DoublyLinkedList<Map<String,Object>>());
		int activeCnt3 = 0;
		for(Map<String,Object> obj:array){
			if( Boolean.parseBoolean(obj.get("active").toString()) )
				activeCnt3++;
		}
		
		assertTrue(count+1==array.size());
		assertTrue(activeCnt==activeCnt3);
	}
	
	@Test
	public void testAppsGet() throws ScriptException {
		io.restassured.response.Response response = RestAssured.given().when().get("/__admin/apps");
		response.then().statusCode(ResponseCodes.Unauthorized);
		
		io.restassured.response.Response pageRes = RestAssured.given().
				when().get("/admin/Admin.html");
		pageRes.then().statusCode(200);
		
		String hash = String.valueOf(System.currentTimeMillis());
		String refer = RestAssured.baseURI +"/admin/Admin.html";
		
		io.restassured.response.Response response2 = RestAssured.given().
				header("Referer", refer).
				header("X-HASH", hash).
				header("Expires", "-1").
				header("Cache-Control", "private, max-age=0").
				header("Server", "clownhggashgasserver").
				header("X-XSS-Protection", "1; mode=block").
				header("Connection", "close").
				when().get("/__admin/apps");
		
		response2.then().statusCode(200);
	}

	@Test
	public void testIconGet() throws ScriptException, IOException {
		final byte[] body = RequestUtil.read(new File("favicon.ico"));
		io.restassured.response.Response response = RestAssured.given().when().get("/favicon.ico");
		response.then().statusCode(200);
		
		byte[] responseBodyAsByteArray = response.asByteArray();
		assertTrue("failed in multibyte contentlen check exp::"+body.length+" got::"+responseBodyAsByteArray.length,responseBodyAsByteArray.length==body.length);
		for(int i=0;i<responseBodyAsByteArray.length;i++){
			if( responseBodyAsByteArray[i]!=body[i] ){
				fail("Filed on Get Content!");
			}
		}
	}
	
	@Test
	public void testDeployAndUnDeploy() throws IOException {
		io.restassured.response.Response pageRes = RestAssured.given().
				when().get("/admin/Admin.html");
		pageRes.then().statusCode(200);
		
		String hash = String.valueOf(System.currentTimeMillis());
		String refer = RestAssured.baseURI +"/admin/Admin.html";
		
		final File appDir = new File("./src/test/resources/apps/download");
		
		assertFalse(appDir.exists());
		
		File inputFile = new File("download.zip");
		io.restassured.response.Response response = RestAssured.given().
					multiPart("dist",inputFile).
					multiPart("name","download").
					multiPart("scanpackages","com.rjil.cloud.snw.download").
					multiPart("X-HASH",hash).
						when().post("/__admin/deploy");
		
		response.then().statusCode(201);
		
		assertTrue(appDir.exists());
		
		io.restassured.response.Response response2 = RestAssured.given().
				param("name", "download").
				header("Referer", refer).
				header("X-HASH", hash).
				header("Expires", "-1").
				header("Cache-Control", "private, max-age=0").
				header("Server", "clownhggashgasserver").
				header("X-XSS-Protection", "1; mode=block").
				header("Connection", "close").
				when().get("/__admin/undeploy");
		
		response2.then().statusCode(200);
		
		assertFalse(appDir.exists());
		new File("./src/test/resources/apps").delete();
	}
}


