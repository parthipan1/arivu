package org.arivu.nioserver;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Request {

	Map<String, String> getHeaders();

	String getUri();

	Map<String, Collection<String>> getParams();

	HttpMethod getMethod();

	List<ByteBuffer> getBody();

	String getProtocol();

	String getUriWithParams();

	HttpMethod getHttpMethod();

	boolean isMultipart();

	byte[] getBoundary();

	Map<String, MultiPart> getMultiParts();

}