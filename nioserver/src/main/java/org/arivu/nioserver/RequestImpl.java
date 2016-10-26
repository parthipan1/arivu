package org.arivu.nioserver;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.arivu.datastructure.DoublyLinkedList;

final class RequestImpl implements Request {

	/**
	 * 
	 */
	final Map<String,String> headers;
	
	/**
	 * 
	 */
	final String uri;
	
	/**
	 * 
	 */
	final String uriWithParams;
	
	/**
	 * 
	 */
	final Map<String,Collection<String>> params ;
	
	/**
	 * 
	 */
	final HttpMethod httpMethod;
	
	/**
	 * 
	 */
	final List<ByteBuffer> body = new DoublyLinkedList<>();

	/**
	 * 
	 */
	final String protocol;
	
	/**
	 * 
	 */
//	final long startTime;
	
	/**
	 * @param httpMethod
	 * @param uri
	 * @param uriWithParams
	 * @param protocol
	 * @param params
	 * @param headers
	 * @param body
	 */
	RequestImpl(HttpMethod httpMethod, String uri, String uriWithParams, String protocol, Map<String, Collection<String>> params,
			Map<String, String> headers) {
		super();
		this.httpMethod = httpMethod;
		this.uri = uri;
		this.uriWithParams = uriWithParams;
		this.protocol = protocol;
		this.params = params;
		this.headers = headers;
//		this.body = body;
	}

	/* (non-Javadoc)
	 * @see org.arivu.nioserver.Request#getHttpMethod()
	 */
	@Override
	public HttpMethod getHttpMethod() {
		return httpMethod;
	}

//	/* (non-Javadoc)
//	 * @see org.arivu.nioserver.Request#getStartTime()
//	 */
//	@Override
//	public long getStartTime() {
//		return startTime;
//	}

	/* (non-Javadoc)
	 * @see org.arivu.nioserver.Request#getHeaders()
	 */
	@Override
	public Map<String, String> getHeaders() {
		return headers;
	}

	/* (non-Javadoc)
	 * @see org.arivu.nioserver.Request#getUri()
	 */
	@Override
	public String getUri() {
		return uri;
	}

	/* (non-Javadoc)
	 * @see org.arivu.nioserver.Request#getParams()
	 */
	@Override
	public Map<String, Collection<String>> getParams() {
		return params;
	}

	/* (non-Javadoc)
	 * @see org.arivu.nioserver.Request#getMethod()
	 */
	@Override
	public HttpMethod getMethod() {
		return httpMethod;
	}

	/* (non-Javadoc)
	 * @see org.arivu.nioserver.Request#getBody()
	 */
	@Override
	public List<ByteBuffer> getBody() {
		return Collections.unmodifiableList(body);
	}

	/* (non-Javadoc)
	 * @see org.arivu.nioserver.Request#getProtocol()
	 */
	@Override
	public String getProtocol() {
		return protocol;
	}

	/* (non-Javadoc)
	 * @see org.arivu.nioserver.Request#getUriWithParams()
	 */
	@Override
	public String getUriWithParams() {
		return uriWithParams;
	}

	@Override
	public String toString() {
		return "RequestImpl [headers=" + headers + ", uri=" + uri + ", params=" + params + ", httpMethod=" + httpMethod + ", body="
				+ body + ", protocol=" + protocol + "]";
	}
	
}
