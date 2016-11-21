package org.arivu.nioserver;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.Threadlocal;
import org.arivu.utils.NullCheck;
import org.arivu.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Route {
	private static final Logger logger = LoggerFactory.getLogger(Route.class);

	String name;
	final String uri;
	final org.arivu.nioserver.HttpMethod httpMethod;
	Class<?> klass;
	Method method;
	final boolean isStatic;
	final MethodInvoker invoker;
	final RequestUriTokens rut;
	Threadlocal<Object> tl;
	Map<String, List<Object>> headers = null;
	
	volatile boolean active = true;
	
	Route(String uri, org.arivu.nioserver.HttpMethod httpMethod) {
		this(null, uri, httpMethod, null, null, false);
	}

	/**
	 * @param name TODO
	 * @param uri
	 * @param httpMethod
	 * @param httpMethod
	 * @param klass
	 */
	Route(String name, String uri, org.arivu.nioserver.HttpMethod httpMethod, Class<?> klass, Method method, boolean isStatic) {
		super();
		this.name = name;
		this.uri = uri;
		this.httpMethod = httpMethod;
		this.klass = klass;
		this.method = method;
		this.isStatic = isStatic;
		
		if (klass != null) {
			if( httpMethod!=null && httpMethod == HttpMethod.HEAD )
				throw new IllegalStateException("Invalid httpMethod @Path declaration on "+name+" method "+method);
			
			this.headers = new Amap<String, List<Object>>(Configuration.defaultResponseHeader);
			int is = uri.indexOf('{');
			if (is == -1) {
				this.invoker = RequestUtil.getMethodInvoker(method);
				this.rut = null;
			} else {
				this.invoker = MethodInvoker.variable;
				this.rut = RequestUtil.parseRequestUriTokens(uri, method);
			}
			if(!isStatic){
				this.tl = new Threadlocal<Object>(new Threadlocal.Factory<Object>() {
					
					@Override
					public Object create(Map<String, Object> params) {
						try {
							return Route.this.klass.newInstance();
						} catch (InstantiationException e) {
							logger.error("Error on creating new instance " + Route.this.klass.getName() + " :: ", e);
						} catch (IllegalAccessException e) {
							logger.error("Error on creating new instance " + Route.this.klass.getName() + " :: ", e);
						}
						return null;
					}
				}, 30000);
			}
		} else {
			this.rut = null;
			this.invoker = null;
			this.tl = null;
		}
	}

//	boolean match(String requri) {
//		if (uri.equals(requri))
//			return true;
//
//		return false;
//	}

	Response getResponse(Request req) {
		return new ResponseImpl(req, headers);
	}

	public void handle(Request req, Response res) {
		try {
			this.invoker.handle(req, res, isStatic, method, tl, this.rut);
		} catch (Throwable e) {
			logger.error("Failed in route " + this + " :: ", e);
			Configuration.exceptionHandler.handle(e);
		}
	}

	void disable(){
		active = false;
		if(tl!=null){
			tl.clearAll();
		}
	}
	
	void enable(){
		active = true;
	}
	
	void close(){
		if(tl!=null){
			tl.close();
			tl.clearAll();
		}
		klass = null;
		method = null;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((httpMethod == null) ? 0 : httpMethod.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Route other = (Route) obj;
		if (httpMethod != other.httpMethod)
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Route [uri=" + uri + ", httpMethod=" + httpMethod + ", klass=" + klass + ", method=" + method
				+ ", isStatic=" + isStatic + "]";
	}

}

class ProxyRoute extends Route {
	private static final Logger logger = LoggerFactory.getLogger(ProxyRoute.class);

	String proxy_pass;
	String dir;
	Threadlocal<JavaHttpMethodCall> proxyTh;

	/**
	 * @param uri
	 * @param httpMethod
	 * @param klass
	 * @param httpMethod
	 * @param isStatic
	 */
	ProxyRoute(String name, String proxy_pass, String dir, String uri, org.arivu.nioserver.HttpMethod httpMethod,
			Class<?> klass, Method method, boolean isStatic, Map<String, List<Object>> defaultResponseHeader) {
		super(null, uri, httpMethod, klass, method, isStatic);
		this.name = name;
		this.proxy_pass = proxy_pass;
		this.dir = dir;
		this.headers = new Amap<String, List<Object>>(defaultResponseHeader);
		if (NullCheck.isNullOrEmpty(proxy_pass) && NullCheck.isNullOrEmpty(dir)) {
			throw new IllegalArgumentException("Invalid config " + name + " !");
		} else if (!NullCheck.isNullOrEmpty(proxy_pass) && !NullCheck.isNullOrEmpty(dir)) {
			throw new IllegalArgumentException("Invalid config " + name + " !");
		} else if (!NullCheck.isNullOrEmpty(dir)) {
		} else if (!NullCheck.isNullOrEmpty(proxy_pass)) {
			this.proxyTh = new Threadlocal<JavaHttpMethodCall>(new Threadlocal.Factory<JavaHttpMethodCall>() {

				@Override
				public JavaHttpMethodCall create(Map<String, Object> params) {
					return new JavaHttpMethodCall();
				}
			}, 30000);
		}

	}

	/**
	 * @param uri
	 * @param httpMethod
	 */
	ProxyRoute(String uri, org.arivu.nioserver.HttpMethod httpMethod) {
		super(uri, httpMethod);
	}

	@Override
	final public void handle(Request req, Response res) {
		try {
			if (!NullCheck.isNullOrEmpty(dir)) {
				handleBrowser(req, res);
			} else {
				handleProxy(req, res);
			}
//			logger.debug("**** handle proxy res {}",res);
		} catch (Throwable e) {
			logger.error("Failed in route " + this + " :: ", e);
			Configuration.exceptionHandler.handle(e);
		}
	}

	final void handleProxy(final Request req, final Response res) throws IOException {
		final AsynContext ctx = StaticRef.getAsynContext();
		ctx.setAsynchronousFinish(true);
		Server.getExecutorService().execute(new Runnable() {
			
			@Override
			public void run() {
				
				try {
					final int indexOf = Math.max(req.getUri().length(), req.getUriWithParams().indexOf("?")) ;
					String queryStr = URLDecoder.decode(req.getUriWithParams().substring(indexOf),
							RequestUtil.ENC_UTF_8);
					String loc = proxy_pass + req.getUri().substring(uri.length()) + queryStr;
					req.getMethod().proxy(proxyTh.get(null), loc, req, res);
				} catch (IOException e) {
					logger.error("Failed in route " + this + " :: ", e);
					Configuration.exceptionHandler.handle(e);
				}finally {
					ctx.finish();
				}
			}
		});
	}

	final void handleBrowser(Request req, Response res) throws IOException {
		String fileLoc = this.dir + Utils.replaceAll(URLDecoder.decode(req.getUri().substring(this.uri.length()), RequestUtil.ENC_UTF_8), "/", File.separator)  ;
		File file = new File(fileLoc);
		if (!file.exists()) {
			res.setResponseCode(404);
		} else if (file.isDirectory()) {
			handleDirectory(req, res, file);
		} else {
			handleFile(res, fileLoc, file);
		}
	}

	void handleFile(Response res, String fileLoc, File file) throws IOException {
		if (!NullCheck.isNullOrEmpty(Configuration.defaultMimeType)) {
			String[] split = file.getName().split("\\.(?=[^\\.]+$)");
			final String ext = "." + split[split.length - 1];
			Map<String, Object> map = Configuration.defaultMimeType.get(ext.toLowerCase(Locale.getDefault()));
			if (map != null) {
				Object typeObj = map.get("type");
				if (typeObj != null) 
					res.putHeader("Content-Type", typeObj);
				
			}
			res.putHeader("Content-Disposition", "inline; filename=\""+file.getName()+"\"");
		}
		res.append(new ByteData(file));
	}

	void handleDirectory(Request req, Response res, File f) throws IOException {
		boolean endsWith = req.getUri().endsWith("/");
		String pathSep = "";
		if (!endsWith)
			pathSep = req.getUri() + "/";

		File[] listFiles = f.listFiles();
		StringBuffer buf = new StringBuffer("<html><body>");
		buf.append("<a href=\"").append("..").append("\" >").append("..").append("</a>").append("<br>");
		for (File f1 : listFiles) {
			if (f1.isDirectory())
				buf.append("<a href=\"").append(pathSep).append(f1.getName()).append("/").append("\" >")
						.append(f1.getName()).append("</a>").append("<br>");
			else
				buf.append("<a href=\"").append(pathSep).append(f1.getName()).append("\" >").append(f1.getName())
						.append("</a>").append("&ensp;").append(f1.length()).append("&nbsp;bytes").append("<br>");
		}
		buf.append("</body></html>");
		res.setResponseCode(200);
		res.append(buf.toString());
		res.putHeader("Content-Type", "text/html;charset=UTF-8");
		res.putHeader("Content-Length", buf.length());
	}

	@Override
	final void disable() {
		super.disable();
		if( this.proxyTh!=null ) this.proxyTh.clearAll();
	}

	@Override
	final void close() {
		super.close();
		if( this.proxyTh!=null ) this.proxyTh.clearAll();
	}

	@Override
	public String toString() {
		return "ProxyRoute [proxy_pass=" + proxy_pass + ", dir=" + dir + ", name=" + name + ", uri=" + uri
				+ ", httpMethod=" + httpMethod + ", headers=" + Utils.toString(headers) + "]";
	}
}
final class AdminRoute extends ProxyRoute {
	static final Map<String,String> authTokens = new Amap<String,String>();
	AdminRoute() {
		super("adminSite", null, Configuration.ADMIN_LOC, "/admin", HttpMethod.ALL, null, null, false, Configuration.defaultResponseHeader);
	}

	@Override
	void handleDirectory(Request req, Response res, File f) throws IOException {
		res.sendRedirect("/admin/Admin.html");
	}

	@Override
	void handleFile(Response res, String fileLoc, File file) throws IOException {
		super.handleFile(res, fileLoc, file);
		if(fileLoc.endsWith("Admin.html")){
//			SelectionKey key = StaticRef.getSelectionKey();
//			if (key!=null) {
				InetAddress remoteHostAddress = StaticRef.getRemoteHostAddress();// ((SocketChannel) key.channel()).socket().getInetAddress();
				if (remoteHostAddress!=null) {
					String keyv = remoteHostAddress.toString();
//					System.err.println("\n\n************** auth token "+keyv+"\n\n");
					authTokens.put(keyv, String.valueOf(System.currentTimeMillis()));
				}
//			}
		}
	}
}

final class JavaHttpMethodCall {

	public void delete(String uri, Map<String, List<Object>> headers, Response res) throws IOException {
		// System.out.println("DELETE "+uri);
		final URL obj = new URL(uri);
		final HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		// optional default is GET
		con.setRequestMethod("DELETE");
		// add requestImpl header
		addReqHeaders(con, headers);

		pipeResponse(con,res);
	}

	private void addReqHeaders(final HttpURLConnection con, Map<String, List<Object>> headers) {
//		con.setRequestProperty("User-Agent", "Java8");
//		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		if (!NullCheck.isNullOrEmpty(headers)) {
			Set<Entry<String, List<Object>>> entrySet = headers.entrySet();
			for (Entry<String, List<Object>> e : entrySet) {
				List<Object> value = e.getValue();
				if( !NullCheck.isNullOrEmpty(value) ){
					for(Object o:value){
						con.setRequestProperty(e.getKey(), o.toString());
					}
				}
			}
		}
	}

	public void get(final String uri, Map<String, List<Object>> headers, Response res) throws IOException {
		final URL obj = new URL(uri);
		final HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");
		addReqHeaders(con, headers);

		pipeResponse(con,res);
	}

	public void post(final String uri, final String body, Map<String, List<Object>> headers, Response res) throws IOException {
		final URL obj = new URL(uri);
		final HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is POST
		con.setRequestMethod("POST");
		addReqHeaders(con, headers);
		// Send post requestImpl
		con.setDoOutput(true);
		final OutputStream wr = con.getOutputStream();
		try {
			wr.write(body.getBytes());
			wr.flush();
		} finally {
			wr.close();
		}
		pipeResponse(con,res);
	}

	public void put(final String uri, final String body, Map<String, List<Object>> headers, Response res) throws IOException {

		final URL obj = new URL(uri);
		final HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is PUT
		con.setRequestMethod("PUT");
		addReqHeaders(con, headers);
		// Send post requestImpl
		con.setDoOutput(true);
		final OutputStream wr = con.getOutputStream();
		try {
			wr.write(body.getBytes());
			wr.flush();
		} finally {
			wr.close();
		}
		pipeResponse(con,res);
	}

	private void pipeResponse(final HttpURLConnection con, Response res) throws IOException {
		final int responseCode = con.getResponseCode();
		res.setResponseCode(responseCode);
		Map<String, List<String>> map = con.getHeaderFields();

		try {
			int nRead = 0;
			byte[] responseData = new byte[Configuration.defaultChunkSize];
			while ((nRead = con.getInputStream().read(responseData)) != -1) {
				if (nRead == responseData.length)
					res.append(new ByteData(responseData));
				else
					res.append(new ByteData(Arrays.copyOfRange(responseData, 0, nRead)));

				responseData = new byte[Configuration.defaultChunkSize];
			} 
		} finally {
			con.getInputStream().close();
		}
		for (Map.Entry<String, List<String>> entry : map.entrySet()) {
			final String key = entry.getKey();
			List<String> value = entry.getValue();
			if (!NullCheck.isNullOrEmpty(key) && !NullCheck.isNullOrEmpty(value)){
				for( String v1:value )
					res.putHeader(key, v1);
			}
		}
	}

	public void trace(String uri, Map<String, List<Object>> headers, Response res) throws IOException {
		final URL obj = new URL(uri);
		final HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is TRACE
		con.setRequestMethod("TRACE");
		addReqHeaders(con, headers);

		pipeResponse(con,res);
	}

	public void head(String uri, Map<String, List<Object>> headers, Response res) throws IOException {
		final URL obj = new URL(uri);
		final HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is HEAD
		con.setRequestMethod("HEAD");
		addReqHeaders(con, headers);

		pipeResponse(con,res);
	}

	public void connect(String uri, Map<String, List<Object>> headers, Response res) throws IOException {
	}

	public void options(String uri, String body, Map<String, List<Object>> headers, Response res) throws IOException {
		final URL obj = new URL(uri);
		final HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is OPTIONS
		con.setRequestMethod("OPTIONS");
		addReqHeaders(con, headers);
		// Send post requestImpl
		con.setDoOutput(true);
		final OutputStream wr = con.getOutputStream();
		try {
			wr.write(body.getBytes());
			wr.flush();
		} finally {
			wr.close();
		}
		pipeResponse(con,res);
	}

}

enum MethodInvoker {
	none {

		@Override
		public void handle(Request req, Response res, boolean isStatic, Method method, Threadlocal<Object> tl,
				RequestUriTokens rut) throws Exception {
			if (isStatic)
				method.invoke(null);
			else
				method.invoke(tl.get(null));
		}

	},
	variable {

		@Override
		public void handle(final Request req, final Response res, final boolean isStatic, final Method method,
				final Threadlocal<Object> tl, final RequestUriTokens rut) throws Exception {

			final Object[] args = new Object[method.getParameters().length];
			String[] split = req.getUri().split("/");
			int arid = 0;
			for (int pi : rut.paramIdx) {
				if (pi != -1) {
					args[rut.argsIdx[arid++]] = split[pi];
				}
			}
			
			if (rut.resIdx != -1)
				args[rut.resIdx] = res;
			if (rut.reqIdx != -1)
				args[rut.reqIdx] = req;

			if (isStatic)
				method.invoke(null, args);
			else
				method.invoke(tl.get(null), args);
		}

	},
	defalt, onlyReq {

		@Override
		public void handle(Request req, Response res, boolean isStatic, Method method, Threadlocal<Object> tl,
				RequestUriTokens rut) throws Exception {
			if (isStatic)
				method.invoke(null, req);
			else
				method.invoke(tl.get(null), req);
		}

	},
	onlyRes {

		@Override
		public void handle(Request req, Response res, boolean isStatic, Method method, Threadlocal<Object> tl,
				RequestUriTokens rut) throws Exception {
			if (isStatic)
				method.invoke(null, res);
			else
				method.invoke(tl.get(null), res);
		}

	},
	reverDef {

		@Override
		public void handle(Request req, Response res, boolean isStatic, Method method, Threadlocal<Object> tl,
				RequestUriTokens rut) throws Exception {
			if (isStatic)
				method.invoke(null, res, req);
			else
				method.invoke(tl.get(null), res, req);
		}

	};

	public void handle(Request req, Response res, boolean isStatic, Method method, Threadlocal<Object> tl,
			RequestUriTokens rut) throws Exception {
		if (isStatic)
			method.invoke(null, req, res);
		else
			method.invoke(tl.get(null), req, res);
	}

}
/**
 * @author P
 *
 */
final class AsynContextImpl  implements AsynContext{
	private static final Logger logger = LoggerFactory.getLogger(AsynContextImpl.class);
	
	boolean flag = false;
	SelectionKey key;
	Selector clientSelector;
	final Request request;
	final Response response;
	ConnectionState state;

	final int threadId = Thread.currentThread().hashCode();
	
	AsynContextImpl(SelectionKey key, Request request, Response response, ConnectionState state, Selector clientSelector) {
		super();
		this.key = key;
		this.clientSelector = clientSelector;
		this.request = request;
		this.response = response;
		this.state = state;
	}

	@Override
	public void setAsynchronousFinish(boolean flag) {
		if( this.threadId == Thread.currentThread().hashCode() ){
			this.flag = flag;
			try {
				((SocketChannel) key.channel()).socket().setSoTimeout(0);
			} catch (SocketException e) {
				logger.error("Error in setSoTimeout " + this + " :: ", e);
			}
		}else{
			throw new IllegalStateException("Cannot modify ouside the created Thread!");
		}
	}

	@Override
	public boolean isAsynchronousFinish() {
		return flag;
	}

	/* (non-Javadoc)
	 * @see org.arivu.nioserver.AsynContext#getRequest()
	 */
	@Override
	public Request getRequest() {
		return request;
	}

	/* (non-Javadoc)
	 * @see org.arivu.nioserver.AsynContext#getResponse()
	 */
	@Override
	public Response getResponse() {
		return response;
	}

	/**
	 * 
	 */
	@Override
	public void finish() {
		if( flag && key.isValid()){
			state.resBuff = RequestUtil.getResponseBytes(request, response);
			if( response instanceof ResponseImpl )
				((ResponseImpl)response).done = true;
			
			logger.debug(" request :: {} response :: {}", request.toString() ,state.resBuff.cl);			
			key.interestOps(SelectionKey.OP_WRITE);
			clientSelector.wakeup();
			clientSelector = null;
			key = null;
		}
	}
	
}
/**
 * @author P
 *
 */
final class AsynContextImplJ7Nio  implements AsynContext{
	private static final Logger logger = LoggerFactory.getLogger(AsynContextImplJ7Nio.class);
	Connection connection;
	AsynchronousSocketChannel asc;
	boolean flag = false;
	final Request request;
	final Response response;
	ConnectionState state;

	final int threadId = Thread.currentThread().hashCode();
	
	AsynContextImplJ7Nio(Connection connection, Request request, Response response, ConnectionState state, AsynchronousSocketChannel asc) {
		super();
		this.connection = connection;
		this.asc = asc;
		this.request = request;
		this.response = response;
		this.state = state;
	}

	@Override
	public void setAsynchronousFinish(boolean flag) {
		if( this.threadId == Thread.currentThread().hashCode() ){
			this.flag = flag;
		}else{
			throw new IllegalStateException("Cannot modify ouside the created Thread!");
		}
	}

	@Override
	public boolean isAsynchronousFinish() {
		return flag;
	}

	/* (non-Javadoc)
	 * @see org.arivu.nioserver.AsynContext#getRequest()
	 */
	@Override
	public Request getRequest() {
		return request;
	}

	/* (non-Javadoc)
	 * @see org.arivu.nioserver.AsynContext#getResponse()
	 */
	@Override
	public Response getResponse() {
		return response;
	}

	/**
	 * 
	 */
	@Override
	public void finish() {
		if( flag ){
			state.resBuff = RequestUtil.getResponseBytes(request, response);
			if( response instanceof ResponseImpl )
				((ResponseImpl)response).done = true;
			
			logger.debug(" request :: {} response :: {}", request.toString() ,state.resBuff.cl);			
			connection.finish(asc);
			asc = null;
			connection = null;
		}
	}
	
}
final class DefaultExceptionHandler implements ExceptionHandler{
	private static final Logger logger = LoggerFactory.getLogger(DefaultExceptionHandler.class);
	
	@Override
	public void handle(Throwable t) {
		Response res = StaticRef.getResponse();
		res.setResponseCode(400);
		try {
			res.append(RequestUtil.getStackTrace(t));
		} catch (IOException e1) {
			logger.error("Failed in route " + this + " :: ", e1);
		}
	}
	
}