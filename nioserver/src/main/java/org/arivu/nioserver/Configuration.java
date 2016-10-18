package org.arivu.nioserver;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.utils.Ason;
import org.arivu.utils.NullCheck;
import org.arivu.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Configuration {
	private static final Logger logger = LoggerFactory.getLogger(Configuration.class);
	static final String stopUri = "/zzxyz";
	static final Map<String, Object> defaultResponseHeader;
	static final Map<String, Object> defaultResponseCodes;
	static final Collection<Route> routes;
	static final int defaultResCode;
	private static final String CONFIGURATION_FILE = "arivu.nioserver.json";

	static {
		final Map<String, Object> json = Ason.loadProperties(CONFIGURATION_FILE);

		defaultResponseHeader = Utils.unmodifiableMap((Map<String, Object>) Ason.getObj(json, "response.header", null));
		defaultResponseCodes = Utils.unmodifiableMap((Map<String, Object>) Ason.getObj(json, "response.codes", null));
		defaultResCode = Ason.getNumber(json, "response.defaultcode", 200).intValue();
		Collection<String> scanPackages = Utils.unmodifiableCollection(Ason.getArray(json, "request.packages", null));

		Collection<Route> tempRequestPaths = new DoublyLinkedList<Route>();
		Map<String, Object> proxies = (Map<String, Object>) Ason.getObj(json, "request.proxies", null);

		for (Entry<String, Object> e : proxies.entrySet()) {
			String name = e.getKey();
			@SuppressWarnings("unchecked")
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
			logger.debug("All request paths : "+routes);
		} catch (Exception e) {
			logger.error("Failed in packagescan :: ", e);
			throw new IllegalStateException(e);
		}
	}

}
