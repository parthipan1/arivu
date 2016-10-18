package org.arivu.nioserver;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.arivu.utils.NullCheck;

public class Response implements AutoCloseable {

	final Map<String, Object> headers = new HashMap<String, Object>();

	final StringBuffer body = new StringBuffer();

	int responseCode = Configuration.defaultResCode;
	
	final SocketChannel socketChannel;
	final Request request;

	Response(Request request, SocketChannel socketChannel, Map<String, Object> headers) {
		this.request = request;
		this.socketChannel = socketChannel;
		if(!NullCheck.isNullOrEmpty(headers)){
			this.headers.putAll(headers);
		}
	}

	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public Object getHeader(Object key) {
		return headers.get(key);
	}

	public Object putHeader(String key, Object value) {
		return headers.put(key, value);
	}

	public Object removeHeader(Object key) {
		return headers.remove(key);
	}

	public void putAllHeader(Map<? extends String, ? extends String> m) {
		headers.putAll(m);
	}

	public Object replaceHeader(String key, Object value) {
		return headers.replace(key, value);
	}

	public StringBuffer append(CharSequence s) {
		return body.append(s);
	}

	@Override
	public void close() throws Exception {
		final StringBuffer responseBody = new StringBuffer();
		
		Object rescodetxt = null;
		if(!NullCheck.isNullOrEmpty(Configuration.defaultResponseCodes)){
			rescodetxt = Configuration.defaultResponseCodes.get(String.valueOf(responseCode));
		}
		
		if(rescodetxt==null)
			responseBody.append(request.protocol).append(" ").append(responseCode).append(" ").append(System.lineSeparator());
		else
			responseBody.append(request.protocol).append(" ").append(responseCode).append(" ").append(rescodetxt).append(System.lineSeparator());
		
		responseBody.append("Date: ").append(new Date().toString()).append(System.lineSeparator());
		
		for (Entry<String, Object> e : headers.entrySet()) {
			responseBody.append(e.getKey()).append(": ").append(e.getValue()).append(System.lineSeparator());
		}
		responseBody.append(System.lineSeparator());
		responseBody.append(body);

		this.socketChannel.write(ByteBuffer.wrap(responseBody.toString().getBytes()));
		this.socketChannel.close();
	}

}
class ProxyResponse extends Response{

	/**
	 * @param request
	 * @param socketChannel
	 * @param headers
	 */
	ProxyResponse(Request request, SocketChannel socketChannel, Map<String, Object> headers) {
		super(request, socketChannel, headers);
	}

	@Override
	public void close() throws Exception {
//		final StringBuffer responseBody = new StringBuffer();
		
//		Object rescodetxt = null;
//		if(!NullCheck.isNullOrEmpty(Configuration.defaultResponseCodes)){
//			rescodetxt = Configuration.defaultResponseCodes.get(String.valueOf(responseCode));
//		}
//		
//		if(rescodetxt==null)
//			responseBody.append(request.protocol).append(" ").append(responseCode).append(" ").append(System.lineSeparator());
//		else
//			responseBody.append(request.protocol).append(" ").append(responseCode).append(" ").append(rescodetxt).append(System.lineSeparator());
//		
//		responseBody.append("Date: ").append(new Date().toString()).append(System.lineSeparator());
//		
//		for (Entry<String, Object> e : headers.entrySet()) {
//			responseBody.append(e.getKey()).append(": ").append(e.getValue()).append(System.lineSeparator());
//		}
//		responseBody.append(System.lineSeparator());
//		responseBody.append(body);

		this.socketChannel.write(ByteBuffer.wrap(body.toString().getBytes()));
		this.socketChannel.close();
	}
	
}