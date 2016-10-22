package org.arivu.nioserver;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.datastructure.DoublyLinkedSet;
import org.arivu.utils.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PackageScanner {
	static final Logger logger = LoggerFactory.getLogger(PackageScanner.class);

	static Collection<Route> getPaths(Collection<String> packageNames)
			throws ClassNotFoundException, IOException {
		Collection<Route> reqPaths = new DoublyLinkedSet<Route>();

		for (String pkgName : packageNames) {
			for (Class<?> kcs : getClassesForPackage(pkgName)) {
				addMethod(reqPaths, kcs);
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

	static void addMethod(Collection<Route> reqPaths, Class<?> clazz) {
		logger.debug("Scanning class "+clazz.getName());
		Method[] methods = clazz.getDeclaredMethods();//Methods();
		for (Method method : methods) {
			if (method.isAnnotationPresent(Path.class)) {
				Path path = method.getAnnotation(Path.class);
				if (path != null) {
					logger.debug("Scanning class "+clazz.getName()+" annotation present "+method);
					try {
						String uri = path.value();
						org.arivu.nioserver.HttpMethod httpMethod = path.httpMethod();
						if (!NullCheck.isNullOrEmpty(uri) && httpMethod != null) {
							boolean isStatic = Modifier.isStatic(method.getModifiers());
							Route e = new Route(uri, httpMethod, clazz, method, isStatic);
							boolean add = reqPaths.add(e);
							if (add) {
								logger.debug("Discovered requestImpl handler :: " + clazz.getName() + " httpMethod "
										+ method.getName());
							} else {
								logger.debug("Duplicate requestImpl handler discovered ignoring :: " + clazz.getName()
										+ " httpMethod " + method.getName());
							}
						}
					} catch (IllegalArgumentException e) {
						logger.error("Error on Scanning annotation addMethod :: ", e);
					}
				}
			}
		}
	}
}
