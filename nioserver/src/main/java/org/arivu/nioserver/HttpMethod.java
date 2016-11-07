package org.arivu.nioserver;

import java.io.IOException;

public enum HttpMethod {
	HEAD {

		@Override
		void proxy(JavaHttpMethodCall caller, String loc, Request req, Response res) throws IOException {
			caller.head(loc, req.getHeaders(), res);
		}

	},
	PUT {

		@Override
		void proxy(JavaHttpMethodCall caller, String loc, Request req, Response res) throws IOException {
			caller.put(loc, RequestUtil.convert(req.getBody()), req.getHeaders(), res);
		}

	},
	DELETE {

		@Override
		void proxy(JavaHttpMethodCall caller, String loc, Request req, Response res) throws IOException {
			caller.delete(loc, req.getHeaders(), res);
		}

	},
	OPTIONS {

		@Override
		void proxy(JavaHttpMethodCall caller, String loc, Request req, Response res) throws IOException {
			caller.options(loc, RequestUtil.convert(req.getBody()), req.getHeaders(), res);
		}

	},
	GET ,
	POST {

		@Override
		void proxy(JavaHttpMethodCall caller, String loc, Request req, Response res) throws IOException {
			caller.post(loc, RequestUtil.convert(req.getBody()), req.getHeaders(), res);
		}

	},
	CONNECT {

		@Override
		void proxy(JavaHttpMethodCall caller, String loc, Request req, Response res) throws IOException {
			caller.connect(loc, req.getHeaders(), res);
		}

	},
	TRACE {

		@Override
		void proxy(JavaHttpMethodCall caller, String loc, Request req, Response res) throws IOException {
			caller.trace(loc, req.getHeaders(), res);
		}

	},
	ALL;

	void proxy(JavaHttpMethodCall caller, String loc, Request req, Response res) throws IOException {
		caller.get(loc, req.getHeaders(), res);
	}

}