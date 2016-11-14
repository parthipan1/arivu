/**
 * 
 */
package org.arivu.nioserver;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.pool.Pool;
import org.arivu.utils.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mr P
 *
 */
final class Connection {
	private static final Logger logger = LoggerFactory.getLogger(Connection.class);

	long startTime = System.currentTimeMillis();

	Pool<Connection> pool;

	Connection() {
		super();
	}

	public Connection(Pool<Connection> pool) {
		super();
		reset();
		this.pool = pool;
	}

	final ConnectionState state = new ConnectionState();
	RequestImpl req = null;
	Route route = null;
	boolean ssl = false;
	Connection assign(boolean ssl) {
		this.ssl = ssl;
		startTime = System.currentTimeMillis();
		return this;
	}

	void reset() {
		state.reset();
		req = null;
		route = null;
		startTime = 0;
	}
	 
	void read(SelectionKey key, Selector clientSelector) throws IOException {
		if(!ssl){
			readNormal(key, clientSelector);
		}else{
			readSsl(key, clientSelector);
		}
	}
	 
	void write(final SelectionKey key, final Selector clientSelector) throws IOException {
		if(!ssl){
			writeNormal(key, clientSelector);
		}else{
			writeSsl(key, clientSelector);
		}
	}
	
	void writeNormal(final SelectionKey key, final Selector clientSelector) throws IOException {
		logger.debug(" write  :: {} ", state.resBuff);
		if (state.resBuff != null) {
			if( Configuration.SINGLE_THREAD_MODE ){
				Server.getExecutorService().execute(new Runnable() {
					
					@Override
					public void run() {
						innerWrite(key);
					}
				});
			}else{
				try {
					if (state.poll == null) {
						state.poll = state.resBuff.queue.poll();
						if (state.poll != null) {
							state.rem = (int) state.poll.length();
							logger.debug("{} 1 write next ByteBuff size :: {} queueSize :: {}", state.resBuff, state.rem,
									state.resBuff.queue.size());
//						} else {
//							logger.debug("{} 2 write next ByteBuff is null! finish!", state.resBuff);
//							finish(key);
//							return;
						}
					}

					final int length = Math.min(Configuration.defaultChunkSize, state.rem - state.pos);
					final SocketChannel socketChannel = (SocketChannel) key.channel();
					final ByteBuffer wrap = ByteBuffer.wrap(state.poll.copyOfRange(state.pos, state.pos + length));
					while (wrap.remaining() > 0) {
//						if( socketChannel.isConnected() )
							socketChannel.write(wrap);
//						else{
//							finish(key);
//							return;
//						}
					}
					logger.debug("{}  3 write bytes from  :: {}  length :: {} to :: {} size :: {}", state.resBuff,
							state.pos, length, (state.pos + length), state.rem);
					state.pos += length;
					finishByteBuff(key, clientSelector);
				} catch (Throwable e) {
					logger.error("Failed in write req " + req + " :: ", e);
					try {
						finish(key);
					} catch (IOException e1) {
						logger.error("Failed in write finish req " + req + " :: ", e1);
					}
				}
			}
		}
	}

	private void innerWrite(final SelectionKey key) {
		try {
			while( (state.poll = state.resBuff.queue.poll()) != null ){
				state.rem = (int) state.poll.length();
				logger.debug("{} 1 write next ByteBuff size :: {} queueSize :: {}", state.resBuff, state.rem,
						state.resBuff.queue.size());
				
				while( state.rem > state.pos ){
					final int length = Math.min(Configuration.defaultChunkSize, state.rem - state.pos);
					final SocketChannel socketChannel = (SocketChannel) key.channel();
					final ByteBuffer wrap = ByteBuffer.wrap(state.poll.copyOfRange(state.pos, state.pos + length));
					while (wrap.hasRemaining()) {
							socketChannel.write(wrap);
					}
					logger.debug("{}  3 write bytes from  :: {}  length :: {} to :: {} size :: {}", state.resBuff,
							state.pos, length, (state.pos + length), state.rem);
					state.pos += length;
				}
				state.clearBytes();
			}
			logger.debug("{} 2 write next ByteBuff is null! finish!", state.resBuff);
			finish(key);
		} catch (Throwable e) {
			logger.error("Failed in write req " + req + " :: ", e);
			try {
				finish(key);
			} catch (IOException e1) {
				logger.error("Failed in write finish req " + req + " :: ", e1);
			}
		}
	}
	
