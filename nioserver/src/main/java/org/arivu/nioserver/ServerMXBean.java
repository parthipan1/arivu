package org.arivu.nioserver;

public interface ServerMXBean {

	/**
	 * @return
	 */
	String[] getAllRoute();

	/**
	 * @param route
	 */
	void removeRoute(String route);
	
	/**
	 * @param packageName
	 * @throws Exception
	 */
	void scanPackage(String packageName) throws Exception;

	/**
	 * @param name
	 * 			Name of the route.
	 * @param method
	 * 			httpMethod , if not specified then defaults to ALL
	 * @param location
	 * 			base uri.
	 * @param proxyPass
	 * 			proxy_pass value for proxy.
	 * @param dir
	 * 			base directory location.
	 */
	void addProxyRoute(String name, String method, String location, String proxyPass, String dir);

	/**
	 * @param uri
	 * @param header
	 */
	void removeRouteHeader(String uri, String header);

	/**
	 * @param uri
	 * @param header
	 * @param value
	 */
	void addRouteHeader(String uri, String header, String value);

	/**
	 * @return
	 */
	int getRequestBufferSize();

	/**
	 * @param size
	 */
	void setRequestBufferSize(int size);

	/**
	 * @return
	 */
	int getResponseChunkSize();

	/**
	 * @param size
	 */
	void setResponseChunkSize(int size);

	/**
	 * @return
	 */
	String getResponseHeader();

	/**
	 * @param header
	 */
	void removeResponseHeader(String header);

	/**
	 * @param header
	 * @param value
	 */
	void addResponseHeader(String header, String value);

	/**
	 * 
	 */
	void shutdown();

	/**
	 * @param jars
	 */
	void addJar(String jars);
}
