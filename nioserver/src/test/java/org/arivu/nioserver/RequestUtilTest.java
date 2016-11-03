package org.arivu.nioserver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.utils.NullCheck;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RequestUtilTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testGetResponseBytes(){
		System.setProperty("lightninglog.json", "./lightninglog.json");
		System.setProperty("arivu.nioserver.json", "./arivu.nioserver.json");
		System.setProperty("access.log", "./access.log");
		
		Map<String,Object> th = new Amap<String,Object>();
		th.put("test", "test");
		
		Map<String, List<Object>> headers = RequestUtil.transform(th);
		Collection<ByteData> out = new DoublyLinkedList<ByteData>();
		out.add(ByteData.wrap("test ".getBytes()));
		
		Ref responseBytes = RequestUtil.getResponseBytes(200, headers, out, "HTTP/1.1", "/test", 5, HttpMethod.GET);
		assertTrue(responseBytes!=null);
		assertTrue(responseBytes.rc==200);
		assertTrue(responseBytes.cl==5);
		assertTrue(responseBytes.method==HttpMethod.GET);
		assertTrue(responseBytes.uri.equals("/test"));
		assertTrue(responseBytes.queue.size()==2);
	}

	@Test
	public void testGetResponseBytes_Case2(){
		System.setProperty("lightninglog.json", "./lightninglog.json");
		System.setProperty("arivu.nioserver.json", "./arivu.nioserver.json");
		System.setProperty("access.log", "./access.log");
		
		Map<String,Object> th = new Amap<String,Object>();
		th.put("test", "test");
		
		Map<String, List<Object>> headers = RequestUtil.transform(th);
		headers.put("test2", new DoublyLinkedList<Object>());
		Collection<ByteData> out = new DoublyLinkedList<ByteData>();
		out.add(ByteData.wrap("test ".getBytes()));
		
		Ref responseBytes = RequestUtil.getResponseBytes(104, headers, out, "HTTP/1.1", "/test", 5, HttpMethod.GET);
		assertTrue(responseBytes!=null);
		assertTrue(responseBytes.rc==104);
		assertTrue(responseBytes.cl==5);
		assertTrue(responseBytes.method==HttpMethod.GET);
		assertTrue(responseBytes.uri.equals("/test"));
		assertTrue(responseBytes.queue.size()==2);
	}
	

	@Test
	public void testGetResponseBytes_Case3() throws IOException{
		System.setProperty("lightninglog.json", "./lightninglog.json");
		System.setProperty("arivu.nioserver.json", "./arivu.nioserver.json");
		System.setProperty("access.log", "./access.log");
		
		Map<String,Object> th = new Amap<String,Object>();
		th.put("test", "test");
		
		Map<String, List<Object>> headers = RequestUtil.transform(th);
		headers.put("test2", new DoublyLinkedList<Object>());
		
		RequestImpl request = new RequestImpl(HttpMethod.GET, "/test", "/test", "HTTP/1.1", null, headers);
		ResponseImpl response = new ResponseImpl(request, headers);
		response.setResponseCode(104);
		response.append(ByteData.wrap("test ".getBytes()));
		
		assertFalse(NullCheck.isNullOrEmpty(response.getOut()));
		assertFalse(NullCheck.isNullOrEmpty(response.getHeaders()));
		
		Ref responseBytes = RequestUtil.getResponseBytes(request,response);
		assertTrue(responseBytes!=null);
		assertTrue(responseBytes.rc==104);
		assertTrue(responseBytes.cl==5);
		assertTrue(responseBytes.method==HttpMethod.GET);
		assertTrue(responseBytes.uri.equals("/test"));
		assertTrue(responseBytes.queue.size()==2);
		
		assertTrue(NullCheck.isNullOrEmpty(response.getOut()));
		assertTrue(NullCheck.isNullOrEmpty(response.getHeaders()));
	}
	
	@Test
	public void testGetHeaderIndex() {
		byte[] bytes = "34".getBytes();
		int headerIndex = RequestUtil.getHeaderIndex("123456".getBytes(), bytes[0], bytes[1], 1);
		assertTrue("Got :: "+headerIndex,headerIndex==3);
		
		headerIndex = RequestUtil.getHeaderIndex("1231456".getBytes(), bytes[0], bytes[1], 1);
		assertTrue("Got :: "+headerIndex,headerIndex==-1);
		
		headerIndex = RequestUtil.getHeaderIndex("123456".getBytes(), bytes[0], bytes[1], 2);
		assertTrue("Got :: "+headerIndex,headerIndex==-1);
		
		headerIndex = RequestUtil.getHeaderIndex("12343456".getBytes(), bytes[0], bytes[1], 2);
		assertTrue("Got :: "+headerIndex,headerIndex==5);
		
		headerIndex = RequestUtil.getHeaderIndex("12043456".getBytes(), bytes[0], bytes[1], 2);
		assertTrue("Got :: "+headerIndex,headerIndex==-1);
		
		headerIndex = RequestUtil.getHeaderIndex("12040456".getBytes(), bytes[0], bytes[1], 2);
		assertTrue("Got :: "+headerIndex,headerIndex==-1);
		
		headerIndex = RequestUtil.getHeaderIndex("12303456".getBytes(), bytes[0], bytes[1], 2);
		assertTrue("Got :: "+headerIndex,headerIndex==-1);
		
		headerIndex = RequestUtil.getHeaderIndex("abcdefghij".getBytes(), bytes[0], bytes[1], 2);
		assertTrue("Got :: "+headerIndex,headerIndex==-1);
		
		headerIndex = RequestUtil.getHeaderIndex("123456".getBytes(), bytes[0], bytes[1], 0);
		assertTrue("Got :: "+headerIndex,headerIndex==-1);
	}
	
	@Test
	public void testGetMatchingRoute_Case1(){
		System.setProperty("lightninglog.json", "./lightninglog.json");
		System.setProperty("arivu.nioserver.json", "./arivu.nioserver.json");
		System.setProperty("access.log", "./access.log");
		
		List<Route> routes = new DoublyLinkedList<Route>();

		RequestUtil.addProxyRouteRuntime("test", null, "/uri", "proxyPass", null, routes, null);
		Route two = routes.get(0);
		Route one = new Route("/one", HttpMethod.ALL);
		routes.add(one);
		
		one.disable();
		two.disable();
		
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.ALL, false)==Configuration.defaultRoute);
		
		one.enable();
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.ALL, true)==one);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.ALL, false)==one);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.GET, true)==one);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.GET, false)==one);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.ALL, false)==Configuration.defaultRoute);
		
		two.enable();
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.ALL, true)==two);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.ALL, false)==two);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.GET, true)==two);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.GET, false)==two);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri/1", HttpMethod.ALL, true)==two);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri/1", HttpMethod.ALL, false)==two);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri/1", HttpMethod.GET, true)==two);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri/1", HttpMethod.GET, false)==two);
	}
	
	@Test
	public void testGetMatchingRoute_Case2(){
		System.setProperty("lightninglog.json", "./lightninglog.json");
		System.setProperty("arivu.nioserver.json", "./arivu.nioserver.json");
		System.setProperty("access.log", "./access.log");
		
		List<Route> routes = new DoublyLinkedList<Route>();

		RequestUtil.addProxyRouteRuntime("test", "GET", "/uri", "proxyPass", null, routes, null);
		Route two = routes.get(0);
		Route one = new Route("/one", HttpMethod.GET);
		routes.add(one);
		
		one.disable();
		two.disable();
		
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.ALL, false)==Configuration.defaultRoute);
		
		one.enable();
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.ALL, false)==Configuration.defaultRoute);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.GET, true)==one);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.GET, false)==one);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.ALL, false)==Configuration.defaultRoute);
		
		two.enable();
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.ALL, false)==Configuration.defaultRoute);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.GET, true)==two);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.GET, false)==two);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri/1", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri/1", HttpMethod.ALL, false)==Configuration.defaultRoute);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri/1", HttpMethod.GET, true)==two);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri/1", HttpMethod.GET, false)==two);
	}
	
	@Test
	public void testGetMatchingRoute_Case3(){
		System.setProperty("lightninglog.json", "./lightninglog.json");
		System.setProperty("arivu.nioserver.json", "./arivu.nioserver.json");
		System.setProperty("access.log", "./access.log");
		
		List<Route> routes = new DoublyLinkedList<Route>();

		RequestUtil.addProxyRouteRuntime("test", "GET", "/uri", "proxyPass", null, routes, null);
		Route two = routes.get(0);
		RequestUtil.addProxyRouteRuntime("test", "GET", "/test", "proxyPass", null, routes, null);
		@SuppressWarnings("unused")
		Route three = routes.get(1);
		Route one = new Route("/one", HttpMethod.GET);
		routes.add(one);
		
		one.disable();
		two.disable();
		
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.ALL, false)==Configuration.defaultRoute);
		
		one.enable();
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.ALL, false)==Configuration.defaultRoute);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.GET, true)==one);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.GET, false)==one);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.ALL, false)==Configuration.defaultRoute);
		
		two.enable();
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.ALL, false)==Configuration.defaultRoute);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.GET, true)==two);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.GET, false)==two);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri/1", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri/1", HttpMethod.ALL, false)==Configuration.defaultRoute);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri/1", HttpMethod.GET, true)==two);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri/1", HttpMethod.GET, false)==two);
	}
	
	@Test
	public void testGetMatchingRoute_Case4(){
		System.setProperty("lightninglog.json", "./lightninglog.json");
		System.setProperty("arivu.nioserver.json", "./arivu.nioserver.json");
		System.setProperty("access.log", "./access.log");
		
		List<Route> routes = new DoublyLinkedList<Route>();

		RequestUtil.addProxyRouteRuntime("test", null, "/uri", "proxyPass", null, routes, null);
		Route two = routes.get(0);
		RequestUtil.addProxyRouteRuntime("test", null, "/test", "proxyPass", null, routes, null);
		@SuppressWarnings("unused")
		Route three = routes.get(1);
		Route one = new Route("/one", HttpMethod.ALL);
		routes.add(one);
		
		one.disable();
		two.disable();
		
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.ALL, false)==Configuration.defaultRoute);
		
		one.enable();
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.ALL, true)==one);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.ALL, false)==one);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.GET, true)==one);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/one", HttpMethod.GET, false)==one);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.ALL, false)==Configuration.defaultRoute);
		
		two.enable();
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.ALL, true)==two);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.ALL, false)==two);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.GET, true)==two);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri", HttpMethod.GET, false)==two);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri/1", HttpMethod.ALL, true)==two);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri/1", HttpMethod.ALL, false)==two);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri/1", HttpMethod.GET, true)==two);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/uri/1", HttpMethod.GET, false)==two);
	}

	@Test
	public void testGetMatchingRoute_Case5(){
		System.setProperty("lightninglog.json", "./lightninglog.json");
		System.setProperty("arivu.nioserver.json", "./arivu.nioserver.json");
		System.setProperty("access.log", "./access.log");
		
		List<Route> routes = new DoublyLinkedList<Route>();

		RequestUtil.addProxyRouteRuntime("test", null, "/uri", "proxyPass", null, routes, null);
		Route two = routes.get(0);
		RequestUtil.addProxyRouteRuntime("test", null, "/tst", "proxyPass", null, routes, null);
		@SuppressWarnings("unused")
		Route three = routes.get(1);
		
		Route one = null;
		for(Route r:Configuration.routes){
			if( r.uri.equals("/test/{p1}/value") ){
				one = r;
				break;
			}
		}
		
		routes.add(one);
		
		one.disable();
		two.disable();
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1", HttpMethod.GET, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1", HttpMethod.GET, false)==Configuration.defaultRoute);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1/value", HttpMethod.GET, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1/value", HttpMethod.GET, false)==Configuration.defaultRoute);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/test/1/value", HttpMethod.GET, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/test/1/value", HttpMethod.GET, false)==Configuration.defaultRoute);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/test/1/value", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/test/1/value", HttpMethod.ALL, false)==Configuration.defaultRoute);
		
		one.enable();
		assertTrue(RequestUtil.getMatchingRoute(routes, "/test/1/value", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/test/1/value", HttpMethod.ALL, false)==Configuration.defaultRoute);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/test/1/value", HttpMethod.GET, true)==one);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/test/1/value", HttpMethod.GET, false)==one);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1", HttpMethod.GET, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1", HttpMethod.GET, false)==Configuration.defaultRoute);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1/value", HttpMethod.GET, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1/value", HttpMethod.GET, false)==Configuration.defaultRoute);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1", HttpMethod.ALL, false)==Configuration.defaultRoute);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1/value", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1/value", HttpMethod.ALL, false)==Configuration.defaultRoute);
	}
	
	
	@Test
	public void testGetMatchingRoute_Case6(){
		System.setProperty("lightninglog.json", "./lightninglog.json");
		System.setProperty("arivu.nioserver.json", "./arivu.nioserver.json");
		System.setProperty("access.log", "./access.log");
		
		List<Route> routes = new DoublyLinkedList<Route>();

		RequestUtil.addProxyRouteRuntime("test", null, "/uri", "proxyPass", null, routes, null);
		Route two = routes.get(0);
		RequestUtil.addProxyRouteRuntime("test", null, "/tst", "proxyPass", null, routes, null);
		@SuppressWarnings("unused")
		Route three = routes.get(1);
		
		Route one = null;
		for(Route r:Configuration.routes){
			if( r.uri.equals("/do/{p1}/value") ){
				one = r;
				break;
			}
		}
		
		routes.add(one);
		
		one.disable();
		two.disable();
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1", HttpMethod.GET, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1", HttpMethod.GET, false)==Configuration.defaultRoute);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1/value", HttpMethod.GET, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1/value", HttpMethod.GET, false)==Configuration.defaultRoute);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/do/1/value", HttpMethod.GET, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/test/1/value", HttpMethod.GET, false)==Configuration.defaultRoute);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/do/1/value", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/do/1/value", HttpMethod.ALL, false)==Configuration.defaultRoute);
		
		one.enable();
		assertTrue(RequestUtil.getMatchingRoute(routes, "/do/1/value", HttpMethod.ALL, true)==one);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/do/1/value", HttpMethod.ALL, false)==one);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/do/1/value", HttpMethod.GET, true)==one);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/do/1/value", HttpMethod.GET, false)==one);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1", HttpMethod.GET, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1", HttpMethod.GET, false)==Configuration.defaultRoute);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1/value", HttpMethod.GET, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1/value", HttpMethod.GET, false)==Configuration.defaultRoute);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1", HttpMethod.ALL, false)==Configuration.defaultRoute);
		
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1/value", HttpMethod.ALL, true)==null);
		assertTrue(RequestUtil.getMatchingRoute(routes, "/ext/1/value", HttpMethod.ALL, false)==Configuration.defaultRoute);
	}
	
	
	@Test
	public void testSearchPattern(){
		assertTrue(RequestUtil.searchPattern("123456".getBytes(), "a".getBytes(), 0, 0)==RequestUtil.BYTE_SEARCH_DEFLT);
		assertTrue(RequestUtil.searchPattern("123456".getBytes(), "6".getBytes(), 0, 0)==5);
		assertTrue(RequestUtil.searchPattern("123456".getBytes(), "34".getBytes(), 0, 0)==2);
		assertTrue(RequestUtil.searchPattern("123456".getBytes(), "67".getBytes(), 0, 0)==-1);
	}
	
	@Test
	public void testParseAsMultiPart(){
		StringBuffer buf = new StringBuffer("Content-Disposition: multipart/form-data;filename=\"test\";name=\"test\";something=\"ignored\"");
		byte[] EOL = { RequestUtil.BYTE_13,RequestUtil.BYTE_10,RequestUtil.BYTE_13,RequestUtil.BYTE_10 };
		buf.append(new String(EOL));
		String bodyContent = "body";
		buf.append(bodyContent);
		
		List<ByteData> messages = new DoublyLinkedList<ByteData>();
		messages.add(ByteData.wrap(("Content-Type: test"+System.lineSeparator()).getBytes()));
		messages.add(ByteData.wrap(buf.toString().getBytes()));
		messages.add(ByteData.wrap(" test".getBytes()));
		MultiPart parseAsMultiPart = RequestUtil.parseAsMultiPart(messages);
		assertTrue(parseAsMultiPart.filename!=null);
		assertTrue(parseAsMultiPart.name!=null);
		assertFalse(NullCheck.isNullOrEmpty(parseAsMultiPart.body));

		assertTrue(RequestUtil.convert(parseAsMultiPart.body).equals(bodyContent+" test"));
	}
	
	@Test
	public void testParseAsMultiPart_Case2(){
		StringBuffer buf = new StringBuffer("Content-Disposition: multipart/form-data");
		byte[] EOL = { RequestUtil.BYTE_13,RequestUtil.BYTE_10,RequestUtil.BYTE_13,RequestUtil.BYTE_10 };
		buf.append(new String(EOL));
		String bodyContent = "body";
		buf.append(bodyContent);
		
		List<ByteData> messages = new DoublyLinkedList<ByteData>();
		messages.add(ByteData.wrap(("Content-Type: test"+System.lineSeparator()).getBytes()));
		messages.add(ByteData.wrap(buf.toString().getBytes()));
		messages.add(ByteData.wrap(" test".getBytes()));
		MultiPart parseAsMultiPart = RequestUtil.parseAsMultiPart(messages);
		assertTrue(parseAsMultiPart.filename==null);
		assertTrue(parseAsMultiPart.name==null);
		assertFalse(NullCheck.isNullOrEmpty(parseAsMultiPart.body));

		assertTrue(RequestUtil.convert(parseAsMultiPart.body).equals(bodyContent+" test"));
	}
	
	@Test
	public void testParseAsMultiPart_Case3(){
		StringBuffer buf = new StringBuffer("");
		byte[] EOL = { RequestUtil.BYTE_13,RequestUtil.BYTE_10,RequestUtil.BYTE_13,RequestUtil.BYTE_10 };
		buf.append(new String(EOL));
		String bodyContent = "body";
		buf.append(bodyContent);
		
		List<ByteData> messages = new DoublyLinkedList<ByteData>();
		messages.add(ByteData.wrap(("Content-Type: test"+System.lineSeparator()).getBytes()));
		messages.add(ByteData.wrap(buf.toString().getBytes()));
		messages.add(ByteData.wrap(" test".getBytes()));
		MultiPart parseAsMultiPart = RequestUtil.parseAsMultiPart(messages);
		assertTrue(parseAsMultiPart.filename==null);
		assertTrue(parseAsMultiPart.name==null);
		assertFalse(NullCheck.isNullOrEmpty(parseAsMultiPart.body));

		assertTrue(RequestUtil.convert(parseAsMultiPart.body).equals(bodyContent+" test"));
	}
	
	@Test
	public void testUnZipAndDel() throws IOException, InterruptedException {
		File dd = new File("testUnzip");
		assertFalse(dd.exists());
		RequestUtil.unzip(dd, new File("arivu.nioserver-1.0.0.zip"));
		assertTrue(dd.exists());
		List<URL> urls = new DoublyLinkedList<URL>();
		RequestUtil.allUrls(dd, urls);
		assertFalse(urls.isEmpty());
		assertTrue(urls.size() == RequestUtil.toArray(urls).length);
		RequestUtil.scanApps(dd);
		RequestUtil.del(dd);
		assertFalse(dd.exists());
	}

	
	@Test
	public void testGetStackTrace() throws IOException {
		assertTrue(RequestUtil.getStackTrace(new Exception())!=null);
	}
	
	@Test
	public void testReadBB() throws IOException {
		assertTrue(RequestUtil.readBB(null) == null);
		assertTrue(RequestUtil.readBB(new File("donotexists")) == null);

		MappedByteBuffer readBB = RequestUtil.readBB(new File("README.md"));
		assertTrue(readBB != null);

		assertTrue(RequestUtil.read(null) == null);
		assertTrue(RequestUtil.read(new File("donotexists")) == null);
		byte[] read = RequestUtil.read(new File("README.md"));
		assertTrue(read != null);
		assertTrue(read.length == 1113);
	}

	@Test
	public void testAddProxyRouteRuntime() {
		Collection<Route> routes = new DoublyLinkedList<Route>();

		routes.add(new Route("/one", HttpMethod.ALL));
		try {
			RequestUtil.addProxyRouteRuntime("test", null, "uri", "proxyPass", null, routes, null);
			fail("Failed on uri validation!");
		} catch (Throwable e) {
			assertTrue(e != null);
		}

		try {
			RequestUtil.addProxyRouteRuntime("test", null, "/uri", null, null, routes, null);
			fail("Failed on proxy and dir null validation!");
		} catch (Throwable e) {
			assertTrue(e != null);
		}

		try {
			RequestUtil.addProxyRouteRuntime("test", null, "/uri", "proxyPass", "proxyPass", routes, null);
			fail("Failed on proxy and dir notnull validation!");
		} catch (Throwable e) {
			assertTrue(e != null);
		}

		// Proxy duplicate
		RequestUtil.addProxyRouteRuntime("test", null, "/uri", "proxyPass", null, routes, null);

		try {
			RequestUtil.addProxyRouteRuntime("test", null, "/uri", "proxyPass", null, routes, null);
			fail("Failed on Duplicate proxy route validation!");
		} catch (Throwable e) {
			assertTrue(e != null);
		}

		routes.clear();

		RequestUtil.addProxyRouteRuntime("test", "ALL", "/uri", "proxyPass", null, routes, null);

		try {
			RequestUtil.addProxyRouteRuntime("test", "GET", "/uri", "proxyPass", null, routes, null);
			fail("Failed on Duplicate proxy route validation!");
		} catch (Throwable e) {
			assertTrue(e != null);
		}

		routes.clear();

		RequestUtil.addProxyRouteRuntime("test", "GET", "/uri", "proxyPass", null, routes, null);

		try {
			RequestUtil.addProxyRouteRuntime("test", "GET", "/uri", "proxyPass", null, routes, null);
			fail("Failed on Duplicate proxy route validation!");
		} catch (Throwable e) {
			assertTrue(e != null);
		}

		routes.clear();

		RequestUtil.addProxyRouteRuntime("test", "GET", "/uri", "proxyPass", null, routes, null);

		try {
			RequestUtil.addProxyRouteRuntime("test", "POST", "/uri", "proxyPass", null, routes, null);
		} catch (Throwable e) {
			fail("Failed on Duplicate proxy route validation!");
		}

		// Dir duplicate
		routes.clear();
		RequestUtil.addProxyRouteRuntime("test", null, "/uri", null, "dir", routes, null);

		try {
			RequestUtil.addProxyRouteRuntime("test", null, "/uri", null, "dir", routes, null);
			fail("Failed on Duplicate dir route validation!");
		} catch (Throwable e) {
			assertTrue(e != null);
		}

		routes.clear();

		RequestUtil.addProxyRouteRuntime("test", "ALL", "/uri", null, "dir", routes, null);

		try {
			RequestUtil.addProxyRouteRuntime("test", "GET", "/uri", null, "dir", routes, null);
			fail("Failed on Duplicate dir route validation!");
		} catch (Throwable e) {
			assertTrue(e != null);
		}

		routes.clear();

		RequestUtil.addProxyRouteRuntime("test", "GET", "/uri", null, "dir", routes, null);

		try {
			RequestUtil.addProxyRouteRuntime("test", "GET", "/uri", null, "dir", routes, null);
			fail("Failed on Duplicate dir route validation!");
		} catch (Throwable e) {
			assertTrue(e != null);
		}

		routes.clear();

		RequestUtil.addProxyRouteRuntime("test", "GET", "/uri", null, "dir", routes, null);

		try {
			RequestUtil.addProxyRouteRuntime("test", "POST", "/uri", null, "dir", routes, null);
		} catch (Throwable e) {
			fail("Failed on Duplicate dir route validation!");
		}
	}

	@Test
	public void testTransform() {
		assertTrue(RequestUtil.transform(null) != null);
		assertTrue(RequestUtil.transform(null).size() == 0);

		Map<String, Object> in = new Amap<String, Object>();

		Map<String, List<Object>> transform = RequestUtil.transform(in);
		assertTrue(transform != null);
		assertTrue(transform.size() == 0);

		in.put("one", "one");

		transform = RequestUtil.transform(in);
		assertTrue(transform != null);
		assertTrue(transform.size() == 1);

		assertFalse(NullCheck.isNullOrEmpty(transform.get("one")));

		assertTrue(transform.get("one").size() == 1);
		assertTrue(transform.get("one").get(0).equals("one"));

	}

	@Test
	public void testParse_GETRequest() {
		List<ByteData> messages = new DoublyLinkedList<ByteData>();

		messages.add(ByteData.wrap(("GET /static/248.png?t1=1quater HTTP/1.1" + System.lineSeparator()).getBytes()));
		messages.add(ByteData.wrap(("Accept-Encoding: gzip,deflate" + System.lineSeparator()).getBytes()));
		messages.add(ByteData.wrap(("Host: localhost:8080" + System.lineSeparator()).getBytes()));
		messages.add(ByteData.wrap(("Connection: Keep-Alive" + System.lineSeparator()).getBytes()));
		messages.add(ByteData.wrap(
				("User-Agent: Apache-HttpClient/4.1.1 (java 1.5)" + System.lineSeparator() + System.lineSeparator())
						.getBytes()));

		// StringBuffer buf = new StringBuffer();
		// buf.append("GET /static/248.png?t1=1quater
		// HTTP/1.1").append(System.lineSeparator());
		// buf.append("Accept-Encoding:
		// gzip,deflate").append(System.lineSeparator());
		// buf.append("Host: localhost:8080").append(System.lineSeparator());
		// buf.append("Connection: Keep-Alive").append(System.lineSeparator());
		// buf.append("User-Agent: Apache-HttpClient/4.1.1 (java
		// 1.5)").append(System.lineSeparator());
		// buf.append(System.lineSeparator());
		//
		// Request parse = RequestUtil.parseRequest(buf);
		//
		Request parse = RequestUtil.parseRequest(messages);
		assertTrue(NullCheck.isNullOrEmpty(parse.getBody()));
		assertTrue(parse.getHttpMethod() == HttpMethod.GET);
		assertTrue(parse.getProtocol().equals("HTTP/1.1"));
		assertTrue(parse.getUri().equals("/static/248.png"));
		assertTrue(parse.getUriWithParams().equals("/static/248.png?t1=1quater"));
		assertTrue(parse.getHeaders().get("Accept-Encoding").get(0).equals("gzip,deflate"));
		assertTrue(parse.getHeaders().get("Host").get(0).equals("localhost:8080"));
		assertTrue(parse.getHeaders().get("Connection").get(0).equals("Keep-Alive"));
		assertTrue(parse.getHeaders().get("User-Agent").get(0).equals("Apache-HttpClient/4.1.1 (java 1.5)"));

	}

	@Test
	public void testParse_POSTRequest() {
		String strBody = "{\"stampversion\":\"2.5.110\", \"postofficeId\":\"700004\"}";

		byte[] eolBytes = new byte[] { (byte) 13, (byte) 10 };
		String EOL = new String(eolBytes);

		List<ByteData> messages = new DoublyLinkedList<ByteData>();

		messages.add(ByteData.wrap(("POST /all/postman/arelazy HTTP/1.1" + System.lineSeparator()).getBytes()));
		messages.add(ByteData.wrap(("Content-Type: application/json" + System.lineSeparator()).getBytes()));
		messages.add(ByteData.wrap(("X-ID: 12345" + System.lineSeparator()).getBytes()));
		messages.add(ByteData.wrap(("Content-Length: 55" + System.lineSeparator()).getBytes()));
		messages.add(ByteData.wrap(("Host: localhost:8080" + System.lineSeparator()).getBytes()));
		messages.add(ByteData.wrap(("Connection: Keep-Alive" + System.lineSeparator()).getBytes()));
		messages.add(ByteData.wrap(("User-Agent: Apache-HttpClient/4.1.1 (java 1.5)" + EOL + EOL).getBytes()));

		RequestImpl parse = RequestUtil.parseRequest(messages);

		parse.body.add(ByteData.wrap(strBody.getBytes()));

		// StringBuffer buf = new StringBuffer();
		// buf.append("POST /all/postman/arelazy
		// HTTP/1.1").append(System.lineSeparator());
		// buf.append("Content-Type:
		// application/json").append(System.lineSeparator());
		// buf.append("X-ID: 12345").append(System.lineSeparator());
		// buf.append("Content-Length: 55").append(System.lineSeparator());
		// buf.append("Host: localhost:8080").append(System.lineSeparator());
		// buf.append("Connection: Keep-Alive").append(System.lineSeparator());
		// buf.append("User-Agent: Apache-HttpClient/4.1.1 (java
		// 1.5)").append(EOL);
		// buf.append(EOL);//.append(EOL);
		// buf.append(strBody);//.append(EOL);
		//
		// Request parse = RequestUtil.parseRequest(buf);
		//
		assertFalse(NullCheck.isNullOrEmpty(parse.getBody()));
		String got = RequestUtil.convert(parse.getBody());
		assertTrue(strBody.equals(got));
		assertTrue(parse.getHttpMethod() == HttpMethod.POST);
		assertTrue(parse.getProtocol().equals("HTTP/1.1"));
		assertTrue(parse.getUri().equals("/all/postman/arelazy"));
		assertTrue(parse.getHeaders().get("Content-Type").get(0).equals("application/json"));
		assertTrue(parse.getHeaders().get("X-ID").get(0).equals("12345"));
		assertTrue(parse.getHeaders().get("Content-Length").get(0).equals("55"));
		assertTrue(parse.getHeaders().get("Host").get(0).equals("localhost:8080"));
		assertTrue(parse.getHeaders().get("Connection").get(0).equals("Keep-Alive"));
		// assertTrue("Expected :: %Apache-HttpClient/4.1.1 (java 1.5)%
		// GOT::%"+parse.getHeaders().get("User-Agent")+"%",parse.getHeaders().get("User-Agent").equals("Apache-HttpClient/4.1.1
		// (java 1.5)"));

	}

	@Test
	public void testParseParams() {
		Map<String, Collection<String>> parseParams = RequestUtil.parseParams("t1=1&t2=2");
		assertFalse(NullCheck.isNullOrEmpty(parseParams));
		assertTrue(parseParams.containsKey("t1"));
		assertTrue(parseParams.containsKey("t2"));

		assertTrue(parseParams.get("t1").size() == 1);
		assertTrue(parseParams.get("t2").size() == 1);

		assertTrue(parseParams.get("t1").toArray(new String[] {})[0].equals("1"));
		assertTrue(parseParams.get("t2").toArray(new String[] {})[0].equals("2"));

	}

	@Test
	public void testParseParamsCase2() {
		Map<String, Collection<String>> parseParams = RequestUtil.parseParams("t1&t2=2&t2=1");
		assertFalse(NullCheck.isNullOrEmpty(parseParams));
		assertTrue(parseParams.containsKey("t1"));
		assertTrue(parseParams.containsKey("t2"));

		assertTrue(parseParams.get("t1").size() == 0);
		assertTrue(parseParams.get("t2").size() == 2);

		assertTrue(parseParams.get("t2").toArray(new String[] {})[0].equals("2"));
		assertTrue(parseParams.get("t2").toArray(new String[] {})[1].equals("1"));

	}

	@Test
	public void testValidateRouteUri() {
		assertTrue(RequestUtil.validateRouteUri("/stop"));
		assertTrue(RequestUtil.validateRouteUri("/-stop"));
		assertTrue(RequestUtil.validateRouteUri("/-stop-"));
		assertTrue(RequestUtil.validateRouteUri("/_stop"));
		assertTrue(RequestUtil.validateRouteUri("/_stop_"));
		assertTrue(RequestUtil.validateRouteUri(Configuration.stopUri));
		assertTrue(RequestUtil.validateRouteUri("/test/{p1}/value"));

		assertFalse(RequestUtil.validateRouteUri(null));
		assertFalse(RequestUtil.validateRouteUri(""));
		assertFalse(RequestUtil.validateRouteUri("/st op"));
		assertFalse(RequestUtil.validateRouteUri("stop"));
		assertFalse(RequestUtil.validateRouteUri("/stop{}"));
		assertFalse(RequestUtil.validateRouteUri("/stop?"));
		assertFalse(RequestUtil.validateRouteUri("/stop&"));
		assertFalse(RequestUtil.validateRouteUri("/stop;"));
		assertFalse(RequestUtil.validateRouteUri("/stop:"));
		assertFalse(RequestUtil.validateRouteUri("/stop+"));

		assertFalse(RequestUtil.validateRouteUri("/test/{}/value"));
		assertFalse(RequestUtil.validateRouteUri("/test/}{/value"));
		assertFalse(RequestUtil.validateRouteUri("/test/}}/value"));
		assertFalse(RequestUtil.validateRouteUri("/test/{}}/value"));
		assertFalse(RequestUtil.validateRouteUri("/test/{{/value"));
		assertFalse(RequestUtil.validateRouteUri("/test/{{}/value"));
		assertFalse(RequestUtil.validateRouteUri("/test/{{}/value"));
		assertFalse(RequestUtil.validateRouteUri("/test/{}p/value"));
	}

	@Test
