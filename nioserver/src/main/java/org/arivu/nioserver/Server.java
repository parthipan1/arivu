/**
 * 
 */
package org.arivu.nioserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
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
import org.arivu.utils.Env;
import org.arivu.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author P
 *
 */
public class Server {

	static final String DEFAULT_HOST = Env.getEnv("host", "localhost");

	private static final int BUFFER_SIZE = 1024;

	private static final Logger logger = LoggerFactory.getLogger(Server.class);

	static final int DEFAULT_PORT = Integer.parseInt(Env.getEnv("port", "8080"));

	private static volatile boolean shutdown = false;

	static final ExecutorService exe = Executors.newCachedThreadPool();
	static String clientChannel = "org.arivu.nioserver.clientChannel";
	static String serverChannel = "org.arivu.nioserver.serverChannel";
	static String channelType = "org.arivu.nioserver.channelType";

	private static final Map<Integer, Connection> CONNECTIONS = new Amap<Integer, Connection>();

	private static final Connection getRequest(Integer id, Selector selector) {
		Connection connection = CONNECTIONS.get(id);
		if (connection == null) {
			connection = new Connection();
			CONNECTIONS.put(id, connection);
		}
		return connection;
	}

	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
		if (args != null && args.length > 0 && args[0].equalsIgnoreCase("stop")) {
			stopRemote();
		} else {
			start(args);
		}
	}

	private static void stopRemote() {
		String url = "http://" + DEFAULT_HOST + ":" + DEFAULT_PORT + Configuration.stopUri;
		// System.out.println("req::"+url);
		BufferedReader in = null;
		try {
			final HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
			int responseCode = con.getResponseCode();
			final StringBuffer response = new StringBuffer();
			in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			if (responseCode == 200) {
				System.out.println("Server stopped");
			}
			// System.out.println(" responseCode :: "+responseCode+" response ::
			// "+response.toString());
		} catch (Throwable e) {
			logger.error("Failed on stop::", e);
			// e.printStackTrace();
//			System.err.println(e.toString());
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

	}

	private static Selector selector = null;

	private static void start(String[] args) throws IOException {
		ServerSocketChannel channel = ServerSocketChannel.open();

		channel.bind(new InetSocketAddress(DEFAULT_HOST, DEFAULT_PORT));
		logger.info("Server listning at " + DEFAULT_HOST + ":" + DEFAULT_PORT + "!");
		channel.configureBlocking(false);

		selector = Selector.open();
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
					if (clientSocketChannel != null) {
						// set the client connection to be non blocking
						clientSocketChannel.configureBlocking(false);
						SelectionKey clientKey = clientSocketChannel.register(selector, SelectionKey.OP_READ,
								SelectionKey.OP_WRITE);
						Map<String, String> clientproperties = new HashMap<String, String>();
						clientproperties.put(channelType, clientChannel);
						clientKey.attach(clientproperties);
						getRequest(reqId, selector).setOut(clientSocketChannel);
					}
				} else {
					ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
					SocketChannel clientSocketChannel = (SocketChannel) key.channel();
					Integer reqId = clientSocketChannel.socket().hashCode();
					int bytesRead = 0;
					if (key.isReadable()) {
						if ((bytesRead = clientSocketChannel.read(buffer)) > 0) {
							buffer.flip();
							getRequest(reqId, selector).read(Charset.defaultCharset().decode(buffer));
							buffer.clear();
						}
						if (bytesRead < BUFFER_SIZE) {
							Connection remove = CONNECTIONS.remove(reqId);
							if (remove != null)
								remove.process();

						}
					}
				}
				iterator.remove();
			}
		}
	}

	public static void stop() {
		shutdown = true;
		try {
			selector.close();
		} catch (IOException e) {
			logger.error("Failed in selector close :: ", e);
		}
		exe.shutdownNow();
		logger.info("Server stopped!");
	}
}

final class Connection {
	private static final Logger logger = LoggerFactory.getLogger(Connection.class);

	final StringBuffer inBuffer = new StringBuffer();
	SocketChannel socketChannel = null;

	public Connection() {
		super();
	}

	public void read(CharBuffer charBuffer) {
		inBuffer.append(charBuffer.array());
	}

	public void setOut(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}

