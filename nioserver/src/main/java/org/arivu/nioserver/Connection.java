/**
 * 
 */
package org.arivu.nioserver;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Date;
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
	
	Connection assign(){
		startTime = System.currentTimeMillis();
		return this;
	}
	
	void reset() {
		state.reset();
		req = null;
		route = null;
		startTime = 0;
	}

	void write(SelectionKey key) throws IOException {
		logger.debug(" write  :: {} " , state.resBuff);
		if (state.resBuff != null) {
			try {
				if (state.poll == null) {
					state.poll = state.resBuff.queue.poll();
					if (state.poll != null) {
						state.rem = (int) state.poll.length();
						logger.debug("{} 1 write next ByteBuff size :: {} queueSize :: {}",state.resBuff,state.rem,state.resBuff.queue.size());
					} else {
						logger.debug("{} 2 write next ByteBuff is null! finish!", state.resBuff);
						finish(key);
						return;
					}
				}

				final int length = Math.min(Configuration.defaultChunkSize, state.rem - state.pos);
				final SocketChannel socketChannel = (SocketChannel) key.channel();
				final ByteBuffer wrap = ByteBuffer.wrap(state.poll.copyOfRange(state.pos, state.pos+length));
//				int rb = wrap.remaining();
//				int writeLen = 0;
				while( wrap.remaining()>0 ){
//					writeLen += 
							socketChannel.write(wrap);
				}
//				int rf = wrap.remaining();
//				logger.debug("{}  3 write bytes from  :: {}  length :: {}/{}({},{}) to :: {} size :: {}",state.resBuff,state.pos,length,writeLen,rb,rf,(state.pos + length),state.rem);
				logger.debug("{}  3 write bytes from  :: {}  length :: {} to :: {} size :: {}",state.resBuff,state.pos,length,(state.pos + length),state.rem);
				state.pos += length;
				finishByteBuff(key);
			} catch (Throwable e) {
				logger.error("Failed in write req "+req+" :: ", e);
				finish(key);
			}
		}
	}

	void finishByteBuff(SelectionKey key) throws IOException {
		boolean empty = state.resBuff.queue.isEmpty();
		logger.debug("{} 4 finishByteBuff! empty :: {} queueSize :: {} read :: {} size :: {}", state.resBuff, empty, state.resBuff.queue.size(), state.pos, state.rem );
		if (state.rem == state.pos) {
			state.clearBytes();
			if (empty) {
				finish(key);
				return;
			}
		}
		key.interestOps(SelectionKey.OP_WRITE);
	}

	void finish(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		SocketAddress remoteSocketAddress = channel.socket().getRemoteSocketAddress();
//		channel.finishConnect();
		channel.close();
		key.cancel();
		if (state.resBuff != null)
			RequestUtil.accessLog(state.resBuff.rc, state.resBuff.uri, startTime, System.currentTimeMillis(), state.resBuff.cl,
					remoteSocketAddress, state.resBuff.method);
		state.writeLen = 0;
		req = null;
		route = null;
		pool.put(this);
	}

	void processMultipartInBytes(final byte[] content) {
		do {
			int searchPattern = RequestUtil.searchPattern(content, req.boundary, state.start, state.mi);
			logger.debug(" searchPattern :: {} start :: {} mi {}", searchPattern, state.start, state.mi);
			if (searchPattern == RequestUtil.BYTE_SEARCH_DEFLT) {
				// System.out.println(" searchPattern :: "+searchPattern+" start
				// :: "+start+" mi "+mi);
				if (state.rollOver != null)
					req.body.add(state.rollOver);
				req.body.add(ByteData.wrap(Arrays.copyOfRange(content, state.start, content.length)));
				state.setValue(0, 0, null);
				break;
			} else if (searchPattern < 0) {
//				logger.debug(" searchPattern :: "+searchPattern+" content("+content.length+") :: "+new String(content)+" boundary("+req.boundary.length+") :: "+new String(req.boundary));
				// System.err.println(" searchPattern :: "+searchPattern+" start
				// :: "+start+" mi "+mi);
				if (state.mi > 0 && state.rollOver != null){
					req.body.add(state.rollOver);
				}
				state.setValue(0, searchPattern * -1 - 1, ByteData.wrap(Arrays.copyOfRange(content,state.start, content.length)));
				break;
			} else if (searchPattern > 0) {
				// System.err.println(" searchPattern :: "+searchPattern+" start
				// :: "+start+" mi "+mi);
				if (state.rollOver != null)
					req.body.add(state.rollOver);
				req.body.add(ByteData.wrap(Arrays.copyOfRange(content, state.start, searchPattern - 2)));
				addMultiPart();
				req.body.clear();
				state.setValue(searchPattern + req.boundary.length + 1, 0, null);
			} else if (searchPattern == 0) {
				if (state.mi > 0) {
					if (state.rollOver != null){
						byte[] prevContent = state.rollOver.array();
						req.body.add(ByteData.wrap(Arrays.copyOfRange(prevContent, 0, prevContent.length - state.mi)));
					}
					addMultiPart();
					req.body.clear();
					state.setValue(req.boundary.length + 1 - state.mi, 0, null);
				} else {
					addMultiPart();
					req.body.clear();
					state.setValue(req.boundary.length + 1 , 0, null);
				}
			}
		} while (true);
	}

	void addMultiPart() {
		MultiPart parseAsMultiPart = RequestUtil.parseAsMultiPart(req.body);
		req.multiParts.put(parseAsMultiPart.name, parseAsMultiPart);
	}

	void read(final SelectionKey key) throws IOException {
		int bytesRead = 0;
		byte EOL0 = 1;
		try {
			byte[] readBuf = new byte[Configuration.defaultRequestBuffer];
			ByteBuffer wrap = ByteBuffer.wrap(readBuf);
			if ((bytesRead = ((SocketChannel) key.channel()).read(wrap)) > 0) {
				EOL0 = wrap.get(wrap.position() - 1);
				if (req == null) {
					int headerIndex = RequestUtil.getHeaderIndex(readBuf, RequestUtil.BYTE_13, RequestUtil.BYTE_10, 2);
					if (headerIndex == -1) {
						if (bytesRead == readBuf.length) {
							state.in.add(ByteData.wrap(readBuf));
						} else {
							state.in.add(ByteData.wrap(Arrays.copyOfRange(readBuf, 0, bytesRead)));
						}
					} else {
						state.in.add(ByteData.wrap(Arrays.copyOfRange(readBuf, 0, headerIndex - 1)));
						req = RequestUtil.parseRequest(state.in);
						route = RequestUtil.getMatchingRoute(Configuration.routes, req.getUri(), req.getHttpMethod(),
								false);
						state.in.clear();
						logger.debug(" Got Request :: {}" , req);
//						 System.out.println(" Got Request :: "+req+" route "+route);
						if (route == Configuration.defaultRoute) {
							processRequest(key);
							return;
						}
						state.contentLen = -1l;
						List<Object> list = req.getHeaders().get("Content-Length");
						if (!NullCheck.isNullOrEmpty(list)) {
							String conLenStrHdr = list.get(0).toString();
							if (!NullCheck.isNullOrEmpty(conLenStrHdr)) {
								state.contentLen = Long.parseLong(conLenStrHdr);
							} 
						}
						// System.out.println(" Got Request :: "+req+"\n total
						// "+(total+headerIndex)+"");
						if (headerIndex + 1 < bytesRead) {
							if (req.isMultipart) {
								state.start = req.boundary.length + 1;
								state.onceFlag = true;
								processMultipartInBytes(Arrays.copyOfRange(readBuf, headerIndex + 1, bytesRead));
							} else {
								req.body.add(ByteData.wrap(Arrays.copyOfRange(readBuf, headerIndex + 1, bytesRead)));
							}
							state.contentLen -= (bytesRead-headerIndex - 1);
						}
					}
				} else {
					if (req.isMultipart) {
						if (!state.onceFlag) {
							state.onceFlag = true;
							state.start = req.boundary.length + 1;
						}
						if (bytesRead == readBuf.length) {
							processMultipartInBytes(readBuf);
						} else {
							byte[] arr = Arrays.copyOfRange(readBuf, 0, bytesRead);
							processMultipartInBytes(arr);
						}
						state.contentLen -= bytesRead;
					} else {
						if (bytesRead == readBuf.length) {
							req.body.add(ByteData.wrap(readBuf));
						} else {
							req.body.add(ByteData.wrap(Arrays.copyOfRange(readBuf, 0, bytesRead)));
						}
						state.contentLen -= bytesRead;
					}
				}
			}
			if (req != null && req.isMultipart) {
				int size = req.body.size();
				if (size == 0) 
					key.interestOps(SelectionKey.OP_READ);
				else  if (bytesRead == -1 || EOL0 == RequestUtil.BYTE_10 && isEndOfLine(bytesRead, readBuf)) 
					processRequest(key);
				else
					key.interestOps(SelectionKey.OP_READ);
			} else {
//				System.out.println(" bytesRead "+bytesRead+" EOL0 "+EOL0+" req "+rh.contentLen);
				if (bytesRead == -1 || state.contentLen == 0l || EOL0 == RequestUtil.BYTE_10)
					processRequest(key);
				else
					key.interestOps(SelectionKey.OP_READ);
			}
		} catch (Throwable e) {
			logger.error("Failed in read :: ", e);
			finish(key);
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

	public void processRequest(final SelectionKey key) {
		logger.debug("process connection from {}" , ((SocketChannel) key.channel()).socket().getRemoteSocketAddress());
		AsynContext ctx = null;
		try {
			if (route != null) {
				final Response response = route.getResponse(req);
				if (response != null) {
					ctx = new AsynContextImpl(key, req, response, state);
					StaticRef.set(req, response, route, ctx, key);
					route.handle(req, response);
				}
			}
		} catch (Throwable e) {
			String formatDate = RequestUtil.dateFormat.format(new Date());
			logger.error("Failed in route.handle(" + formatDate + ") :: " + RequestUtil.convert(state.in));
			logger.error("Failed in route.handle(" + formatDate + ") :: ", e);
		} finally {
			StaticRef.clear();
			if(ctx==null)
				key.interestOps(SelectionKey.OP_WRITE);
			else if( !ctx.isAsynchronousFinish() ){
				ctx.setAsynchronousFinish(true);
				ctx.finish();
			}
		}
	}

	void handleErrorReq(Throwable e, SelectionKey key) {
		String formatDate = RequestUtil.dateFormat.format(new Date());
		errorAccessLog(formatDate);
		logger.error("Failed in request parse(" + formatDate + ") :: " + RequestUtil.convert(state.in));
		logger.error("Failed in request parse(" + formatDate + ") :: ", e);
		try {
			((SocketChannel) key.channel()).close();
		} catch (IOException e1) {
			logger.error("Failed in closing channel :: ", e1);
		}
		if (key != null)
			key.cancel();
	}

	private void errorAccessLog(String formatDate) {
		StringBuffer access = new StringBuffer();
		access.append("[").append(formatDate).append("] ").append(RequestUtil.convert(state.in)).append(" ").append("500");
		Server.accessLog.append(access.toString());
	}

}
final class ConnectionState{
//	private static final Logger logger = LoggerFactory.getLogger(ConnectionState.class);
	// Write state
	int writeLen = 0;
	ByteData poll = null;
	int pos = 0;
	int rem = 0;
	Ref resBuff = null;
	
	// Read state
	final List<ByteData> in = new DoublyLinkedList<ByteData>();
	boolean onceFlag = false;
	long contentLen = 0l;
	int start = 0;
	int mi = 0;
	ByteData rollOver = null;
	
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
		contentLen = 0;
	}

	void clearBytes() {
//		if(poll!=null){
//			try {
//				poll.close();
//			} catch (IOException e) {
//				logger.error("Error closing RandomAccessFile :: ", e);
//			}
//		}
		poll = null;
		pos = 0;
		rem = 0;	
	}
	
	void setValue(int s,int m, ByteData bb){
		start = s;
		mi = m;
		rollOver = bb;
	}
}
//final class ReadHelper{
//	final List<ByteData> in = new DoublyLinkedList<>();
//	boolean onceFlag = false;
//	int contentLen = 0;
//	int start = 0;
//	int mi = 0;
//	ByteData rollOver = null;
//	
//	void reset(){
//		in.clear();
//		onceFlag = false;
//		start = 0;
//		mi = 0;
//		rollOver = null;
//		contentLen = 0;
//	}
//	
//	void setValue(int s,int m, ByteData bb){
//		start = s;
//		mi = m;
//		rollOver = bb;
//	}
//}