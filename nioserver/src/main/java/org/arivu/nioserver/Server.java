/**
 * 
 */
package org.arivu.nioserver;

import java.io.File;
import java.io.IOException;

import org.arivu.log.Appender;
import org.arivu.log.appender.Appenders;
import org.arivu.utils.Env;

/**
 * @author P
 *
 */
public class Server {

	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
		if (args != null && args.length > 0 && args[0].equalsIgnoreCase("stop")) {
			RequestUtil.stopRemote();
		} else {
			accessLog = Appenders.file
					.get(Env.getEnv("access.log", ".." + File.separator + "logs" + File.separator + "access.log"));
			(handler = new SelectorHandler()).sync();
		}
	}

	static SelectorHandler handler = null;

	static Appender accessLog = null;

	static final String DEFAULT_HOST = Env.getEnv("host", "localhost");

	static final int DEFAULT_PORT = Integer.parseInt(Env.getEnv("port", "8080"));
	static final int DEFAULT_SOCKET_BACKLOG = Integer.parseInt(Env.getEnv("socket.backlog", "1024"));

	
}

