/**
 * 
 */
package org.arivu.nioserver;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
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
 * This is the Nio server main class , entry point for all Apps. Server download able from main <a href="https://github.com/parthipan1/arivu/blob/master/nioserver/arivu.nioserver-1.0.1.zip">distribution</a> or can be used part of your build.
 * If used part of build following command will start the server 
 * <pre>
 * 	java -server -classpath XXX org.arivu.nioserver.Server start
 * 		OR
 *  java -jar nioserver-1.0.1.jar
 * </pre>
 * Some of the configuration parameters all are optional.
 * <ul>
 * <li>
 * -DsingleThread=true/false -> Either run on single thread mode or multi-threaded mode (only read and process request). default to true
 *     Note: Write operation is asynchronous and always happens in new seperate thread even on single thread mode. Performance may vary based on the use case.</li>
 * <li>-DuseJ7Nio=true/false -> Either to use Java7Nio async library. default false.</li>
 * <li>-DthreadCnt=xxx(Number)  -> no of threads (only on multi threaded mode) default 50</li>
 * <li>-DschedulerCnt=xxx(Number)  -> no of schedule threads default 2</li>
 * <li>-Daccess.log=<Location of access.log> -> default ../logs/access.log. Format of access log is standard "[EEE, dd MMM yyyy HH:mm:ss z] uri httpMethod responseCode contentLength [ProcessTime in millisecs] remoteAddress"</li>
 * <li>-Dport=xxx(Number)  -> port on which the sever will run. default to 8080</li>
 * <li>-Dsocket.backlog=xxx(Number) -> Server socket backlog. default 1024</li>
 * <li>-Dsocket.timeout=xxx(Number) -> server socket timeout. default to 0</li>
 * <li>-DadminMod=true/false  -> Enable admin module as home page. default false , Enabled on server <a href="https://github.com/parthipan1/arivu/blob/master/nioserver/arivu.nioserver-1.0.1.zip">distribution</a> </li>
 * <li>-DadminLoc=(admin module directory location)  -> admin module location. default ../admin</li>
 * <li>-DdeployLoc=(app deployment directory location) -> location to deploy apps. default ../apps</li>
 * <li>-Darivu.nioserver.json=(location of json config file) -> default ./arivu.nioserver.json</li>
 * <li>-Dssl=true/false  -> Enable ssl protocol. default false</li>
 * <li>-Dssl.bufferSize=xxx(Number)  -> Buffer size. default 1048576</li>
 * <li>-Dssl.ksfile=(keystore file Location)  -> ssl keystore file Location. default 'nioserver.jks'</li>
 * <li>-Dssl.pass=<keystore password>  -> ssl keystore password. default 'nioserver'</li>
 * <li>-Dssl.protocol=(<a href="https://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#SSLContext">protocol</a>)  ->  ssl protocol. default 'TLSv1.2'</li> 
 * <li>-Dssl.cipherSuites=(comma separated list of <a href="https://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#ciphersuites">ciphersuites</a>)  -> default TLS_RSA_WITH_AES_128_CBC_SHA</li>
 * 
 * </ul>
 * 
 * @author P
 *
 */
public final class Server {
	private static final String DEFAULT_J7NIO = "false";
	private static final String DEFAULT_SSL = "false";

	public static final String DEFAULT_PORT = "8080";

	private static final Logger logger = LoggerFactory.getLogger(Server.class);
	
	static final ServerMXBean mxBean = new ServerMXBean() {

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
				if (rt.active)
					ret[i++] = rt.uri + " " + rt.httpMethod;
			}

