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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.log.Appender;
import org.arivu.log.appender.Appenders;
import org.arivu.pool.ConcurrentPool;
import org.arivu.pool.Pool;
import org.arivu.pool.PoolFactory;
import org.arivu.utils.Env;
import org.arivu.utils.NullCheck;
import org.arivu.utils.Utils;
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
	static void stop() throws Exception {
		StaticRef.getResponse().setResponseCode(200);
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
		public String[] getAllRoute() {
			Collection<Route> rts = Configuration.routes;

			String[] ret = new String[rts.size()];
			int i = 0;
			for (Route rt : rts) {
				ret[i++] = rt.uri + " " + rt.httpMethod;
			}

			return ret;
		}

		@Override
		public void removeRoute(String route) {
			Route route2 = getRoute(route);
			if(route2!=null)
				Configuration.routes.remove(route2);
		}
		
		Route getRoute(String route) {
			if( NullCheck.isNullOrEmpty(route) ) return null;
			Collection<Route> rts = Configuration.routes;
			for (Route rt : rts) {
				if ((rt.uri + " " + rt.httpMethod).equals(route))
					return rt;
			}
			return null;
		}

		@Override
		public void addProxyRoute(String name, String method, String location, String proxyPass, String dir) {
			HttpMethod httpMethod = HttpMethod.ALL;
			if (!NullCheck.isNullOrEmpty(method))
				httpMethod = HttpMethod.valueOf(method);

			if(!RequestUtil.validateRouteUri(location))
				throw new IllegalArgumentException(
						"Illegal location(" + location + ") specified!");
			
			String proxy_pass = proxyPass;
			boolean notNullProxy = !NullCheck.isNullOrEmpty(proxy_pass);
			boolean notNullDir = !NullCheck.isNullOrEmpty(dir);
			if (notNullProxy && notNullDir)
				throw new IllegalArgumentException(
						"Illegal proxy_pass(" + proxyPass + ") and dir(" + dir + ") specified!");
			if (notNullProxy) {
				proxy_pass = Utils.replaceAll(proxy_pass, "$host", Server.DEFAULT_HOST);
				proxy_pass = Utils.replaceAll(proxy_pass, "$port", String.valueOf(Server.DEFAULT_PORT));
			}
			if (notNullDir) {
				dir = Utils.replaceAll(dir, "$home", new File(".").getAbsolutePath());
			}
			ProxyRoute prp = new ProxyRoute(name, proxy_pass, dir, location, httpMethod, null, null, false, null);
			Collection<Route> rts = Configuration.routes;
			for (Route rt : rts) {
				if (rt instanceof ProxyRoute) {
					ProxyRoute prt = (ProxyRoute) rt;
					if (prt.uri.equals(location)
							&& (httpMethod == prt.httpMethod || prt.httpMethod == HttpMethod.ALL)) {
						if (NullCheck.isNullOrEmpty(prt.dir) && !notNullDir && proxy_pass.equals(prt.proxy_pass))
							throw new IllegalArgumentException(
									"Duplicate proxy proxy_pass(" + proxyPass + ") and dir(" + dir + ") specified!");
						else if (NullCheck.isNullOrEmpty(prt.proxy_pass) && !notNullProxy && dir.equals(prt.dir))
							throw new IllegalArgumentException(
									"Duplicate proxy proxy_pass(" + proxyPass + ") and dir(" + dir + ") specified!");
					}
				}
			}
			Configuration.routes.add(prp);
			logger.info("Added Proxy setting ::" + prp.toString());
		}

		@Override
		public void removeRouteHeader(String route, String header) {
			Route route2 = getRoute(route);
			if (route2 != null && !NullCheck.isNullOrEmpty(route2.headers)) {
				route2.headers.remove(header);
			}
		}

		@Override
		public void addRouteHeader(String route, String header, String value) {
			Route route2 = getRoute(route);
			if (route2 != null && !NullCheck.isNullOrEmpty(route2.headers)) {
				route2.headers.put(header, value);
			}
		}

		@Override
		public int getRequestBufferSize() {
			return Configuration.defaultRequestBuffer;
		}

		@Override
		public void setRequestBufferSize(int size) {
			Configuration.defaultRequestBuffer = size;
		}

		@Override
		public int getResponseChunkSize() {
			return Configuration.defaultChunkSize;
		}

		@Override
		public void setResponseChunkSize(int size) {
			Configuration.defaultChunkSize = size;
		}

		@Override
		public void scanPackage(String packageName) throws Exception {
			if (!NullCheck.isNullOrEmpty(packageName)) {
				PackageScanner.getPaths(Configuration.routes, packageName);
			}
		}

		@Override
		public void removeResponseHeader(String header) {
			Configuration.defaultResponseHeader.remove(header);
		}

		@Override
		public void addResponseHeader(String header, String value) {
			Configuration.defaultResponseHeader.put(header, value);
		}

		@Override
		public String getResponseHeader() {
			Map<String, Object> defaultresponseheader = Configuration.defaultResponseHeader;
			return getString(defaultresponseheader);
		}

		String getString(Map<String, Object> defaultresponseheader) {
			if (defaultresponseheader == null)
				return "";
			Set<Entry<String, Object>> entrySet = defaultresponseheader.entrySet();
			StringBuffer buf = new StringBuffer();
			for (Entry<String, Object> e : entrySet) {
				buf.append(e.getKey()).append("=").append(e.getValue().toString()).append(",");
			}
			return buf.toString();
		}

		@Override
		public void addJar(String jars) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getRouteResponseHeader(String route) {
			Route route2 = getRoute(route);
			if( route2!= null ) return getString(route2.headers);
			return null;
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
			logger.info(" Jmx bean beanName " + beanNameStr + " registered!");
		} catch (Exception e) {
			logger.error("Failed with Error::", e);
		}
	}

	void unregisterMXBean() {
		if (beanNameStr != null) {
			try {
				ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName(beanNameStr));
				logger.info("Unregister Jmx bean " + beanNameStr);
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
						if (Server.SINGLE_THREAD_MODE) {
							process(key);
						} else {
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
//	ByteBuffer buff = null;
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
//		buff = ByteBuffer.allocateDirect(Configuration.defaultRequestBuffer);
		writeLen = 0;
		in.clear();
		req = null;
	}
	
	ByteBuffer poll = null;
	int pos = 0;
	int rem = 0;
	final byte[] dst = new byte[Configuration.defaultChunkSize];
	
	void write(SelectionKey key) throws IOException {
		logger.debug(" write  :: " + resBuff);
		if (resBuff != null) {
			try {
				if( poll == null ){
					poll = resBuff.queue.poll();
					if( poll != null ){
						rem = poll.remaining();
						logger.debug(resBuff+" 1 write next ByteBuff size :: "+rem+" queueSize :: "+resBuff.queue.size());
					}else{
						logger.debug(resBuff+" 2 write next ByteBuff is null! finish! ");
						finish(key);
						return;
					}
				}
				
				int length = Math.min(Configuration.defaultChunkSize, rem-pos);
				logger.debug(resBuff+"  3 write bytes from  :: " + pos + "  length :: " + (length)
						+ " to :: " + (pos+length)+" size :: "+rem);
				byte[] dstt = dst;// new byte[length];
				if(length==Configuration.defaultChunkSize){
					for( int i=0;i<dstt.length;i++ )
						dstt[i] = poll.get(i+pos);
				}else{
					dstt = new byte[length];
					for( int i=0;i<dstt.length;i++ )
						dstt[i] = poll.get(i+pos);
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
		logger.debug(resBuff+" 4 finishByteBuff! empty :: "+empty+" queueSize :: "+resBuff.queue.size()+" read :: "+pos+" size :: "+rem);
		if( rem == pos ){
			poll = null;
			pos = 0;
			rem = 0;
			if(empty){
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
		RequestUtil.accessLog(resBuff.rc, resBuff.uri, startTime, System.currentTimeMillis(), resBuff.cl,
				remoteSocketAddress);
//		buff = null;
		inBuffer = null;
		writeLen = 0;
		pool.put(this);
	}

	final List<ByteBuffer> in = new DoublyLinkedList<>();
	RequestImpl req = null;
	void read(final SelectionKey key) throws IOException {
		int bytesRead = 0;
		byte EOL0 = 1;
		try {
			byte[] readBuf = new byte[Configuration.defaultRequestBuffer];
			ByteBuffer wrap = ByteBuffer.wrap(readBuf);
			if ((bytesRead = ((SocketChannel) key.channel()).read(wrap)) > 0) {
				EOL0 = wrap.get(wrap.position() - 1);
				if( req==null ){
					int headerIndex = RequestUtil.getHeaderIndex(readBuf);
					if( headerIndex == -1 ){
						in.add(wrap);
					}else{
						List<ByteBuffer> h = new DoublyLinkedList<>();
						h.addAll(in);
						h.add( ByteBuffer.wrap(Arrays.copyOfRange(readBuf, 0, headerIndex-1)) );
						in.clear();
						req = RequestUtil.parseRequest(h);
						logger.debug(" Got Request :: "+req);
						req.body.add( ByteBuffer.wrap(Arrays.copyOfRange(readBuf, headerIndex, bytesRead)) );
					}
				}else{
					req.body.add(wrap);
				}
				
//				buff.flip();
//				inBuffer.append(Charset.defaultCharset().decode(buff).array());
//				buff.clear();
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
		Request request = req;
		Route route = null;
		Response response = null;
		logger.debug("process connection from " + ((SocketChannel) key.channel()).socket().getRemoteSocketAddress());
		try {
//			request = req;//RequestUtil.parseRequest(inBuffer);
			// System.out.println(" request :: " + request.toString());
			route = RequestUtil.getMatchingRoute(Configuration.routes, request.getUri(), request.getHttpMethod(),
					false);
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
				if (resBuff != null 
						&& resBuff.cl > Configuration.defaultChunkSize) {
					((SocketChannel) key.channel()).socket().setSoTimeout(0);
				}
				logger.debug(" request :: " + request.toString() + " response :: " + resBuff.cl);
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
