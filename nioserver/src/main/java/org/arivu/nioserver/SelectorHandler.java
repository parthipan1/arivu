/**
 * 
 */
package org.arivu.nioserver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Iterator;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

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

	SelectorHandler() {
		super();
	}

	void start(final int port, final boolean ssl) throws Exception {
		SSLContext sslContext = getSSLContext(ssl);
		
		clientSelector = Selector.open();
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
							SocketChannel clientSocket = ssc.accept();
					        clientSocket.configureBlocking(false);

					        SSLEngine engine = sslContext.createSSLEngine();
					        engine.setEnabledCipherSuites(Env.getEnv("ssl.cipherSuites", "TLS_RSA_WITH_AES_128_CBC_SHA").split(","));
					        engine.setUseClientMode(false);
					        engine.beginHandshake();

					        Connection sllConn = (Connection) Server.connectionPool.get(null).assign(ssl);
					        if (sllConn.doSslHandshake(clientSocket, engine)) {
					        	SelectionKey key1 = clientSocket.register(clientSelector, SelectionKey.OP_READ);
					        	key1.attach(sllConn);
					        } else {
					        	clientSocket.close();
					        	Server.connectionPool.put(sllConn);
					            logger.debug("SSLConnection closed due to handshake failure.");
					        }
						}else{
							SocketChannel clientSocket = ssc.accept();
							clientSocket.configureBlocking(false);
							SelectionKey key1 = clientSocket.register(clientSelector, SelectionKey.OP_READ);
							key1.attach(Server.connectionPool.get(null).assign(ssl));
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

	private SSLContext getSSLContext(final boolean ssl) throws NoSuchAlgorithmException, KeyStoreException, IOException,
			CertificateException, FileNotFoundException, UnrecoverableKeyException, KeyManagementException {
		SSLContext sslContext = null;
		if(ssl){
			String keyStorePath = Env.getEnv("ssl.ksfile", "nioserver.jks");
			String keyStorePassword = Env.getEnv("ssl.pass", "nioserver");
			
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());
			keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
			
			sslContext = SSLContext.getInstance( Env.getEnv("ssl.protocol", "TLSv1.2") );
			sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());
		}
		return sslContext;
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
