package org.arivu.nioserver;

import java.util.Collection;
import java.util.Map;

public class Request {

	enum Method{
		HEAD,PUT,DELETE,OPTIONS,GET,POST,CONNECT,TRACE;
	}
	
	final Map<String,String> headers;
	
	final String uri;
	
	final Map<String,Collection<String>> params ;
	
	final Method method;
	
	final String body;

	Request(Method method, String uri, Map<String, Collection<String>> params, Map<String, String> headers,
			String body) {
		super();
		this.method = method;
		this.uri = uri;
		this.params = params;
		this.headers = headers;
		this.body = body;
	}
	
	

}
