package org.arivu.nioserver;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.arivu.utils.NullCheck;
import org.arivu.utils.Utils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestHttpMethods {
//	private static final Logger logger = LoggerFactory.getLogger(TestHttpMethods.class);
	static TestHttpMethodsMultiThreaded instance = new TestHttpMethodsMultiThreaded();
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestApis.runAsync = false;
		TestHttpMethodsMultiThreaded.init("true", false);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		TestHttpMethodsMultiThreaded.tearDownAfterClass();
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
		
		instance.testGet1();
	}

	@Test
	public void testGet2() {
		
		instance.testGet2();
	}

	@Test
	public void testGet3() {
		
		instance.testGet3();
	}

	@Test
	public void testGet4() throws IOException {
		
		instance.testGet4();
	}

	@Test
	public void testGet5() throws IOException {
		
		instance.testGet5();
	}

	@Test
	public void testGet6() throws IOException {
		
		instance.testGet6();
	}

	@Test
	public void testGet7() throws IOException {
		
		instance.testGet7();
	}

	@Test
	public void testGet8() throws IOException {
		
		instance.testGet8();
	}

	@Test
	public void testGet9() throws IOException {
		
		instance.testGet9();
	}

	@Test
	public void testGetProxyDir1() throws IOException {
		
		instance.testGetProxyDir1();
	}

	@Test
	public void testGetProxyDir2() throws IOException {
		
		instance.testGetProxyDir2();
	}

	@Test
	public void testGetProxyDir3() throws IOException {
		
		instance.testGetProxyDir3();
	}

	@Test @Ignore
	public void testGetProxy1() throws IOException {
		
		instance.testGetProxy1();
	}

	@Test
	public void testGetAdminProxy1() throws IOException {
		
		instance.testGetAdminProxy1();
	}

	@Test
	public void testGetAdminProxy2() throws IOException {
		
		instance.testGetAdminProxy2();
	}

	@Test
	public void testGetAdminProxy3() throws IOException {
		
		instance.testGetAdminProxy3();
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

final class TestApis{

	interface Handler{
		void handle(Request request, Response response);
	}
	
	static boolean runAsync = false;
	
	static void execute(final Handler handler,final Request request,final Response response){
		if( runAsync ){
			final AsynContext asynContext = StaticRef.getAsynContext();
			asynContext.setAsynchronousFinish(true);
			Server.getExecutorService().execute(new Runnable() {
				
				@Override
				public void run() {
					try {
						handler.handle(request, response);
					} catch (Throwable e) {
						response.setResponseCode(400);
					}finally {
						asynContext.finish();
					}
				}
			});
		}else{
			handler.handle(request, response);
		}
	}
	
	@Path(value="/test/del1",httpMethod=HttpMethod.DELETE)
	public static void testDelete1(Request request) throws IOException{
		execute(new Handler(){

			@Override
			public void handle(Request request, Response response) {
				response.setResponseCode(ResponseCodes.Accepted);
			}
			
		}, StaticRef.getRequest(), StaticRef.getResponse());
	}
	
	@Path(value="/test/get1",httpMethod=HttpMethod.GET)
	public static void testGet1(Request request) throws IOException{
		execute(new Handler(){

			@Override
			public void handle(Request request, Response response) {
				
				response.setResponseCode(201);
				byte[] data = new byte[Configuration.defaultChunkSize];
				final byte v = (byte) 48;
				for(int i=0;i<data.length;i++)
					data[i] = v;
				
				try {
					response.append(data);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}, StaticRef.getRequest(), StaticRef.getResponse());
		
	}
	
	@Path(value="/test/get2",httpMethod=HttpMethod.GET)
	public static void testGet2(Response response) throws IOException{
		execute(new Handler(){

			@Override
			public void handle(Request request, Response response) {
				response.setResponseCode(201);
				byte[] data = new byte[Configuration.defaultChunkSize+2];
				final byte v = (byte) 48;
				for(int i=0;i<data.length;i++)
					data[i] = v;
				
				try {
					response.append(data);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
			
		}, StaticRef.getRequest(), StaticRef.getResponse());
	}

	@Path(value="/test/get3",httpMethod=HttpMethod.GET)
	public static void testGet3(Response response,Request request) throws IOException{
		execute(new Handler(){

			@Override
			public void handle(Request request, Response response) {
				
				response.setResponseCode(201);
				byte[] data = new byte[Configuration.defaultChunkSize/2];
				final byte v = (byte) 48;
				for(int i=0;i<data.length;i++)
					data[i] = v;
				
				try {
					response.append(data);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}, StaticRef.getRequest(), StaticRef.getResponse());
		
	}
	
	@Path(value="/test/get4",httpMethod=HttpMethod.GET) // Normal file
	public static void testGet4(Request request,Response response) throws IOException{
		execute(new Handler(){

			@Override
			public void handle(Request request, Response response) {
				try {
					response.append(new ByteData(new File(TestHttpMethodsMultiThreaded.README_MD)));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}, StaticRef.getRequest(), StaticRef.getResponse());
	}

	@Path(value="/test/get5",httpMethod=HttpMethod.GET) // multibyte file
	public static void testGet5(Request request,Response response) throws IOException{
		execute(new Handler(){

			@Override
			public void handle(Request request, Response response) {
				try {
					response.append(new ByteData(new File(TestHttpMethodsMultiThreaded.SRC_TEST_RESOURCES_MULTI_BYTE_TXT)));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}, StaticRef.getRequest(), StaticRef.getResponse());
	}
	
	@Path(value="/test/get6",httpMethod=HttpMethod.GET) // Large file
	public static void testGet6(Request request,Response response) throws IOException{
		execute(new Handler(){

			@Override
			public void handle(Request request, Response response) {
				try {
					response.append(new ByteData(new File(TestHttpMethodsMultiThreaded.DOWNLOAD_ZIP)));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}, StaticRef.getRequest(), StaticRef.getResponse());
	}

	@Path(value="/test/get7",httpMethod=HttpMethod.GET) // Large file
	public static void testGet7(Request request,Response response) throws IOException{
		execute(new Handler(){

			@Override
			public void handle(Request request, Response response) {
				try {
					response.append(Utils.toString(request.getParams())+","+request.getUriWithParams());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}, StaticRef.getRequest(), StaticRef.getResponse());
	}
	
	@Path(value="/test/getexp",httpMethod=HttpMethod.GET) // Large file
	public static void testGetExp(Request request,Response response) throws IOException{
		execute(new Handler(){

			@Override
			public void handle(Request request, Response response) {
				throw new RuntimeException("Test Exception!");
			}
			
		}, StaticRef.getRequest(), StaticRef.getResponse());
	}
	@Path(value = "/test/multipart", httpMethod = HttpMethod.POST)
	static void multiPart() throws Exception {
		execute(new Handler(){

			@Override
			public void handle(Request request, Response response) {
				
				response.setResponseCode(200);
				
				Map<String, MultiPart> multiParts = request.getMultiParts();
				for (Entry<String, MultiPart> e : multiParts.entrySet()) {
					MultiPart mp = e.getValue();
					if (NullCheck.isNullOrEmpty(mp.filename)) {
//				System.out.println("Headers :: \n" + RequestUtil.getString(mp.headers));
//				System.out.println("body :: \n" + RequestUtil.convert(mp.body));
					} else {
						File file = new File("1_" + mp.filename);
//				System.out.println("Headers :: \n" + RequestUtil.getString(mp.headers));
//				System.out.println("uploaded file to :: " + file.getAbsolutePath());
						try {
							mp.writeTo(file, true);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
//			System.out.println("*********************************************************************************");
				}
			}
			
		}, StaticRef.getRequest(), StaticRef.getResponse());
	}

	@Path(value="/test/post1",httpMethod=HttpMethod.POST) 
	public static void testPost1(Request request,Response response) throws IOException{
		execute(new Handler(){

			@Override
			public void handle(Request request, Response response) {
				List<ByteData> body = request.getBody();
				for(ByteData d:body)
					try {
						response.append(d);
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
			
		}, StaticRef.getRequest(), StaticRef.getResponse());
	}

	@Path(value="/test/postexp",httpMethod=HttpMethod.POST) 
	public static void testPostExp(Request request,Response response) throws IOException{
		execute(new Handler(){

			@Override
			public void handle(Request request, Response response) {
				throw new RuntimeException("Test Exception!");
			}
			
		}, StaticRef.getRequest(), StaticRef.getResponse());
	}
	

	@Path(value="/test/put1",httpMethod=HttpMethod.PUT) 
	public static void testPut1(Request request,Response response) throws IOException{
		execute(new Handler(){

			@Override
			public void handle(Request request, Response response) {
				List<ByteData> body = request.getBody();
				for(ByteData d:body)
					try {
						response.append(d);
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
			
		}, StaticRef.getRequest(), StaticRef.getResponse());
	}

	@Path(value="/test/putexp",httpMethod=HttpMethod.PUT) 
	public static void testPutExp(Request request,Response response) throws IOException{
		execute(new Handler(){

			@Override
			public void handle(Request request, Response response) {
				throw new RuntimeException("Test Exception!");
			}
			
		}, StaticRef.getRequest(), StaticRef.getResponse());
	}
}
final class ProxyTestApi{
	
	@Path(value="/test/proxy",httpMethod=HttpMethod.ALL) 
	public static void testProxy(Request request,Response response) throws IOException{
		response.append(request.getHttpMethod().name());
	}
}
