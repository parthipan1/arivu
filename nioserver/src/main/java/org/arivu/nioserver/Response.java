package org.arivu.nioserver;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
		final StringBuffer responseBody = new StringBuffer();
		
		for(Entry<String, String> e:headers.entrySet()){
			responseBody.append(e.getKey()).append(": ").append(e.getValue()).append(System.lineSeparator());
		}
		responseBody.append(System.lineSeparator());
		responseBody.append(body);
		
		this.socketChannel.write(ByteBuffer.wrap(responseBody.toString().getBytes()));
		this.socketChannel.close();
	}
	
}