	public void process() throws IOException {
		logger.debug("process connection from " + socketChannel.socket().getRemoteSocketAddress());
		Server.exe.submit(new Runnable() {
			@Override
			public void run() {
				try {
					final Request req = new RequestParser().parse(inBuffer);
					RequestPath requestPath = Request.get(Configuration.requestPaths, req);
					if (requestPath != null) {
						final Response response = requestPath.getResponse(req, socketChannel);
						try{
						requestPath.handle(req,  response );
						}finally{
							try {
								response.close();
							} catch (Throwable e) {
								logger.error("Failed in response close :: ", e);
							}
						}
					}
				} catch (Throwable e) {
					long currentTimeMillis = System.currentTimeMillis();
					logger.error("Failed in request("+currentTimeMillis+") :: "+inBuffer);
					logger.error("Failed in request("+currentTimeMillis+") :: ", e);
				}
			}
		});

	}

}

final class RequestParser {
	private static final Logger logger = LoggerFactory.getLogger(RequestParser.class);
	
	private static final String ENC_UTF_8 = "UTF-8";
	private static final byte BYTE_13 = (byte) 13;
	private static final byte BYTE_10 = (byte) 10;
	static final String divider = System.lineSeparator() + System.lineSeparator();

	Request parse(final StringBuffer buffer) {
		String content = buffer.toString();
		byte[] bytes = content.getBytes();
		int indexOf = -1;
		for (int i = 3; i < bytes.length; i++) {
			if (bytes[i] == bytes[i - 2] && bytes[i] == BYTE_10 && bytes[i - 1] == bytes[i - 3]
					&& bytes[i - 1] == BYTE_13) {
				indexOf = i;
				break;
			}
		}

		String metadata = null;
		String body = null;

		if (indexOf == -1) {
			metadata = content;
		} else {
			metadata = content.substring(0, indexOf - 1);
			body = content.substring(indexOf);
		}

		String[] split = metadata.split(System.lineSeparator());

		String[] split2 = split[0].split(" ");

//		System.out.println("REQ METHOD :: "+split2[0]);
		logger.debug("Parsing Request :: "+content);
		Method valueOf = Request.Method.valueOf(split2[0]);
		if (valueOf == null)
			throw new IllegalArgumentException("Unknown Request " + metadata);
		String uriWithParams = split2[1];
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

		int indexOf3 = uriWithParams.indexOf("?");
		String uri = uriWithParams;
		Map<String, Collection<String>> tempparams = null;
		if (indexOf3 > 0) {
			uri = uriWithParams.substring(0, indexOf3);
			tempparams = parseParams(uriWithParams.substring(indexOf3 + 1));
		}
		return new Request(valueOf, uri, uriWithParams, protocol, tempparams, Utils.unmodifiableMap(tempheaders), body);
	}

	Map<String, Collection<String>> parseParams(String uriparams) {
		Map<String, Collection<String>> tempparams = new HashMap<String, Collection<String>>();
		String[] split3 = uriparams.split("&");
		for (String p : split3) {
			int indexOf2 = p.indexOf("=");
			if (indexOf2 == -1) {
				Collection<String> collection = tempparams.get(p);
				if (collection == null) {
					collection = new ArrayList<String>();
				}
				try {
					tempparams.put(URLDecoder.decode(p, ENC_UTF_8), collection);
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}
			} else {
				String key = p.substring(0, indexOf2);
				String value = p.substring(indexOf2 + 1);
				try {
					String decodeKey = URLDecoder.decode(key, ENC_UTF_8);
					Collection<String> collection = tempparams.get(decodeKey);
					if (collection == null) {
						collection = new ArrayList<String>();
						tempparams.put(decodeKey, collection);
					}
					collection.add(URLDecoder.decode(value, ENC_UTF_8));
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}
			}

		}
		for (Entry<String, Collection<String>> e : tempparams.entrySet()) {
			e.setValue(Collections.unmodifiableCollection(e.getValue()));
		}
		return Utils.unmodifiableMap(tempparams);
	}
}

final class DefaultRequestHandler {
	private static final Logger logger = LoggerFactory.getLogger(DefaultRequestHandler.class);

	@Path(value = "/*", method = Request.Method.ALL)
	static public void handle(Request req, Response res) throws Exception {
		logger.debug(req.toString());
		res.setResponseCode(404);
	}

	@Path(value = Configuration.stopUri, method = Request.Method.GET)
	static public void stop(Request req, Response res) throws Exception {
		res.setResponseCode(200);
		Server.stop();
	}
}