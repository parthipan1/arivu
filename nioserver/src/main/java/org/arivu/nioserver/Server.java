/**
 * 
 */
package org.arivu.nioserver;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.log.Appender;
import org.arivu.log.appender.Appenders;
import org.arivu.utils.Env;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author P
 *
 */
public class Server {
	private static final Logger logger = LoggerFactory.getLogger(Server.class);
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
			InetSocketAddress sa = new InetSocketAddress(Integer.parseInt(Env.getEnv("port", "8080")));
			logger.info("Server started at " + sa);
			(handler = new SelectorHandler()).handle(sa);
			closeAccessLog();
			runAllShutdownHooks();
			logger.info("Server stopped!");
		}
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

	static SelectorHandler handler = null;

	static Appender accessLog = null;

	static final int DEFAULT_SOCKET_BACKLOG = Integer.parseInt(Env.getEnv("socket.backlog", "1024"));

	public static ExecutorService getExecutorService() {
		return handler.exe;
	}

	public static ScheduledExecutorService getScheduledExecutorService() {
		return handler.sexe;
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
	
	private static void closeAccessLog() {
		if (Server.accessLog != null) {
			try {
				Server.accessLog.close();
			} catch (Exception e) {
				logger.error("Failed to close accesslog::", e);
			}
		}
	}

	public static void stop() {
		handler.close();
	}
	
}

