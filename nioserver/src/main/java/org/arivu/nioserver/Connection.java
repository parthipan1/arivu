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

	Ref resBuff = null;
	int writeLen = 0;
	Pool<Connection> pool;

	public Connection(Pool<Connection> pool) {
		super();
		reset();
		this.pool = pool;
	}

	void reset() {
		writeLen = 0;
		in.clear();
		req = null;

		part = new DoublyLinkedList<>();
		start = 0;
		mi = 0;
		rollOver = null;
		onceFlag = false;
		// total = 0;
	}

	ByteBuffer poll = null;
	int pos = 0;
	int rem = 0;
	final byte[] dst = new byte[Configuration.defaultChunkSize];

	void write(SelectionKey key) throws IOException {
		logger.debug(" write  :: " + resBuff);
		if (resBuff != null) {
			try {
				if (poll == null) {
					poll = resBuff.queue.poll();
					if (poll != null) {
						rem = poll.remaining();
						logger.debug(resBuff + " 1 write next ByteBuff size :: " + rem + " queueSize :: "
								+ resBuff.queue.size());
					} else {
						logger.debug(resBuff + " 2 write next ByteBuff is null! finish! ");
						finish(key);
						return;
					}
				}

				int length = Math.min(Configuration.defaultChunkSize, rem - pos);
				logger.debug(resBuff + "  3 write bytes from  :: " + pos + "  length :: " + (length) + " to :: "
						+ (pos + length) + " size :: " + rem);
				byte[] dstt = dst;// new byte[length];
				if (length == Configuration.defaultChunkSize) {
					for (int i = 0; i < dstt.length; i++)
						dstt[i] = poll.get(i + pos);
				} else {
					dstt = new byte[length];
					for (int i = 0; i < dstt.length; i++)
						dstt[i] = poll.get(i + pos);
				}
				SocketChannel socketChannel = (SocketChannel) key.channel();
				socketChannel.write(ByteBuffer.wrap(dstt));
				pos += length;
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
				+ " read :: " + pos + " size :: " + rem);
		if (rem == pos) {
			poll = null;
			pos = 0;
			rem = 0;
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
		writeLen = 0;
		pool.put(this);
	}

	List<ByteBuffer> part = new DoublyLinkedList<>();
	int start = 0;
	int mi = 0;
	ByteBuffer rollOver = null;

	void processMultipartInBytes(final byte[] content) {
		do {
			int searchPattern = RequestUtil.searchPattern(content, req.boundary, start, mi);
			logger.debug(" searchPattern :: " + searchPattern + " start :: " + start + " mi " + mi);
			if (searchPattern == RequestUtil.BYTE_SEARCH_DEFLT) {
				// System.out.println(" searchPattern :: "+searchPattern+" start
				// :: "+start+" mi "+mi);
				if (rollOver != null)
					part.add(rollOver);
				part.add(ByteBuffer.wrap(Arrays.copyOfRange(content, start, content.length)));
				start = 0;
				mi = 0;
				rollOver = null;
				break;
			} else if (searchPattern < 0) {
				// System.err.println(" searchPattern :: "+searchPattern+" start
				// :: "+start+" mi "+mi);
				mi = searchPattern * -1 - 1;
				rollOver = ByteBuffer.wrap(Arrays.copyOfRange(content, start, content.length));
				start = 0;
				break;
			} else if (searchPattern > 0) {
				// System.err.println(" searchPattern :: "+searchPattern+" start
				// :: "+start+" mi "+mi);
				if (rollOver != null)
					part.add(rollOver);
				part.add(ByteBuffer.wrap(Arrays.copyOfRange(content, start, searchPattern - 2)));
				addMultiPart();
				part = new DoublyLinkedList<>();
				start = searchPattern + req.boundary.length + 1;
				rollOver = null;
				mi = 0;
			} else if (searchPattern == 0) {
				if (mi > 0) {
					byte[] prevContent = rollOver.array();
					part.add(ByteBuffer.wrap(Arrays.copyOfRange(prevContent, 0, prevContent.length - mi)));
					addMultiPart();
					part = new DoublyLinkedList<>();
					rollOver = null;
					start = req.boundary.length + 1 - mi;
					mi = 0;
				} else {
					addMultiPart();
					part = new DoublyLinkedList<>();
					rollOver = null;
					start = req.boundary.length + 1;
					mi = 0;
				}
			}
		} while (true);
	}

	void addMultiPart() {
		MultiPart parseAsMultiPart = RequestUtil.parseAsMultiPart(part);
		req.multiParts.put(parseAsMultiPart.name, parseAsMultiPart);
	}

	final List<ByteBuffer> in = new DoublyLinkedList<>();
	RequestImpl req = null;
	Route route = null;
	// int total = 0;
	boolean onceFlag = false;

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
							in.add(wrap);
						} else {
							in.add(ByteBuffer.wrap(Arrays.copyOfRange(readBuf, 0, bytesRead)));
						}
					} else {
						in.add(ByteBuffer.wrap(Arrays.copyOfRange(readBuf, 0, headerIndex - 1)));
						req = RequestUtil.parseRequest(in);
						route = RequestUtil.getMatchingRoute(Configuration.routes, req.getUri(), req.getHttpMethod(),
								false);
						in.clear();
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
								start = req.boundary.length + 1;
								onceFlag = true;
								processMultipartInBytes(Arrays.copyOfRange(readBuf, headerIndex + 1, bytesRead));
							} else {
								req.body.add(ByteBuffer.wrap(Arrays.copyOfRange(readBuf, headerIndex + 1, bytesRead)));
							}
						}
					}
				} else {
					if (req.isMultipart) {
						if (!onceFlag) {
							onceFlag = true;
							start = req.boundary.length + 1;
						}
						if (bytesRead == readBuf.length) {
							processMultipartInBytes(readBuf);
						} else {
							byte[] arr = Arrays.copyOfRange(readBuf, 0, bytesRead);
							processMultipartInBytes(arr);
						}
					} else {
						if (bytesRead == readBuf.length) {
							req.body.add(wrap);
						} else {
							req.body.add(ByteBuffer.wrap(Arrays.copyOfRange(readBuf, 0, bytesRead)));
						}
					}
				}
			}
			// total += bytesRead;
			// logger.debug(" read :: "+bytesRead+" req.isMultipart
			// "+req.isMultipart+" total "+total);
			if (req != null && req.isMultipart) {
				int size = part.size();
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
			// e.printStackTrace();
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
		// Request request = req;
		Response response = null;
		logger.debug("process connection from " + ((SocketChannel) key.channel()).socket().getRemoteSocketAddress());
		// try {
		// request = req;//RequestUtil.parseRequest(inBuffer);
		// System.out.println(" request :: " + request.toString());
		// route = RequestUtil.getMatchingRoute(Configuration.routes,
		// req.getUri(), req.getHttpMethod(),
		// false);
		if (route != null) {
			response = route.getResponse(req);
		}
		// } catch (Throwable e) {
		// handleErrorReq(e, key);
		// return;
		// }
		try {
			if (response != null) {
				route.handle(req, response);
				resBuff = RequestUtil.getResponseBytes(req, response);
				if (resBuff != null && resBuff.cl > Configuration.defaultChunkSize) {
					((SocketChannel) key.channel()).socket().setSoTimeout(0);
				}
				logger.debug(" request :: " + req.toString() + " response :: " + resBuff.cl);
			}
			// request = null;
			req = null;
			route = null;
			response = null;
		} catch (Throwable e) {
			String formatDate = RequestUtil.dateFormat.format(new Date());
			logger.error("Failed in route.handle(" + formatDate + ") :: " + RequestUtil.convert(in));
			logger.error("Failed in route.handle(" + formatDate + ") :: ", e);
		} finally {
			key.interestOps(SelectionKey.OP_WRITE);
		}
	}

	void handleErrorReq(Throwable e, SelectionKey key) {
		String formatDate = RequestUtil.dateFormat.format(new Date());
		errorAccessLog(formatDate);
		logger.error("Failed in request parse(" + formatDate + ") :: " + RequestUtil.convert(in));
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
		access.append("[").append(formatDate).append("] ").append(RequestUtil.convert(in)).append(" ").append("500");
		Server.accessLog.append(access.toString());
	}

}
