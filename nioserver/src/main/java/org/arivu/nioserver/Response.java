package org.arivu.nioserver;

import java.util.Map;

public interface Response extends AutoCloseable {

	int getResponseCode();

	void setResponseCode(int responseCode);

	Object getHeader(Object key);

	Object putHeader(String key, Object value);

	Object removeHeader(Object key);

	void putAllHeader(Map<? extends String, ? extends String> m);

	Object replaceHeader(String key, Object value);

	StringBuffer append(CharSequence s);

	StringBuffer append(byte[] s);

}