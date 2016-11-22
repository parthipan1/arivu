package org.arivu.nioserver;

import java.net.InetAddress;
import java.nio.channels.SelectionKey;
import java.util.Map;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.Threadlocal;
import org.arivu.datastructure.Threadlocal.Factory;

/**
 * Mdc implementation for access to Request,Response and AsynContext.
 * 
 * @author Mr P
 *
 */
public final class StaticRef {

	private static final String RESPONSE_TOKEN = "res";
	private static final String REQUEST_TOKEN = "req";
	private static final String ASYNC_CTX_TOKEN = "ctx";
	private static final String SELECT_KEY_TOKEN = "selk";
	private static final String REMOTE_ADD_TOKEN = "ria";
	private static final Threadlocal<Map<String,Object>> mdc = new Threadlocal<Map<String,Object>>(new Factory<Map<String,Object>>(){

		@Override
		public Map<String, Object> create(Map<String, Object> params) {
			return new Amap<String, Object>();
		}
		
	});
	
	static void set(Request req,Response res, AsynContext actx, SelectionKey key, InetAddress remoteHostAddress){
		Map<String, Object> map = mdc.get(null);
		map.put(REQUEST_TOKEN, req);
		map.put(RESPONSE_TOKEN, res);
		map.put(ASYNC_CTX_TOKEN, actx);
		map.put(SELECT_KEY_TOKEN, key);
		map.put(REMOTE_ADD_TOKEN, remoteHostAddress);
	}

	static void clear(){
		mdc.remove();
	}

	public static Request getRequest(){
		Map<String, Object> map = mdc.get();
		if(map!=null) return (Request) map.get(REQUEST_TOKEN);
		return null;
	}

	public static Response getResponse(){
		Map<String, Object> map = mdc.get();
		if(map!=null) return (Response) map.get(RESPONSE_TOKEN);
		return null;
	}

	public static AsynContext getAsynContext(){
		Map<String, Object> map = mdc.get();
		if(map!=null) return (AsynContext) map.get(ASYNC_CTX_TOKEN);
		return null;
	}

	public static SelectionKey getSelectionKey(){
		Map<String, Object> map = mdc.get();
		if(map!=null) return (SelectionKey) map.get(SELECT_KEY_TOKEN);
		return null;
	}
	
	public static InetAddress getRemoteHostAddress(){
		Map<String, Object> map = mdc.get();
		if(map!=null) return (InetAddress) map.get(REMOTE_ADD_TOKEN);
		return null;
	}
	
	
	
}