	void finishByteBuff(SelectionKey key, Selector clientSelector) throws IOException {
		boolean empty = state.resBuff.queue.isEmpty();
		logger.debug("{} 4 finishByteBuff! empty :: {} queueSize :: {} read :: {} size :: {}", state.resBuff, empty,
				state.resBuff.queue.size(), state.pos, state.rem);
		if (state.rem == state.pos) {
			state.clearBytes();
			if (empty) {
				finish(key);
				return;
			}
		}
		key.interestOps(SelectionKey.OP_WRITE);
		clientSelector.wakeup();
	}

	void finish(final SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		SocketAddress remoteSocketAddress = channel.socket().getRemoteSocketAddress();
		// channel.finishConnect();
		channel.close();
		key.cancel();
		if (state.resBuff != null)
			RequestUtil.accessLog(state.resBuff.rc, state.resBuff.uri, startTime, System.currentTimeMillis(),
					state.resBuff.cl, remoteSocketAddress, state.resBuff.method);
		state.writeLen = 0;
		req = null;
		route = null;
		pool.put(this);
	}

	ReadState processMultipartInBytes(final byte[] content) {
		do {
			int searchPattern = RequestUtil.searchPattern(content, req.boundary, state.start, state.mi);
			if (searchPattern == RequestUtil.BYTE_SEARCH_DEFLT) {
				logger.debug(" d searchPattern :: {} start :: {} mi {} content.length {} req.boundary.length {} ",
						searchPattern, state.start, state.mi, content.length, req.boundary.length);
				if (state.rollOver != null) {
					req.body.add(state.rollOver);
				}
				req.body.add(ByteData.wrap(Arrays.copyOfRange(content, state.start, content.length)));
				state.setValue(0, 0, null);
				break;
			} else if (searchPattern < 0) {
				logger.debug(" l searchPattern :: {} start :: {} mi {} content.length {} req.boundary.length {} ",
						searchPattern, state.start, state.mi, content.length, req.boundary.length);
				if (state.mi > 0 && state.rollOver != null) {
					req.body.add(state.rollOver);
				}
				state.setValue(0, searchPattern * -1,
						ByteData.wrap(Arrays.copyOfRange(content, state.start, content.length)));
				break;
			} else if (searchPattern >= 0) {
				logger.debug(" e searchPattern :: {} start :: {} mi {} content.length {} req.boundary.length {} ",
						searchPattern, state.start, state.mi, content.length, req.boundary.length);
				if (searchPattern <= req.boundary.length) {
					if (state.rollOver != null) {
						byte[] prevContent = state.rollOver.array();
						req.body.add(ByteData.wrap(Arrays.copyOfRange(prevContent, state.start,
								prevContent.length - (req.boundary.length + 1 - searchPattern))));
					}
				} else {
					if (state.rollOver != null)
						req.body.add(state.rollOver);

					req.body.add(ByteData
							.wrap(Arrays.copyOfRange(content, state.start, searchPattern - req.boundary.length - 1)));

				}
				addMultiPart();
				req.body.clear();
				state.setValue(searchPattern + 1, 0, null);
				if (searchPattern + 5 == content.length) {
					return ReadState.proc;
				}
			}
		} while (true);

		return ReadState.nextMp;
	}

	void addMultiPart() {
		// System.out.println("\n%"+RequestUtil.convert(req.body)+"%\n");
		MultiPart parseAsMultiPart = RequestUtil.parseAsMultiPart(req.body);
		req.multiParts.put(parseAsMultiPart.name, parseAsMultiPart);
	}

	void readNormal(final SelectionKey key, final Selector clientSelector) throws IOException {
		int bytesRead = 0;
		byte endOfLineByte = 1;
		try {
			final byte[] readBuf = new byte[Configuration.defaultRequestBuffer];// ByteData.getChunkData(false);//
			final ByteBuffer wrap = ByteBuffer.wrap(readBuf);
			if ((bytesRead = ((SocketChannel) key.channel()).read(wrap)) > 0) {
				endOfLineByte = wrap.get(wrap.position() - 1);
				if (req == null) {
					readRawRequestHeader(key, clientSelector, bytesRead, readBuf).andProcessIt(this, key,
							bytesRead, endOfLineByte, readBuf, clientSelector);
				} else {
					readRawRequestBody(key, clientSelector, bytesRead, readBuf).andProcessIt(this, key, bytesRead,
							endOfLineByte, readBuf, clientSelector);
				}
			}
		} catch (Throwable e) {
			logger.error("Failed in read :: ", e);
			finish(key);
		}
	}

