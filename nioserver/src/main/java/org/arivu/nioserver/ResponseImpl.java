package org.arivu.nioserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.arivu.datastructure.Amap;
import org.arivu.utils.NullCheck;

final class ResponseImpl implements Response {

	final Map<String, Object> headers = new Amap<String, Object>();

	final ByteArrayOutputStream out = new ByteArrayOutputStream();

	int responseCode = Configuration.defaultResCode;

	final SocketChannel socketChannel;
	final Request request;

	ResponseImpl(Request request, SocketChannel socketChannel, Map<String, Object> headers) {
		this.request = request;
		this.socketChannel = socketChannel;
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
		out.write(s);
	}

	volatile boolean closed = false;

	@Override
	public void close() throws Exception {
		if (closed)
			return;
		closed = true;
		final StringBuffer responseBody = new StringBuffer();

		Object rescodetxt = null;
		if (!NullCheck.isNullOrEmpty(Configuration.defaultResponseCodes)) {
			rescodetxt = Configuration.defaultResponseCodes.get(String.valueOf(responseCode));
		}

		if (rescodetxt == null)
			responseBody.append(request.getProtocol()).append(" ").append(responseCode).append(" ")
					.append(System.lineSeparator());
		else
			responseBody.append(request.getProtocol()).append(" ").append(responseCode).append(" ").append(rescodetxt)
					.append(System.lineSeparator());

		Date enddate = new Date();
		responseBody.append("Date: ").append(enddate.toString()).append(System.lineSeparator());

		for (Entry<String, Object> e : headers.entrySet()) {
			responseBody.append(e.getKey()).append(": ").append(e.getValue()).append(System.lineSeparator());
		}
		responseBody.append(System.lineSeparator());

		byte[] bytes = responseBody.toString().getBytes();
		this.socketChannel.write(ByteBuffer.wrap(bytes));
		this.socketChannel.write(ByteBuffer.wrap(out.toByteArray()));
		this.socketChannel.close();
		this.out.close();

		if (!request.getUri().equals(Configuration.stopUri)) {
			StringBuffer access = new StringBuffer();
			access.append("[").append(dateFormat.format(new Date(request.getStartTime()))).append("] ").append(request.getUri())
					.append(" ").append(responseCode).append(" ").append(bytes.length).append(" [")
					.append(dateFormat.format(enddate)).append("]");
			Server.accessLog.append(access.toString());
		}
	}

	final static DateFormat dateFormat = new SimpleDateFormat("EEE MMM d hh:mm:ss.SSS yyyy");
}
// class ProxyResponse extends ResponseImpl{
//
// /**
// * @param requestImpl
// * @param socketChannel
// * @param headers
// */
// ProxyResponse(RequestImpl requestImpl, SocketChannel socketChannel,
// Map<String, Object> headers) {
// super(requestImpl, socketChannel, headers);
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