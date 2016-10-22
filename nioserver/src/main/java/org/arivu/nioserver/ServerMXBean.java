package org.arivu.nioserver;

public interface ServerMXBean {

	String[] getAllRoute();
	
	void removeRoute(String route);
	
	void shutdown();
	
}
