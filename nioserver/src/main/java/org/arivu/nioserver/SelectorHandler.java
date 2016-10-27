/**
 * 
 */
package org.arivu.nioserver;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.arivu.pool.ConcurrentPool;
import org.arivu.pool.Pool;
import org.arivu.pool.PoolFactory;
import org.arivu.utils.NullCheck;
import org.arivu.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mr P
 *
 */
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
			return RequestUtil.getString(defaultresponseheader);
		}

		@Override
		public void addJar(String jars) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getRouteResponseHeader(String route) {
			Route route2 = getRoute(route);
			if( route2!= null ) return RequestUtil.getString(route2.headers);
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
		InetSocketAddress sa = new InetSocketAddress(Server.DEFAULT_PORT);//InetAddress.getByName(Server.DEFAULT_HOST),
		ssc.socket().bind(sa, 1024);
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
//			e.printStackTrace();
			logger.error("Failed with Error::", e);
		}
	}
}
