/**
 * 
 */
package org.arivu.nioserver;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.datastructure.DoublyLinkedSet;
import org.arivu.utils.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mr P
 *
 */
final class Admin {
	private static final Logger logger = LoggerFactory.getLogger(Admin.class);
	//
	// @Path(value = "/multipart", httpMethod = HttpMethod.POST)
	// static void multiPart() throws Exception {
	// StaticRef.getResponse().setResponseCode(200);
	//
	// Map<String, MultiPart> multiParts =
	// StaticRef.getRequest().getMultiParts();
	// for (Entry<String, MultiPart> e : multiParts.entrySet()) {
	// MultiPart mp = e.getValue();
	// if (NullCheck.isNullOrEmpty(mp.filename)) {
	// System.out.println("Headers :: \n" + RequestUtil.getString(mp.headers));
	// System.out.println("body :: \n" + RequestUtil.convert(mp.body));
	// } else {
	// File file = new File("1_" + mp.filename);
	// System.out.println("Headers :: \n" + RequestUtil.getString(mp.headers));
	// System.out.println("uploaded file to :: " + file.getAbsolutePath());
	// mp.writeTo(file, true);
	// }
	// System.out.println("*********************************************************************************");
	// }
	// }

//	private static final String serverInfo = "Arivu Nio Server 1.0.1";

//	@Path(value = "/__admin/greet", httpMethod = HttpMethod.GET)
//	static void greet() {
//		Response response = StaticRef.getResponse();
//		Request request = StaticRef.getRequest();
//		System.out.println(" request :: " + request);
//		try {
//			response.append("Hello, " + request.getParams().get("txt").toArray()[0] + "!<br><br>I am running "
//					+ serverInfo + ".<br><br>It looks like you are using:<br>");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		response.setResponseCode(200);
//	}

