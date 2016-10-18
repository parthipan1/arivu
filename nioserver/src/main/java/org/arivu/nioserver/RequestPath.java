package org.arivu.nioserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.MemoryMappedFiles;
import org.arivu.datastructure.Threadlocal;
import org.arivu.utils.NullCheck;

class RequestPath {
	final String uri;
	final org.arivu.nioserver.HttpMethod httpMethod;
	final Class<?> klass;
	final Method method;
	final boolean isStatic;
	Threadlocal<Object> tl;

	RequestPath(String uri, org.arivu.nioserver.HttpMethod httpMethod) {
		this(uri, httpMethod, null, null, false);
	}

	/**
	 * @param uri
	 * @param httpMethod
	 * @param klass
	 * @param httpMethod
	 */
	RequestPath(String uri, org.arivu.nioserver.HttpMethod httpMethod, Class<?> klass, Method method,
			boolean isStatic) {
		super();
		this.uri = uri;
		this.httpMethod = httpMethod;
		this.klass = klass;
		this.method = method;
		this.isStatic = isStatic;
		if (klass != null) {
			this.tl = new Threadlocal<Object>(new Threadlocal.Factory<Object>() {

				@Override
				public Object create(Map<String, Object> params) {
					try {
						return RequestPath.this.klass.newInstance();
					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
					return null;
				}
			}, 30000);
		} else {
			this.tl = null;
		}
	}

	ResponseImpl getResponse(Request req, SocketChannel socketChannel) {
		return new ResponseImpl(req, socketChannel, Configuration.defaultResponseHeader);
	}

	public void handle(Request req, Response res) throws Exception {
		if (isStatic)
			method.invoke(null, req, res);
		else
			method.invoke(tl.get(null), req, res);
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
		RequestPath other = (RequestPath) obj;
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
		return "RequestPath [uri=" + uri + ", httpMethod=" + httpMethod + ", klass=" + klass + ", method=" + method
				+ ", isStatic=" + isStatic + "]";
	}

}

final class ProxyRequestPath extends RequestPath {

	String name;
	String proxy_pass;
	Map<String, Object> defaultResponseHeader;
	String dir;
	MemoryMappedFiles files = null;
	Threadlocal<HttpMethodCall> proxyTh;

	/**
	 * @param uri
	 * @param httpMethod
	 * @param klass
	 * @param httpMethod
	 * @param isStatic
	 */
	ProxyRequestPath(String name, String proxy_pass, String dir, String uri,
			org.arivu.nioserver.HttpMethod httpMethod, Class<?> klass, Method method, boolean isStatic,
			Map<String, Object> defaultResponseHeader) {
		super(uri, httpMethod, klass, method, isStatic);
		this.name = name;
		this.proxy_pass = proxy_pass;
		this.dir = dir;
		this.defaultResponseHeader = defaultResponseHeader;
		if (NullCheck.isNullOrEmpty(proxy_pass) && NullCheck.isNullOrEmpty(dir)) {
			throw new IllegalArgumentException("Invalid config " + name + " !");
		} else if (!NullCheck.isNullOrEmpty(proxy_pass) && !NullCheck.isNullOrEmpty(dir)) {
			throw new IllegalArgumentException("Invalid config " + name + " !");
		} else if (!NullCheck.isNullOrEmpty(dir)) {
			files = new MemoryMappedFiles();
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
	ProxyRequestPath(String uri, org.arivu.nioserver.HttpMethod httpMethod) {
		super(uri, httpMethod);
	}

	@Override
	public void handle(Request req, Response res) throws Exception {
		if (!NullCheck.isNullOrEmpty(dir)) {
			String file = this.dir + req.getUri().substring(this.uri.length());
			File f = new File(file);
			if(!f.exists()){
				res.setResponseCode(404);
			}else if(f.isDirectory()){
				File[] listFiles = f.listFiles();
				StringBuffer buf = new StringBuffer("<html><body>");
				buf.append("<a href=\"").append("..").append("\" >").append("..").append("</a>").append("<br>");
				for(File f1:listFiles){
					buf.append("<a href=\"").append(f1.getName()).append("\" >").append(f1.getName()).append("</a>").append("<br>");
				}
				buf.append("</body></html>");
				res.setResponseCode(200);
				res.append(buf.toString());
//				res.putHeader("Content-Type", "text/html;charset=UTF-8");
				res.putHeader("Content-Length", buf.length());
			}else{
				String content = files.get(file);
				if (content == null) {
					files.addBytes(file);
					content = files.get(file);
				}
				res.append(content);
				res.putHeader("Content-Length", content.length());
			}
		} else {
			String queryStr = req.getUriWithParams().substring(req.getUriWithParams().indexOf("?"));
			String loc = this.proxy_pass + req.getUri().substring(this.uri.length()) + queryStr;
//			logger.debug("loc :: " + loc);
			HttpMethodCall httpMethodCall = proxyTh.get(null);
			ProxyRes pres = null;
			switch (req.getMethod()) {
			case HEAD:
				pres = httpMethodCall.head(loc, req.getHeaders());
				break;
			case OPTIONS:
				pres = httpMethodCall.options(loc, req.getBody(), req.getHeaders());
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
				pres = httpMethodCall.post(loc, req.getBody(), req.getHeaders());
				break;
			case PUT:
				pres = httpMethodCall.put(loc, req.getBody(), req.getHeaders());
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
	}

	@Override
	ResponseImpl getResponse(Request req, SocketChannel socketChannel) {
		if (!NullCheck.isNullOrEmpty(dir)) {
			return super.getResponse(req, socketChannel);
		} else {
			return new ResponseImpl(req, socketChannel, defaultResponseHeader);
		}
	}

	@Override
	public String toString() {
		return "ProxyRequestPath [name=" + name + ", uri=" + uri + ", httpMethod=" + httpMethod + "]";
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
			proxyRes.headers.put(entry.getKey(), entry.getValue().get(0));
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