/**
 * 
 */
package org.arivu.nioserver;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

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

	void start(final int port, final boolean ssl) throws Exception {
		SSLContext sslContext = null;
		if(ssl){
			String keyStorePath = Env.getEnv("ssl.ksfile", "keystore.jks");
			String keyStorePassword = Env.getEnv("ssl.pass", "parthipan");
			
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());
			keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
			
			sslContext = SSLContext.getInstance( Env.getEnv("ssl.protocol", "TLS") );
			sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());
			
			clientSelector = SelectorProvider.provider().openSelector();
		}else{
			clientSelector = Selector.open();
		}
		
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		InetSocketAddress sa = new InetSocketAddress(port);
		logger.info("Server started at " + sa);
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
						if(ssl){
							SocketChannel clientSocket = ((ServerSocketChannel) key.channel()).accept();
					        clientSocket.configureBlocking(false);

					        SSLEngine engine = sslContext.createSSLEngine();
					        engine.setUseClientMode(false);
					        engine.beginHandshake();

					        Connection sllConn = (Connection) connectionPool.get(null).assign(ssl);
					        if (sllConn.doHandshake(clientSocket, engine)) {
					        	SelectionKey key1 = clientSocket.register(clientSelector, SelectionKey.OP_READ);
					        	key1.attach(sllConn);
					        } else {
					        	clientSocket.close();
					        	connectionPool.put(sllConn);
					            logger.debug("SSLConnection closed due to handshake failure.");
					        }
						}else{
							SocketChannel clientSocket = ssc.accept();
							clientSocket.configureBlocking(false);
							SelectionKey key1 = clientSocket.register(clientSelector, SelectionKey.OP_READ);
							key1.attach(connectionPool.get(null).assign(ssl));
						}
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
