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
import java.util.Map;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.pool.ConcurrentPool;
import org.arivu.pool.Pool;
import org.arivu.pool.PoolFactory;
import org.arivu.utils.Env;
import org.arivu.utils.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mr P
 *
 */
final class Connection {
	private static final Logger logger = LoggerFactory.getLogger(Connection.class);

	private static final int BUFFER_SIZE = Integer.parseInt(Env.getEnv("ssl.bufferSize", "1048576")) ;
	
	private static final class SSLByteBuffer{
		final ByteBuffer bb = ByteBuffer.allocateDirect(BUFFER_SIZE);
	} 
	
	static final Pool<SSLByteBuffer> sslbufferPool = new ConcurrentPool<SSLByteBuffer>(new PoolFactory<SSLByteBuffer>() {

		@Override
		public SSLByteBuffer create(Map<String, Object> params) {
			return new SSLByteBuffer();
		}

		@Override
		public void close(SSLByteBuffer t) {
		}

		@Override
		public void clear(SSLByteBuffer t) {
			if (t != null){
				t.bb.clear();
				t.bb.position(0);
			}

		}
	}, SSLByteBuffer.class);
	
	static{
		sslbufferPool.setMaxPoolSize(-1);
		sslbufferPool.setIdleTimeout(30000);
		sslbufferPool.setLifeSpan(-1);
		sslbufferPool.setMaxReuseCount(-1);
		Server.registerShutdownHook(new Runnable() {
			
			@Override
			public void run() {
				try {
					sslbufferPool.close();
				} catch (Exception e) {
					logger.error("Failed in close sslbufferPool :: ", e);
				}
			}
		});
	}
	
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
//	ByteBuffer myAppData;
//	ByteBuffer myNetData;
//	ByteBuffer peerAppData;
//	ByteBuffer peerNetData;
	SSLEngine engine;
//	int appBufferSize = 0;
	
	Connection assign(boolean ssl) {
		this.ssl = ssl;
		startTime = System.currentTimeMillis();
		logger.debug("start connection {}",this);
		return this;
	}

	void reset() {
		state.reset();
		req = null;
		route = null;
		startTime = 0;
		
		ssl = true;
//		sslbufferPool.put(myAppData);
//		sslbufferPool.put(myNetData);
//		sslbufferPool.put(peerAppData);
//		sslbufferPool.put(peerNetData);
//		myAppData = null;
//		myNetData = null;
//		peerAppData = null;
//		peerNetData = null;
		engine = null;
//		appBufferSize = 0;
	}
	 
	void read(SelectionKey key, Selector clientSelector) throws IOException {
		logger.debug("read connection {}",this);
		if(!ssl){
			readNormal(key, clientSelector);
		}else{
			readSsl(key, clientSelector);
		}
	}
	 
	void write(final SelectionKey key, final Selector clientSelector) throws IOException {
		logger.debug("write connection {}",this);
		if(!ssl){
			writeNormal(key, clientSelector);
		}else{
			writeSsl(key, clientSelector);
		}
	}
	