	byte[] getBytesRead(final int bytesRead,
			final byte[] readBuf){
		if (bytesRead == readBuf.length) {
			return readBuf;
		}else{
			return Arrays.copyOfRange(readBuf, 0, bytesRead);
		}
	}
	
	ReadState readRawRequestBody(final SelectionKey key, final Selector clientSelector, final int bytesRead,
			final byte[] readBuf) {
		state.contentLen -= bytesRead;
		if (!state.is404Res) {
			if (req.isMultipart) {
				if (!state.onceFlag) {
					state.onceFlag = true;
					state.start = req.boundary.length + 1;
				}
				return processMultipartInBytes(getBytesRead( bytesRead, readBuf));
			} else {
				req.body.add(ByteData.wrap(getBytesRead( bytesRead, readBuf)));
			}
		}
		return ReadState.next;
	}

	ReadState readRawRequestHeader(final SelectionKey key, final Selector clientSelector, final int bytesRead, final byte[] readBuf) {
		final int headerIndex = RequestUtil.getHeaderIndex(readBuf, RequestUtil.BYTE_13,
				RequestUtil.BYTE_10, 2);
		if (headerIndex == -1) {
			state.in.add(ByteData.wrap(readBuf));
		} else {
			state.in.add(ByteData.wrap(Arrays.copyOfRange(readBuf, 0, headerIndex - 1)));
			if (parseRequestAndRoute()) {
				return ReadState.proc;
			}
			if (state.contentLen == 0l) {
				return ReadState.proc;
			} else if (headerIndex + 1 < bytesRead) {
				state.contentLen -= (bytesRead - headerIndex - 1);
				if (req.isMultipart) {
					state.start = req.boundary.length + 1;
					state.onceFlag = true;
					return processMultipartInBytes(Arrays.copyOfRange(readBuf, headerIndex + 1, bytesRead));
				} else {
					req.body.add(ByteData.wrap(Arrays.copyOfRange(readBuf, headerIndex + 1, bytesRead)));
				}
			}
		}
		return ReadState.next;
	}

	boolean parseRequestAndRoute() {
		req = RequestUtil.parseRequest(state.in);
		route = RequestUtil.getMatchingRoute(Configuration.routes, req.getUri(), req.getHttpMethod(), false);
		state.in.clear();
		logger.debug(" Got Request :: {}", req);
		setContentLen();
		if (route == Configuration.defaultRoute) {
			if (req.getHttpMethod() == HttpMethod.GET || req.getHttpMethod() == HttpMethod.HEAD
					|| req.getHttpMethod() == HttpMethod.TRACE) {

				return true;
			} else if (state.contentLen > 0) {
				state.is404Res = true;
			} else {
				return true;
			}
		}else if (req.getHttpMethod() == HttpMethod.GET || req.getHttpMethod() == HttpMethod.HEAD
				|| req.getHttpMethod() == HttpMethod.TRACE){
			return true;
		}
		return false;
	}

	void nextMultiPartNext(final SelectionKey key, int bytesRead, byte EOL0, byte[] readBuf, Selector clientSelector) {
		int size = req.body.size();
		if (size == 0) {
			key.interestOps(SelectionKey.OP_READ);
			clientSelector.wakeup();
		} else if (bytesRead == -1 || EOL0 == RequestUtil.BYTE_10 && isEndOfLine(bytesRead, readBuf))
			processRequest(key, clientSelector);
		else {
			key.interestOps(SelectionKey.OP_READ);
			clientSelector.wakeup();
		}
	}

	void nextRead(final SelectionKey key, int bytesRead, byte EOL0, Selector clientSelector) {
		if (bytesRead == -1 || state.contentLen == 0l || (state.contentLen == -1l && EOL0 == RequestUtil.BYTE_10))
			processRequest(key, clientSelector);
		else {
			key.interestOps(SelectionKey.OP_READ);
			clientSelector.wakeup();
		}
	}

	void setContentLen() {
		state.contentLen = -1l;
		List<Object> list = req.getHeaders().get("Content-Length");
		if (!NullCheck.isNullOrEmpty(list)) {
			String conLenStrHdr = list.get(0).toString();
			if (!NullCheck.isNullOrEmpty(conLenStrHdr)) {
				state.contentLen = Long.parseLong(conLenStrHdr);
			}
		}
	}

