/**
 * 
 */
package org.arivu.nioserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.arivu.datastructure.Amap;
import org.arivu.utils.Env;
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
//			System.err.println(e.toString());
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					logger.error("Failed on stop::", e);
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
		Map<String, String> properties = new Amap<String, String>();
		properties.put(channelType, serverChannel);
		socketServerSelectionKey.attach(properties);
		// logger.debug("Created SelectionKey!");
		while (!shutdown) {
			// logger.debug("shutdown loop!");
			if (selector.select() == 0)
				continue;

			// the select httpMethod returns with a list of selected keys
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
	
	@Path(value = "/*", httpMethod = HttpMethod.ALL)
	static void handle(Request req, Response res) throws Exception {
		logger.debug(req.toString());
		res.setResponseCode(404);
	}

	@Path(value = Configuration.stopUri, httpMethod = HttpMethod.GET)
	static void stop(Request req, Response res) throws Exception {
		res.setResponseCode(200);
		final ScheduledExecutorService exe = Executors.newScheduledThreadPool(1);
		exe.schedule(new Runnable() {
			
			@Override
			public void run() {
				exe.shutdownNow();
				stop();
			}
		}, 1, TimeUnit.SECONDS);
		
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
					final Request req = RequestUtil.parse(inBuffer);
					Route route = get(Configuration.routes, req);
					if (route != null) {
						final Response responseImpl = route.getResponse(req, socketChannel);
						try{
						route.handle(req,  responseImpl );
						}finally{
							try {
								responseImpl.close();
							} catch (Throwable e) {
								logger.error("Failed in response close :: ", e);
							}
						}
					}
				} catch (Throwable e) {
					long currentTimeMillis = System.currentTimeMillis();
					logger.error("Failed in requestImpl("+currentTimeMillis+") :: "+inBuffer);
					logger.error("Failed in requestImpl("+currentTimeMillis+") :: ", e);
				}
			}
		});

	}

	static Route get(Collection<Route> paths,Request req){
		Route df = null;
		Route in = new Route(req.getUri(), req.getMethod());
		for( Route rq: paths ){
			if( in.equals(rq) ) return rq;
			else if( rq.httpMethod == HttpMethod.ALL ){
				if(rq.uri.equals("/*"))
					df = rq;
				else if(rq.uri.equals(req.getUri()))
					return rq;
				else if( rq instanceof ProxyRoute && req.getUri().startsWith(rq.uri) )
					return rq;
			}else if( rq instanceof ProxyRoute && req.getUri().startsWith(rq.uri)  ){
				return rq;
			}
		}
		return df;
	}
}
