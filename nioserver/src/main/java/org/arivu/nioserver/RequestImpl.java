package org.arivu.nioserver;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.utils.Utils;

final class RequestImpl implements Request {

	/**
	 * 
	 */
	final Map<String, List<Object>> headers;

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
	final Map<String, Collection<String>> params;

	/**
	 * 
	 */
	final HttpMethod httpMethod;

	/**
	 * 
	 */
	final List<ByteData> body = new DoublyLinkedList<>();

	/**
	 * 
	 */
	final String protocol;

	/**
	 * 
	 */
	boolean isMultipart = false;
	byte[] boundary = null;
	Map<String, MultiPart> multiParts = new Amap<String, MultiPart>();

	/**
	 * @param httpMethod
	 * @param uri
	 * @param uriWithParams
	 * @param protocol
	 * @param params
	 * @param headers
	 * @param body
	 */
	RequestImpl(HttpMethod httpMethod, String uri, String uriWithParams, String protocol,
			Map<String, Collection<String>> params, Map<String, List<Object>> headers) {
		super();
		this.httpMethod = httpMethod;
		this.uri = uri;
		this.uriWithParams = uriWithParams;
		this.protocol = protocol;
		this.params = params;
		this.headers = headers;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Request#getHttpMethod()
	 */
	@Override
	public HttpMethod getHttpMethod() {
		return httpMethod;
	}

	// /* (non-Javadoc)
	// * @see org.arivu.nioserver.Request#getStartTime()
	// */
	// @Override
	// public long getStartTime() {
	// return startTime;
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Request#getHeaders()
	 */
	@Override
	public Map<String, List<Object>> getHeaders() {
		return headers;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Request#getUri()
	 */
	@Override
	public String getUri() {
		return uri;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Request#getParams()
	 */
	@Override
	public Map<String, Collection<String>> getParams() {
		return params;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Request#getMethod()
	 */
	@Override
	public HttpMethod getMethod() {
		return httpMethod;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Request#getBody()
	 */
	@Override
	public List<ByteData> getBody() {
		return Collections.unmodifiableList(body);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Request#getProtocol()
	 */
	@Override
	public String getProtocol() {
		return protocol;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Request#isMultipart()
	 */
	@Override
	public boolean isMultipart() {
		return isMultipart;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Request#getUriWithParams()
	 */
	@Override
	public String getUriWithParams() {
		return uriWithParams;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Request#getMultiParts()
	 */
	@Override
	public Map<String, MultiPart> getMultiParts() {
		return multiParts;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Request#getBoundary()
	 */
	@Override
	public byte[] getBoundary() {
		return boundary;
	}

	@Override
	public String toString() {
		return "RequestImpl [uri=" + uri + ", httpMethod=" + httpMethod + ", headers=" + Utils.toString(headers) + ", params=" + Utils.toString(params)
				+ ", protocol=" + protocol + "]";
	}

}
