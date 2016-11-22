/**
 * 
 */
package org.arivu.nioserver;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.script.ScriptException;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.datastructure.DoublyLinkedSet;
import org.arivu.utils.Ason;
import org.arivu.utils.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mr P
 *
 */
final class Admin {
	private static final File ICON_FILE = new File("favicon.ico");

	private static final String HASH_HEADER = "X-HASH";

	private static final Logger logger = LoggerFactory.getLogger(Admin.class);

	static final Map<String, App> allHotDeployedArtifacts = new Amap<String, App>();

	static boolean isOriginateFromAdminPage(Request request) {
		List<Object> list = request.getHeaders().get("Referer");
		if (!NullCheck.isNullOrEmpty(list)) {
			boolean endsWith = list.get(0).toString().endsWith("/admin/Admin.html");
			if(endsWith){
				List<Object> listHash = request.getHeaders().get(HASH_HEADER);
				if (!NullCheck.isNullOrEmpty(listHash)) {
					return isHashMatching(listHash.get(0).toString());
				}
			}
//			return endsWith;
		}
		return false;
	}

	static boolean isHashMatching(String clientHash) {
//		SelectionKey key = StaticRef.getRemoteHostAddress()
//		if (key!=null) {
			InetAddress remoteHostAddress = StaticRef.getRemoteHostAddress();//((SocketChannel) key.channel()).socket().getInetAddress();
			if (remoteHostAddress!=null) {
				String keyv = remoteHostAddress.toString();
//				System.err.println("\n\n************** auth token isHashMatching "+keyv+"\n\n");
				String serverHash = AdminRoute.authTokens.get(keyv);
				if (!NullCheck.isNullOrEmpty(serverHash)) {
					logger.debug("Hash auth server hash {} client hash{}", serverHash, clientHash);
					return Long.parseLong(clientHash.trim())>=Long.parseLong(serverHash);
				} 
			}
//		}
		return false;
	}

	@Path(value = "/__admin/routes", httpMethod = HttpMethod.POST)
	static void addProxyRoute() throws IOException, ScriptException {
		Response response = StaticRef.getResponse();
		Request request = StaticRef.getRequest();

		if (!isOriginateFromAdminPage(request)) {
			response.setResponseCode(401);
			return;
		}

		String convert = RequestUtil.convert(request.getBody());
		logger.debug("addProxyRoute json -> {}", convert);
		Map<String, Object> fromJson = new Ason().fromJson(convert);
		if (!NullCheck.isNullOrEmpty(fromJson)) {
			Object uriObj = fromJson.get("uri");
			Object nameObj = fromJson.get("name");
			Object locObj = fromJson.get("loc");
			Object typeObj = fromJson.get("type");
			if (uriObj != null && nameObj != null && locObj != null && typeObj != null) {
				Route route = getRoute(uriObj.toString(), nameObj.toString());
				if (route == null) {
					if (typeObj.toString().equals("browser")) {
						RequestUtil.addProxyRouteRuntime(nameObj.toString(), "ALL", uriObj.toString(), null,
								locObj.toString(), Configuration.routes, null);
					} else {
						RequestUtil.addProxyRouteRuntime(nameObj.toString(), "ALL", uriObj.toString(),
								locObj.toString(), null, Configuration.routes, null);
					}
					StringBuffer buf = getAllActiveRoutes();
					response.append(buf.toString());
					response.putHeader("Content-Length", buf.length());
					response.putHeader("Content-Type", "application/json");
					response.setResponseCode(200);
					return;
				}
			}
		}
		response.setResponseCode(304);
	}

	@Path(value = "/__admin/routes", httpMethod = HttpMethod.PUT)
	static void disableRoute() throws IOException, ScriptException {
		Response response = StaticRef.getResponse();
		Request request = StaticRef.getRequest();

		if (!isOriginateFromAdminPage(request)) {
			response.setResponseCode(401);
			return;
		}

		String convert = RequestUtil.convert(request.getBody());
		logger.debug("disable/enable route json -> %{}%", convert);
		Map<String, Object> fromJson = new Ason().fromJson(convert);
		if (!NullCheck.isNullOrEmpty(fromJson)) {
			Object uriObj = fromJson.get("uri");
			Object methodObj = fromJson.get("method");
			Object activeObj = fromJson.get("active");
			if (uriObj != null && methodObj != null && activeObj != null) {
				Route route = getRoute(uriObj.toString(), methodObj.toString());
				if (route != null) {
					boolean a = Boolean.parseBoolean(activeObj.toString());
					if (a)
						route.enable();
					else
						route.disable();

					StringBuffer buf = getAllActiveRoutes();
					response.append(buf.toString());
					response.putHeader("Content-Length", buf.length());
					response.putHeader("Content-Type", "application/json");
					response.setResponseCode(200);
					return;
				}
			}
		}
		response.setResponseCode(304);
	}

