package org.arivu.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public final class Ason {

	private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");

	@SuppressWarnings("unchecked")
	public Map<String, Object> fromJson(String json) throws ScriptException {
		final String variable = "x" + String.valueOf(System.currentTimeMillis());
		engine.eval("var " + variable + " = " + json + ";\n");
		Object object = engine.get(variable);
		engine.eval(" " + variable + " = null;\n");
		Map<String, Object> object2 = (Map<String, Object>) object;
//		 Set<Entry<String, Object>> entrySet = object2.entrySet();
//		 for( Entry<String, Object> e:entrySet ){
//		 System.out.println(e.getKey()+" :: "+e.getValue());
//		 }
		return Collections.unmodifiableMap(object2);
	}

	public Map<String, Object> fromJson(Reader reader) throws ScriptException, IOException {
		char[] arr = new char[8 * 1024];
		StringBuilder buffer = new StringBuilder();
		int numCharsRead;
		while ((numCharsRead = reader.read(arr, 0, arr.length)) != -1) {
			buffer.append(arr, 0, numCharsRead);
		}
		reader.close();
		return fromJson(buffer.toString());
	}

	public Map<String, Object> fromJson(InputStream in) throws ScriptException, IOException {
		return fromJson(new BufferedReader(new InputStreamReader(in)));
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
		while( index >=0 ){
			arr.add(txt.substring(0, index));
			txt = txt.substring(index+k.length(), txt.length());
			index = txt.indexOf(k);
		}
		arr.add(txt);
		String[] retArr = new String[arr.size()]; 
		int i=0;
		for(String s:arr)
			retArr[i++] = s;
		return retArr;
	}

	@SuppressWarnings("unchecked")
	private static Object get(Map<String, Object> json,String token,Object deflt){
		String[] split = split(token, ".");
		Map<String, Object> obj = json;
		
		for(int i=0;i<split.length;i++){
			Object object = obj.get(split[i]);
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
		
		return deflt;
	}
	
	public static String getStr(Map<String, Object> json,String token,String deflt){
		return (String) get(json, token, deflt);
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String,Object> getObj(Map<String, Object> json,String token,Map<String,Object> deflt){
		return (Map<String, Object>) get(json, token, deflt);
	}

	public static Collection<String> getArray(Map<String, Object> json,String token,Collection<String> deflt){
		@SuppressWarnings("unchecked")
		Collection<String> arr = convert((Map<String, String>) get(json, token, null));
		if( arr==null || arr.size()==0 )
			return deflt;
		else
			return arr;
	}
	
	@SuppressWarnings("unchecked")
	public static Collection<Map<String,Object>> getList(Map<String, Object> json,String token,Collection<Map<String,Object>> deflt){
		Object object = get(json, token, null);
		if( object instanceof Collection<?>){
			Collection<Map<String,Object>> arr = (Collection<Map<String,Object>>) object;
			if( arr==null || arr.size()==0 )
				return deflt;
			else
				return arr;
		}else if( object instanceof Map){
			Map<String,Object> map = (Map<String,Object>)object;
			Collection<Map<String,Object>> arr = new ArrayList<>();
			for(Entry<String, Object> e:map.entrySet()){
				arr.add((Map<String,Object>)e.getValue());
			}
			return arr;
		}
		return null;
	}
	
	public static Number getNumber(Map<String, Object> json,String token,Number deflt){
		return (Number) get(json, token, deflt);
	}
	
	public static Map<String, Object> loadProperties(final String file,final ClassLoader classLoader) {
		InputStream in = null;
		String instr = Env.getEnv(file, null);
		if (instr == null) {
			in = AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
				public InputStream run() {
					return classLoader.getResourceAsStream(file);
				}
			});
		} else {
			try {
				in = new FileInputStream(new File(instr));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		if (null != in) {
			try {
				Map<String, Object> fromJson = new Ason().fromJson(in);
				in.close();
				return fromJson;
			} catch (Exception e) {
				System.err.println(e.toString());
			}
		}
		return null;
	}
	
	public static Map<String, Object> loadProperties(final String file) {
		ClassLoader threadCL = Thread.currentThread().getContextClassLoader();
		if (threadCL != null) {
			return loadProperties(file, threadCL);//threadCL.getResourceAsStream(file);
		} else {
			return loadProperties(file, ClassLoader.getSystemClassLoader());//ClassLoader.getSystemResourceAsStream(file);
		}
	}
}