	boolean isEndOfLine(int bytesRead, byte[] array) {
		StringBuffer endOfLineBuf = new StringBuffer();
		int start = bytesRead - req.boundary.length - 4;
		if (start >= 0) {
			for (int i = start; i < array.length - 1; i++)
				endOfLineBuf.append((char) array[i]);
		}
		String string = new String(req.boundary);
		return endOfLineBuf.toString().startsWith(string + "--");
	}

	public void processRequest(final SelectionKey key, Selector clientSelector) {
		logger.debug("process connection from {}", ((SocketChannel) key.channel()).socket().getRemoteSocketAddress());
		AsynContext ctx = null;
		try {
			if (route != null) {
				final Response response = route.getResponse(req);
				if (response != null) {
					ctx = new AsynContextImpl(key, req, response, state, clientSelector);
					StaticRef.set(req, response, route, ctx, key);
					route.handle(req, response);
				}
			}
		} finally {
			StaticRef.clear();
			if (ctx == null) {
				key.interestOps(SelectionKey.OP_WRITE);
				clientSelector.wakeup();
			} else if (!ctx.isAsynchronousFinish()) {
				ctx.setAsynchronousFinish(true);
				ctx.finish();
			}
		}
	}


	ByteBuffer myAppData;
	ByteBuffer myNetData;
	ByteBuffer peerAppData;
	ByteBuffer peerNetData;
	SSLEngine engine;
	int appBufferSize = 0;
	
	void readSsl(SelectionKey key, Selector clientSelector) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		logger.debug("About to read from a client...");

