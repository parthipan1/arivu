/**
 * 
 */
package org.arivu.nioserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.arivu.pool.ConcurrentPool;
import org.arivu.pool.Pool;
import org.arivu.pool.PoolFactory;
import org.arivu.utils.Env;
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
	}

	void start(int port) throws IOException {
		InetSocketAddress sa = new InetSocketAddress(port);
		logger.info("Server started at " + sa);
		clientSelector = Selector.open();
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		ssc.socket().bind(sa, Server.DEFAULT_SOCKET_BACKLOG);
		ssc.socket().setSoTimeout(Integer.parseInt(Env.getEnv("socket.timeout", "0")));
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
						key1.attach(connectionPool.get(null).assign());
					} else {
						key.interestOps(0);
						if (Configuration.SINGLE_THREAD_MODE) {
							process(key);
						} else {
							Server.getExecutorService().execute(new Runnable() {
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

	void close() {
		if (shutdown)
			return;
		
		shutdown = true;
		try {
			clientSelector.close();
		} catch (IOException e) {
			logger.error("Failed in clientSelector close :: ", e);
		}
			
	}

	void process(final SelectionKey key) {
		try {
			Connection client = (Connection) key.attachment();
			if (key.isReadable()) {
				client.read(key, clientSelector);
			} else {
				client.write(key, clientSelector);
			}
		} catch (IOException e) {
			logger.error("Failed with Error::", e);
		}
	}
}