			return ret;
		}

		@Override
		public void removeRoute(String route) {
			Route route2 = getRoute(route);
			if (route2 != null) {
				route2.disable();
			}
		}

		Route getRoute(String route) {
			if (NullCheck.isNullOrEmpty(route))
				return null;
			Collection<Route> rts = Configuration.routes;
			for (Route rt : rts) {
				if ((rt.uri + " " + rt.httpMethod).equals(route))
					return rt;
			}
			return null;
		}

		@Override
		public void addProxyRoute(String name, String method, String location, String proxyPass, String dir) {
			try {
				RequestUtil.addProxyRouteRuntime(name, method, location, proxyPass, dir, Configuration.routes, null);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
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
				List<Object> list = route2.headers.get(header);
				if (list == null) {
					list = new DoublyLinkedList<Object>();
					route2.headers.put(header, list);
				}
				list.add(value);
			}
		}

		@Override
		public int getRequestBufferSize() {
			return Configuration.defaultRequestBuffer;
		}

		@Override
		public void setRequestBufferSize(int size) {
			Configuration.defaultRequestBuffer = Math.max(1024, size);
		}

		@Override
		public int getResponseChunkSize() {
			return Configuration.defaultChunkSize;
		}

		@Override
		public void setResponseChunkSize(int size) {
			Configuration.defaultChunkSize = Math.max(1024, size);
		}

		@Override
		public void scanPackage(String packageName) throws Exception {
			if (!NullCheck.isNullOrEmpty(packageName)) {
				PackageScanner.getPaths(Configuration.routes, packageName, "System");
			}
		}

		@Override
		public void removeResponseHeader(String header) {
			Configuration.defaultResponseHeader.remove(header);
		}

		@Override
		public void addResponseHeader(String header, String value) {
			List<Object> list = Configuration.defaultResponseHeader.get(header);
			if (list == null) {
				list = new DoublyLinkedList<Object>();
				Configuration.defaultResponseHeader.put(header, list);
			}
			list.add(value);
		}

		@Override
		public String getResponseHeader() {
			Map<String, List<Object>> defaultresponseheader = Configuration.defaultResponseHeader;
			return Utils.toString(defaultresponseheader);
		}

		@Override
		public String getRouteResponseHeader(String route) {
			Route route2 = getRoute(route);
			if (route2 != null)
				return Utils.toString(route2.headers);
			return null;
		}

		@Override
		public int getByteCacheCnt() {
			return ByteData.mdc.size();
		}

		@Override
		public void clearByteCache() {
			ByteData.clean(true, null);
		}

	};
	
	static Pool<Connection> connectionPool = null;
	
	static AsynchronousChannelGroup group = null;
	static CountDownLatch  waitLatch = null;
	static CompletionHandler<AsynchronousSocketChannel, Connection> completionHandler = null;
//	static final boolean ssl = Boolean.parseBoolean(Env.getEnv("ssl", "false"));
//	static final boolean useJ7Nio =  Boolean.parseBoolean(Env.getEnv("useJ7Nio", "true"));
	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void main(String[] args) {
		if (args != null && args.length > 0 && args[0].equalsIgnoreCase("stop")) {
			RequestUtil.stopRemote();
		} else {
			try {
				beforeStart();
				final int port = Integer.parseInt(Env.getEnv("port", Server.DEFAULT_PORT));
				final boolean ssl = Boolean.parseBoolean(Env.getEnv("ssl", DEFAULT_SSL));
				final boolean useJ7Nio =  Boolean.parseBoolean(Env.getEnv("useJ7Nio", DEFAULT_J7NIO));
				if(ssl){
					(handler = new SelectorHandler()).start(port, ssl);
				}else if(useJ7Nio){
					startAsync(port, ssl); 
				}else{
					(handler = new SelectorHandler()).start(port, ssl);
				}
			} catch (Throwable e) {
				e.printStackTrace();
				logger.error("Server Failed :: ",e); 
			} finally {
				afterStop();
			}
		}
	}

	static void startAsync(final int port, final boolean ssl) throws IOException {
		if( !Configuration.SINGLE_THREAD_MODE )
			group = AsynchronousChannelGroup.withCachedThreadPool(getExecutorService(), Math.max(50, Integer.parseInt(Env.getEnv("threadCnt", "50")) ) ); 
		
		try (final AsynchronousServerSocketChannel serverSocketChannel = 
				AsynchronousServerSocketChannel.open(group).bind(new InetSocketAddress(port))) { 
			serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 4 * 1024); 
		    serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			serverSocketChannel.accept(connectionPool.get(null).assign(ssl), new CompletionHandler<AsynchronousSocketChannel, Connection>() {
				@Override
				public void completed(AsynchronousSocketChannel channel, Connection connection) {	
					logger.debug("completionHandler accept completed "); 
					serverSocketChannel.accept(connectionPool.get(null).assign(ssl), this); 
					connection.handle(channel);
				}
				@Override
				public void failed(Throwable t, Connection connection) {
					logger.error("Failed connection {} ",connection); 
					connectionPool.put(connection);
				}					
			});
			
			try { 
				if( !Configuration.SINGLE_THREAD_MODE )
					group.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
				else
					(waitLatch=new CountDownLatch(1)).await();
			} catch (InterruptedException ignored) { 
			}
		} catch (IOException e) {
			logger.error("Failed Server::", e);
		} 
		
	}

	private static void afterStop() {
		closeAccessLog();
		runAllShutdownHooks();
		exe.shutdownNow();
		sexe.shutdownNow();
		logger.info("Server stopped!");
		System.exit(0);
	}

	
