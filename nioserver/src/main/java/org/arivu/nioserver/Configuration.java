package org.arivu.nioserver;

import java.io.File;
import java.util.Collection;
import java.util.List;
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
	static final boolean ADMIN_MODULE_ENABLED = Boolean.parseBoolean(Env.getEnv("adminMod", "false"));
	static final String DEPLOY_LOC = Env.getEnv("deployLoc", ".." + File.separator + "apps");
	static final String ADMIN_LOC = Env.getEnv("adminLoc", ".." + File.separator + "admin");

	static final Map<String, List<Object>> defaultResponseHeader;
	static final Map<String, Object> defaultResponseCodes;
	static final Map<String, Map<String, Object>> defaultMimeType;
	static Collection<Route> routes;
	static Route defaultRoute = null;
	static final int defaultResCode;
	static int defaultChunkSize;
	static int defaultRequestBuffer;
	private static final String CONFIGURATION_FILE = "arivu.nioserver.json";

	private static final String RESPONSE_CODES = "arivu.nioserver.response.json";

	static ExceptionHandler exceptionHandler = new DefaultExceptionHandler();
	
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

		String exceptionHandlerStr = Ason.getStr(json, "exceptionHandler", "default");
		if(!"default".equalsIgnoreCase(exceptionHandlerStr)){
			try {
				Class<?> loadClass = Configuration.class.getClassLoader().loadClass(exceptionHandlerStr);
				if( loadClass.isAssignableFrom(ExceptionHandler.class) ){
					exceptionHandler = (ExceptionHandler) loadClass.newInstance();
					logger.info("New {} registered as {} ",exceptionHandlerStr,ExceptionHandler.class.getCanonicalName());
				}else{
					logger.info("{} config value is not an implementation of {} ",exceptionHandlerStr,ExceptionHandler.class.getCanonicalName());
				}
			} catch (Throwable e1) {
				logger.error("Failed in ExceptionHandler registering :: ", e1);
				throw new IllegalStateException(e1);
			}
		}
		
		defaultResponseHeader = RequestUtil
				.transform((Map<String, Object>) Ason.getObj(json, "response.header", new Amap<String, Object>()));

		defaultResCode = Ason.getNumber(json, "response.defaultcode", 200).intValue();
		defaultChunkSize = Ason.getNumber(json, "response.chunkSize", 131072).intValue();
		defaultRequestBuffer = Ason.getNumber(json, "request.buffer", 131072).intValue();

		Collection<String> array = Ason.getArray(json, "request.scanpackages", null);
		if (NullCheck.isNullOrEmpty(array)) {
			array = new DoublyLinkedList<String>();
		} else {
			array = new DoublyLinkedList<String>(array);
		}
		array.add("org.arivu.nioserver");
		Collection<String> scanPackages = Utils.unmodifiableCollection(array);

		routes = new DoublyLinkedList<Route>();
		final Map<String, Object> proxies = (Map<String, Object>) Ason.getObj(json, "request.proxies", null);

		if (!NullCheck.isNullOrEmpty(proxies)) {
			for (Entry<String, Object> e : proxies.entrySet()) {
				Map<String, Object> proxy = (Map<String, Object>) e.getValue();
				RequestUtil.addProxyRouteRuntime(e.getKey(), Ason.getStr(proxy, "httpMethod", "ALL"),
						Ason.getStr(proxy, "location", null), Ason.getStr(proxy, "proxy_pass", null),
						Ason.getStr(proxy, "dir", null), routes,
						RequestUtil.transform((Map<String, Object>) Ason.getObj(proxy, "header", null)));
			}
		}
		try {
			routes.addAll(PackageScanner.getPaths("System", scanPackages));
			RequestUtil.scanApps(new File(DEPLOY_LOC));
			if (ADMIN_MODULE_ENABLED)
				routes.add(new AdminRoute());

			for (Route r : routes) {
				if (defaultRoute == null && r.uri.equals("/*") && r.httpMethod == HttpMethod.ALL) {
					defaultRoute = r;
					logger.info("Default Route discovered :: {}", r);
				} else {
					logger.info("Route discovered :: {}", r);
				}
			}
		} catch (Exception e) {
			logger.error("Failed in packagescan :: ", e);
			throw new IllegalStateException(e);
		}
	}

}
