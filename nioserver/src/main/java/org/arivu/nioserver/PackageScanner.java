package org.arivu.nioserver;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.datastructure.DoublyLinkedSet;
import org.arivu.datastructure.MemoryMappedFiles;
import org.arivu.datastructure.Threadlocal;
import org.arivu.utils.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackageScanner {
	static final Logger logger = LoggerFactory.getLogger(PackageScanner.class);
	// public static void main(String[] args) throws ClassNotFoundException{
	// for( Class<?> kcs: getClassesForPackage("org.arivu")){
	//// reqPaths.addAll(parse(kcs));
	// System.out.println("class :: "+kcs.getName());
	// }
	// }

	public static Collection<RequestPath> getPaths(Collection<String> packageNames)
			throws ClassNotFoundException, IOException {
		Collection<RequestPath> reqPaths = new DoublyLinkedSet<RequestPath>();

		for (String pkgName : packageNames) {
			for (Class<?> kcs : getClassesForPackage(pkgName)) {
				parse(reqPaths, kcs);
			}
		}

		return reqPaths;
	}

	static Collection<Class<?>> getClassesForPackage(String pckgname) throws ClassNotFoundException {
		// This will hold a list of directories matching the pckgname. There may
		// be more than one if a package is split over multiple jars/paths
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
			throw new ClassNotFoundException(
					pckgname + " does not appear to be a valid package (Null pointer exception)");
		} catch (UnsupportedEncodingException encex) {
			throw new ClassNotFoundException(
					pckgname + " does not appear to be a valid package (Unsupported encoding)");
		} catch (IOException ioex) {
			throw new ClassNotFoundException("IOException was thrown when trying to get all resources for " + pckgname);
		}

		Collection<Class<?>> classes = new DoublyLinkedSet<Class<?>>();
		// For every directoryFile identified capture all the .class files
		while (!directories.isEmpty()) {
			File directoryFile = directories.remove(0);
			if (directoryFile.exists()) {
				// Get the list of the files contained in the package
				File[] files = directoryFile.listFiles();

				for (File file : files) {
					// we are only interested in .class files
					if ((file.getName().endsWith(".class")) && (!file.getName().contains("$"))) {
						// removes the .class extension
						int index = directoryFile.getPath().indexOf(packageToPath);
						String packagePrefix = directoryFile.getPath().substring(index).replace('/', '.');
						;
						try {
							String className = packagePrefix + '.'
									+ file.getName().substring(0, file.getName().length() - 6);
							classes.add(Class.forName(className));
						} catch (NoClassDefFoundError e) {
							// do nothing. this class hasn't been found by the
							// loader, and we don't care.
						}
					} else if (file.isDirectory()) { // If we got to a
														// subdirectory
						directories.add(new File(file.getPath()));
					}
				}
			} else {
				throw new ClassNotFoundException(
						pckgname + " (" + directoryFile.getPath() + ") does not appear to be a valid package");
			}
		}
		return classes;
	}

	// public static Collection<Class<?>> getClasses(String packageName)
	// throws ClassNotFoundException, IOException {
	// ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	// assert classLoader != null;
	// String path = packageName.replace('.', '/');
	// Enumeration<URL> resources = classLoader.getResources(path);
	// List<File> dirs = new ArrayList<File>();
	// while (resources.hasMoreElements()) {
	// URL resource = resources.nextElement();
	// dirs.add(new File(resource.getFile()));
	// }
	// Collection<Class<?>> classes = new ArrayList<Class<?>>();
	// for (File directory : dirs) {
	// classes.addAll(findClasses(directory, packageName));
	// }
	// return classes;//.toArray(new Class[classes.size()]);
	// }

	// private static Collection<Class<?>> findClasses(File directory, String
	// packageName) throws ClassNotFoundException {
	// Collection<Class<?>> classes = new ArrayList<Class<?>>();
	// if (!directory.exists()) {
	// return classes;
	// }
	// File[] files = directory.listFiles();
	// for (File file : files) {
	// if (file.isDirectory()) {
	// assert !file.getName().contains(".");
	// classes.addAll(findClasses(file, packageName + "." + file.getName()));
	// } else if (file.getName().endsWith(".class")) {
	// classes.add(Class.forName(packageName + '.' + file.getName().substring(0,
	// file.getName().length() - 6)));
	// }
	// }
	// return classes;
	// }

	static void parse(Collection<RequestPath> reqPaths, Class<?> clazz) {
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
		// return reqPaths;
	}
}

class RequestPath {
	final String uri;
	final Request.Method httpMethod;
	final Class<?> klass;
	final Method method;
	final boolean isStatic;
	final Threadlocal<Object> tl;

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

	Response getResponse(Request req,SocketChannel socketChannel){
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

class ProxyRequestPath extends RequestPath{

	String name;
	String proxy_pass;
	Map<String, Object> defaultResponseHeader;
	String dir;
	MemoryMappedFiles files = null;
	/**
	 * @param uri
	 * @param httpMethod
	 * @param klass
	 * @param method
	 * @param isStatic
	 */
	ProxyRequestPath(String name,String proxy_pass,String dir,String uri, org.arivu.nioserver.Request.Method httpMethod, Class<?> klass, Method method,
			boolean isStatic,Map<String, Object> defaultResponseHeader) {
		super(uri, httpMethod, klass, method, isStatic);
		this.name = name;
		this.proxy_pass = proxy_pass;
		this.dir = dir;
		this.defaultResponseHeader = defaultResponseHeader;
		if( NullCheck.isNullOrEmpty(proxy_pass) && NullCheck.isNullOrEmpty(dir) ){
			throw new IllegalArgumentException("Invalid config "+name+" !");
		}else if( !NullCheck.isNullOrEmpty(proxy_pass) && !NullCheck.isNullOrEmpty(dir) ){
			throw new IllegalArgumentException("Invalid config "+name+" !");
		}else if(!NullCheck.isNullOrEmpty(dir)){
			files = new MemoryMappedFiles();
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
		if(!NullCheck.isNullOrEmpty(dir)){
			// static
//			String replaceAll = this.dir.replaceAll("$home", "." );
//			System.out.println("static :: this.uri :: "+this.uri+" this.dir :: "+this.dir);
			String file = this.dir+req.uri.substring(this.uri.length());
//			String file = req.uri.replaceFirst(this.uri, this.dir);
//			System.out.println("static :: "+file);
			ByteBuffer bytes = files.getBytes(file);
			if(bytes==null){
				bytes=files.addBytes(file);
			}
			byte[] array = bytes.array();
			res.append(array);
			res.putHeader("Content-Length", array.length);
		}else{
			// proxy
			super.handle(req, res);
		}
	}

	@Override
	Response getResponse(Request req, SocketChannel socketChannel) {
		if(!NullCheck.isNullOrEmpty(dir)){
			return super.getResponse(req, socketChannel);
		}else{
			return new ProxyResponse(req, socketChannel, defaultResponseHeader);
		}
	}

	@Override
	public String toString() {
		return "ProxyRequestPath [name=" + name + ", uri=" + uri + ", httpMethod=" + httpMethod + "]";
	}
	
}