package org.arivu.nioserver;

public interface RequestHandler {
	void handle(Request req,Response res) throws Exception;
}
