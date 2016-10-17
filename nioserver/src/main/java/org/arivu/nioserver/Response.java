package org.arivu.nioserver;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class Response implements AutoCloseable {

	final Map<String, String> headers = new HashMap<String, String>();

	final StringBuffer body = new StringBuffer();
	
	final SocketChannel socketChannel;
	
	Response(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}

	public String getHeader(Object key) {
		return headers.get(key);
	}

	public String putHeader(String key, String value) {
		return headers.put(key, value);
	}

	public String removeHeader(Object key) {
		return headers.remove(key);
	}

	public void putAllHeader(Map<? extends String, ? extends String> m) {
		headers.putAll(m);
	}

	public String replaceHeader(String key, String value) {
		return headers.replace(key, value);
	}

	public StringBuffer append(CharSequence s) {
		return body.append(s);
	}

	@Override
	public void close() throws Exception {
		
		this.socketChannel.close();
	}
	
}
