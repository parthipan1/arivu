/**
 * 
 */
package org.arivu.nioserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.arivu.datastructure.Amap;
import org.arivu.nioserver.Request.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author P
 *
 */
public class Server {

	private static final int BUFFER_SIZE = 1024;

	private static final Logger logger = LoggerFactory.getLogger(Server.class);

	private static final int DEFAULT_PORT = Integer.parseInt(getEnv("port", "8080"));

	private static volatile boolean shutdown = false;

	static final ExecutorService exe = Executors.newCachedThreadPool();
	static String clientChannel = "clientChannel";
	static String serverChannel = "serverChannel";
	static String channelType = "channelType";

	private static final Map<Integer, Session> SESSIONS = new Amap<Integer, Session>();

	private static final Session getRequest(Integer id) {
		Session session = SESSIONS.get(id);
		if (session == null) {
			session = new Session();
			SESSIONS.put(id, session);
		}
		return session;
	}

	private static String getEnv(String key, String dvalue) {
		return System.getProperty(key, (System.getenv().get(key) == null ? dvalue : System.getenv().get(key)));
	}

	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
		start(args);
		exe.shutdownNow();
	}

	private static void start(String[] args) throws IOException {
		ServerSocketChannel channel = ServerSocketChannel.open();

		channel.bind(new InetSocketAddress("localhost", DEFAULT_PORT));
		logger.debug("Server listning at " + DEFAULT_PORT + "!");
		channel.configureBlocking(false);

		final Selector selector = Selector.open();
		final SelectionKey socketServerSelectionKey = channel.register(selector, SelectionKey.OP_ACCEPT);
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(channelType, serverChannel);
		socketServerSelectionKey.attach(properties);
		// logger.debug("Created SelectionKey!");
		while (!shutdown) {
			// logger.debug("shutdown loop!");
			if (selector.select() == 0)
				continue;

			// the select method returns with a list of selected keys
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			// logger.debug("get selectedKeys!");
			Iterator<SelectionKey> iterator = selectedKeys.iterator();
			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();

				if (((Map<?, ?>) key.attachment()).get(channelType).equals(serverChannel)) {
					ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
					SocketChannel clientSocketChannel = serverSocketChannel.accept();
					Integer reqId = clientSocketChannel.socket().hashCode();
					// logger.debug("write channel! " + reqId);
					if (clientSocketChannel != null) {
						// set the client connection to be non blocking
						clientSocketChannel.configureBlocking(false);
						SelectionKey clientKey = clientSocketChannel.register(selector, SelectionKey.OP_READ,
								SelectionKey.OP_WRITE);
						Map<String, String> clientproperties = new HashMap<String, String>();
						clientproperties.put(channelType, clientChannel);
						clientKey.attach(clientproperties);

						getRequest(reqId).setOut(clientSocketChannel);

					}
				} else {
					ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
					SocketChannel clientSocketChannel = (SocketChannel) key.channel();
					Integer reqId = clientSocketChannel.socket().hashCode();
					// logger.debug("read channel! " + reqId);
					int bytesRead = 0;
					if (key.isReadable()) {
						if ((bytesRead = clientSocketChannel.read(buffer)) > 0) {
							buffer.flip();
							CharBuffer decode = Charset.defaultCharset().decode(buffer);
							// logger.debug("clientSocketChannel
							// "+clientSocketChannel.);
							getRequest(reqId).read(decode);
							buffer.clear();
						}
						// logger.debug("bytesRead " + bytesRead);
						if (bytesRead < BUFFER_SIZE) {
							Session remove = SESSIONS.remove(reqId);
							if (remove != null)
								remove.process();

						}
					}

				}

				// once a key is handled, it needs to be removed
				iterator.remove();

			}
		}
	}

	public static void stop() {
		shutdown = true;
	}
}

final class Session {
	private static final Logger logger = LoggerFactory.getLogger(Session.class);

	final StringBuffer inBuffer = new StringBuffer();
	SocketChannel socketChannel = null;

