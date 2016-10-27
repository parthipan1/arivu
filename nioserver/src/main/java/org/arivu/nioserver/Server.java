/**
 * 
 */
package org.arivu.nioserver;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.arivu.log.Appender;
import org.arivu.log.appender.Appenders;
import org.arivu.utils.Env;

/**
 * @author P
 *
 */
public class Server {

	static final String DEFAULT_HOST = Env.getEnv("host", "localhost");

	static final int DEFAULT_PORT = Integer.parseInt(Env.getEnv("port", "8080"));

	static final boolean SINGLE_THREAD_MODE = Boolean.parseBoolean(Env.getEnv("singleThread", "false"));

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

	private static SelectorHandler handler = null;

	static Appender accessLog = null;

//	@Path(value = "/multipart", httpMethod = HttpMethod.POST)
//	static void multiPart() throws Exception {
//		StaticRef.getResponse().setResponseCode(200);
//		
//		Map<String, MultiPart> multiParts = StaticRef.getRequest().getMultiParts();
//		for(Entry<String,MultiPart> e:multiParts.entrySet()){
//			MultiPart mp = e.getValue();
//			if(NullCheck.isNullOrEmpty(mp.filename)){
//				System.out.println( "Headers :: \n"+RequestUtil.getString(mp.headers) );
//				System.out.println( "body :: \n"+RequestUtil.convert(mp.body) );
//			}else{
//				File file = new File("1_"+mp.filename);
//				System.out.println( "Headers :: \n"+RequestUtil.getString(mp.headers) );
//				System.out.println("uploaded file to :: "+file.getAbsolutePath());
//				mp.writeTo(file, true);
//			}
//			System.out.println("*********************************************************************************");
//		}
//	}
	
	@Path(value = Configuration.stopUri, httpMethod = HttpMethod.GET)
	static void stop() throws Exception {
		StaticRef.getResponse().setResponseCode(200);
		final ScheduledExecutorService exe = Executors.newScheduledThreadPool(1);
		exe.schedule(new Runnable() {

			@Override
			public void run() {
				exe.shutdownNow();
				handler.stop();
			}
		}, 1, TimeUnit.SECONDS);

	}
}

