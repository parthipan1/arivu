package org.arivu.nioserver;

import java.util.Map;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.Threadlocal;
import org.arivu.datastructure.Threadlocal.Factory;

public final class StaticRef {

	private static final Threadlocal<Map<String,Object>> mdc = new Threadlocal<Map<String,Object>>(new Factory<Map<String,Object>>(){

		@Override
		public Map<String, Object> create(Map<String, Object> params) {
			return new Amap<String, Object>();
		}
		
	});
	
	static void set(Request req,Response res, Route route){
		Map<String, Object> map = mdc.get(null);
		map.put("req", req);
		map.put("res", res);
		map.put("rte", route);
	}

	static void clear(){
		mdc.remove();
	}

	public static Request getRequest(){
		Map<String, Object> map = mdc.get();
		if(map!=null) return (Request) map.get("req");
		return null;
	}


	public static Response getResponse(){
		Map<String, Object> map = mdc.get();
		if(map!=null) return (Response) map.get("res");
		return null;
	}

	public static Route getRoute(){
		Map<String, Object> map = mdc.get();
		if(map!=null) return (Route) map.get("rte");
		return null;
	}
}
