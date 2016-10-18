package org.arivu.nioserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.datastructure.DoublyLinkedSet;
import org.arivu.datastructure.MemoryMappedFiles;
import org.arivu.datastructure.Threadlocal;
import org.arivu.utils.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackageScanner {
	static final Logger logger = LoggerFactory.getLogger(PackageScanner.class);

	public static Collection<RequestPath> getPaths(Collection<String> packageNames)
			throws ClassNotFoundException, IOException {
		Collection<RequestPath> reqPaths = new DoublyLinkedSet<RequestPath>();

		for (String pkgName : packageNames) {
			for (Class<?> kcs : getClassesForPackage(pkgName)) {
				AddMethod(reqPaths, kcs);
			}
		}

		return reqPaths;
	}

	static Collection<Class<?>> getClassesForPackage(String pckgname) throws ClassNotFoundException {
		List<File> directories = new DoublyLinkedList<File>();
		String packageToPath = pckgname.replace('.', '/');
		try {
			ClassLoader cld = Thread.currentThread().getContextClassLoader();
			if (cld == null) {
				throw new ClassNotFoundException("Can't get class loader.");
			}

			// Ask for all resources for the packageToPath
			Enumeration<URL> resources = cld.getResources(packageToPath);
			while (resources.hasMoreElements()) {
				directories.add(new File(URLDecoder.decode(resources.nextElement().getPath(), "UTF-8")));
			}
		} catch (NullPointerException x) {
			logger.error("Error on Scanning annotation :: ", x);
			throw new ClassNotFoundException(
					pckgname + " does not appear to be a valid package (Null pointer exception)");
		} catch (UnsupportedEncodingException encex) {
			logger.error("Error on Scanning annotation :: ", encex);
			throw new ClassNotFoundException(
					pckgname + " does not appear to be a valid package (Unsupported encoding)");
		} catch (IOException ioex) {
			logger.error("Error on Scanning annotation :: ", ioex);
			throw new ClassNotFoundException("IOException was thrown when trying to get all resources for " + pckgname);
		}

		Collection<Class<?>> classes = new DoublyLinkedSet<Class<?>>();
		while (!directories.isEmpty()) {
			File directoryFile = directories.remove(0);
			if (directoryFile.exists()) {
				File[] files = directoryFile.listFiles();

				for (File file : files) {
					if ( file.getName().endsWith(".class") && !file.getName().contains("$") ) {
						int index = directoryFile.getPath().indexOf(packageToPath);
						String packagePrefix = directoryFile.getPath().substring(index).replace('/', '.');
						try {
							String className = packagePrefix + '.'
									+ file.getName().substring(0, file.getName().length() - 6);
							classes.add(Class.forName(className));
						} catch (NoClassDefFoundError e) {
							logger.error("Error on Scanning annotation :: ", e);
						}
					} else if (file.isDirectory()) { // If we got to a
														// subdirectory
						directories.add(new File(file.getPath()));
					}
				}
			} else {
				logger.error(
						pckgname + " (" + directoryFile.getPath() + ") does not appear to be a valid package");
				throw new ClassNotFoundException(
						pckgname + " (" + directoryFile.getPath() + ") does not appear to be a valid package");
			}
		}
		return classes;
	}

	static void AddMethod(Collection<RequestPath> reqPaths, Class<?> clazz) {
		Method[] methods = clazz.getMethods();
		for (Method method : methods) {
			if (method.isAnnotationPresent(Path.class)) {
				Path path = method.getAnnotation(Path.class);
				if (path != null) {
					try {
						String uri = path.value();
						Request.Method httpMethod = path.method();
						if (!NullCheck.isNullOrEmpty(uri) && httpMethod != null) {
							boolean isStatic = Modifier.isStatic(method.getModifiers());
							RequestPath e = new RequestPath(uri, httpMethod, clazz, method, isStatic);
							boolean add = reqPaths.add(e);
							if (add) {
								logger.info("Discovered request handler :: " + clazz.getName() + " method "
										+ method.getName());
							} else {
								logger.info("Duplicate request handler discovered ignoring :: " + clazz.getName()
										+ " method " + method.getName());
							}
						}
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}

class RequestPath {
	final String uri;
	final Request.Method httpMethod;
	final Class<?> klass;
	final Method method;
	final boolean isStatic;
	Threadlocal<Object> tl;

	RequestPath(String uri, org.arivu.nioserver.Request.Method httpMethod) {
		this(uri, httpMethod, null, null, false);
	}

	/**
	 * @param uri
	 * @param httpMethod
	 * @param klass
	 * @param method
	 */
	RequestPath(String uri, org.arivu.nioserver.Request.Method httpMethod, Class<?> klass, Method method,
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
			}, -1);
		} else {
			this.tl = null;
		}
	}

	Response getResponse(Request req, SocketChannel socketChannel) {
		return new Response(req, socketChannel, Configuration.defaultResponseHeader);
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

}

final class ProxyRequestPath extends RequestPath {

	String name;
	String proxy_pass;
	Map<String, Object> defaultResponseHeader;
	String dir;
	MemoryMappedFiles files = null;
	Threadlocal<HttpMethods> proxyTh;

	/**
	 * @param uri
	 * @param httpMethod
	 * @param klass
	 * @param method
	 * @param isStatic
	 */
	ProxyRequestPath(String name, String proxy_pass, String dir, String uri,
			org.arivu.nioserver.Request.Method httpMethod, Class<?> klass, Method method, boolean isStatic,
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
			this.proxyTh = new Threadlocal<HttpMethods>(new Threadlocal.Factory<HttpMethods>() {

				@Override
				public HttpMethods create(Map<String, Object> params) {
					return new JavaHttpMethods();
				}
			}, -1);
		}

	}

	/**
	 * @param uri
	 * @param httpMethod
	 */
	ProxyRequestPath(String uri, org.arivu.nioserver.Request.Method httpMethod) {
		super(uri, httpMethod);
	}

	@Override
	public void handle(Request req, Response res) throws Exception {
		if (!NullCheck.isNullOrEmpty(dir)) {
			String file = this.dir + req.uri.substring(this.uri.length());
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
			String queryStr = req.uriWithParams.substring(req.uriWithParams.indexOf("?"));
			String loc = this.proxy_pass + req.uri.substring(this.uri.length()) + queryStr;
			System.out.println("loc :: " + loc);
			HttpMethods httpMethods = proxyTh.get(null);
			ProxyRes pres = null;
			switch (req.method) {
			case HEAD:
				pres = httpMethods.head(loc, req.headers);
				break;
			case OPTIONS:
				pres = httpMethods.options(loc, req.body, req.headers);
				break;
			case CONNECT:
				pres = httpMethods.connect(loc, req.headers);
				break;
			case TRACE:
				pres = httpMethods.trace(loc, req.headers);
				break;
			case GET:
				pres = httpMethods.get(loc, req.headers);
				break;
			case POST:
				pres = httpMethods.post(loc, req.body, req.headers);
				break;
			case PUT:
				pres = httpMethods.put(loc, req.body, req.headers);
				break;
			case DELETE:
				pres = httpMethods.delete(loc, req.headers);
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
	Response getResponse(Request req, SocketChannel socketChannel) {
		if (!NullCheck.isNullOrEmpty(dir)) {
			return super.getResponse(req, socketChannel);
		} else {
			return new Response(req, socketChannel, defaultResponseHeader);
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

interface HttpMethods {
	ProxyRes trace(String uri, Map<String, String> headers) throws IOException;
	
	ProxyRes head(String uri, Map<String, String> headers) throws IOException;
	
	ProxyRes connect(String uri, Map<String, String> headers) throws IOException;
	
	ProxyRes options(String uri, String body, Map<String, String> headers) throws IOException;
	
	ProxyRes get(String uri, Map<String, String> headers) throws IOException;

	ProxyRes post(String uri, String body, Map<String, String> headers) throws IOException;

	ProxyRes put(String uri, String body, Map<String, String> headers) throws IOException;

	ProxyRes delete(String uri, Map<String, String> headers) throws IOException;
}

class JavaHttpMethods implements HttpMethods {

	@Override
	public ProxyRes delete(String uri, Map<String, String> headers) throws IOException {
		// System.out.println("DELETE "+uri);
		final URL obj = new URL(uri);
		final HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		// optional default is GET
		con.setRequestMethod("DELETE");
		// add request header
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
		// Send post request
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
		// Send post request
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ProxyRes options(String uri, String body, Map<String, String> headers) throws IOException {
		final URL obj = new URL(uri);
		final HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is OPTIONS
		con.setRequestMethod("OPTIONS");
		addReqHeaders(con, headers);
		// Send post request
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