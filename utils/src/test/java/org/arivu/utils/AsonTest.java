package org.arivu.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.script.ScriptException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AsonTest {
	private static final String CONFIGURATION_FILE = "asynclogger.properties";
	
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
	public void testFromJsonString() throws ScriptException, IOException {
		
		InputStream systemResourceAsStream = ClassLoader.getSystemResourceAsStream(CONFIGURATION_FILE);
		Map<String, Object> json = new Ason().fromJson(systemResourceAsStream);
		
//		System.out.println("fromJson "+json);
		
//		Map<String, String> loggers = (Map<String, String>) get(json, "loggers", null );
//		System.out.println("loggers "+loggers);
//		
//		System.out.println("DEFAULT_LOG_LEVEL "+get(json, "loggers.root","info"));
//		System.out.println("RINGBUFFER_LEN "+get(json, "buffer.ring",300));
//		System.out.println("BATCH_SIZE "+get(json, "buffer.batch",100));
//		System.out.println("SHOW_LOG_NAME "+get(json, "log.showName",true));
//		System.out.println("SHOW_SHORT_LOG_NAME "+get(json, "log.showShortName",false));
//		System.out.println("SHOW_DATE_TIME "+get(json, "log.showDateTime",false));
//		System.out.println("SHOW_THREAD_NAME "+get(json, "log.showThreadName",false));
//		System.out.println("DATE_TIME_FORMAT_STR "+get(json, "log.dateTimeFormat",null));
//		System.out.println("FILE_EXT_FORMAT "+get(json, "log.fileDateTimeExt",null));
//		System.out.println("LOG_FILE "+get(json, "log.file","System.err"));
//		System.out.println("FILE_THRESHOLD_LIMIT "+get(json, "log.fileSize",5242880));
//		
		Collection<String> split = (Collection<String>) convert((Map<String, String>) get(json, "appenders", null));// Arrays.asList("rollingfile,console".split(",")) 
		System.out.println(" appenders "+split);
//		
//		Collection<String> customs = (Collection<String>) convert((Map<String, String>) get(json, "customAppenders", null));//get(json, "customAppenders", null);
//		System.out.println(" customAppenders "+customs);
		
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
	
	private static Object print(Map<String, Object> object2 ){
		Set<Entry<String, Object>> entrySet = object2.entrySet();
		 for( Entry<String, Object> e:entrySet ){
		 System.out.println(e.getKey()+" :: "+e.getValue());
		 }
		 return object2;
	}
	
//
//	@Test
//	public void testFromJsonReader() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testFromJsonInputStream() {
//		fail("Not yet implemented");
//	}

}
