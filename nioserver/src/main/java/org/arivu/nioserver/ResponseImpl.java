package org.arivu.nioserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.utils.NullCheck;

final class ResponseImpl implements Response {

	final Map<String, Object> headers = new Amap<String, Object>();

	final List<ByteBuffer> out = new DoublyLinkedList<>();

	int responseCode = Configuration.defaultResCode;

	final Request request;

	String redirectUrl = null;
	
	int contentLength = 0;
	
	ResponseImpl(Request request, Map<String, Object> headers) {
		this.request = request;
		if (!NullCheck.isNullOrEmpty(headers)) {
			this.headers.putAll(headers);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Response#getResponseCode()
	 */
	@Override
	public int getResponseCode() {
		return responseCode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Response#setResponseCode(int)
	 */
	@Override
	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	@Override
	public Map<String, Object> getHeaders() {
		return headers;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Response#getHeader(java.lang.Object)
	 */
	@Override
	public Object getHeader(Object key) {
		return headers.get(key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Response#putHeader(java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public Object putHeader(String key, Object value) {
		return headers.put(key, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Response#removeHeader(java.lang.Object)
	 */
	@Override
	public Object removeHeader(Object key) {
		return headers.remove(key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Response#putAllHeader(java.util.Map)
	 */
	@Override
	public void putAllHeader(Map<? extends String, ? extends String> m) {
		headers.putAll(m);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Response#replaceHeader(java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public Object replaceHeader(String key, Object value) {
		return headers.replace(key, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Response#append(java.lang.CharSequence)
	 */
	@Override
	public void append(CharSequence s) throws IOException {
		if (s != null)
			append(s.toString().getBytes());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Response#append(byte[])
	 */
	@Override
	public void append(byte[] s) throws IOException {
		if (s != null){
			append(ByteBuffer.wrap(s));
		}
	}

	/* (non-Javadoc)
	 * @see org.arivu.nioserver.Response#append(java.nio.ByteBuffer)
	 */
	@Override
	public void append(ByteBuffer buf) throws IOException {
		if(buf!=null){
			out.add(buf);
			contentLength += buf.remaining();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.arivu.nioserver.Response#getOut()
	 */
	@Override
	public List<ByteBuffer> getOut() {
		return Collections.unmodifiableList(out);
	}

	/* (non-Javadoc)
	 * @see org.arivu.nioserver.Response#getContentLength()
	 */
	@Override
	public int getContentLength() {
		return contentLength;
	}

	/* (non-Javadoc)
	 * @see org.arivu.nioserver.Response#sendRedirect(java.lang.String)
	 */
	@Override
	public void sendRedirect(String url){
		this.responseCode = 301;
		this.headers.clear();
		this.headers.put("X-Redirect-Src", request.getUriWithParams());
		this.headers.put("Location", url);
		this.redirectUrl = url;
	}
	
	/* (non-Javadoc)
	 * @see org.arivu.nioserver.Response#getSendRedirectUrl()
	 */
	@Override
	public String getSendRedirectUrl(){
		return this.redirectUrl;
	}
	
//	volatile boolean closed = false;
//
//	@Override
//	public void close() throws Exception {
//		if (closed)
//			return;
//		closed = true;
//		RequestUtil.getResponseBytes(this.getResponseCode(), this.getHeaders(), this.getOut(), request.getProtocol());
//		
//		this.socketChannel.close();
//		this.out.close();
//	}

	
}
// class ProxyResponse extends ResponseImpl{
//
// /**
// * @param requestImpl
// * @param clientSocket
// * @param headers
// */
// ProxyResponse(RequestImpl requestImpl, SocketChannel clientSocket,
// Map<String, Object> headers) {
// super(requestImpl, clientSocket, headers);
// }
//
//// @Override
//// public void close() throws Exception {
//// final StringBuffer responseBody = new StringBuffer();
////
//// Object rescodetxt = null;
//// if(!NullCheck.isNullOrEmpty(Configuration.defaultResponseCodes)){
//// rescodetxt =
// Configuration.defaultResponseCodes.get(String.valueOf(responseCode));
//// }
////
//// if(rescodetxt==null)
//// responseBody.append(requestImpl.protocol).append("
// ").append(responseCode).append(" ").append(System.lineSeparator());
//// else
//// responseBody.append(requestImpl.protocol).append("
// ").append(responseCode).append("
// ").append(rescodetxt).append(System.lineSeparator());
////
//// responseBody.append("Date: ").append(new
// Date().toString()).append(System.lineSeparator());
//////
//// for (Entry<String, Object> e : headers.entrySet()) {
//// responseBody.append(e.getKey()).append(":
// ").append(e.getValue()).append(System.lineSeparator());
//// }
//// responseBody.append(System.lineSeparator());
////// responseBody.append(body);
////
//// this.socketChannel.write(ByteBuffer.wrap(body.toString().getBytes()));
//// this.socketChannel.close();
//// }
//
// }