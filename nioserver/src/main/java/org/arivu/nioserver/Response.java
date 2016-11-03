package org.arivu.nioserver;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface Response {

	int getResponseCode();

	void setResponseCode(int responseCode);

	List<Object> getHeader(Object key);

	Object putHeader(String key, Object value);

	List<Object> removeHeader(Object key);

	void putAllHeader(Map<? extends String, ? extends List<Object>> m);

//	Object replaceHeader(String key, Object value);

	void append(CharSequence s) throws IOException;

	void append(byte[] s) throws IOException;

	List<ByteData> getOut();

	Map<String, List<Object>> getHeaders();

	void sendRedirect(String url);

	String getSendRedirectUrl();

	long getContentLength();

	void append(ByteData buf) throws IOException;

}