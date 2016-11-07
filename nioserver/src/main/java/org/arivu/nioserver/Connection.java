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

	void write(SelectionKey key, Selector clientSelector) throws IOException {
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
				while( wrap.remaining()>0 ){
					socketChannel.write(wrap);
				}
				logger.debug("{}  3 write bytes from  :: {}  length :: {} to :: {} size :: {}",state.resBuff,state.pos,length,(state.pos + length),state.rem);
				state.pos += length;
				finishByteBuff(key, clientSelector);
			} catch (Throwable e) {
				logger.error("Failed in write req "+req+" :: ", e);
				finish(key);
			}
		}
	}

	void finishByteBuff(SelectionKey key, Selector clientSelector) throws IOException {
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
		clientSelector.wakeup();
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
			logger.debug(" searchPattern :: {} start :: {} mi {} content.length {}", searchPattern, state.start, state.mi, content.length);
			if (searchPattern == RequestUtil.BYTE_SEARCH_DEFLT) {
				logger.debug(" searchPattern :: {} start :: {} mi {}", searchPattern, state.start, state.mi );
//				System.out.println(" searchPattern :: "+searchPattern+" start :: "+state.start+" mi "+state.mi);
				if (state.rollOver != null){
					req.body.add(state.rollOver);
				}
				req.body.add(ByteData.wrap(Arrays.copyOfRange(content, state.start, content.length)));
				state.setValue(0, 0, null);
				break;
			} else if (searchPattern < 0) {
//				logger.debug(" searchPattern :: "+searchPattern+" content("+content.length+") :: "+new String(content)+" boundary("+req.boundary.length+") :: "+new String(req.boundary));
//				System.out.println(" searchPattern :: "+searchPattern+" start :: "+state.start+" mi "+state.mi+"  ");//+new String(content)
				logger.debug(" searchPattern :: {} start :: {} mi {}", searchPattern, state.start, state.mi );
				if (state.mi > 0 && state.rollOver != null){
					req.body.add(state.rollOver);
				}
				state.setValue(0, searchPattern * -1 , ByteData.wrap(Arrays.copyOfRange(content,state.start, content.length)));
				break;
			} else if (searchPattern > 0) {
//				System.err.println(" searchPattern :: "+searchPattern+" start :: "+state.start+" mi "+state.mi);
				logger.debug(" searchPattern :: {} start :: {} mi {}", searchPattern, state.start, state.mi );
				if( searchPattern <= req.boundary.length ){
					if (state.rollOver != null){
						byte[] prevContent = state.rollOver.array();
						req.body.add(ByteData.wrap(Arrays.copyOfRange(prevContent, state.start, prevContent.length - (req.boundary.length+1-searchPattern))));
					}
				}else{
					if (state.rollOver != null)
						req.body.add(state.rollOver);
					
					req.body.add(ByteData.wrap(Arrays.copyOfRange(content, state.start, searchPattern - req.boundary.length - 1)));
					
				}
				addMultiPart();
				req.body.clear();
				state.setValue(searchPattern + 1, 0, null);
			} else if (searchPattern == 0) {
//				System.err.println(" searchPattern :: "+searchPattern+" start :: "+state.start+" mi "+state.mi);
				logger.debug(" searchPattern :: {} start :: {} mi {}", searchPattern, state.start, state.mi );
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
//		System.out.println("\n%"+RequestUtil.convert(req.body)+"%\n");
		MultiPart parseAsMultiPart = RequestUtil.parseAsMultiPart(req.body);
		req.multiParts.put(parseAsMultiPart.name, parseAsMultiPart);
	}

	void read(final SelectionKey key, final Selector clientSelector) throws IOException {
		int bytesRead = 0;
		byte EOL0 = 1;
		try {
			final byte[] readBuf = new byte[Configuration.defaultRequestBuffer];
			final ByteBuffer wrap = ByteBuffer.wrap(readBuf);
			if ((bytesRead = ((SocketChannel) key.channel()).read(wrap)) > 0) {
				EOL0 = wrap.get(wrap.position() - 1);
//				logger.info("\n ******%"+new String(readBuf)+"%******\n");
//				logger.debug("Message read {}",new String(readBuf));
				if (req == null) {
					final int headerIndex = RequestUtil.getHeaderIndex(readBuf, RequestUtil.BYTE_13, RequestUtil.BYTE_10, 2);
					if (headerIndex == -1) {
//						if (bytesRead == readBuf.length) {
							state.in.add(ByteData.wrap(readBuf));
//						} else {
//							state.in.add(ByteData.wrap(Arrays.copyOfRange(readBuf, 0, bytesRead)));
//						}
					} else {
						state.in.add(ByteData.wrap(Arrays.copyOfRange(readBuf, 0, headerIndex - 1)));
						req = RequestUtil.parseRequest(state.in);
						route = RequestUtil.getMatchingRoute(Configuration.routes, req.getUri(), req.getHttpMethod(),
								false);
						state.in.clear();
						logger.debug(" Got Request :: {}" , req);
						setContentLen();
//						 System.out.println(" Got Request :: "+req+" route "+route);
						if (route == Configuration.defaultRoute) {
							if( req.getHttpMethod() == HttpMethod.GET ||
								req.getHttpMethod() == HttpMethod.HEAD ||
								req.getHttpMethod() == HttpMethod.TRACE
							){
								processRequest(key, clientSelector);
								return;
							}else if( state.contentLen>0) {
								state.is404Res = true;
							}else{
								processRequest(key, clientSelector);
								return;
							}
						}
						if( state.contentLen == 0l){
							processRequest(key, clientSelector);
							return;
						}else if (headerIndex + 1 < bytesRead) {
							if (req.isMultipart) {
								state.start = req.boundary.length + 1;
								state.onceFlag = true;
								processMultipartInBytes(Arrays.copyOfRange(readBuf, headerIndex + 1, bytesRead));
							} else {
								req.body.add(ByteData.wrap(Arrays.copyOfRange(readBuf, headerIndex + 1, bytesRead)));
							}
							state.contentLen -= (bytesRead-headerIndex - 1);
						}
						
						// System.out.println(" Got Request :: "+req+"\n total
						// "+(total+headerIndex)+"");
					}
					nextRead(key, bytesRead, EOL0, clientSelector);
				} else {
					state.contentLen -= bytesRead;
					if(!state.is404Res){
						if (req.isMultipart) {
							if (!state.onceFlag) {
								state.onceFlag = true;
								state.start = req.boundary.length + 1;
							}
							if (bytesRead == readBuf.length) {
								processMultipartInBytes(readBuf);
							} else {
								processMultipartInBytes(Arrays.copyOfRange(readBuf, 0, bytesRead));
							}
							nextMultiPartNext(key, bytesRead, EOL0, readBuf, clientSelector);
						} else {
							if (bytesRead == readBuf.length) {
								req.body.add(ByteData.wrap(readBuf));
							} else {
								req.body.add(ByteData.wrap(Arrays.copyOfRange(readBuf, 0, bytesRead)));
							}
							nextRead(key, bytesRead, EOL0, clientSelector);
						}
					}else{
						nextRead(key, bytesRead, EOL0, clientSelector);
					}
				}
			}
		} catch (Throwable e) {
			logger.error("Failed in read :: ", e);
			finish(key);
		}
	}

	void nextMultiPartNext(final SelectionKey key, int bytesRead, byte EOL0, byte[] readBuf, Selector clientSelector) {
		int size = req.body.size();
		if (size == 0) {
			key.interestOps(SelectionKey.OP_READ);
			clientSelector.wakeup();
		}else  if (bytesRead == -1 || EOL0 == RequestUtil.BYTE_10 && isEndOfLine(bytesRead, readBuf)) 
			processRequest(key, clientSelector);
		else{
			key.interestOps(SelectionKey.OP_READ);
			clientSelector.wakeup();
		}
	}

	void nextRead(final SelectionKey key, int bytesRead, byte EOL0, Selector clientSelector) {
		if (bytesRead == -1 || state.contentLen == 0l || (state.contentLen == -1l && EOL0 == RequestUtil.BYTE_10))
			processRequest(key, clientSelector);
		else{
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
		logger.debug("process connection from {}" , ((SocketChannel) key.channel()).socket().getRemoteSocketAddress());
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
			if(ctx==null){
				key.interestOps(SelectionKey.OP_WRITE);
				clientSelector.wakeup();
			}else if( !ctx.isAsynchronousFinish() ){
				ctx.setAsynchronousFinish(true);
				ctx.finish();
			}
		}
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
	
	void setValue(int s,int m, ByteData bb){
		start = s;
		mi = m;
		rollOver = bb;
	}
}