		peerNetData.clear();
		int bytesRead = socketChannel.read(peerNetData);
		if (bytesRead > 0) {
			peerNetData.flip();
			while (peerNetData.hasRemaining()) {
				peerAppData.clear();
				SSLEngineResult result = engine.unwrap(peerNetData, peerAppData);
				switch (result.getStatus()) {
				case OK:
					peerAppData.flip();
					byte[] array = peerAppData.array();
					logger.debug("OK from client bytesRead {} msg {} array.length {} ",bytesRead,new String(array),array.length);
					byte endOfLineByte = array[bytesRead-1];//peerAppData.get(peerAppData.position() - 1);
					if (req == null) {
						readRawRequestHeader(key, clientSelector, bytesRead, array).andProcessIt(this, key,
								bytesRead, endOfLineByte, array, clientSelector);
					} else {
						readRawRequestBody(key, clientSelector, bytesRead, array).andProcessIt(this, key, bytesRead,
								endOfLineByte, array, clientSelector);
					}
					break;
				case BUFFER_OVERFLOW:
					peerAppData = enlargeApplicationBuffer(peerAppData);
					break;
				case BUFFER_UNDERFLOW:
					peerNetData = handleBufferUnderflow(peerNetData);
					break;
				case CLOSED:
					logger.debug("Client wants to close connection...");
					closeConnection(key);
					logger.debug("Goodbye client!");
					return;
				default:
					throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
				}
			}
		} else if (bytesRead < 0) {
			logger.error("Received end of stream. Will try to close connection with client...");
			handleEndOfStream(key);
			logger.debug("Goodbye client!");
		}
	}

	void writeSsl(SelectionKey key, Selector clientSelector) throws IOException {

		SocketChannel socketChannel = (SocketChannel) key.channel();
		logger.debug("About to write to a client...");

		if (state.poll == null) {
			state.poll = state.resBuff.queue.poll();
			if (state.poll != null) {
				state.rem = (int) state.poll.length();
				logger.debug("{} 1 write next ByteBuff size :: {} queueSize :: {}", state.resBuff, state.rem,
						state.resBuff.queue.size());
			}
		}
		final int length = Math.min(appBufferSize, state.rem - state.pos);
		myAppData.clear();
		myAppData.put(state.poll.copyOfRange(state.pos, state.pos + length));
		myAppData.flip();
		
		while (myAppData.hasRemaining()) {
			// The loop has a meaning for (outgoing) messages larger than 16KB.
			// Every wrap call will remove 16KB from the original message and
			// send it to the remote peer.
			myNetData.clear();
			SSLEngineResult result = engine.wrap(myAppData, myNetData);
			switch (result.getStatus()) {
			case OK:
				myNetData.flip();
				while (myNetData.hasRemaining()) {
					socketChannel.write(myNetData);
				}
				logger.debug("Message sent to the client: " + "Thanks");
				break;
			case BUFFER_OVERFLOW:
				myNetData = enlargePacketBuffer(myNetData);
				break;
			case BUFFER_UNDERFLOW:
				throw new SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.");
			case CLOSED:
				closeConnection(key);
				return;
			default:
				throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
			}
		}
		state.pos += length;
		finishByteBuff(key, clientSelector);
	}

	boolean doHandshake(SocketChannel socketChannel, SSLEngine engine) throws IOException {

		this.engine = engine;
		logger.debug("About to do handshake...");

		SSLEngineResult result;
		HandshakeStatus handshakeStatus;

		SSLSession session = engine.getSession();
		appBufferSize = session.getApplicationBufferSize();
		myAppData = ByteBuffer.allocate(appBufferSize);
		myNetData = ByteBuffer.allocate(session.getPacketBufferSize());
		peerAppData = ByteBuffer.allocate(appBufferSize);
		peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());

		// NioSsl fields myAppData and peerAppData are supposed to be
		// large enough to hold all message data the peer
		// will send and expects to receive from the other peer respectively.
		// Since the messages to be exchanged will usually be less
		// than 16KB long the capacity of these fields should also be smaller.
		// Here we initialize these two local buffers
		// to be used for the handshake, while keeping client's buffers at the
		// same size.
		int appBufferSize = session.getApplicationBufferSize();
		ByteBuffer myAppData = ByteBuffer.allocate(appBufferSize);
		ByteBuffer peerAppData = ByteBuffer.allocate(appBufferSize);
		myNetData.clear();
		peerNetData.clear();

		handshakeStatus = engine.getHandshakeStatus();
		while (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED
				&& handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
			switch (handshakeStatus) {
			case NEED_UNWRAP:
				if (socketChannel.read(peerNetData) < 0) {
					if (engine.isInboundDone() && engine.isOutboundDone()) {
						return false;
					}
					try {
						engine.closeInbound();
					} catch (SSLException e) {
						logger.error(
								"This engine was forced to close inbound, without having received the proper SSL/TLS close notification message from the peer, due to end of stream.");
					}
					engine.closeOutbound();
					// After closeOutbound the engine will be set to WRAP state,
					// in order to try to send a close message to the client.
					handshakeStatus = engine.getHandshakeStatus();
					break;
				}
				peerNetData.flip();
				try {
					result = engine.unwrap(peerNetData, peerAppData);
					peerNetData.compact();
					handshakeStatus = result.getHandshakeStatus();
				} catch (SSLException sslException) {
					logger.error(
							"A problem was encountered while processing the data that caused the SSLEngine to abort. Will try to properly close connection...");
					engine.closeOutbound();
					handshakeStatus = engine.getHandshakeStatus();
					break;
				}
				switch (result.getStatus()) {
				case OK:
					break;
				case BUFFER_OVERFLOW:
					// Will occur when peerAppData's capacity is smaller than
					// the data derived from peerNetData's unwrap.
					peerAppData = enlargeApplicationBuffer(peerAppData);
					break;
				case BUFFER_UNDERFLOW:
					// Will occur either when no data was read from the peer or
					// when the peerNetData buffer was too small to hold all
					// peer's data.
					peerNetData = handleBufferUnderflow(peerNetData);
					break;
				case CLOSED:
					if (engine.isOutboundDone()) {
						return false;
					} else {
						engine.closeOutbound();
						handshakeStatus = engine.getHandshakeStatus();
						break;
					}
				default:
					throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
				}
				break;
			case NEED_WRAP:
				myNetData.clear();
				try {
					result = engine.wrap(myAppData, myNetData);
					handshakeStatus = result.getHandshakeStatus();
				} catch (SSLException sslException) {
					logger.error(
							"A problem was encountered while processing the data that caused the SSLEngine to abort. Will try to properly close connection...");
					engine.closeOutbound();
					handshakeStatus = engine.getHandshakeStatus();
					break;
				}
				switch (result.getStatus()) {
				case OK:
					myNetData.flip();
					while (myNetData.hasRemaining()) {
						socketChannel.write(myNetData);
					}
					break;
				case BUFFER_OVERFLOW:
					// Will occur if there is not enough space in myNetData
					// buffer to write all the data that would be generated by
					// the method wrap.
					// Since myNetData is set to session's packet size we should
					// not get to this point because SSLEngine is supposed
					// to produce messages smaller or equal to that, but a
					// general handling would be the following:
					myNetData = enlargePacketBuffer(myNetData);
					break;
				case BUFFER_UNDERFLOW:
					throw new SSLException(
							"Buffer underflow occured after a wrap. I don't think we should ever get here.");
				case CLOSED:
					try {
						myNetData.flip();
						while (myNetData.hasRemaining()) {
							socketChannel.write(myNetData);
						}
						// At this point the handshake status will probably be
						// NEED_UNWRAP so we make sure that peerNetData is clear
						// to read.
						peerNetData.clear();
					} catch (Exception e) {
						logger.error("Failed to send server's CLOSE message due to socket channel's failure.");
						handshakeStatus = engine.getHandshakeStatus();
					}
					break;
				default:
					throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
				}
				break;
			case NEED_TASK:
				Runnable task;
				while ((task = engine.getDelegatedTask()) != null) {
					Server.getExecutorService().execute(task);
				}
				handshakeStatus = engine.getHandshakeStatus();
				break;
			case FINISHED:
				break;
			case NOT_HANDSHAKING:
				break;
			default:
				throw new IllegalStateException("Invalid SSL status: " + handshakeStatus);
			}
		}

		return true;

	}

	ByteBuffer enlargePacketBuffer(ByteBuffer buffer) {
		return enlargeBuffer(buffer, engine.getSession().getPacketBufferSize());
	}

	ByteBuffer enlargeApplicationBuffer(ByteBuffer buffer) {
		return enlargeBuffer(buffer, engine.getSession().getApplicationBufferSize());
	}

	ByteBuffer enlargeBuffer(ByteBuffer buffer, int sessionProposedCapacity) {
		if (sessionProposedCapacity > buffer.capacity()) {
			buffer = ByteBuffer.allocate(sessionProposedCapacity);
		} else {
			buffer = ByteBuffer.allocate(buffer.capacity() * 2);
		}
		return buffer;
	}

	ByteBuffer handleBufferUnderflow(ByteBuffer buffer) {
		if (engine.getSession().getPacketBufferSize() < buffer.limit()) {
			return buffer;
		} else {
			ByteBuffer replaceBuffer = enlargePacketBuffer(buffer);
			buffer.flip();
			replaceBuffer.put(buffer);
			return replaceBuffer;
		}
	}
	
	void closeConnection(SelectionKey key) throws IOException  {
		SocketChannel socketChannel = (SocketChannel) key.channel();
        engine.closeOutbound();
        doHandshake(socketChannel, engine);
        finish(key);
    }
	
	void handleEndOfStream(SelectionKey key) throws IOException  {
        try {
            engine.closeInbound();
        } catch (Exception e) {
            logger.error("This engine was forced to close inbound, without having received the proper SSL/TLS close notification message from the peer, due to end of stream.");
        }
        closeConnection(key);
    }

}

