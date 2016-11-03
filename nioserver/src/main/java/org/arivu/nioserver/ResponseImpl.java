package org.arivu.nioserver;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.utils.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ResponseImpl implements Response {
	private static final Logger logger = LoggerFactory.getLogger(ResponseImpl.class);
	
	final Map<String, List<Object>> headers = new Amap<String, List<Object>>();

	final List<ByteData> out = new DoublyLinkedList<>();

	int responseCode = Configuration.defaultResCode;

	final Request request;

	String redirectUrl = null;
	
	long contentLength = 0;
	
	ResponseImpl(Request request, Map<String, List<Object>> headers) {
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
	public Map<String, List<Object>> getHeaders() {
		return headers;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Response#getHeader(java.lang.Object)
	 */
	@Override
	public List<Object> getHeader(Object key) {
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
		List<Object> list = this.headers.get(key);
		if( list==null ){
			list = new DoublyLinkedList<>();
			this.headers.put(key, list);
		}
		list.add(value);
		return value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Response#removeHeader(java.lang.Object)
	 */
	@Override
	public List<Object> removeHeader(Object key) {
		return headers.remove(key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.nioserver.Response#putAllHeader(java.util.Map)
	 */
	@Override
	public void putAllHeader(Map<? extends String, ? extends List<Object>> m) {
		headers.putAll(m);
	}

//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see org.arivu.nioserver.Response#replaceHeader(java.lang.String,
//	 * java.lang.Object)
//	 */
//	@Override
//	public Object replaceHeader(String key, Object value) {
//		return headers.replace(key, value);
//	}

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
			append(new ByteData(s));
		}
	}

	/* (non-Javadoc)
	 * @see org.arivu.nioserver.Response#append(ByteData)
	 */
	@Override
	public void append(ByteData buf) throws IOException {
		if(buf!=null){
			out.add(buf);
			contentLength += buf.length();//remaining();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.arivu.nioserver.Response#getOut()
	 */
	@Override
	public List<ByteData> getOut() {
		return Collections.unmodifiableList(out);
	}

	/* (non-Javadoc)
	 * @see org.arivu.nioserver.Response#getContentLength()
	 */
	@Override
	public long getContentLength() {
		return contentLength;
	}

	/* (non-Javadoc)
	 * @see org.arivu.nioserver.Response#sendRedirect(java.lang.String)
	 */
	@Override
	public void sendRedirect(String url){
		this.responseCode = 301;
		this.headers.clear();
		putHeader("X-Redirect-Src", request.getUriWithParams());
		putHeader("Location", url);
		this.redirectUrl = url;
		try {
			append("<!DOCTYPE html><head><meta http-equiv=\"refresh\" content=\"0; url="+url+"\"></head><body><p>The page has moved to:<a href=\""+url+"\">this page</a></p></body></html>");
		} catch (IOException e) {
			logger.error("Failed on sendredirect :: ", e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.arivu.nioserver.Response#getSendRedirectUrl()
	 */
	@Override
	public String getSendRedirectUrl(){
		return this.redirectUrl;
	}
	
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