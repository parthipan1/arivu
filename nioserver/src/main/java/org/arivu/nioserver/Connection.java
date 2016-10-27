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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mr P
 *
 */
final class Connection {
	private static final Logger logger = LoggerFactory.getLogger(Connection.class);

	final long startTime = System.currentTimeMillis();
	
	Pool<Connection> pool;

	public Connection(Pool<Connection> pool) {
		super();
		reset();
		this.pool = pool;
	}

	final WriteHelper wh = new WriteHelper();

	final ReadHelper rh = new ReadHelper();

	RequestImpl req = null;
	Route route = null;
	Ref resBuff = null;
	
	void reset() {
		wh.reset();
		rh.reset();
		req = null;
		route = null;
		resBuff = null;
	}

	void write(SelectionKey key) throws IOException {
		logger.debug(" write  :: " + resBuff);
		if (resBuff != null) {
			try {
				if (wh.poll == null) {
					wh.poll = resBuff.queue.poll();
					if (wh.poll != null) {
						wh.rem = wh.poll.array().length;
						logger.debug(resBuff + " 1 write next ByteBuff size :: " + wh.rem + " queueSize :: "
								+ resBuff.queue.size());
					} else {
						logger.debug(resBuff + " 2 write next ByteBuff is null! finish! ");
						finish(key);
						return;
					}
				}

				int length = Math.min(Configuration.defaultChunkSize, wh.rem - wh.pos);
				logger.debug(resBuff + "  3 write bytes from  :: " + wh.pos + "  length :: " + (length) + " to :: "
						+ (wh.pos + length) + " size :: " + wh.rem);
				byte[] dstt = Arrays.copyOfRange(wh.poll.array(), wh.pos, wh.pos+length);
				SocketChannel socketChannel = (SocketChannel) key.channel();
				socketChannel.write(ByteBuffer.wrap(dstt));
				wh.pos += length;
				finishByteBuff(key, socketChannel);
			} catch (Throwable e) {
				logger.error("Failed in write :: ", e);
				finish(key);
				throw e;
			}
		}
	}

