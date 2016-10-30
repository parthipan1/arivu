package org.arivu.nioserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.Threadlocal;
import org.arivu.utils.NullCheck;
import org.arivu.utils.lock.AtomicWFReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Route {
	private static final Logger logger = LoggerFactory.getLogger(Route.class);

	final String uri;
	final org.arivu.nioserver.HttpMethod httpMethod;
	Class<?> klass;
	Method method;
	final boolean isStatic;
	final MethodInvoker invoker;
	final RequestUriTokens rut;
	Threadlocal<Object> tl;
	Map<String, Object> headers = null;
	
	Route(String uri, org.arivu.nioserver.HttpMethod httpMethod) {
		this(uri, httpMethod, null, null, false);
	}

	/**
	 * @param uri
	 * @param httpMethod
	 * @param httpMethod
	 * @param klass
	 */
	Route(String uri, org.arivu.nioserver.HttpMethod httpMethod, Class<?> klass, Method method, boolean isStatic) {
		super();
		this.headers = new Amap<>(Configuration.defaultResponseHeader);
		this.uri = uri;
		this.httpMethod = httpMethod;
		this.klass = klass;
		this.method = method;
		this.isStatic = isStatic;
		if (klass != null) {
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

	boolean match(String requri) {
		if (uri.equals(requri))
			return true;

		return false;
	}

	Response getResponse(Request req) {
		return new ResponseImpl(req, headers);
	}

	public void handle(Request req, Response res) {
		try {
			StaticRef.set(req, res, this);
			this.invoker.handle(req, res, isStatic, method, tl, this.rut);
		} catch (Throwable e) {
			logger.error("Failed in route " + this + " :: ", e);
			res.setResponseCode(400);
			try {
				res.append(RequestUtil.getStackTrace(e));
			} catch (IOException e1) {
				logger.error("Failed in route " + this + " :: ", e1);
			}
		}finally {
			StaticRef.clear();
		}
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

	String name;
	String proxy_pass;
	String dir;
	Map<String,FileData> files;
	Threadlocal<HttpMethodCall> proxyTh;

	/**
	 * @param uri
	 * @param httpMethod
	 * @param klass
	 * @param httpMethod
	 * @param isStatic
	 */
	ProxyRoute(String name, String proxy_pass, String dir, String uri, org.arivu.nioserver.HttpMethod httpMethod,
			Class<?> klass, Method method, boolean isStatic, Map<String, Object> defaultResponseHeader) {
		super(uri, httpMethod, klass, method, isStatic);
		this.name = name;
		this.proxy_pass = proxy_pass;
		this.dir = dir;
		this.headers = new Amap<>(defaultResponseHeader);
		if (NullCheck.isNullOrEmpty(proxy_pass) && NullCheck.isNullOrEmpty(dir)) {
			throw new IllegalArgumentException("Invalid config " + name + " !");
		} else if (!NullCheck.isNullOrEmpty(proxy_pass) && !NullCheck.isNullOrEmpty(dir)) {
			throw new IllegalArgumentException("Invalid config " + name + " !");
		} else if (!NullCheck.isNullOrEmpty(dir)) {
//			files = new MemoryMappedFiles();
			files = new Amap<>();
		} else if (!NullCheck.isNullOrEmpty(proxy_pass)) {
			this.proxyTh = new Threadlocal<HttpMethodCall>(new Threadlocal.Factory<HttpMethodCall>() {

				@Override
				public HttpMethodCall create(Map<String, Object> params) {
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
	public void handle(Request req, Response res) {
		try {
			if (!NullCheck.isNullOrEmpty(dir)) {
				handleDirectory(req, res);
			} else {
				handleProxy(req, res);
			}
		} catch (Throwable e) {
			logger.error("Failed in route " + this + " :: ", e);
			res.setResponseCode(400);
			try {
				res.append(RequestUtil.getStackTrace(e));
			} catch (IOException e1) {
				logger.error("Failed in route " + this + " :: ", e1);
			}
		}
	}

	void handleProxy(Request req, Response res) throws IOException {
		String queryStr = URLDecoder.decode(req.getUriWithParams().substring(req.getUriWithParams().indexOf("?")),
				RequestUtil.ENC_UTF_8);
		String loc = this.proxy_pass + req.getUri().substring(this.uri.length()) + queryStr;
		// System.out.println("loc :: " + loc);
		HttpMethodCall httpMethodCall = proxyTh.get(null);
		ProxyRes pres = null;
		switch (req.getMethod()) {
		case HEAD:
			pres = httpMethodCall.head(loc, req.getHeaders());
			break;
		case OPTIONS:
			pres = httpMethodCall.options(loc, RequestUtil.convert(req.getBody()) , req.getHeaders());
			break;
		case CONNECT:
			pres = httpMethodCall.connect(loc, req.getHeaders());
			break;
		case TRACE:
			pres = httpMethodCall.trace(loc, req.getHeaders());
			break;
		case GET:
			pres = httpMethodCall.get(loc, req.getHeaders());
			break;
		case POST:
			pres = httpMethodCall.post(loc, RequestUtil.convert(req.getBody()), req.getHeaders());
			break;
		case PUT:
			pres = httpMethodCall.put(loc, RequestUtil.convert(req.getBody()), req.getHeaders());
			break;
		case DELETE:
			pres = httpMethodCall.delete(loc, req.getHeaders());
			break;
		default:
			break;
		}
		if (pres != null) {
			res.setResponseCode(pres.responseCode);
			res.append(pres.response);
			res.putAllHeader(pres.headers);
		}
	}

	final Lock readLock = new AtomicWFReentrantLock();
	
	void handleDirectory(Request req, Response res) throws IOException {
		String file = this.dir + URLDecoder.decode(req.getUri().substring(this.uri.length()), RequestUtil.ENC_UTF_8);
		File file2 = new File(file);
		File f = file2;
		if (!f.exists()) {
			res.setResponseCode(404);
		} else if (f.isDirectory()) {
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
		} else {
			if (!NullCheck.isNullOrEmpty(Configuration.defaultMimeType)) {
				String[] split = f.getName().split("\\.(?=[^\\.]+$)");
				final String ext = "." + split[split.length - 1];
				Map<String, Object> map = Configuration.defaultMimeType.get(ext.toLowerCase(Locale.getDefault()));
				if (map != null) {
					Object typeObj = map.get("type");
					if (typeObj != null) 
						res.putHeader("Content-Type", typeObj);
					
				}
			}
			ByteData bytes = getWr(files.get(file)) ;//getOriginalBytes(file);
			if (bytes == null) {
				readLock.lock();
				bytes = getWr(files.get(file));
				if( bytes == null ){
					try {
						byte[] data = RequestUtil.read(file2);//new byte[bb.remaining()];
						if (data!=null) {
							bytes = new ByteData(data);
							files.put(file, new FileData(new WeakReference<ByteData>(bytes), file2));
						}
					} finally {
						readLock.unlock();
					}
				}else{
					readLock.unlock();
				}
			}
			if (bytes != null) {
				byte[] array = bytes.array();//bytes.array();//new byte[bytes.remaining()];
				//			bytes.get(array, 0, array.length);
				res.append(array);
				res.putHeader("Content-Length", array.length);
			}else{
				res.setResponseCode(404);
			}
		}
	}

	ByteData getWr(FileData ref){
		if( ref == null ) return null;
		else{
			if(!ref.file.exists()){
				files.remove(ref.file.getAbsolutePath());
				return null;
			}else if ( ref.time < ref.file.lastModified() ){
				files.remove(ref.file.getAbsolutePath());
				return null;
			}else{
				return ref.data.get();
			}
		} 
	}
	
	@Override
	Response getResponse(Request req) {
		if (!NullCheck.isNullOrEmpty(dir)) {
			return super.getResponse(req);
		} else {
			return new ResponseImpl(req, headers);
		}
	}

	@Override
	public String toString() {
		return "ProxyRoute [name=" + name + ", uri=" + uri + ", httpMethod=" + httpMethod + "]";
	}

}
final class AdminRoute extends ProxyRoute {
//	private static final Logger logger = LoggerFactory.getLogger(AdminRoute.class);
	
	AdminRoute() {
		super("adminSite", null, Configuration.ADMIN_LOC, "/admin", HttpMethod.ALL, null, null, false, Configuration.defaultResponseHeader);
	}

	@Override
	public void handle(Request req, Response res) {
		if("/admin".equals(req.getUri())){
			res.sendRedirect("/admin/Admin.html");
		}else{
			super.handle(req, res);
		}
	}
}
final class FileData{
	final long time;
	final WeakReference<ByteData> data;
	final File file;
	FileData(WeakReference<ByteData> data,File file) {
		super();
		this.data = data;
		this.file = file;
		this.time = file.lastModified();
	}
	
}
final class ProxyRes {
	final String response;
	final int responseCode;
	Map<String, String> headers = new Amap<String, String>();

	ProxyRes(int responseCode, String response) {
		super();
		this.responseCode = responseCode;
		this.response = response;
	}

}

interface HttpMethodCall {
	ProxyRes trace(String uri, Map<String, String> headers) throws IOException;

	ProxyRes head(String uri, Map<String, String> headers) throws IOException;

	ProxyRes connect(String uri, Map<String, String> headers) throws IOException;

	ProxyRes options(String uri, String body, Map<String, String> headers) throws IOException;

	ProxyRes get(String uri, Map<String, String> headers) throws IOException;

	ProxyRes post(String uri, String body, Map<String, String> headers) throws IOException;

	ProxyRes put(String uri, String body, Map<String, String> headers) throws IOException;

	ProxyRes delete(String uri, Map<String, String> headers) throws IOException;
}

class JavaHttpMethodCall implements HttpMethodCall {

	@Override
	public ProxyRes delete(String uri, Map<String, String> headers) throws IOException {
		// System.out.println("DELETE "+uri);
		final URL obj = new URL(uri);
		final HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		// optional default is GET
		con.setRequestMethod("DELETE");
		// add requestImpl header
		addReqHeaders(con, headers);

		return extractResponse(con);
	}

	private void addReqHeaders(final HttpURLConnection con, Map<String, String> headers) {
		con.setRequestProperty("User-Agent", "Java8");
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		if (!NullCheck.isNullOrEmpty(headers)) {
			Set<Entry<String, String>> entrySet = headers.entrySet();
			for (Entry<String, String> e : entrySet) {
				con.setRequestProperty(e.getKey(), e.getValue());
			}
		}
	}

	@Override
	public ProxyRes get(final String uri, Map<String, String> headers) throws IOException {
		final URL obj = new URL(uri);
		final HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");
		addReqHeaders(con, headers);

		return extractResponse(con);
	}

	@Override
	public ProxyRes post(final String uri, final String body, Map<String, String> headers) throws IOException {
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
		return extractResponse(con);
	}

	@Override
	public ProxyRes put(final String uri, final String body, Map<String, String> headers) throws IOException {

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
		return extractResponse(con);
	}

	private ProxyRes extractResponse(final HttpURLConnection con) throws IOException {
		int responseCode = con.getResponseCode();

		Map<String, List<String>> map = con.getHeaderFields();

		final StringBuffer response = new StringBuffer();
		final BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		try {
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
		} finally {
			in.close();
		}
		ProxyRes proxyRes = new ProxyRes(responseCode, response.toString());
		for (Map.Entry<String, List<String>> entry : map.entrySet()) {
			final String key = entry.getKey();
			if (!NullCheck.isNullOrEmpty(key) && !NullCheck.isNullOrEmpty(entry.getValue()))
				proxyRes.headers.put(key, entry.getValue().get(0));
		}
		return proxyRes;
	}

	@Override
	public ProxyRes trace(String uri, Map<String, String> headers) throws IOException {
		final URL obj = new URL(uri);
		final HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is TRACE
		con.setRequestMethod("TRACE");
		addReqHeaders(con, headers);

		return extractResponse(con);
	}

	@Override
	public ProxyRes head(String uri, Map<String, String> headers) throws IOException {
		final URL obj = new URL(uri);
		final HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is HEAD
		con.setRequestMethod("HEAD");
		addReqHeaders(con, headers);

		return extractResponse(con);
	}

	@Override
	public ProxyRes connect(String uri, Map<String, String> headers) throws IOException {
		// TODO Auto-generated httpMethod stub
		return null;
	}

	@Override
	public ProxyRes options(String uri, String body, Map<String, String> headers) throws IOException {
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
		return extractResponse(con);
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
			
//			System.out.println(" rut uriParts   size "+rut.uriParts.size());
//			for (int i = 0; i < rut.uriParts.size(); i++) {
//				System.out.println("  uri  "+i+" "+rut.uriParts.get(i));
//			}
//			System.out.println(" rut tokenParts size "+rut.tokenParts.size());
//			for (int i = 0; i < rut.tokenParts.size(); i++) {
//				System.out.println("  token "+i+" "+rut.tokenParts.get(i));
//			}
			
//			final String inUrl = req.getUri();
//			int idx = rut.uriParts.get(0).length();
//			for (int i = 0; i < rut.tokenParts.size(); i++) {
//				int endIndex = inUrl.indexOf(rut.uriParts.get(i + 1), idx);
//				if (i == rut.tokenParts.size() - 1) {
//					if (rut.tokenParts.size() == rut.uriParts.size() + 1) {
//						args[rut.tokenParts.get(i).indx] = inUrl.substring(idx, endIndex);
//					} else {
//						args[rut.tokenParts.get(i).indx] = inUrl.substring(idx, endIndex);
//					}
//				} else {
//					args[rut.tokenParts.get(i).indx] = inUrl.substring(idx, endIndex);
//				}
//				idx = endIndex;
//			}

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