final class ConnectionState {
	// private static final Logger logger =
	// LoggerFactory.getLogger(ConnectionState.class);
	// Write state
	int writeLen = 0;
	ByteData poll = null;
	int pos = 0;
	int rem = 0;
	Ref resBuff = null;

	// Read state
	final List<ByteData> in = new DoublyLinkedList<>();
	boolean onceFlag = false;
	long contentLen = -1l;
	int start = 0;
	int mi = 0;
	ByteData rollOver = null;
	boolean is404Res = false;

	void reset() {
		writeLen = 0;
		poll = null;
		resBuff = null;
		pos = 0;
		rem = 0;

		in.clear();
		onceFlag = false;
		start = 0;
		mi = 0;
		rollOver = null;
		contentLen = -1l;
		is404Res = false;
	}

	void clearBytes() {
		poll = null;
		pos = 0;
		rem = 0;
	}

	void setValue(int s, int m, ByteData bb) {
		start = s;
		mi = m;
		rollOver = bb;
	}
}
enum ReadState {
	next {

		@Override
		void andProcessIt(Connection c, SelectionKey key, int bytesRead, byte EOL0, byte[] readBuf,
				Selector clientSelector) {
			c.nextRead(key, bytesRead, EOL0, clientSelector);
		}

	},
	nextMp {

		@Override
		void andProcessIt(Connection c, SelectionKey key, int bytesRead, byte EOL0, byte[] readBuf,
				Selector clientSelector) {
			c.nextMultiPartNext(key, bytesRead, EOL0, readBuf, clientSelector);
		}

	},
	proc;

	void andProcessIt(Connection c, final SelectionKey key, int bytesRead, byte EOL0, byte[] readBuf,
			Selector clientSelector) {
		c.processRequest(key, clientSelector);
	}
}