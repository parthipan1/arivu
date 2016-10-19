package org.arivu.nioserver;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.utils.Ason;
import org.arivu.utils.NullCheck;
import org.arivu.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
final class Configuration {
	private static final Logger logger = LoggerFactory.getLogger(Configuration.class);
	static final String stopUri = "/zzxyz";
	static final Map<String, Object> defaultResponseHeader;
	static final Map<String, Object> defaultResponseCodes;
	static final Map<String, Map<String, Object>> defaultMimeType;
	static final Collection<Route> routes;
	static final int defaultResCode;
	private static final String CONFIGURATION_FILE = "arivu.nioserver.json";

	static {
		final Map<String, Object> json = Ason.loadProperties(CONFIGURATION_FILE);

		defaultResponseHeader = Utils.unmodifiableMap((Map<String, Object>) Ason.getObj(json, "response.header", null));
		defaultResponseCodes = Utils.unmodifiableMap((Map<String, Object>) Ason.getObj(json, "response.codes", null));
		defaultResCode = Ason.getNumber(json, "response.defaultcode", 200).intValue();
		Collection<String> array = Ason.getArray(json, "request.scanpackages", null);
		if( NullCheck.isNullOrEmpty(array) ){
			array = new DoublyLinkedList<String>();
			array.add("org.arivu.nioserver");
		}else{
			array = new DoublyLinkedList<String>(array);
			array.add("org.arivu.nioserver");
		}
		Collection<String> scanPackages = Utils.unmodifiableCollection(array);

		Map<String, Object> jmt = (Map<String, Object>) Ason.getObj(json, "response.mime", null);
		Map<String, Map<String, Object>> tempMimeType = new Amap<String, Map<String,Object>>();
		for (Entry<String, Object> e : jmt.entrySet()) {
			tempMimeType.put(e.getKey(), Utils.unmodifiableMap((Map<String, Object>)e.getValue()));
		}
		defaultMimeType = Utils.unmodifiableMap(tempMimeType);
		
		Collection<Route> tempRequestPaths = new DoublyLinkedList<Route>();
		Map<String, Object> proxies = (Map<String, Object>) Ason.getObj(json, "request.proxies", null);

		for (Entry<String, Object> e : proxies.entrySet()) {
			String name = e.getKey();
			Map<String, Object> proxy = (Map<String, Object>) e.getValue();
			String strMethod = Ason.getStr(proxy, "httpMethod", null);
			if (NullCheck.isNullOrEmpty(strMethod))
				strMethod = "ALL";
			HttpMethod httpMethod = HttpMethod.valueOf(strMethod);
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
			ProxyRoute prp = new ProxyRoute(name, proxy_pass, dir, Ason.getStr(proxy, "location", null),
					httpMethod, null, null, false, header);
			logger.debug("Discovered Proxy setting ::" + prp.toString());
			tempRequestPaths.add(prp);
		}

		try {
			tempRequestPaths.addAll(PackageScanner.getPaths(scanPackages));
			routes = Utils.unmodifiableCollection(tempRequestPaths);
//			routes = tempRequestPaths;
			logger.debug("All request paths : "+routes);
		} catch (Exception e) {
			logger.error("Failed in packagescan :: ", e);
			throw new IllegalStateException(e);
		}
	}

}
