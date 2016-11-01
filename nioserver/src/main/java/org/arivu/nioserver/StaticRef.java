package org.arivu.nioserver;

import java.util.Map;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.Threadlocal;
import org.arivu.datastructure.Threadlocal.Factory;

public final class StaticRef {

	private static final String ROUTE_TOKEN = "rte";
	private static final String RESPONSE_TOKEN = "res";
	private static final String REQUEST_TOKEN = "req";
	private static final String ASYNC_CTX_TOKEN = "ctx";
	private static final Threadlocal<Map<String,Object>> mdc = new Threadlocal<Map<String,Object>>(new Factory<Map<String,Object>>(){

		@Override
		public Map<String, Object> create(Map<String, Object> params) {
			return new Amap<String, Object>();
		}
		
	});
	
	static void set(Request req,Response res, Route route, AsynContext actx){
		Map<String, Object> map = mdc.get(null);
		map.put(REQUEST_TOKEN, req);
		map.put(RESPONSE_TOKEN, res);
		map.put(ROUTE_TOKEN, route);
		map.put(ASYNC_CTX_TOKEN, actx);
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

	public static Route getRoute(){
		Map<String, Object> map = mdc.get();
		if(map!=null) return (Route) map.get(ROUTE_TOKEN);
		return null;
	}

	public static AsynContext getAsynContext(){
		Map<String, Object> map = mdc.get();
		if(map!=null) return (AsynContext) map.get(ASYNC_CTX_TOKEN);
		return null;
	}
}
