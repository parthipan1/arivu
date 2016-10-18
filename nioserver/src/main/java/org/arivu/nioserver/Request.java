package org.arivu.nioserver;

import java.util.Collection;
import java.util.Map;

public interface Request {

	Map<String, String> getHeaders();

	String getUri();

	Map<String, Collection<String>> getParams();

	HttpMethod getMethod();

	String getBody();

	String getProtocol();

	String getUriWithParams();

	long getStartTime();

	HttpMethod getHttpMethod();

}