	void finishByteBuff(SelectionKey key, SocketChannel socketChannel) throws IOException {
		boolean empty = resBuff.queue.isEmpty();
		logger.debug(resBuff + " 4 finishByteBuff! empty :: " + empty + " queueSize :: " + resBuff.queue.size()
				+ " read :: " + wh.pos + " size :: " + wh.rem);
		if (wh.rem == wh.pos) {
			wh.clearBytes();
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
		channel.finishConnect();
		channel.close();
		key.cancel();
		if (resBuff != null)
			RequestUtil.accessLog(resBuff.rc, resBuff.uri, startTime, System.currentTimeMillis(), resBuff.cl,
					remoteSocketAddress);
		wh.writeLen = 0;
		pool.put(this);
	}

	void processMultipartInBytes(final byte[] content) {
		do {
			int searchPattern = RequestUtil.searchPattern(content, req.boundary, rh.start, rh.mi);
			logger.debug(" searchPattern :: " + searchPattern + " start :: " + rh.start + " mi " + rh.mi);
			if (searchPattern == RequestUtil.BYTE_SEARCH_DEFLT) {
				// System.out.println(" searchPattern :: "+searchPattern+" start
				// :: "+start+" mi "+mi);
				if (rh.rollOver != null)
					req.body.add(rh.rollOver);
				req.body.add(ByteData.wrap(Arrays.copyOfRange(content, rh.start, content.length)));
				rh.setValue(0, 0, null);
				break;
			} else if (searchPattern < 0) {
				// System.err.println(" searchPattern :: "+searchPattern+" start
				// :: "+start+" mi "+mi);
				rh.setValue(0, searchPattern * -1 - 1, ByteData.wrap(Arrays.copyOfRange(content,rh.start, content.length)));
				break;
			} else if (searchPattern > 0) {
				// System.err.println(" searchPattern :: "+searchPattern+" start
				// :: "+start+" mi "+mi);
				if (rh.rollOver != null)
					req.body.add(rh.rollOver);
				req.body.add(ByteData.wrap(Arrays.copyOfRange(content, rh.start, searchPattern - 2)));
				addMultiPart();
				req.body.clear();
				rh.setValue(searchPattern + req.boundary.length + 1, 0, null);
			} else if (searchPattern == 0) {
				if (rh.mi > 0) {
					byte[] prevContent = rh.rollOver.array();
					req.body.add(ByteData.wrap(Arrays.copyOfRange(prevContent, 0, prevContent.length - rh.mi)));
					addMultiPart();
					req.body.clear();
					rh.setValue(req.boundary.length + 1 - rh.mi, 0, null);
				} else {
					addMultiPart();
					req.body.clear();
					rh.setValue(req.boundary.length + 1 , 0, null);
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
							rh.in.add(ByteData.wrap(readBuf));
						} else {
							rh.in.add(ByteData.wrap(Arrays.copyOfRange(readBuf, 0, bytesRead)));
						}
					} else {
						rh.in.add(ByteData.wrap(Arrays.copyOfRange(readBuf, 0, headerIndex - 1)));
						req = RequestUtil.parseRequest(rh.in);
						route = RequestUtil.getMatchingRoute(Configuration.routes, req.getUri(), req.getHttpMethod(),
								false);
						rh.in.clear();
						logger.debug(" Got Request :: " + req);
						// System.out.println(" Got Request :: "+req+" route
						// "+route);
						if (route == Configuration.defaultRoute) {
							processRequest(key);
							return;
						}
						// System.out.println(" Got Request :: "+req+"\n total
						// "+(total+headerIndex)+"");
						if (headerIndex + 1 < bytesRead) {
							if (req.isMultipart) {
								rh.start = req.boundary.length + 1;
								rh.onceFlag = true;
								processMultipartInBytes(Arrays.copyOfRange(readBuf, headerIndex + 1, bytesRead));
							} else {
								req.body.add(ByteData.wrap(Arrays.copyOfRange(readBuf, headerIndex + 1, bytesRead)));
							}
						}
					}
				} else {
					if (req.isMultipart) {
						if (!rh.onceFlag) {
							rh.onceFlag = true;
							rh.start = req.boundary.length + 1;
						}
						if (bytesRead == readBuf.length) {
							processMultipartInBytes(readBuf);
						} else {
							byte[] arr = Arrays.copyOfRange(readBuf, 0, bytesRead);
							processMultipartInBytes(arr);
						}
					} else {
						if (bytesRead == readBuf.length) {
							req.body.add(ByteData.wrap(readBuf));
						} else {
							req.body.add(ByteData.wrap(Arrays.copyOfRange(readBuf, 0, bytesRead)));
						}
					}
				}
			}
			// total += bytesRead;
			// logger.debug(" read :: "+bytesRead+" req.isMultipart
			// "+req.isMultipart+" total "+total);
			if (req != null && req.isMultipart) {
				int size = req.body.size();
				// System.out.println(" req.isMultipart read :: "+bytesRead+"
				// size "+size);
				if (size == 0) {
					key.interestOps(SelectionKey.OP_READ);
					return;
				}
				String endOfLine = getEndOfLine(bytesRead, wrap);
				// System.out.println(" req.isMultipart read :: "+bytesRead+"
				// endOfLine "+endOfLine);
				String string = new String(req.boundary);
				if (bytesRead == -1 || (EOL0 == RequestUtil.BYTE_10 && endOfLine.startsWith(string + "--"))) {
					// System.out.println(" proces request before
					// parseAndSetMultiPart cl "+req.);
					// RequestUtil.parseAndSetMultiPart(req);
					// System.out.println(" proces request after
					// parseAndSetMultiPart ");
					processRequest(key);
				} else
					key.interestOps(SelectionKey.OP_READ);
			} else {
				if ((bytesRead == -1 || EOL0 == RequestUtil.BYTE_10))
					processRequest(key);
				else
					key.interestOps(SelectionKey.OP_READ);
			}
		} catch (Throwable e) {
			logger.error("Failed in read :: ", e);
			finish(key);
			throw e;
		}
	}

	String getEndOfLine(int bytesRead, ByteBuffer bb) {
		byte[] array = bb.array();
		StringBuffer endOfLineBuf = new StringBuffer();
		int start = bytesRead - req.boundary.length - 4;
		if (start >= 0) {
			for (int i = start; i < array.length - 1; i++)
				endOfLineBuf.append((char) array[i]);
		}

		String endOfLine = endOfLineBuf.toString();
		return endOfLine;
	}

	public void processRequest(final SelectionKey key) {
		logger.debug("process connection from " + ((SocketChannel) key.channel()).socket().getRemoteSocketAddress());
		try {
			if (route != null) {
				Response response = route.getResponse(req);
				if (response != null) {
					route.handle(req, response);
					resBuff = RequestUtil.getResponseBytes(req, response);
					if (resBuff != null && resBuff.cl > Configuration.defaultChunkSize) {
						((SocketChannel) key.channel()).socket().setSoTimeout(0);
					}
					logger.debug(" request :: " + req.toString() + " response :: " + resBuff.cl);
				}
				req = null;
				route = null;
				response = null;
			}
		} catch (Throwable e) {
			String formatDate = RequestUtil.dateFormat.format(new Date());
			logger.error("Failed in route.handle(" + formatDate + ") :: " + RequestUtil.convert(rh.in));
			logger.error("Failed in route.handle(" + formatDate + ") :: ", e);
		} finally {
			key.interestOps(SelectionKey.OP_WRITE);
		}
	}

	void handleErrorReq(Throwable e, SelectionKey key) {
		String formatDate = RequestUtil.dateFormat.format(new Date());
		errorAccessLog(formatDate);
		logger.error("Failed in request parse(" + formatDate + ") :: " + RequestUtil.convert(rh.in));
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
		access.append("[").append(formatDate).append("] ").append(RequestUtil.convert(rh.in)).append(" ").append("500");
		Server.accessLog.append(access.toString());
	}

}
final class WriteHelper{
	int writeLen = 0;
	ByteData poll = null;
	int pos = 0;
	int rem = 0;
//	final byte[] dst = new byte[Configuration.defaultChunkSize];
	
	void reset() {
		writeLen = 0;
		poll = null;
		pos = 0;
		rem = 0;
	}

	void clearBytes() {
		poll = null;
		pos = 0;
		rem = 0;	
	}
}
final class ReadHelper{
	final List<ByteData> in = new DoublyLinkedList<>();
	boolean onceFlag = false;
	int start = 0;
	int mi = 0;
	ByteData rollOver = null;
	
	void reset(){
		in.clear();
		onceFlag = false;
		start = 0;
		mi = 0;
		rollOver = null;
	}
	
	void setValue(int s,int m, ByteData bb){
		start = s;
		mi = m;
		rollOver = bb;
	}
}