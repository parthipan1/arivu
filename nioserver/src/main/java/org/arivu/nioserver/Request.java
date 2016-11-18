package org.arivu.nioserver;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Request class representing a valid request from a client.
 * 
 * 
 * @author Mr P
 *
 */
public interface Request {

	Map<String, List<Object>> getHeaders();

	String getUri();

	Map<String, Collection<String>> getParams();

	HttpMethod getMethod();

	List<ByteData> getBody();

	String getProtocol();

	String getUriWithParams();

	HttpMethod getHttpMethod();

	boolean isMultipart();

	byte[] getBoundary();

	Map<String, MultiPart> getMultiParts();

}