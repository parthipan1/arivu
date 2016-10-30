package org.arivu.nioserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.datastructure.DoublyLinkedSet;
import org.arivu.utils.NullCheck;
import org.arivu.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PackageScanner {
	static final Logger logger = LoggerFactory.getLogger(PackageScanner.class);

	static Collection<Route> getPaths(String name, Collection<String> packageNames) throws ClassNotFoundException, IOException {
		Collection<Route> reqPaths = new DoublyLinkedSet<Route>();

		for (String pkgName : packageNames) {
			getPaths(reqPaths, pkgName, name);
		}

		return reqPaths;
	}

	static void getPaths(Collection<Route> reqPaths, String pkgName, String name) throws ClassNotFoundException {
		for (Class<?> kcs : getClassesForPackage(Thread.currentThread().getContextClassLoader(), pkgName, false)) {
			addMethod(name, reqPaths, kcs);
		}
	}

	static Collection<Class<?>> getClassesForPackage(ClassLoader cld, String pckgname, boolean isHotDeploy) throws ClassNotFoundException {
		List<File> directories = new DoublyLinkedList<File>();
		String packageToPath = pckgname.replace('.', '/');
		try {
			// ClassLoader cld = Thread.currentThread().getContextClassLoader();
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
			String path = directoryFile.getPath();
			if (directoryFile.exists()) {
				File[] files = directoryFile.listFiles();

				for (File file : files) {
					if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
						int index = path.indexOf(packageToPath);
						String packagePrefix = path.substring(index).replace('/', '.');
						try {
							String className = packagePrefix + '.'
									+ file.getName().substring(0, file.getName().length() - 6);
							if ("org.arivu.nioserver.Configuration.class".equals(className)
									|| "org.arivu.nioserver.PackageScanner.class".equals(className)) {
								continue;
							} else {
								classes.add(Class.forName(className));
							}
						} catch (NoClassDefFoundError e) {
							logger.error("Error on Scanning annotation :: ", e);
						}
					} else if (file.isDirectory()) {
						directories.add(new File(file.getPath()));
					}
				}
			} else {
				int indexOf = path.indexOf("!");
				if (indexOf == -1) {
					logger.error(pckgname + " (" + path + ") does not appear to be a valid package");
					throw new ClassNotFoundException(
							pckgname + " (" + path + ") does not appear to be a valid package");
				} else {
					classes.addAll(getClasseNamesInPackage(cld, path.substring(0, indexOf), pckgname, isHotDeploy));
				}
			}
		}
		return classes;
	}

	static Collection<Class<?>> getClasseNamesInPackage(ClassLoader cld, String jarName, String packageName, boolean isHotDeploy) {
		Collection<Class<?>> classes = new DoublyLinkedList<Class<?>>();
		jarName = Utils.replaceAll(jarName, "file:", "");// jarName.replaceFirst("file:",
															// "");
		packageName = packageName.replaceAll("\\.", "/");
		try (JarInputStream jarFile = new JarInputStream(new FileInputStream(new File(jarName)));) {
			JarEntry jarEntry;
			while ((jarEntry = jarFile.getNextJarEntry()) != null) {
				if ((jarEntry.getName().startsWith(packageName)) && (jarEntry.getName().endsWith(".class"))) {
					String className = jarEntry.getName().replaceAll("/", "\\.");
					if ("org.arivu.nioserver.Configuration.class".equals(className)
							|| "org.arivu.nioserver.PackageScanner.class".equals(className)) {
						continue;
					} else {
						try {
							if(isHotDeploy){
								classes.add(Class.forName (Utils.replaceAll(className, ".class", ""), true, cld));
							}else{
								classes.add(Class.forName(Utils.replaceAll(className, ".class", "")));
							}
						} catch (NoClassDefFoundError e) {
							logger.error("Error on Scanning annotation :: ", e);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error on Scanning getClasseNamesInPackage :: ", e);
		}
		return classes;
	}

	static void addMethod(String name, Collection<Route> reqPaths, Class<?> clazz) {
		logger.debug("Scanning class " + clazz.getName());
		Method[] methods = clazz.getDeclaredMethods();// Methods();
		for (Method method : methods) {
			if (method.isAnnotationPresent(Path.class)) {
				Path path = method.getAnnotation(Path.class);
				if (path != null) {
					logger.debug("Scanning class " + clazz.getName() + " annotation present " + method);
					try {
						String uri = path.value();
						org.arivu.nioserver.HttpMethod httpMethod = path.httpMethod();
						if (!NullCheck.isNullOrEmpty(uri) && httpMethod != null) {
							boolean validateRouteUri = RequestUtil.validateRouteUri(uri);
							if (uri.equals("/*") || uri.equals("/favicon.ico") || validateRouteUri) {
								boolean isStatic = Modifier.isStatic(method.getModifiers());
								Route e = new Route(name, uri, httpMethod, clazz, method, isStatic);
								Route matchingRoute = RequestUtil.getMatchingRoute(reqPaths, uri, httpMethod, true);
								if (matchingRoute == null) {
									reqPaths.add(e);
									logger.debug("Discovered request handler :: " + clazz.getName() + " httpMethod "
											+ method.getName());
								} else {
									logger.info("Duplicate request handler discovered ignoring :: " + clazz.getName()
											+ " httpMethod " + method.getName());
								}
							} else {
								logger.info("Invalid request Uri (" + uri + ") handler discovered ignoring :: "
										+ clazz.getName() + " httpMethod " + method.getName());
							}
						} else {
							logger.info("Invalid request Uri (" + uri + ") handler discovered ignoring :: "
									+ clazz.getName() + " httpMethod " + method.getName());

						}
					} catch (IllegalArgumentException e) {
						logger.error("Error on Scanning annotation addMethod :: ", e);
					}
				}
			}
		}
	}

}
