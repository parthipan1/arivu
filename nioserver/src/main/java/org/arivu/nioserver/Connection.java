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

	public Connection(Pool<Connection> pool) {
		super();
		reset();
		this.pool = pool;
	}

	final ConnectionState state = new ConnectionState();
	RequestImpl req = null;
	Route route = null;

	Connection assign() {
		startTime = System.currentTimeMillis();
		return this;
	}

	void reset() {
		state.reset();
		req = null;
		route = null;
		startTime = 0;
	}

	void write(final SelectionKey key, final Selector clientSelector) throws IOException {
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
				innerWrite(key);
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
//	void finishByteBuff(SelectionKey key, Selector clientSelector) throws IOException {
//		boolean empty = state.resBuff.queue.isEmpty();
//		logger.debug("{} 4 finishByteBuff! empty :: {} queueSize :: {} read :: {} size :: {}", state.resBuff, empty,
//				state.resBuff.queue.size(), state.pos, state.rem);
//		if (state.rem == state.pos) {
//			state.clearBytes();
//			if (empty) {
//				finish(key);
//				return;
//			}
//		}
//		key.interestOps(SelectionKey.OP_WRITE);
//		clientSelector.wakeup();
//	}

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
//			logger.debug(" m searchPattern :: {} start :: {} mi {} content.length {} req.boundary.length {} ",
//					searchPattern, state.start, state.mi, content.length, req.boundary.length);
			if (searchPattern == RequestUtil.BYTE_SEARCH_DEFLT) {
//				logger.debug(" d searchPattern :: {} start :: {} mi {}", searchPattern, state.start, state.mi);
				logger.debug(" d searchPattern :: {} start :: {} mi {} content.length {} req.boundary.length {} ",
						searchPattern, state.start, state.mi, content.length, req.boundary.length);
				if (state.rollOver != null) {
					req.body.add(state.rollOver);
				}
				req.body.add(ByteData.wrap(Arrays.copyOfRange(content, state.start, content.length)));
				state.setValue(0, 0, null);
				break;
			} else if (searchPattern < 0) {
//				logger.debug(" l searchPattern :: {} start :: {} mi {}", searchPattern, state.start, state.mi);
				logger.debug(" l searchPattern :: {} start :: {} mi {} content.length {} req.boundary.length {} ",
						searchPattern, state.start, state.mi, content.length, req.boundary.length);
				if (state.mi > 0 && state.rollOver != null) {
					req.body.add(state.rollOver);
				}
				state.setValue(0, searchPattern * -1,
						ByteData.wrap(Arrays.copyOfRange(content, state.start, content.length)));
				break;
			} else if (searchPattern >= 0) {
//				logger.debug(" e searchPattern :: {} start :: {} mi {}", searchPattern, state.start, state.mi);
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

	void read(final SelectionKey key, final Selector clientSelector) throws IOException {
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