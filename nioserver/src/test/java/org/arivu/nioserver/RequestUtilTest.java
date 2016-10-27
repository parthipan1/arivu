package org.arivu.nioserver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
	public void testParse_GETRequest() {
		List<ByteData> messages = new DoublyLinkedList<ByteData>();

		messages.add(ByteData.wrap(("GET /static/248.png?t1=1quater HTTP/1.1" + System.lineSeparator()).getBytes()));
		messages.add(ByteData.wrap(("Accept-Encoding: gzip,deflate" + System.lineSeparator()).getBytes()));
		messages.add(ByteData.wrap(("Host: localhost:8080" + System.lineSeparator()).getBytes()));
		messages.add(ByteData.wrap(("Connection: Keep-Alive" + System.lineSeparator()).getBytes()));
		messages.add(ByteData.wrap(
				("User-Agent: Apache-HttpClient/4.1.1 (java 1.5)" + System.lineSeparator() + System.lineSeparator())
						.getBytes()));
		
		Request parse = RequestUtil.parseRequest(messages);
		assertTrue(NullCheck.isNullOrEmpty(parse.getBody()));
		assertTrue(parse.getHttpMethod()==HttpMethod.GET);
		assertTrue(parse.getProtocol().equals("HTTP/1.1"));
		assertTrue(parse.getUri().equals("/static/248.png"));
		assertTrue(parse.getUriWithParams().equals("/static/248.png?t1=1quater"));
		assertTrue(parse.getHeaders().get("Accept-Encoding").equals("gzip,deflate"));
		assertTrue(parse.getHeaders().get("Host").equals("localhost:8080"));
		assertTrue(parse.getHeaders().get("Connection").equals("Keep-Alive"));
		assertTrue(parse.getHeaders().get("User-Agent").equals("Apache-HttpClient/4.1.1 (java 1.5)"));
		
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
		assertTrue(parse.getHeaders().get("Content-Type").equals("application/json"));
		assertTrue(parse.getHeaders().get("X-ID").equals("12345"));
		assertTrue(parse.getHeaders().get("Content-Length").equals("55"));
		assertTrue(parse.getHeaders().get("Host").equals("localhost:8080"));
		assertTrue(parse.getHeaders().get("Connection").equals("Keep-Alive"));
	}

	@Test
	public void testParseParams() {
		Map<String, Collection<String>> parseParams = RequestUtil.parseParams("t1=1&t2=2");
		assertFalse(NullCheck.isNullOrEmpty(parseParams));
		assertTrue(parseParams.containsKey("t1"));
		assertTrue(parseParams.containsKey("t2"));
		
		assertTrue(parseParams.get("t1").size()==1);
		assertTrue(parseParams.get("t2").size()==1);
		
		assertTrue(parseParams.get("t1").toArray(new String[]{})[0].equals("1"));
		assertTrue(parseParams.get("t2").toArray(new String[]{})[0].equals("2"));
		
	}

	@Test
	public void testParseParamsCase2() {
		Map<String, Collection<String>> parseParams = RequestUtil.parseParams("t1&t2=2&t2=1");
		assertFalse(NullCheck.isNullOrEmpty(parseParams));
		assertTrue(parseParams.containsKey("t1"));
		assertTrue(parseParams.containsKey("t2"));
		
		assertTrue(parseParams.get("t1").size()==0);
		assertTrue(parseParams.get("t2").size()==2);
		
		assertTrue(parseParams.get("t2").toArray(new String[]{})[0].equals("2"));
		assertTrue(parseParams.get("t2").toArray(new String[]{})[1].equals("1"));
		
	}
}