	@Path(value = "/__admin/routes", httpMethod = HttpMethod.GET)
	static void allRoutes() throws IOException {
		Response response = StaticRef.getResponse();
		Request request = StaticRef.getRequest();
		if (!isOriginateFromAdminPage(request)) {
			response.setResponseCode(ResponseCodes.Unauthorized);
			return;
		}
		StringBuffer buf = getAllActiveRoutes();
		response.append(buf.toString());
		response.putHeader("Content-Length", buf.length());
		response.putHeader("Content-Type", "application/json");
		response.setResponseCode(200);
	}

	@Path(value = "/__admin/apps", httpMethod = HttpMethod.GET)
	static void allApps() throws IOException {
		Response response = StaticRef.getResponse();
		Request request = StaticRef.getRequest();
		if (!isOriginateFromAdminPage(request)) {
			response.setResponseCode(401);
			return;
		}
		StringBuffer buf = new StringBuffer("[");
		Set<Entry<String, App>> entrySet = allHotDeployedArtifacts.entrySet();
		for (Entry<String, App> e : entrySet) {
			if (buf.length() > 1) {
				buf.append(",");
			}
			buf.append("{\"name\":\"" + e.getKey() + "\"}");
		}
		buf.append("]");
		response.append(buf.toString());
		response.putHeader("Content-Length", buf.length());
		response.putHeader("Content-Type", "application/json");
		response.setResponseCode(200);
	}

	static Route getRoute(String uri, String method) {
		for (Route route : Configuration.routes) {
			if (route.uri.equals(uri) && route.httpMethod.toString().equals(method))
				return route;
		}
		return null;
	}

	static StringBuffer getAllActiveRoutes() {
		Collection<Route> routes = Configuration.routes;
		StringBuffer buf = new StringBuffer("[");
		for (Route route : routes) {
			if (route.uri.equals("/*")) {
				continue;
			} else if (route.uri.startsWith("/__admin")) {
				continue;
			} else if (route.uri.startsWith("/favicon.ico")) {
				continue;
			} else if (route.uri.startsWith("/admin")) {
				continue;
			} else {
				boolean proxy = route instanceof ProxyRoute;
				HttpMethod httpMethod = route.httpMethod;
				if (httpMethod == null)
					httpMethod = HttpMethod.ALL;

				if (buf.length() > 1) {
					buf.append(",");
				}
				buf.append("{\"name\":\"" + route.name + "\",\"uri\":\"" + route.uri + "\",\"method\":\"" + httpMethod
						+ "\",\"proxy\":\"" + proxy + "\",\"active\":\"" + route.active + "\"}");
			}
		}
		buf.append("]");
		return buf;
	}

	@Path(value = Configuration.stopUri, httpMethod = HttpMethod.GET)
	static void stop() throws Exception {
		Server.getScheduledExecutorService().schedule(new Runnable() {
			@Override
			public void run() {
				Server.stop();
			}
		}, 2, TimeUnit.SECONDS);

	}

	@Path(value = "/*", httpMethod = HttpMethod.ALL)
	static void handle404(Request request, Response res) throws Exception {
		if (Configuration.ADMIN_MODULE_ENABLED && request.getUri().equals("/")) {
			res.sendRedirect("/admin/Admin.html");
		} else if ( request.getMethod() == HttpMethod.TRACE  && request.getUri().equals("/") ){
			//
		}else {
			logger.debug(request.toString());
			res.setResponseCode(404);
		}
	}

	@Path(value = "/favicon.ico", httpMethod = HttpMethod.GET)
	static void handleIcon() throws Exception {
		Response res = StaticRef.getResponse();
		res.setResponseCode(200);
		ByteData bytes = new ByteData(ICON_FILE);
		res.append(bytes);
//		res.putHeader("Content-Length", bytes.length());
		res.putHeader("Content-Type", "image/x-icon");
		res.putHeader("Cache-Control", "max-age=31536000");
	}

	@Path(value = "/__admin/undeploy", httpMethod = HttpMethod.GET)
	static void hotundeploy() throws IOException, ClassNotFoundException {
		Request request = StaticRef.getRequest();
		Response res = StaticRef.getResponse();
		if (!isOriginateFromAdminPage(request)) {
			res.setResponseCode(401);
			return;
		}
		Collection<String> collection = request.getParams().get("name");
		if (NullCheck.isNullOrEmpty(collection)) {
			res.setResponseCode(400);
			res.append("Invalid Request for hot deploy!");
			return;
		}
		String name = collection.toArray()[0].toString();
		if (!NullCheck.isNullOrEmpty(name)) {
			App hotDeploy = allHotDeployedArtifacts.get(name);
			if (hotDeploy != null) {
				hotDeploy.undeploy();
				res.setResponseCode(200);
			} else {
				res.setResponseCode(301);
				return;
			}
		} else {
			res.setResponseCode(400);
			res.append("Invalid Request for hot deploy!");
		}
	}