//	@Ignore
	public void testParseUriTokens() throws Exception {

		System.setProperty("lightninglog.json", "./lightninglog.json");
		System.setProperty("arivu.nioserver.json", "./arivu.nioserver.json");
		System.setProperty("access.log", "./access.log");

		Collection<String> packages = new DoublyLinkedList<String>();
		packages.add("org.arivu.nioserver");
		Collection<Route> routes = PackageScanner.getPaths("System", packages);
		assertFalse(NullCheck.isNullOrEmpty(routes));
		Route varia = null;
		for (Route c : routes) {
			if (c.uri.equals("/test/{p1}/value") && c.httpMethod == HttpMethod.GET) {
				varia = c;
				break;
			}
		}

		assertTrue(RequestUtil.getMatchingRoute(routes, "/test/1/value", HttpMethod.GET, false) == varia);

		Request req = new RequestImpl(HttpMethod.GET, "/test/1/value", "/test/1/value", null, null, null);
		Response res = varia.getResponse(req);
		varia.handle(req, res);

		List<ByteData> out = res.getOut();
		ByteData poll = out.get(0);
		assertTrue(new String(poll.array()).equals("1"));
	}

	/*
	 * 
	 * %--------------------------3a2b8db3933749e7 Content-Disposition:
	 * form-data; name="key1"
	 * 
	 * value1 --------------------------3a2b8db3933749e7 Content-Disposition:
	 * form-data; name="upload"; filename="README.md" Content-Type:
	 * application/octet-stream
	 * 
	 * # Arivu NIO server :
	 * 
	 * Nio Server for high performance java application. Excellent fit for
	 * microservice . Just add annotations and bundle up with jar files and run
	 * it as micro service.
	 * 
	 * Sample config: => ```java "request":{ "scanpackages":[ ], "proxies":{
	 * "googleSite":{ "header":{ "Expires":-1, "Cache-Control":
	 * "private, max-age=0", "Server":"clownserver", "X-XSS-Protection":
	 * "1; mode=block", "Connection":"close" }%
	 ***********************
	 * 
	 * Req Body(2,2):: %, "location":"/search", "method":"GET",
	 * "proxy_pass":"https://www.google.co.in/search" }, "staticContent":{
	 * "header":{ "Expires":-1, "Cache-Control":"private, max-age=0",
	 * "Server":"clownserver", "X-XSS-Protection":"1; mode=block",
	 * "Connection":"close" }, "location":"/static", "method":"GET",
	 * "dir":"/Users/parthipangounder/Downloads" } } },
	 * 
	 * ```
	 * 
	 * --------------------------3a2b8db3933749e7-- %
	 * 
	 * 
	 */
	@Test
	public void testByteSearch() {
		byte[] pattern = "--------------------------3a2b8db3933749e7".getBytes();
		byte[] content = ("--------------------------3a2b8db3933749e7" + System.lineSeparator()
				+ "Content-Disposition: form-data; name=\"key1\"" + System.lineSeparator() + System.lineSeparator()
				+ "value1" + System.lineSeparator() + "--------------------------3a2b8db3933749e7"
				+ System.lineSeparator() + "Content-Disposition: form-data; name=\"upload\"; filename=\"README.md\""
				+ System.lineSeparator() + "Content-Type: application/octet-stream" + System.lineSeparator()
				+ System.lineSeparator()).getBytes();

		assertTrue(RequestUtil.searchPattern(content, pattern, 0, 0) == 0);

		byte[] copyOfRange = Arrays.copyOfRange(content,
				RequestUtil.searchPattern(content, pattern, 0, 0) + pattern.length + 1,
				RequestUtil.searchPattern(content, pattern, pattern.length, 0));
		int headerIndex = RequestUtil.getHeaderIndex(copyOfRange, RequestUtil.BYTE_10, RequestUtil.BYTE_10, 1);
		byte[] header = Arrays.copyOfRange(copyOfRange, 0, headerIndex - 1);
		byte[] body = Arrays.copyOfRange(copyOfRange, headerIndex + 1, copyOfRange.length - 1);
		System.out.println("%" + new String(header) + "%");
		System.out.println("%" + new String(body) + "%");

	}

}

class TestRoute {

	@Path(value = "/test/{p1}/value", httpMethod = HttpMethod.GET)
	void handle(Request req, Response res, String p1) throws IOException {
		res.append(p1);
	}
	

	@Path(value = "/do/{p1}/value", httpMethod = HttpMethod.ALL)
	void doAll(Request req, Response res, String p1) throws IOException {
		res.append(p1);
	}
}