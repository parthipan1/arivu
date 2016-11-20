package org.arivu.utils;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.script.ScriptException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AsonTest {
	private static final String CONFIGURATION_FILE = "test.json";
	
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

	@SuppressWarnings("unchecked")
	@Test
	public void testFromJsonString() throws ScriptException, IOException {
		
		InputStream systemResourceAsStream = ClassLoader.getSystemResourceAsStream(CONFIGURATION_FILE);
		Map<String, Object> json = new Ason().fromJson(systemResourceAsStream);
		
		assertTrue(json!=null);
		assertTrue(json.size()>0);
		
//		System.out.println("fromJson "+json);
		
		Map<String, String> loggers = (Map<String, String>) get(json, "loggers", null );
		assertTrue(loggers!=null);
		assertTrue(loggers.size()>0);
		
		
		assertTrue(get(json, "loggers.root","info").equals("debug"));
		assertTrue(((Integer)get(json, "buffer.ring",300))==50);
		assertTrue(((Integer)get(json, "buffer.batch",300))==50);
		assertTrue((Boolean)get(json, "log.showName",false));
		assertTrue((Boolean)get(json, "log.showShortName",false));
		assertTrue((Boolean)get(json, "log.showDateTime",false));
		assertTrue((Boolean)get(json, "log.showThreadName",false));
		assertTrue(get(json, "log.dateTimeFormat",null)!=null);
		assertTrue(get(json, "log.fileDateTimeExt",null)!=null);
		assertTrue(get(json, "log.fileDateTimeExt",null)!=null);
		assertTrue(get(json, "log.file","System.err").equals("logs//rp.test.log"));
		assertTrue(((Long)get(json, "log.fileSize",300))==5242880000l);
		
		Collection<String> split = (Collection<String>) convert((Map<String, String>) get(json, "appenders", null));// Arrays.asList("rollingfile,console".split(",")) 
		assertTrue(split!=null);
		assertTrue(split.size()>0);
	}
	
	private static Collection<String> convert(final Map<String,String> map){
		if(map != null){
			Collection<String> arr = new ArrayList<String>();
			Set<Entry<String, String>> entrySet = map.entrySet();
			for( Entry<String, String> e:entrySet ){
				arr.add(e.getValue());
			}
			return arr;
		}
		return null;
	}
	
	private static String[] split(String txt,String k){
		int index = txt.indexOf(k);
		Collection<String> arr = new ArrayList<String>();
//		if(index>=0){
			while( index >=0 ){
				arr.add(txt.substring(0, index));
				txt = txt.substring(index+k.length(), txt.length());
				index = txt.indexOf(k);
			}
//		}else{
			arr.add(txt);
//		}
		return arr.toArray(new String[]{});
	}

	@SuppressWarnings("unchecked")
	private static Object get(Map<String, Object> json,String token,Object defailt){
		String[] split = split(token, ".");
		Map<String, Object> obj = json;
		
		for(int i=0;i<split.length;i++){
			Object object = obj.get(split[i]);
//			System.out.println("\nTOKEN :: "+token+" "+split[i]+" i "+i+" split.length "+split.length);
//			print((Map<String, Object>)object);
			if(object==null){
				break;	
			}else if( i==split.length-1 ){
				return object;
			}else{
				try {
					obj = (Map<String, Object>) object;
				} catch (Exception e) {
					break;
				}
			}
		}
		
		return defailt;
	}
	
	static Object print(Map<String, Object> object2 ){
		Set<Entry<String, Object>> entrySet = object2.entrySet();
		 for( Entry<String, Object> e:entrySet ){
		 System.out.println(e.getKey()+" :: "+e.getValue());
		 }
		 return object2;
	}
	

	@Test
	public void testFromJson_Parallel() throws InterruptedException {
		final int nThreads = 100;
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch end = new CountDownLatch(1);
		final AtomicInteger f = new AtomicInteger(nThreads);
		final ExecutorService exe = Executors.newFixedThreadPool( nThreads);
		final Ason ason = new Ason();
		for (int i = 0; i < nThreads; i++) {
			exe.submit(new Runnable() {
				
				@Override
				public void run() {
					try {
						start.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					String key = Thread.currentThread().getName();
					String json = "{\"name\":\""+key+"\"}";
					Map<String, Object> fromJson;
					try {
						fromJson = ason.fromJson(json);
						if( !key.equals(fromJson.get("name")) ){
							throw new IllegalStateException("Failed on thread "+key);
						}else{
//							System.out.println( key+" :: "+fromJson.get("name") );
						}
					} catch (ScriptException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}finally {
						if( f.decrementAndGet() == 0 ){
							end.countDown();
						}
					}
				}
			});
		}
		start.countDown();
		end.await();
		
	}

	@Test
	public void testSplit() {
		assertTrue(Ason.split(null, ",")==null);
		assertTrue(Ason.split("1", ",")!=null);
		assertTrue(Ason.split("1", ",").length==1);
		assertTrue(Ason.split("1,2", ",").length==2);
	}

	@Test
	public void testConvert() {
		assertTrue(Ason.convert(null)==null);
		Map<String,String> map = new HashMap<String,String>();
		assertTrue(Ason.convert(map).size()==0);
		map.put("0", "0");
		assertTrue(Ason.convert(map).size()==1);
	}

	@Test
	public void testGetObject1() {
		Map<String, Object> json = new HashMap<String, Object>();
		String key = "test";
		json.put(key, key);
		String token = "one";
		String dfltvalue = "two";
		assertTrue(Ason.get(json, token, null)==null);
		assertTrue(Ason.get(json, token, dfltvalue)==dfltvalue);
		assertTrue(Ason.get(json, key, dfltvalue)==key);
	}

	@Test
	public void testGetObject2() {
		String key = "test";
		String token = "one";
		String dfltvalue = "two";

		Map<String, Object> json = new HashMap<String, Object>();
		
		Map<String, Object> innerjson = new HashMap<String, Object>();
		json.put(key, innerjson);
		innerjson.put(token, dfltvalue);
		
		assertTrue(Ason.get(json, "test.one", null)==dfltvalue);
		assertTrue(Ason.get(json, "test.two", dfltvalue)==dfltvalue);
		assertTrue(Ason.get(json, "test.two", null)==null);
	}

	@Test
	public void testLoadProperties() {
		Map<String, Object> json = Ason.loadProperties("test.json");
		assertTrue(((Number)Ason.get(json, "buffer.batch", null)).intValue()==50);
	}
	
	@Test
	public void testGetList() {
		Map<String, Object> json = Ason.loadProperties("test2.json");
		Collection<Map<String,Object>> deflt = new ArrayList<Map<String,Object>>();
		assertTrue(Ason.getList(json, "test1", null)==null);
		assertTrue(Ason.getList(json, "test1", deflt)==deflt);
		assertTrue(Ason.getList(json, "test", null)!=null);
		assertTrue(Ason.getList(json, "test", null).size()==2);
	}

	@Test
	public void testGetStr() {
		String key = "test";
		String token = "one";
		String dfltvalue = "two";

		Map<String, Object> json = new HashMap<String, Object>();
		
		Map<String, Object> innerjson = new HashMap<String, Object>();
		json.put(key, innerjson);
		innerjson.put(token, dfltvalue);
		
		assertTrue(Ason.getStr(json, "test.one", null)==dfltvalue);
		assertTrue(Ason.getStr(json, "test.two", dfltvalue)==dfltvalue);
		assertTrue(Ason.getStr(json, "test.two", null)==null);
	}

	@Test
	public void testGetObj() {
		String key = "test";
		String token = "one";
		Map<String,Object> dfltvalue = new HashMap<String,Object>();

		Map<String, Object> json = new HashMap<String, Object>();
		
		Map<String, Object> innerjson = new HashMap<String, Object>();
		json.put(key, innerjson);
		innerjson.put(token, dfltvalue);
		
		assertTrue(Ason.getObj(json, "test.one", null)==dfltvalue);
		assertTrue(Ason.getObj(json, "test.two", dfltvalue)==dfltvalue);
		assertTrue(Ason.getObj(json, "test.two", null)==null);
	}

	@Test
	public void testGetNumber() {
		String key = "test";
		String token = "one";
		Number dfltvalue = 11;

		Map<String, Object> json = new HashMap<String, Object>();
		
		Map<String, Object> innerjson = new HashMap<String, Object>();
		json.put(key, innerjson);
		innerjson.put(token, dfltvalue);
		
		assertTrue(Ason.getNumber(json, "test.one", null)==dfltvalue);
		assertTrue(Ason.getNumber(json, "test.two", dfltvalue)==dfltvalue);
		assertTrue(Ason.getNumber(json, "test.two", null)==null);
	}
	
	@Test
	public void testGetArray() {
		String key = "test";
		String token = "one";
		Collection<String> dfltvalue = new ArrayList<String>();

		Map<String, Object> json = new HashMap<String, Object>();
		
		Map<String, Object> innerjson = new HashMap<String, Object>();
		json.put(key, innerjson);
		innerjson.put(token, dfltvalue);
		
//		assertTrue(Ason.getArray(json, "test.one", null)==dfltvalue);
		assertTrue(Ason.getArray(json, "test.two", dfltvalue)==dfltvalue);
		assertTrue(Ason.getArray(json, "test.two", null)==null);
	}
}