	@Path(value = "/__admin/deploy", httpMethod = HttpMethod.POST)
	static void hotdeploy() throws IOException, ClassNotFoundException {

		Request request = StaticRef.getRequest();
		Response response = StaticRef.getResponse();
		
		if (!request.isMultipart()) {
			response.setResponseCode(400);
			response.append("Invalid Request for hot deploy!");
			return;
		}
		final Map<String, MultiPart> multiParts = request.getMultiParts();

		final MultiPart namePart = multiParts.get("name");
		final MultiPart scanpackagesPart = multiParts.get("scanpackages");
		final MultiPart distPart = multiParts.get("dist");
		final MultiPart hashPart = multiParts.get(HASH_HEADER);
		
		String clientHash = RequestUtil.convert(hashPart.getBody());
		if(!isHashMatching(clientHash)){
			response.setResponseCode(401);
			return;
		}
		
		if (namePart != null && scanpackagesPart != null && distPart != null) {
			final String name = RequestUtil.convert(namePart.getBody());
			final String scanpackages = RequestUtil.convert(scanpackagesPart.getBody());
			logger.debug("X-HASH code sent by req :: {}",clientHash);
			if (!NullCheck.isNullOrEmpty(name) && !NullCheck.isNullOrEmpty(scanpackages)
					&& !NullCheck.isNullOrEmpty(distPart.getBody()) && RequestUtil.validUrl.matcher(name).matches()) {

				App hd = new App(name, scanpackages);
				App dup = allHotDeployedArtifacts.remove(name);
				if (dup != null) {
					dup.undeploy();
				}
				try {
					boolean deploy = hd.deploy(scanpackagesPart, distPart);
					if (deploy)
						response.setResponseCode(201);
					else
						response.setResponseCode(304);
				} catch (Throwable e) {
					logger.error("Failed on hotdeploy!", e);
					dup = allHotDeployedArtifacts.remove(name);
					if (dup != null) {
						dup.undeploy();
						response.setResponseCode(304);
					}
				}

			} else {
				response.setResponseCode(400);
				response.append("Invalid Request for hot deploy!");
			}
		} else {
			response.setResponseCode(400);
			response.append("Invalid Request for hot deploy!");
		}
	}

}

final class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);

	final String name, scanpackages;
	final File rootDir;
	final Collection<Route> reqPaths = new DoublyLinkedSet<Route>();
	URLClassLoader dynamicClassLoader;

	App(String name, String scanpackages) {
		super();
		this.name = name;
		this.scanpackages = scanpackages;
		this.rootDir = new File(Configuration.DEPLOY_LOC + File.separator + name);
	}

	boolean deploy() throws MalformedURLException, ClassNotFoundException {
		File libsFile = new File(Configuration.DEPLOY_LOC + File.separator + name + File.separator + "libs");
		List<URL> urls = new DoublyLinkedList<URL>();
		RequestUtil.allUrls(libsFile, urls);
		dynamicClassLoader = new URLClassLoader(RequestUtil.toArray(urls), Admin.class.getClassLoader());

		String[] split = scanpackages.split(",");
		for (String pkgName : split) {
			for (Class<?> kcs : PackageScanner.getClassesForPackage(dynamicClassLoader, pkgName, true)) {
				PackageScanner.addMethod(name, reqPaths, kcs);
			}
		}
		Collection<Route> dupReqPaths = new DoublyLinkedSet<Route>();
		for (Route r : reqPaths) {
			if (Configuration.routes.contains(r)) {
				dupReqPaths.add(r);
				logger.info("Duplicate route discovered ignoring :: " + r);
				r.close();
			} else {
				Configuration.routes.add(r);
				logger.info("Discovered new route in app(" + name + ") :: " + r);
			}
		}
		reqPaths.removeAll(dupReqPaths);
		if (NullCheck.isNullOrEmpty(reqPaths)) {
			logger.info("Discovered no valid route undeploying " + name);
			undeploy();
			return false;
		} else {
			Admin.allHotDeployedArtifacts.put(name, this);
			return true;
		}
	}

	boolean deploy(final MultiPart scanpackagesPart, final MultiPart distPart)
			throws IOException, InterruptedException, MalformedURLException, ClassNotFoundException {
		if (!rootDir.mkdirs()) {
			logger.info("Unable to create directory! " + rootDir.getAbsolutePath());
			return false;
		}
		File zipFile = new File(
				Configuration.DEPLOY_LOC + File.separator + name + File.separator + distPart.getFilename());
		distPart.writeTo(zipFile, false);
		scanpackagesPart.writeTo(
				new File(Configuration.DEPLOY_LOC + File.separator + name + File.separator + "scanpackages"), false);
		File libsFile = new File(Configuration.DEPLOY_LOC + File.separator + name + File.separator + "libs");
		RequestUtil.unzip(libsFile, zipFile);
		return deploy();
	}

	void undeploy() {
		Configuration.routes.removeAll(reqPaths);
		for (Route r : reqPaths) {
			r.disable();
			r.close();
		}
		try {
			dynamicClassLoader.close();
		} catch (IOException e) {
			logger.error("Error closing classloader(" + name + ") :: ", e);
		}
		RequestUtil.del(rootDir);
		Admin.allHotDeployedArtifacts.remove(name);
	}

}