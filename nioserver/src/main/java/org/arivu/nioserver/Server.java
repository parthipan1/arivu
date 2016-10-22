/**
 * 
 */
package org.arivu.nioserver;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.arivu.log.Appender;
import org.arivu.log.appender.Appenders;
import org.arivu.utils.Env;
import org.arivu.utils.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author P
 *
 */
public class Server {

	static final String DEFAULT_HOST = Env.getEnv("host", "localhost");

	static final int DEFAULT_PORT = Integer.parseInt(Env.getEnv("port", "8080"));

	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
		if (args != null && args.length > 0 && args[0].equalsIgnoreCase("stop")) {
			RequestUtil.stopRemote();
		} else {
			accessLog = Appenders.file
					.get(Env.getEnv("access.log", ".." + File.separator + "logs" + File.separator + "access.log"));
			handler.sync();
		}
	}

	private static SelectorHandler handler = new SelectorHandler();

	static Appender accessLog = null;

	@Path(value = Configuration.stopUri, httpMethod = HttpMethod.GET)
	static void stop(Request req, Response res) throws Exception {
		res.setResponseCode(200);
		final ScheduledExecutorService exe = Executors.newScheduledThreadPool(1);
		exe.schedule(new Runnable() {

			@Override
			public void run() {
				exe.shutdownNow();
				handler.stop();
			}
		}, 1, TimeUnit.SECONDS);

	}
}

final class SelectorHandler {
	private static final Logger logger = LoggerFactory.getLogger(SelectorHandler.class);
	volatile boolean shutdown = false;
	Selector clientSelector = null;
	String beanNameStr = null;
	final ExecutorService exe = Executors.newCachedThreadPool();
	final ServerMXBean mxBean = new ServerMXBean() {

		@Override
		public void shutdown() {
			stop();
		}

		@Override
		public void removeRoute(String route) {
			if (NullCheck.isNullOrEmpty(route))
				return;

			throw new RuntimeException("Cannot remove route!");
			// Collection<Route> rts = Configuration.routes;
			// for( Route rt:rts ){
			// if( route.equals(rt.uri+" "+rt.httpMethod) ){
			// rts.remove(rt);
			// break;
			// }
			// }
		}

		@Override
		public String[] getAllRoute() {
			Collection<Route> rts = Configuration.routes;

			String[] ret = new String[rts.size()];
			int i = 0;
			for (Route rt : rts) {
				ret[i++] = rt.uri + " " + rt.httpMethod;
			}

			return ret;
		}
	};

	void registerMXBean() {
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			beanNameStr = "org.arivu.niosever:type=" + Server.class.getSimpleName() + "." + Server.DEFAULT_PORT;
			mbs.registerMBean(mxBean, new ObjectName(beanNameStr));
			logger.debug(" Jmx bean beanName " + beanNameStr + " registered!");
		} catch (Exception e) {
			logger.error("Failed with Error::", e);
		}
	}

	void unregisterMXBean() {
		if (beanNameStr != null) {
			try {
				ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName(beanNameStr));
				logger.debug("Unregister Jmx bean " + beanNameStr);
			} catch (Exception e) {
				logger.error("Failed with Error::", e);
			}
		}
	}

	void sync() throws IOException {
		registerMXBean();
		clientSelector = Selector.open();
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), Server.DEFAULT_PORT);
		ssc.socket().bind(sa);
		ssc.register(clientSelector, SelectionKey.OP_ACCEPT);

		while (!shutdown) {
			if (clientSelector.select() == 0)
				continue;
			try {
				Set<SelectionKey> readySet = clientSelector.selectedKeys();
				for (Iterator<SelectionKey> it = readySet.iterator(); it.hasNext();) {
					final SelectionKey key = it.next();
					it.remove();
					if (key.isAcceptable()) {
						SocketChannel clientSocket = ssc.accept();
						clientSocket.configureBlocking(false);
						SelectionKey key1 = clientSocket.register(clientSelector, SelectionKey.OP_READ);
						key1.attach(new Connection());
					} else {
						key.interestOps(0);
						exe.execute(new Runnable() {
							public void run() {
								try {
									Connection client = (Connection) key.attachment();
									if (key.isReadable()) {
										client.read(key);
									} else {
										client.write(key);
									}
									clientSelector.wakeup();
								} catch (IOException e) {
									e.printStackTrace();
									logger.error("Failed with Error::", e);
								}
							}
						});
					}
				}
			} catch (IOException e) {
				logger.error("Failed with Error::", e);
			}
		}
	}

	void stop() {
		shutdown = true;
		try {
			clientSelector.close();
		} catch (IOException e) {
			logger.error("Failed in clientSelector close :: ", e);
		}
		exe.shutdownNow();
		unregisterMXBean();
		if (Server.accessLog != null) {
			try {
				Server.accessLog.close();
			} catch (Exception e) {
				logger.error("Failed to close accesslog::", e);
			}
		}
		logger.info("Server stopped!");
	}
}

