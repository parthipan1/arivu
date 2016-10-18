package org.arivu.nioserver;

public interface ServerMXBean {

	String[] getAllRoute();
	
	void removeRoute(String route);
	
	int noOfConnections();
	
	void shutdown();
	
}
