package org.arivu.nioserver;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface Response {

	int getResponseCode();

	void setResponseCode(int responseCode);

	Object getHeader(Object key);

	Object putHeader(String key, Object value);

	Object removeHeader(Object key);

	void putAllHeader(Map<? extends String, ? extends String> m);

	Object replaceHeader(String key, Object value);

	void append(CharSequence s) throws IOException;

	void append(byte[] s) throws IOException;

	List<ByteData> getOut();

	Map<String, Object> getHeaders();

	void sendRedirect(String url);

	String getSendRedirectUrl();

	int getContentLength();

	void append(ByteData buf) throws IOException;

}