	public Session() {
		super();
	}

	public void read(CharBuffer charBuffer) {
		inBuffer.append(charBuffer.array());
	}

	public void setOut(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}

	public void process() throws IOException {
		Server.exe.submit(new Runnable() {
			@Override
			public void run() {
				try {
				new ConsoleRequestHandler().handle(new RequestParser().parse(inBuffer),new Response(socketChannel));
//					CharBuffer buffer = CharBuffer.wrap("Hello client " + System.currentTimeMillis());
//					while (buffer.hasRemaining()) {
//						socketChannel.write(Charset.defaultCharset().encode(buffer));
//					}
//					buffer.clear();
//					socketChannel.close();
//					logger.debug("got :: " + inBuffer.toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

}

final class RequestParser {
	private static final byte BYTE_13 = (byte)13;
	private static final byte BYTE_10 = (byte)10;
	static final String divider = System.lineSeparator() + System.lineSeparator();

	Request parse(final StringBuffer buffer) {
		String content = buffer.toString();
		byte[] bytes = content.getBytes();
		int indexOf = -1;
		for(int i=3;i<bytes.length;i++){
			boolean c1 = bytes[i] == bytes[i-2];
			boolean c2 = bytes[i] == BYTE_10;
			boolean c3 = bytes[i-1] == bytes[i-3];
			boolean c4 = bytes[i-1] == BYTE_13;
			boolean b = c1 && c2  &&
				c3 && c4;
			System.out.println("bytes["+i+"] %"+bytes[i]+"% b "+b+" c1 "+c1+" c2 "+c2+" c3 "+c3+" c4 "+c4);
			if( b ){
				indexOf = i;
				break;
			}
		}
//		int indexOf = content.indexOf(divider);
		System.out.println();
		String metadata = content.substring(0, indexOf - 1);
		String body = content.substring(indexOf);
		// POST /snw/apps/sync HTTP/1.1

		String[] split = metadata.split(System.lineSeparator());

		String[] split2 = split[0].split(" ");

		Method valueOf = Request.Method.valueOf(split2[0]);
		if (valueOf == null)
			throw new IllegalArgumentException("Unknown Request " + metadata);
		String uri = split2[1];
		String protocol = split2[2];

		Map<String, String> tempheaders = new HashMap<String, String>();
		for (int i = 1; i < split.length; i++) {
			String h = split[i];
			int indexOf2 = h.indexOf(": ");
			if (indexOf2 == -1) {
				tempheaders.put(h, "");
			} else {
				tempheaders.put(h.substring(0, indexOf2), h.substring(indexOf2 + 2));
			}
		}

		String uriparams = uri.substring(uri.indexOf("?"));
		Map<String, Collection<String>> tempparams = new HashMap<String, Collection<String>>();
		String[] split3 = uriparams.split("&");
		for (String p : split3) {
			int indexOf2 = p.indexOf("=");
			if (indexOf2 == -1) {
				Collection<String> collection = tempparams.get(p);
				if (collection == null) {
					collection = new ArrayList<String>();
				}
				tempparams.put(p, collection);
			} else {
				String key = p.substring(0, indexOf2);
				String value = p.substring(indexOf2 + 1);
				Collection<String> collection = tempparams.get(key);
				if (collection == null) {
					collection = new ArrayList<String>();
				}
				collection.add(value);
				tempparams.put(key, collection);
			}
		}

		for (Entry<String, Collection<String>> e : tempparams.entrySet()) {
			e.setValue(Collections.unmodifiableCollection(e.getValue()));
		}

		return new Request(valueOf, uri, protocol, Collections.unmodifiableMap(tempparams),
				Collections.unmodifiableMap(tempheaders), body);
	}
}
final class ConsoleRequestHandler implements RequestHandler{

	@Override
	public void handle(Request req, Response res) throws Exception {
		System.out.println(req.toString());
		res.append("Ok");
		res.close();
	}
	
}