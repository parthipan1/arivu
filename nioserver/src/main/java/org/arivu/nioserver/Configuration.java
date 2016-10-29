package org.arivu.nioserver;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.utils.Ason;
import org.arivu.utils.Env;
import org.arivu.utils.NullCheck;
import org.arivu.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
final class Configuration {
	private static final Logger logger = LoggerFactory.getLogger(Configuration.class);
	
	static final String stopUri = "/__admin/shutdown";
	static final boolean SINGLE_THREAD_MODE = Boolean.parseBoolean(Env.getEnv("singleThread", "false"));
	static final String DEPLOY_LOC = Env.getEnv("deployLoc", ".." + File.separator + "apps");
	
	static final Map<String, Object> defaultResponseHeader;
	static final Map<String, Object> defaultResponseCodes;
	static final Map<String, Map<String, Object>> defaultMimeType;
	static Collection<Route> routes;
	static Route defaultRoute = null;
	static final int defaultResCode;
	static int defaultChunkSize;
	static int defaultRequestBuffer;
	private static final String CONFIGURATION_FILE = "arivu.nioserver.json";

	private static final String RESPONSE_CODES = "arivu.nioserver.response.json";

	static {
		final Map<String, Object> responseJson = Ason.loadProperties(RESPONSE_CODES);
		defaultResponseCodes = Utils
				.unmodifiableMap((Map<String, Object>) Ason.getObj(responseJson, "response.codes", null));

		Map<String, Object> jmt = (Map<String, Object>) Ason.getObj(responseJson, "response.mime", null);
		Map<String, Map<String, Object>> tempMimeType = new Amap<String, Map<String, Object>>();
		for (Entry<String, Object> e : jmt.entrySet()) {
			tempMimeType.put(e.getKey(), Utils.unmodifiableMap((Map<String, Object>) e.getValue()));
		}
		defaultMimeType = Utils.unmodifiableMap(tempMimeType);

		final Map<String, Object> json = Ason.loadProperties(CONFIGURATION_FILE);

//		headers = Utils.unmodifiableMap((Map<String, Object>) Ason.getObj(json, "response.header", null));
		defaultResponseHeader = (Map<String, Object>) Ason.getObj(json, "response.header", new Amap<String, Object>());

		defaultResCode = Ason.getNumber(json, "response.defaultcode", 200).intValue();
		defaultChunkSize = Ason.getNumber(json, "response.chunkSize", 1024).intValue();
		defaultRequestBuffer = Ason.getNumber(json, "request.buffer", 1024).intValue();

		Collection<String> array = Ason.getArray(json, "request.scanpackages", null);
		if (NullCheck.isNullOrEmpty(array)) {
			array = new DoublyLinkedList<String>();
		} else {
			array = new DoublyLinkedList<String>(array);
		}
		array.add("org.arivu.nioserver");
		Collection<String> scanPackages = Utils.unmodifiableCollection(array);

		Collection<Route> tempRequestPaths = new DoublyLinkedList<Route>();
		Map<String, Object> proxies = (Map<String, Object>) Ason.getObj(json, "request.proxies", null);

		if (proxies != null) {
			for (Entry<String, Object> e : proxies.entrySet()) {
				String name = e.getKey();
				Map<String, Object> proxy = (Map<String, Object>) e.getValue();
				HttpMethod httpMethod = HttpMethod.valueOf(Ason.getStr(proxy, "httpMethod", "ALL"));
				Map<String, Object> header = (Map<String, Object>) Ason.getObj(proxy, "header", null);
				String proxy_pass = Ason.getStr(proxy, "proxy_pass", null);
				if (proxy_pass != null) {
					proxy_pass = Utils.replaceAll(proxy_pass, "$host", Server.DEFAULT_HOST);
					proxy_pass = Utils.replaceAll(proxy_pass, "$port", String.valueOf(Server.DEFAULT_PORT));
				}
				String dir = Ason.getStr(proxy, "dir", null);
				if (dir != null) {
					dir = Utils.replaceAll(dir, "$home", new File(".").getAbsolutePath());
				}
				ProxyRoute prp = new ProxyRoute(name, proxy_pass, dir, Ason.getStr(proxy, "location", null), httpMethod,
						null, null, false, header);
				logger.debug("Discovered Proxy setting ::" + prp.toString());
				tempRequestPaths.add(prp);
			}
		}
		try {
			tempRequestPaths.addAll(PackageScanner.getPaths(scanPackages));
//			routes = Utils.unmodifiableCollection(tempRequestPaths);
			routes = tempRequestPaths;
			RequestUtil.scanApps(new File(DEPLOY_LOC));
			for (Route r : routes){
				if ( defaultRoute == null && r.uri.equals("/*") && r.httpMethod == HttpMethod.ALL ){
					defaultRoute = r;
					logger.info("Default Route discovered :: " + r);
				}else{
					logger.info("Route discovered :: " + r);
				}
//				logger.info("Route discovered :: " + r);
			}
		} catch (Exception e) {
			logger.error("Failed in packagescan :: ", e);
			throw new IllegalStateException(e);
		}
	}

}
