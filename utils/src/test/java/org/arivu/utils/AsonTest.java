package org.arivu.utils;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
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
//
//	@Test
//	public void testFromJsonInputStream() {
//		fail("Not yet implemented");
//	}

}