	@Path(value = "/__admin/routes", httpMethod = HttpMethod.GET)
	static void allRoutes() throws IOException {
		Response response = StaticRef.getResponse();
//		Request request = StaticRef.getRequest();
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
				if( httpMethod == null )
					httpMethod = HttpMethod.ALL;
				
				if (buf.length() > 1) {
					buf.append(",");
				}
				buf.append("{\"uri\":\""+route.uri+"\",\"method\":\""+httpMethod+"\",\"proxy\":\""+proxy+"\"}");
			}
		}
		buf.append("]");
		response.append(buf.toString());
		response.putHeader("Content-Length", buf.length());
		response.putHeader("Content-Type", "application/json");
		response.setResponseCode(200);
	}

	@Path(value = Configuration.stopUri, httpMethod = HttpMethod.GET)
	static void stop() throws Exception {
		StaticRef.getResponse().setResponseCode(200);
		final ScheduledExecutorService exe = Executors.newScheduledThreadPool(1);
		exe.schedule(new Runnable() {

			@Override
			public void run() {
				exe.shutdownNow();
				Server.handler.stop();
			}
		}, 1, TimeUnit.SECONDS);

	}

	@Path(value = "/*", httpMethod = HttpMethod.ALL)
	static void handle404() throws Exception {
		Request request = StaticRef.getRequest();
		Response res = StaticRef.getResponse();
		if( Configuration.ADMIN_MODULE_ENABLED && request.getUri().equals("/") ){
			res.sendRedirect("/admin/Admin.html");
		}else{
			logger.debug(StaticRef.getRequest().toString());
			res.setResponseCode(404);
		}
	}

	static byte[] iconBytes = null;

	@Path(value = "/favicon.ico", httpMethod = HttpMethod.GET)
	static void handleIcon() throws Exception {
		Response res = StaticRef.getResponse();
		res.setResponseCode(200);
		if (iconBytes == null) {
			iconBytes = RequestUtil.read(new File("favicon.ico"));
		}
		res.append(iconBytes);
		res.putHeader("Content-Length", iconBytes.length);
		res.putHeader("Content-Type", "image/x-icon");
		res.putHeader("Cache-Control", "max-age=31536000");
	}

	static final Map<String, App> allHotDeployedArtifacts = new Amap<>();

	@Path(value = "/__admin/undeploy", httpMethod = HttpMethod.GET)
	static void hotundeploy() throws IOException, ClassNotFoundException {
		Request request = StaticRef.getRequest();
		Response res = StaticRef.getResponse();
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

		// Map<String, MultiPart> multiParts =
		// StaticRef.getRequest().getMultiParts();
		// for (Entry<String, MultiPart> e : multiParts.entrySet()) {
		// MultiPart mp = e.getValue();
		// if (NullCheck.isNullOrEmpty(mp.filename)) {
		// System.out.println(e.getKey()+" Headers :: \n" +
		// RequestUtil.getString(mp.headers));
		// System.out.println("body :: \n" + RequestUtil.convert(mp.body));
		// } else {
		//// File file = new File("1_" + mp.filename);
		// System.out.println(e.getKey()+" Headers :: \n" +
		// RequestUtil.getString(mp.headers));
		// System.out.println("uploaded file to :: " + mp.filename);
		// }
		// }

		Request request = StaticRef.getRequest();
		Response res = StaticRef.getResponse();
		if (!request.isMultipart()) {
			res.setResponseCode(400);
			res.append("Invalid Request for hot deploy!");
			return;
		}
		final Map<String, MultiPart> multiParts = request.getMultiParts();

		final MultiPart namePart = multiParts.get("name");
		final MultiPart scanpackagesPart = multiParts.get("scanpackages");
		final MultiPart distPart = multiParts.get("dist");

		if (namePart != null && scanpackagesPart != null && distPart != null) {
			final String name = RequestUtil.convert(namePart.getBody());
			final String scanpackages = RequestUtil.convert(scanpackagesPart.getBody());
			if (!NullCheck.isNullOrEmpty(name) && !NullCheck.isNullOrEmpty(scanpackages)
					&& !NullCheck.isNullOrEmpty(distPart.getBody()) && RequestUtil.validUrl.matcher(name).matches()) {

				res.setResponseCode(200);
				App hd = new App(name, scanpackages);
				App dup = allHotDeployedArtifacts.remove(name);
				if (dup != null) {
					dup.undeploy();
				}
				try {
					hd.deploy(scanpackagesPart, distPart);
				} catch (Throwable e) {
					logger.error("Failed on hotdeploy!", e);
					dup = allHotDeployedArtifacts.remove(name);
					if (dup != null) {
						dup.undeploy();
					}
				}

			} else {
				res.setResponseCode(400);
				res.append("Invalid Request for hot deploy!");
			}
		} else {
			res.setResponseCode(400);
			res.append("Invalid Request for hot deploy!");
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

	void deploy() throws MalformedURLException, ClassNotFoundException {
		File libsFile = new File(Configuration.DEPLOY_LOC + File.separator + name + File.separator + "libs");
		List<URL> urls = new DoublyLinkedList<>();
		RequestUtil.allUrls(libsFile, urls);
		dynamicClassLoader = new URLClassLoader(RequestUtil.toArray(urls), Admin.class.getClassLoader());

		String[] split = scanpackages.split(",");
		for (String pkgName : split) {
			for (Class<?> kcs : PackageScanner.getClassesForPackage(dynamicClassLoader, pkgName, true)) {
				PackageScanner.addMethod(reqPaths, kcs);
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
		} else {
			Admin.allHotDeployedArtifacts.put(name, this);
		}
	}

	void deploy(final MultiPart scanpackagesPart, final MultiPart distPart)
			throws IOException, InterruptedException, MalformedURLException, ClassNotFoundException {
		if (!rootDir.mkdirs()) {
			logger.info("Unable to create directory! " + rootDir.getAbsolutePath());
			return;
		}
		File zipFile = new File(
				Configuration.DEPLOY_LOC + File.separator + name + File.separator + distPart.getFilename());
		distPart.writeTo(zipFile, false);
		scanpackagesPart.writeTo(
				new File(Configuration.DEPLOY_LOC + File.separator + name + File.separator + "scanpackages"), false);
		File libsFile = new File(Configuration.DEPLOY_LOC + File.separator + name + File.separator + "libs");
		RequestUtil.unzip(libsFile, zipFile);
		deploy();
	}

	void undeploy() {
		Configuration.routes.removeAll(reqPaths);
		for (Route r : reqPaths) {
			r.close();
		}
		try {
			dynamicClassLoader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		RequestUtil.del(rootDir);
	}

}