//	private static final Thread systemShutdownHook = new Thread(new Runnable() {
//		
//		@Override
//		public void run() {
//			afterStop();
//		}
//	});
	
	private static void beforeStart() throws IOException {
		final boolean ssl = Boolean.parseBoolean(Env.getEnv("ssl", DEFAULT_SSL));
		final boolean useJ7Nio =  Boolean.parseBoolean(Env.getEnv("useJ7Nio", DEFAULT_J7NIO));
		if(ssl){
			if( Configuration.SINGLE_THREAD_MODE ){
				exe = Executors.newCachedThreadPool();
			}else{
				exe = Executors.newFixedThreadPool( Math.max(50, Integer.parseInt(Env.getEnv("threadCnt", "50")) ) );
			}
		}else if(useJ7Nio){
			exe = Executors.newCachedThreadPool();
		}else{
			if( Configuration.SINGLE_THREAD_MODE ){
				exe = Executors.newCachedThreadPool();
			}else{
				exe = Executors.newFixedThreadPool( Math.max(50, Integer.parseInt(Env.getEnv("threadCnt", "50")) ) );
			}
		}
		
		sexe = Executors.newScheduledThreadPool( Math.max(2, Integer.parseInt(Env.getEnv("schedulerCnt", "2")) ) );
		accessLog = Appenders.file
				.get(Env.getEnv("access.log", ".." + File.separator + "logs" + File.separator + "access.log"));
		registerMXBean();
		connectionPool = new ConcurrentPool<Connection>(new PoolFactory<Connection>() {

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
		connectionPool.setMaxPoolSize(-1);
		connectionPool.setMaxReuseCount(-1);
		connectionPool.setLifeSpan(-1);
		connectionPool.setIdleTimeout(30000);
		Server.registerShutdownHook(new Runnable() {
			
			@Override
			public void run() {
				try {
					connectionPool.close();
				} catch (Exception e) {
					logger.error("Failed to close connectionPool::", e);
				}
			}
		});
//		Runtime.getRuntime().addShutdownHook(systemShutdownHook);
	}

	static void runAllShutdownHooks() {
		for( Runnable r:Server.shutdownHooks ){
			try {
				r.run();
			} catch (Throwable e) {
				logger.error("Failed to shutdown::", e);
			}
		}
		Server.shutdownHooks.clear();
	}

	static ExecutorService exe;
	static ScheduledExecutorService sexe;
	
	static SelectorHandler handler = null;

	static Appender accessLog = null;

	static final int DEFAULT_SOCKET_BACKLOG = Integer.parseInt(Env.getEnv("socket.backlog", "1024"));

	public static ExecutorService getExecutorService() {
		return exe;
	}

	public static ScheduledExecutorService getScheduledExecutorService() {
		return sexe;
	}
	
	static final List<Runnable> shutdownHooks = new DoublyLinkedList<>();

	public static ShutdownHookRegistration registerShutdownHook(final Runnable e) {
		shutdownHooks.add(e);
		return new ShutdownHookRegistration() {
			
			@Override
			public void remove() {
				shutdownHooks.remove(e);
			}
		};
	}
	
	private static String beanNameStr = null;
	private static void registerMXBean() {
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			beanNameStr = "org.arivu.niosever:type=" + Server.class.getSimpleName() + "."
					+ Integer.parseInt(Env.getEnv("port", DEFAULT_PORT));
			mbs.registerMBean(mxBean, new ObjectName(beanNameStr));
			logger.info(" Jmx bean beanName {} registered!", beanNameStr);
			registerShutdownHook(new Runnable() {
				
				@Override
				public void run() {
					unregisterMXBean();	
				}
			});
		} catch (Exception e) {
			logger.error("Failed with Error::", e);
		}
	}

	private static 	void unregisterMXBean() {
		if (beanNameStr != null) {
			try {
				ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName(beanNameStr));
				logger.info("Unregister Jmx bean {}", beanNameStr);
			} catch (Exception e) {
				logger.error("Failed with Error::", e);
			}
		}
	}

	private static void closeAccessLog() {
		if (Server.accessLog != null) {
			try {
				Server.accessLog.close();
			} catch (Exception e) {
				logger.error("Failed to close accesslog::", e);
			}
		}
	}

	static void stop() {
//		Runtime.getRuntime().removeShutdownHook(systemShutdownHook);
		final boolean ssl = Boolean.parseBoolean(Env.getEnv("ssl", DEFAULT_SSL));
		final boolean useJ7Nio =  Boolean.parseBoolean(Env.getEnv("useJ7Nio", DEFAULT_J7NIO));
		if(ssl){
			handler.close();
		}else if(useJ7Nio){
			try {
				if( !Configuration.SINGLE_THREAD_MODE )
					group.shutdownNow();
				else 
					waitLatch.countDown();
			} catch (IOException e) {
				logger.error("Failed to stop Server::", e);
			}
		}else{
			handler.close();
		}
	}
	
}