final class Connection {
	private static final Logger logger = LoggerFactory.getLogger(Connection.class);

	final long startTime = System.currentTimeMillis();

	StringBuffer inBuffer = null;
	ByteBuffer buff = null;
	Ref resBuff = null;

	public Connection() {
		super();
		inBuffer = new StringBuffer();
		buff = ByteBuffer.allocateDirect(Configuration.defaultRequestBuffer);
	}

	int writeLen = 0;

	void write(SelectionKey key) throws IOException {
		logger.debug(" write  :: "+resBuff);
		if (resBuff != null) {
			if (resBuff.headerBytes != null) {
				((SocketChannel) key.channel()).write(ByteBuffer.wrap(resBuff.headerBytes));
				resBuff.headerBytes = null;
			}
			if (resBuff.bodyBytes != null && resBuff.bodyBytes.length > 0) {
				int subArrLen = Math.min(resBuff.bodyBytes.length, writeLen + Configuration.defaultChunkSize);
				((SocketChannel) key.channel())
						.write(ByteBuffer.wrap(resBuff.bodyBytes, writeLen, subArrLen - writeLen));
				writeLen = subArrLen;
				if (writeLen == resBuff.bodyBytes.length) {
					finish(key);
				} else {
					key.interestOps(SelectionKey.OP_WRITE);
				}
			} else {
				finish(key);
			}
		}
	}

	void finish(SelectionKey key) throws IOException {
		((SocketChannel) key.channel()).close();
		key.cancel();
		RequestUtil.accessLog(resBuff.rc, resBuff.uri, startTime, System.currentTimeMillis(), resBuff.bodyBytes.length);
		buff = null;
		inBuffer = null;
		writeLen = 0;
	}

	void read(final SelectionKey key) throws IOException {
		int bytesRead = 0;
		byte EOL0 = 1;
		if ((bytesRead = ((SocketChannel) key.channel()).read(buff)) > 0) {
			EOL0 = buff.get(buff.position() - 1);
			buff.flip();
			inBuffer.append(Charset.defaultCharset().decode(buff).array());
			buff.clear();
		}

		if ((bytesRead == -1 || EOL0 == RequestUtil.BYTE_10))
			processRequest(key);
		else
			key.interestOps(SelectionKey.OP_READ);
	}

	public void processRequest(final SelectionKey key) {
		Request request = null;
		Route route = null;
		Response response = null;
		logger.debug("process connection from " + ((SocketChannel) key.channel()).socket().getRemoteSocketAddress());
		try {
			request = RequestUtil.parse(inBuffer, startTime);
//			System.out.println(" request :: " + request.toString());
			route = get(Configuration.routes, request);
			if (route != null) {
				response = route.getResponse(request);
			}
		} catch (Throwable e) {
			handleErrorReq(e, key);
			return;
		}
		try {
			if (response != null) {
				route.handle(request, response);
				resBuff = RequestUtil.getResponseBytes(request, response);
				logger.debug(" request :: " + request.toString()+" response :: "+resBuff.bodyBytes.length);
			}
			request = null;
			route = null;
			response = null;
		} catch (Throwable e) {
			String formatDate = RequestUtil.dateFormat.format(new Date());
			logger.error("Failed in route.handle(" + formatDate + ") :: " + inBuffer);
			logger.error("Failed in route.handle(" + formatDate + ") :: ", e);
		} finally {
			key.interestOps(SelectionKey.OP_WRITE);
		}
	}

	void handleErrorReq(Throwable e, SelectionKey key) {
		StringBuffer access = new StringBuffer();
		String formatDate = RequestUtil.dateFormat.format(new Date());
		access.append("[").append(formatDate).append("] ").append(inBuffer.toString().split(" ")[1]).append(" ")
				.append("400");
		Server.accessLog.append(access.toString());
		logger.error("Failed in request parse(" + formatDate + ") :: " + inBuffer);
		logger.error("Failed in request parse(" + formatDate + ") :: ", e);
		try {
			((SocketChannel) key.channel()).close();
		} catch (IOException e1) {
			logger.error("Failed in closing channel :: ", e1);
		}
		if (key != null)
			key.cancel();
	}

	static Route get(Collection<Route> paths, Request req) {
		Route df = null;
		Route in = new Route(req.getUri(), req.getMethod());
		for (Route rq : paths) {
			if (in.equals(rq))
				return rq;
			else if (rq.httpMethod == HttpMethod.ALL) {
				if (rq.uri.equals("/*"))
					df = rq;
				else if (rq.uri.equals(req.getUri()))
					return rq;
				else if (rq instanceof ProxyRoute && req.getUri().startsWith(rq.uri))
					return rq;
			} else if (rq instanceof ProxyRoute && req.getUri().startsWith(rq.uri)) {
				return rq;
			}
		}
		return df;
	}
}