	void writeNormal(final SelectionKey key, final Selector clientSelector) throws IOException {
		logger.debug(" writeNormal  :: {} ", state.resBuff);
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
							logger.debug("writeNormal Poll {} next ByteBuff size :: {} queueSize :: {}", state.resBuff, state.rem,
									state.resBuff.queue.size());
						}
					}

					final int length = Math.min(Configuration.defaultChunkSize, state.rem - state.pos);
					final SocketChannel socketChannel = (SocketChannel) key.channel();
					final ByteBuffer wrap = ByteBuffer.wrap(state.poll.copyOfRange(state.pos, state.pos + length));
					while (wrap.remaining() > 0) {
						socketChannel.write(wrap);
					}
					logger.debug("writeNormal copyOfRange {} bytes from  :: {}  length :: {} to :: {} size :: {}", state.resBuff,
							state.pos, length, (state.pos + length), state.rem);
					state.pos += length;
					finishByteBuff(key, clientSelector);
				} catch (Throwable e) {
					logger.error("Failed in writeNormal req " + req + " :: ", e);
					try {
						finish(key);
					} catch (IOException e1) {
						logger.error("Failed in writeNormal finish req " + req + " :: ", e1);
					}
				}
			}
		}else{
			finish(key);
		}
	}

	private void innerWrite(final SelectionKey key) {
		try {
			while( (state.poll = state.resBuff.queue.poll()) != null ){
				state.rem = (int) state.poll.length();
				logger.debug("InnerPoll {} write next ByteBuff size :: {} queueSize :: {}", state.resBuff, state.rem,
						state.resBuff.queue.size());
				
				while( state.rem > state.pos ){
					final int length = Math.min(Configuration.defaultChunkSize, state.rem - state.pos);
					final SocketChannel socketChannel = (SocketChannel) key.channel();
					final ByteBuffer wrap = ByteBuffer.wrap(state.poll.copyOfRange(state.pos, state.pos + length));
					while (wrap.hasRemaining()) {
							socketChannel.write(wrap);
					}
					logger.debug("copyOfRange InnerWrite {} write bytes from  :: {}  length :: {} to :: {} size :: {}", state.resBuff,
							state.pos, length, (state.pos + length), state.rem);
					state.pos += length;
				}
				state.clearBytes();
			}
			logger.debug("Innerwrite {} write next ByteBuff is null! finish!", state.resBuff);
			finish(key);
		} catch (Throwable e) {
			logger.error("Failed in innerWrite req " + req + " :: ", e);
			try {
				finish(key);
			} catch (IOException e1) {
				logger.error("Failed in innerWrite finish req " + req + " :: ", e1);
			}
		}
	}
	
	void finishByteBuff(SelectionKey key, Selector clientSelector) throws IOException {
		boolean empty = state.resBuff.queue.isEmpty();
		logger.debug("finishByteBuff {}  empty :: {} queueSize :: {} read :: {} size :: {}", state.resBuff, empty,
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
		
//		if(ssl && engine!=null)
//			engine.closeOutbound();
		
		channel.close();
		key.cancel();
		if (state.resBuff != null)
			RequestUtil.accessLog(state.resBuff.rc, state.resBuff.uri, startTime, System.currentTimeMillis(),
					state.resBuff.cl, remoteSocketAddress, state.resBuff.method);
		state.writeLen = 0;
		req = null;
		route = null;
		pool.put(this);
		logger.debug("finish connection {}",this);
	}

	ReadState processMultipartInBytes(final byte[] content) {
		do {
			int searchPattern = RequestUtil.searchPattern(content, req.boundary, state.start, state.mi);
			if (searchPattern == RequestUtil.BYTE_SEARCH_DEFLT) {
				logger.debug("No Match searchPattern :: {} start :: {} mi {} content.length {} req.boundary.length {} ",
						searchPattern, state.start, state.mi, content.length, req.boundary.length);
				if (state.rollOver != null) {
					req.body.add(state.rollOver);
				}
				req.body.add(ByteData.wrap(Arrays.copyOfRange(content, state.start, content.length)));
				state.setValue(0, 0, null);
				break;
			} else if (searchPattern < 0) {
				logger.debug(" overLap searchPattern :: {} start :: {} mi {} content.length {} req.boundary.length {} ",
						searchPattern, state.start, state.mi, content.length, req.boundary.length);
				if (state.mi > 0 && state.rollOver != null) {
					req.body.add(state.rollOver);
				}
				state.setValue(0, searchPattern * -1,
						ByteData.wrap(Arrays.copyOfRange(content, state.start, content.length)));
				break;
			} else if (searchPattern >= 0) {
				logger.debug(" Match searchPattern :: {} start :: {} mi {} content.length {} req.boundary.length {} ",
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
					readRawRequestHeader(bytesRead, readBuf).andProcessIt(this, key,
							bytesRead, endOfLineByte, readBuf, clientSelector);
				} else {
					readRawRequestBody(bytesRead, readBuf).andProcessIt(this, key, bytesRead,
							endOfLineByte, readBuf, clientSelector);
				}
			}else if(bytesRead==0){
				ReadState.next.andProcessIt(this, key, bytesRead,
							endOfLineByte, readBuf, clientSelector);
			}else{
				ReadState.proc.andProcessIt(this, key, bytesRead,
						endOfLineByte, readBuf, clientSelector);
			}
		} catch (Throwable e) {
			logger.error("Failed in readNormal :: ", e);
			finish(key);
		}
	}

	static byte[] getBytesRead(final int bytesRead,
			final byte[] readBuf){
		if (bytesRead == readBuf.length) {
			return readBuf;
		}else{
			return Arrays.copyOfRange(readBuf, 0, bytesRead);
		}
	}
	
	ReadState readRawRequestBody(final int bytesRead, final byte[] readBuf) {
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
		if (req.isMultipart) 
			return ReadState.nextMp;
		else
			return ReadState.next;
	}

	ReadState readRawRequestHeader(final int bytesRead, final byte[] readBuf) {
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
					if (state.contentLen < 0l) {
						return ReadState.proc;
					}
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
				logger.debug(" 404 response for :: {}", req);
				return true;
			} else if (state.contentLen > 0) {
				logger.debug(" 404 content read for :: {}", req);
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
			logger.debug(" read next multipart ");
		} else if (bytesRead == -1 || EOL0 == RequestUtil.BYTE_10 && isEndOfLine(bytesRead, readBuf)){
			logger.debug(" multipart processRequest ");
			processRequest(key, clientSelector);
		}else {
			key.interestOps(SelectionKey.OP_READ);
			clientSelector.wakeup();
			logger.debug(" read next multipart ");
		}
	}

	void nextRead(final SelectionKey key, int bytesRead, byte EOL0, Selector clientSelector) {
		if (bytesRead == -1 || state.contentLen == 0l || (state.contentLen == -1l && EOL0 == RequestUtil.BYTE_10))
			processRequest(key, clientSelector);
		else {
			key.interestOps(SelectionKey.OP_READ);
			clientSelector.wakeup();
			logger.debug(" read next ");
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

	void processRequest(final SelectionKey key, Selector clientSelector) {
		logger.debug("process connection from {}", ((SocketChannel) key.channel()).socket().getRemoteSocketAddress());
		AsynContext ctx = null;
		try {
			if (route != null) {
				final Response response = route.getResponse(req);
				if (response != null) {
					ctx = new AsynContextImpl(key, req, response, state, clientSelector);
					StaticRef.set(req, response, ctx, key);
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

	void readSsl(SelectionKey key, Selector clientSelector) throws IOException {
		logger.debug(" readSsl connection {} ",this);
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		SSLByteBuffer peerAppDataRef = sslbufferPool.get(null);
		SSLByteBuffer peerNetDataRef = sslbufferPool.get(null);
		
		ByteBuffer peerAppData = peerAppDataRef.bb;
		ByteBuffer peerNetData = peerNetDataRef.bb;
		
		peerNetData.clear();
		int bytesRead = socketChannel.read(peerNetData);
		if (bytesRead > 0) {
			peerNetData.flip();
			while (peerNetData.hasRemaining()) {
				peerAppData.clear();
				SSLEngineResult result = engine.unwrap(peerNetData, peerAppData);
				switch (result.getStatus()) {
				case OK:
					logger.debug(" readSsl OK connection {} ",this);
					peerAppData.flip();
					final int bytesRemaining = peerAppData.remaining();
					int noofReads = bytesRemaining/Configuration.defaultChunkSize;
					int tailLen = bytesRemaining%Configuration.defaultChunkSize;
					for(int i=0;i<noofReads;i++){
						byte[] array = new byte[Configuration.defaultChunkSize];
						peerAppData.get(array);
						readIn(key, clientSelector, array);
					}
					byte[] array = new byte[tailLen];
					peerAppData.get(array);
					readIn(key, clientSelector, array);
					break;
				case BUFFER_OVERFLOW:
					logger.debug(" readSsl BUFFER_OVERFLOW connection {} ",this);
					if(peerAppData.capacity()==BUFFER_SIZE){
						peerAppData = enlargeSslApplicationDataBuffer(peerAppData,peerAppDataRef);
						peerAppDataRef = null;
					}else
						peerAppData = enlargeSslApplicationDataBuffer(peerAppData,null);
					break;
				case BUFFER_UNDERFLOW:
					logger.debug(" readSsl BUFFER_UNDERFLOW connection {} ",this);
					if(peerNetData.capacity()==BUFFER_SIZE){
						peerNetData = handleSslDataBufferUnderflow(peerNetData,peerNetDataRef);
						peerNetDataRef = null;
					}else
						peerNetData = handleSslDataBufferUnderflow(peerNetData,null);
//					ByteBuffer replaceBuffer = ByteBuffer.allocate(peerNetData.capacity() * 2);
//					peerNetData.flip();
//					replaceBuffer.put(peerNetData);
//					peerNetData = replaceBuffer;
					break;
				case CLOSED:
					logger.debug(" readSsl CLOSED connection {} ",this);
					closeSslConnection(key);
					sslbufferPool.put(peerAppDataRef);
					sslbufferPool.put(peerNetDataRef);
					return;
				default:
					throw new IllegalStateException("Invalid SSL status :: " + result.getStatus());
				}
			}
		} else if (bytesRead < 0) {
			logger.debug(" readSsl handleSslEndOfStream connection {} ",this);
			handleSslEndOfStream(key);
		}
		sslbufferPool.put(peerAppDataRef);
		sslbufferPool.put(peerNetDataRef);
	}

	private void readIn(SelectionKey key, Selector clientSelector,
			byte[] array) {
		logger.debug("readIn {} contentLn {} array.length {} ",this,state.contentLen,array.length);
		byte endOfLineByte = array[array.length-1];//peerAppData.get(peerAppData.position() - 1);
		ReadState rstate = ReadState.next;
		if (req == null) {
			rstate = readRawRequestHeader(array.length, array);
		} else {
			rstate = readRawRequestBody(array.length, array);
		}
		rstate.andProcessIt(this, key,
				array.length, endOfLineByte, array, clientSelector);
	}

	void writeSsl(SelectionKey key, Selector clientSelector) throws IOException {
		logger.debug(" writeSsl connection {} ",this);
		SocketChannel socketChannel = (SocketChannel) key.channel();

		if (state.poll == null) {
			state.poll = state.resBuff.queue.poll();
			if (state.poll != null) {
				state.rem = (int) state.poll.length();
				logger.debug("writessl {} next ByteBuff size :: {} queueSize :: {}", state.resBuff, state.rem,
						state.resBuff.queue.size());
			}
		}
		SSLByteBuffer myAppDataRef = sslbufferPool.get(null);
		SSLByteBuffer myNetDataRef = sslbufferPool.get(null);
		
		ByteBuffer myAppData = myAppDataRef.bb;
		ByteBuffer myNetData = myNetDataRef.bb;
		
		final int length = Math.min(BUFFER_SIZE, state.rem - state.pos);
		myAppData.clear();
		myAppData.put(state.poll.copyOfRange(state.pos, state.pos + length));
		myAppData.flip();
		
		while (myAppData.hasRemaining()) {
			myNetData.clear();
			SSLEngineResult result = engine.wrap(myAppData, myNetData);
			switch (result.getStatus()) {
			case OK:
				logger.debug(" writeSsl OK connection {} ",this);
				myNetData.flip();
				while (myNetData.hasRemaining()) {
					socketChannel.write(myNetData);
				}
				break;
			case BUFFER_OVERFLOW:
				logger.debug(" writeSsl BUFFER_OVERFLOW connection {} ",this);
				if(myNetData.capacity()==BUFFER_SIZE){
					myNetData = enlargeSslDataBuffer(myNetData,myNetDataRef);
					myNetDataRef = null;
				}else
					myNetData = enlargeSslDataBuffer(myNetData,null);
				break;
			case BUFFER_UNDERFLOW:
				logger.debug(" writeSsl BUFFER_UNDERFLOW connection {} ",this);
				sslbufferPool.put(myAppDataRef);
				sslbufferPool.put(myNetDataRef);
				throw new SSLException("Buffer underflow occured after a wrap!");
			case CLOSED:
				logger.debug(" writeSsl CLOSED connection {} ",this);
				closeSslConnection(key);
				sslbufferPool.put(myAppDataRef);
				sslbufferPool.put(myNetDataRef);
				return;
			default:
				throw new IllegalStateException("Invalid SSL status :: " + result.getStatus());
			}
		}
		state.pos += length;
		finishByteBuff(key, clientSelector);
		sslbufferPool.put(myAppDataRef);
		sslbufferPool.put(myNetDataRef);
	}

	boolean doSslHandshake(SocketChannel socketChannel, SSLEngine engine) throws IOException {
		logger.debug(" doSslHandshake connection {} ",this);
		this.engine = engine;

		SSLEngineResult result;
		HandshakeStatus handshakeStatus;

		SSLByteBuffer myAppDataRef = sslbufferPool.get(null);
		SSLByteBuffer myNetDataRef = sslbufferPool.get(null);
		SSLByteBuffer peerAppDataRef = sslbufferPool.get(null);
		SSLByteBuffer peerNetDataRef = sslbufferPool.get(null);
		
		ByteBuffer myAppData = myAppDataRef.bb;
		ByteBuffer myNetData = myNetDataRef.bb;
		ByteBuffer peerAppData = peerAppDataRef.bb;
		ByteBuffer peerNetData = peerNetDataRef.bb;

		int peerNetDataCap = BUFFER_SIZE;
		myNetData.clear();
		peerNetData.clear();

		handshakeStatus = engine.getHandshakeStatus();
		while (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED
				&& handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
			logger.debug(" doSslHandshake while loop {} connection {} ",handshakeStatus,this);
			switch (handshakeStatus) {
			case NEED_UNWRAP:
				logger.debug(" doSslHandshake NEED_UNWRAP connection {} ",this);
				if (socketChannel.read(peerNetData) < 0) {
					if (engine.isInboundDone() && engine.isOutboundDone()) {
						sslbufferPool.put(myAppDataRef);
						sslbufferPool.put(myNetDataRef);
						sslbufferPool.put(peerAppDataRef);
						sslbufferPool.put(peerNetDataRef);
						return false;
					}
					try {
						engine.closeInbound();
					} catch (SSLException e) {
						logger.error("Error handshake :: ", e);
					}
					engine.closeOutbound();
					handshakeStatus = engine.getHandshakeStatus();
					break;
				}
				peerNetData.flip();
				try {
					result = engine.unwrap(peerNetData, peerAppData);
					peerNetData.compact();
					handshakeStatus = result.getHandshakeStatus();
				} catch (SSLException sslException) {
					logger.error("Error Unwrapping handshake :: ",sslException);
					engine.closeOutbound();
					handshakeStatus = engine.getHandshakeStatus();
					break;
				}
				switch (result.getStatus()) {
				case OK:
					logger.debug(" doSslHandshake NEED_UNWRAP OK connection {} ",this);
					break;
				case BUFFER_OVERFLOW:
					logger.debug(" doSslHandshake NEED_UNWRAP BUFFER_OVERFLOW connection {} ",this);
					if(peerAppData.capacity()==BUFFER_SIZE){
						peerAppData = enlargeSslApplicationDataBuffer(peerAppData,peerAppDataRef);
						peerAppDataRef = null;
					}
					else
						peerAppData = enlargeSslApplicationDataBuffer(peerAppData,null);
					break;
				case BUFFER_UNDERFLOW:
					logger.debug(" doSslHandshake NEED_UNWRAP BUFFER_UNDERFLOW connection {} ",this);
//					peerNetData = handleSslDataBufferUnderflow(peerNetData);
					peerNetDataCap += BUFFER_SIZE;
					logger.debug(" doSslHandshake NEED_UNWRAP BUFFER_UNDERFLOW peerNetDataCap {} ",peerNetDataCap);
					ByteBuffer replaceBuffer = ByteBuffer.allocateDirect(peerNetDataCap);
					peerNetData.flip();
					replaceBuffer.put(peerNetData);
					
					if(peerNetDataCap == BUFFER_SIZE*2){
						sslbufferPool.put(peerNetDataRef);
						peerNetDataRef = null;
					}
					
					peerNetData = replaceBuffer;
					break;
				case CLOSED:
					logger.debug(" doSslHandshake NEED_UNWRAP CLOSED connection {} ",this);
					if (engine.isOutboundDone()) {
						sslbufferPool.put(myAppDataRef);
						sslbufferPool.put(myNetDataRef);
						sslbufferPool.put(peerAppDataRef);
						sslbufferPool.put(peerNetDataRef);
						return false;
					} else {
						engine.closeOutbound();
						handshakeStatus = engine.getHandshakeStatus();
						break;
					}
				default:
					sslbufferPool.put(myAppDataRef);
					sslbufferPool.put(myNetDataRef);
					sslbufferPool.put(peerAppDataRef);
					sslbufferPool.put(peerNetDataRef);
					throw new IllegalStateException("Invalid SSL status :: " + result.getStatus());
				}
				break;
			case NEED_WRAP:
				logger.debug(" doSslHandshake NEED_WRAP connection {} ",this);
				myNetData.clear();
				try {
					result = engine.wrap(myAppData, myNetData);
					handshakeStatus = result.getHandshakeStatus();
				} catch (SSLException sslException) {
					logger.error("Error wraping handshake :: ", sslException);
					engine.closeOutbound();
					handshakeStatus = engine.getHandshakeStatus();
					break;
				}
				switch (result.getStatus()) {
				case OK:
					logger.debug(" doSslHandshake NEED_WRAP OK connection {} ",this);
					myNetData.flip();
					while (myNetData.hasRemaining()) {
						socketChannel.write(myNetData);
					}
					break;
				case BUFFER_OVERFLOW:
					logger.debug(" doSslHandshake NEED_WRAP BUFFER_OVERFLOW connection {} ",this);
					if(myNetData.capacity()==BUFFER_SIZE){
						myNetData = enlargeSslDataBuffer(myNetData,myNetDataRef);
						myNetDataRef = null;
					}else
						myNetData = enlargeSslDataBuffer(myNetData,null);
					break;
				case BUFFER_UNDERFLOW:
					logger.debug(" doSslHandshake NEED_WRAP BUFFER_UNDERFLOW connection {} ",this);
					sslbufferPool.put(myAppDataRef);
					sslbufferPool.put(myNetDataRef);
					sslbufferPool.put(peerAppDataRef);
					sslbufferPool.put(peerNetDataRef);
					throw new SSLException(
							"Buffer underflow occured after wrap!");
				case CLOSED:
					logger.debug(" doSslHandshake NEED_WRAP CLOSED connection {} ",this);
					try {
						myNetData.flip();
						while (myNetData.hasRemaining()) {
							socketChannel.write(myNetData);
						}
						peerNetData.clear();
					} catch (Exception e) {
						logger.error("Error on socket write :: ",e);
						handshakeStatus = engine.getHandshakeStatus();
					}
					break;
				default:
					sslbufferPool.put(myAppDataRef);
					sslbufferPool.put(myNetDataRef);
					sslbufferPool.put(peerAppDataRef);
					sslbufferPool.put(peerNetDataRef);
					throw new IllegalStateException("Invalid SSL status :: " + result.getStatus());
				}
				break;
			case NEED_TASK:
				logger.debug(" doSslHandshake NEED_TASK connection {} ",this);
				Runnable task;
				while ((task = engine.getDelegatedTask()) != null) {
					Server.getExecutorService().execute(task);
				}
				handshakeStatus = engine.getHandshakeStatus();
				break;
			case FINISHED:
				logger.debug(" doSslHandshake FINISHED connection {} ",this);
				break;
			case NOT_HANDSHAKING:
				logger.debug(" doSslHandshake NOT_HANDSHAKING connection {} ",this);
				break;
			default:
				sslbufferPool.put(myAppDataRef);
				sslbufferPool.put(myNetDataRef);
				sslbufferPool.put(peerAppDataRef);
				sslbufferPool.put(peerNetDataRef);
				throw new IllegalStateException("Invalid SSL status :: " + handshakeStatus);
			}
		}
		
		sslbufferPool.put(myAppDataRef);
		sslbufferPool.put(myNetDataRef);
		sslbufferPool.put(peerAppDataRef);
		sslbufferPool.put(peerNetDataRef);
		
		return true;

	}

	ByteBuffer enlargeSslDataBuffer(ByteBuffer buffer,SSLByteBuffer ref) {
		return enlargeSslDataBuffer(buffer, BUFFER_SIZE, ref);
	}

	ByteBuffer enlargeSslApplicationDataBuffer(ByteBuffer buffer,SSLByteBuffer ref) {
		return enlargeSslDataBuffer(buffer, BUFFER_SIZE, ref);
	}

	ByteBuffer enlargeSslDataBuffer(ByteBuffer buffer, int sessionProposedCapacity,SSLByteBuffer ref) {
		if (sessionProposedCapacity > buffer.capacity()) {
			buffer = ByteBuffer.allocateDirect(sessionProposedCapacity);
		} else {
			buffer = ByteBuffer.allocateDirect(buffer.capacity() * 2);
		}
		return buffer;
	}

	ByteBuffer handleSslDataBufferUnderflow(ByteBuffer buffer,SSLByteBuffer ref) {
		ByteBuffer replaceBuffer = enlargeSslDataBuffer(buffer, ref);
		buffer.flip();
		replaceBuffer.put(buffer);
		sslbufferPool.put(ref);
		return replaceBuffer;
	}
	
	void closeSslConnection(SelectionKey key) throws IOException  {
		SocketChannel socketChannel = (SocketChannel) key.channel();
        engine.closeOutbound();
        doSslHandshake(socketChannel, engine);
//        engine = null;
        finish(key);
    }
	
	void handleSslEndOfStream(SelectionKey key) throws IOException  {
        try {
            engine.closeInbound();
        } catch (Exception e) {
            logger.error("Error handleSslEndOfStream :: ",e);
        }
        closeSslConnection(key);
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
	final List<ByteData> in = new DoublyLinkedList<ByteData>();
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