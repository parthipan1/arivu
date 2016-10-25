/**
 * 
 */
package org.arivu.nioserver;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.arivu.log.Appender;
import org.arivu.log.appender.Appenders;
import org.arivu.pool.ConcurrentPool;
import org.arivu.pool.Pool;
import org.arivu.pool.PoolFactory;
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
	
	static final boolean SINGLE_THREAD_MODE = Boolean.parseBoolean(Env.getEnv("singleThread", "false"));

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
			(handler = new SelectorHandler()).sync();
		}
	}

	private static SelectorHandler handler = null;

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
	final ExecutorService exe;
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

	final Pool<Connection> connectionPool = new ConcurrentPool<Connection>(new PoolFactory<Connection>() {

		@Override
		public Connection create(Map<String, Object> params) {
			return new Connection(connectionPool);
		}

		@Override
		public void close(Connection t) {
			if (t != null)
				t.pool = null;
		}

		@Override
		public void clear(Connection t) {
			if (t != null)
				t.reset();

		}
	}, Connection.class);

	SelectorHandler() {
		super();
		this.connectionPool.setMaxPoolSize(-1);
		this.connectionPool.setMaxReuseCount(-1);
		this.connectionPool.setLifeSpan(-1);
		this.connectionPool.setIdleTimeout(30000);
		this.exe = Executors.newCachedThreadPool();
	}

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
		logger.info("Server started at " + sa);
		ssc.register(clientSelector, SelectionKey.OP_ACCEPT);

		while (!shutdown) {
			if (clientSelector.select() == 0)
				continue;
			try {
				Set<SelectionKey> readySet = clientSelector.selectedKeys();
				for (Iterator<SelectionKey> it = readySet.iterator(); it.hasNext();) {
					final SelectionKey key = it.next();
					it.remove();
					if (!key.isValid()) {
						continue;
					} else if (key.isAcceptable()) {
						SocketChannel clientSocket = ssc.accept();
						clientSocket.configureBlocking(false);
						SelectionKey key1 = clientSocket.register(clientSelector, SelectionKey.OP_READ);
						key1.attach(connectionPool.get(null));
					} else {
						key.interestOps(0);
						if(Server.SINGLE_THREAD_MODE){
							process(key);
						}else{
							exe.execute(new Runnable() {
								public void run() {
									process(key);
								}
							});
						}
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
		try {
			connectionPool.close();
		} catch (Exception e) {
			logger.error("Failed to close connectionPool::", e);
		}
		logger.info("Server stopped!");
		System.exit(0);
	}

	void process(final SelectionKey key) {
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
}

final class Connection {
	private static final Logger logger = LoggerFactory.getLogger(Connection.class);

	final long startTime = System.currentTimeMillis();

	StringBuffer inBuffer = null;
	ByteBuffer buff = null;
	Ref resBuff = null;
	int writeLen = 0;
	Pool<Connection> pool;

	public Connection(Pool<Connection> pool) {
		super();
		reset();
		this.pool = pool;
	}

	void reset() {
		inBuffer = new StringBuffer();
		buff = ByteBuffer.allocateDirect(Configuration.defaultRequestBuffer);
		writeLen = 0;
	}

	void write(SelectionKey key) throws IOException {
		logger.debug(" write  :: " + resBuff);
		if (resBuff != null) {
			try {
				SocketChannel socketChannel = (SocketChannel) key.channel();
				if (resBuff.headerBytes != null) {
					socketChannel.write(ByteBuffer.wrap(resBuff.headerBytes));
					resBuff.headerBytes = null;
				}
				if (resBuff.bodyBytes != null && resBuff.bodyBytes.length > 0) {
					int subArrLen = Math.min(resBuff.bodyBytes.length, writeLen + Configuration.defaultChunkSize);
					socketChannel
							.write(ByteBuffer.wrap(resBuff.bodyBytes, writeLen, subArrLen - writeLen));
//					System.out.println( "  write bytes from  :: "+writeLen+"  length :: "+(subArrLen - writeLen) +" to :: "+subArrLen);
					writeLen = subArrLen;
					if (writeLen == resBuff.bodyBytes.length) {
						finish(key);
					} else {
						key.interestOps(SelectionKey.OP_WRITE);
					}
				} else {
					finish(key);
				}
			}catch (IOException e) {
				logger.error("Failed in write :: ", e);
				finish(key);
				throw e;
			}
		}
	}

	void finish(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		SocketAddress remoteSocketAddress = channel.socket().getRemoteSocketAddress();
		channel.finishConnect();
		channel.close();
		key.cancel();
		RequestUtil.accessLog(resBuff.rc, resBuff.uri, startTime, System.currentTimeMillis(), resBuff.bodyBytes.length,
				remoteSocketAddress);
		buff = null;
		inBuffer = null;
		writeLen = 0;
		pool.put(this);
	}

	void read(final SelectionKey key) throws IOException {
		int bytesRead = 0;
		byte EOL0 = 1;
		try {
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
		} catch (IOException e) {
			logger.error("Failed in read :: ", e);
			finish(key);
			throw e;
		}
	}

	public void processRequest(final SelectionKey key) {
		Request request = null;
		Route route = null;
		Response response = null;
		logger.debug("process connection from " + ((SocketChannel) key.channel()).socket().getRemoteSocketAddress());
		try {
			request = RequestUtil.parseRequest(inBuffer);
			// System.out.println(" request :: " + request.toString());
			route = RequestUtil.getMatchingRoute(Configuration.routes, request.getUri(), request.getHttpMethod(), false);
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
				if( resBuff!=null && resBuff.bodyBytes!=null && resBuff.bodyBytes.length > Configuration.defaultChunkSize ){
					((SocketChannel) key.channel()).socket().setSoTimeout(0);
				}
				logger.debug(" request :: " + request.toString() + " response :: " + resBuff.bodyBytes.length);
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
		String formatDate = RequestUtil.dateFormat.format(new Date());
		errorAccessLog(formatDate);
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

	private void errorAccessLog(String formatDate) {
		StringBuffer access = new StringBuffer();
		access.append("[").append(formatDate).append("] ").append(inBuffer.toString()).append(" ").append("500");
		Server.accessLog.append(access.toString());
	}

	
}
