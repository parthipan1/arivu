/**
 * 
 */
package org.arivu.nioserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author P
 *
 */
public class Server {

	private static final Logger logger = LoggerFactory.getLogger(Server.class);

	private static final int DEFAULT_PORT = 8080;

	private static final CountDownLatch shutdownHook = new CountDownLatch(1);

	private static volatile boolean shutdown = false;

	private static final ExecutorService exe = Executors.newCachedThreadPool();

	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
		start(args);
		shutdownHook.await();
		exe.shutdownNow();
	}

	private static void start(String[] args) throws IOException {
		int port = DEFAULT_PORT;
		if (args != null && args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (Exception e) {
			}
		}

		final ServerSocketChannel ssc = ServerSocketChannel.open();

		ssc.socket().bind(new InetSocketAddress(port));
		ssc.configureBlocking(false);

		while (!shutdown) {
			logger.info("Waiting for connections");
			final SocketChannel sc = ssc.accept();
			exe.submit(new RequestHandler(sc));
		}
	}

	public static void stop() {
		shutdown = true;
		shutdownHook.countDown();
	}
}

class RequestHandler implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);
	final SocketChannel sc;

	public RequestHandler(SocketChannel sc) {
		super();
		this.sc = sc;
	}

	@Override
	public void run() {

		try {

		} finally {
			try {
				this.sc.close();
			} catch (IOException e) {
				logger.error("Failed to close socket::", e);
			}
		}
	}

}
