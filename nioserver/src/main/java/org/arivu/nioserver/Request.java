package org.arivu.nioserver;

import java.util.Collection;
import java.util.Map;

public class Request {

	enum Method{
		HEAD,PUT,DELETE,OPTIONS,GET,POST,CONNECT,TRACE,ALL;
	}
	
	final Map<String,String> headers;
	
	final String uri;
	
	final Map<String,Collection<String>> params ;
	
	final Method method;
	
	final String body;

	final String protocol;
	
	Request(Method method, String uri, String protocol, Map<String, Collection<String>> params, Map<String, String> headers,
			String body) {
		super();
		this.method = method;
		this.uri = uri;
		this.protocol = protocol;
		this.params = params;
		this.headers = headers;
		this.body = body;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public String getUri() {
		return uri;
	}

	public Map<String, Collection<String>> getParams() {
		return params;
	}

	public Method getMethod() {
		return method;
	}

	public String getBody() {
		return body;
	}

	public String getProtocol() {
		return protocol;
	}

	@Override
	public String toString() {
		return "Request [headers=" + headers + ", uri=" + uri + ", params=" + params + ", method=" + method + ", body="
				+ body + ", protocol=" + protocol + "]";
	}
	
	static RequestPath get(Collection<RequestPath> paths,Request req){
		RequestPath df = null;
		RequestPath in = new RequestPath(req.uri, req.method);
		for( RequestPath rq: paths ){
			if( in.equals(rq) ) return rq;
			else if( rq.httpMethod == Method.ALL && rq.uri.equals("/*") ){
				df = rq;
			}
		}
		return df;
	}
}
