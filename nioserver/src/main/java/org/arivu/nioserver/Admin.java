/**
 * 
 */
package org.arivu.nioserver;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.datastructure.DoublyLinkedSet;
import org.arivu.utils.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mr P
 *
 */
final class Admin {
	private static final Logger logger = LoggerFactory.getLogger(Admin.class);

	// @Path(value = "/multipart", httpMethod = HttpMethod.POST)
	// static void multiPart() throws Exception {
	// StaticRef.getResponse().setResponseCode(200);
	//
	// Map<String, MultiPart> multiParts =
	// StaticRef.getRequest().getMultiParts();
	// for(Entry<String,MultiPart> e:multiParts.entrySet()){
	// MultiPart mp = e.getValue();
	// if(NullCheck.isNullOrEmpty(mp.filename)){
	// System.out.println( "Headers :: \n"+RequestUtil.getString(mp.headers) );
	// System.out.println( "body :: \n"+RequestUtil.convert(mp.body) );
	// }else{
	// File file = new File("1_"+mp.filename);
	// System.out.println( "Headers :: \n"+RequestUtil.getString(mp.headers) );
	// System.out.println("uploaded file to :: "+file.getAbsolutePath());
	// mp.writeTo(file, true);
	// }
	// System.out.println("*********************************************************************************");
	// }
	// }

	@Path(value = Configuration.stopUri, httpMethod = HttpMethod.GET)
	static void stop() throws Exception {
		StaticRef.getResponse().setResponseCode(200);
		final ScheduledExecutorService exe = Executors.newScheduledThreadPool(1);
		exe.schedule(new Runnable() {

			@Override
			public void run() {
				exe.shutdownNow();
				Server.handler.stop();
			}
		}, 1, TimeUnit.SECONDS);

	}

	@Path(value = "/*", httpMethod = HttpMethod.ALL)
	static void handle404() throws Exception {
		Response res = StaticRef.getResponse();
		logger.debug(StaticRef.getRequest().toString());
		res.setResponseCode(404);
	}

	static ByteBuffer iconBytes = null;

	@Path(value = "/favicon.ico", httpMethod = HttpMethod.GET)
	static void handleIcon() throws Exception {
		Response res = StaticRef.getResponse();
		res.setResponseCode(200);

		if (iconBytes == null) {
			RandomAccessFile randomAccessFile = null;
			try {
				randomAccessFile = new RandomAccessFile(new File("favicon.ico"), "r");
				final FileChannel fileChannel = randomAccessFile.getChannel();
				iconBytes = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
			} finally {
				if (randomAccessFile != null) {
					randomAccessFile.close();
				}
			}
		}
		byte[] array = new byte[iconBytes.remaining()];
		iconBytes.get(array, 0, array.length);
		res.append(array);
		res.putHeader("Content-Length", array.length);
		res.putHeader("Content-Type", "image/x-icon");
	}

	private static final Map<String, HotDeploy> allHotDeployedArtifacts = new Amap<>();

	@Path(value = "/__admin/undeploy", httpMethod = HttpMethod.GET)
	static void hotundeploy() throws IOException, ClassNotFoundException {
		Request request = StaticRef.getRequest();
		Response res = StaticRef.getResponse();
		Collection<String> collection = request.getParams().get("name");
		if (NullCheck.isNullOrEmpty(collection)) {
			res.setResponseCode(400);
			res.append("Invalid Request for hot deploy!");
			return;
		}
		String name = collection.toArray()[0].toString();
		if (!NullCheck.isNullOrEmpty(name)) {
			HotDeploy hotDeploy = allHotDeployedArtifacts.get(name);
			if (hotDeploy != null) {
				Configuration.routes.removeAll(hotDeploy.reqPaths);
				for (Route r : hotDeploy.reqPaths) {
					r.close();
				}
				hotDeploy.dynamicClassLoader.close();
				del(hotDeploy.rootDir);
			} else {
				res.setResponseCode(301);
				return;
			}
		} else {
			res.setResponseCode(400);
			res.append("Invalid Request for hot deploy!");
		}
	}

	@Path(value = "/__admin/deploy", httpMethod = HttpMethod.POST)
	static void hotdeploy() throws IOException, ClassNotFoundException {

		Map<String, MultiPart> multiParts = StaticRef.getRequest().getMultiParts();
		for (Entry<String, MultiPart> e : multiParts.entrySet()) {
			MultiPart mp = e.getValue();
			if (NullCheck.isNullOrEmpty(mp.filename)) {
				System.out.println(e.getKey()+"  Headers :: \n" + RequestUtil.getString(mp.headers));
				System.out.println("body :: \n" + RequestUtil.convert(mp.body));
			} else {
//				File file = new File("1_" + mp.filename);
				System.out.println(e.getKey()+"  Headers :: \n" + RequestUtil.getString(mp.headers));
				System.out.println("uploaded file to :: " + mp.filename);
			}
		}

		Request request = StaticRef.getRequest();
		Response res = StaticRef.getResponse();
		if (!request.isMultipart()) {
			res.setResponseCode(400);
			res.append("Invalid Request for hot deploy!");
			return;
		}
		// Map<String, MultiPart> multiParts = request.getMultiParts();

		MultiPart namePart = multiParts.get("name");
		MultiPart scanpackagesPart = multiParts.get("scanpackages");
		MultiPart distPart = multiParts.get("dist");

		if (namePart != null && scanpackagesPart != null && distPart != null) {
			String name = RequestUtil.convert(namePart.getBody());
			String scanpackages = RequestUtil.convert(scanpackagesPart.getBody());
			List<ByteData> zipFileBody = distPart.getBody();
			if (!NullCheck.isNullOrEmpty(name) && !NullCheck.isNullOrEmpty(scanpackages)
					&& !NullCheck.isNullOrEmpty(zipFileBody) && RequestUtil.validUrl.matcher(name).matches() ) {

				String deployDirPathname = ".." + File.separator + "hotdeploy" + File.separator + name;
				HotDeploy hd = new HotDeploy(name, scanpackages, new File(deployDirPathname));
				if (hd.rootDir.mkdirs()) {
					try {
						File zipFile = new File(".." + File.separator + "hotdeploy" + File.separator + name + File.separator
								+ distPart.getFilename());
						distPart.writeTo(zipFile, false);
						scanpackagesPart.writeTo(new File(".." + File.separator + "hotdeploy" + File.separator + name
								+ File.separator + "scanpackages"), false);
						File libsFile = new File(
								".." + File.separator + "hotdeploy" + File.separator + name + File.separator + "libs");
						unzip(libsFile, zipFile);
						List<URL> urls = new DoublyLinkedList<>();
						allUrls(libsFile, urls);
						Object[] objarr = urls.toArray();
						URL[] array = new URL[objarr.length];
						for(int i=0;i<objarr.length;i++)
							array[i] = (URL)objarr[i];
						hd.dynamicClassLoader = new URLClassLoader(array, Admin.class.getClassLoader());

						String[] split = scanpackages.split(",");
						for (String pkgName : split) {
							for (Class<?> kcs : PackageScanner.getClassesForPackage(hd.dynamicClassLoader, pkgName, true)) {
								PackageScanner.addMethod(hd.reqPaths, kcs);
							}
						}

						Configuration.routes.addAll(hd.reqPaths);
						allHotDeployedArtifacts.put(name, hd);
					} catch (Throwable e1) {
						e1.printStackTrace();
					}

				} else {
					res.setResponseCode(400);
					res.append("Unable to create directory!");
				}
			} else {
				res.setResponseCode(400);
				res.append("Invalid Request for hot deploy!");
			}
		} else {
			res.setResponseCode(400);
			res.append("Invalid Request for hot deploy!");
		}
	}

	private static void allUrls(File root, List<URL> urls) throws MalformedURLException {
		File[] list = root.listFiles();
		if (NullCheck.isNullOrEmpty(list))
			return;

		for (File f : list) {
			if (f.isDirectory()) {
				allUrls(f, urls);
			} else {
				urls.add(f.toURI().toURL());
				logger.info("Hotdeploy :: Added file " + f.getAbsoluteFile());
			}
		}
	}

	private static void unzip(File destinationFolder, File zipFile) throws IOException, InterruptedException {

		if (!destinationFolder.exists())
			destinationFolder.mkdirs();

		String exe = "unzip "+zipFile.getAbsolutePath()+" -d "+ destinationFolder.getAbsolutePath();
		Runtime.getRuntime().exec(exe).waitFor();
		
//		byte[] buffer = new byte[2048];
//
//		try (FileInputStream fInput = new FileInputStream(zipFile);
//				ZipInputStream zipInput = new ZipInputStream(fInput);) {
//
//			ZipEntry entry = zipInput.getNextEntry();
//
//			while (entry != null) {
//				String entryName = entry.getName();
//				File file = new File(destinationFolder.getAbsolutePath() + File.separator + entryName);
//
//				logger.info("Hotdeploy :: Unzip file " + entryName + " to " + file.getAbsolutePath());
//
//				// create the directories of the zip directory
//				if (entry.isDirectory()) {
//					File newDir = new File(file.getAbsolutePath());
//					if (!newDir.exists()) {
//						boolean success = newDir.mkdirs();
//						if (success == false) {
//							logger.info("Problem creating Folder");
//						}
//					}
//				} else {
//					FileOutputStream fOutput = new FileOutputStream(file);
//					int count = 0;
//					while ((count = zipInput.read(buffer)) > 0) {
//						// write 'count' bytes to the file output stream
//						fOutput.write(buffer, 0, count);
//					}
//					fOutput.close();
//				}
//				// close ZipEntry and take the next one
//				zipInput.closeEntry();
//				entry = zipInput.getNextEntry();
//			}
//
//			// close the last ZipEntry
//			zipInput.closeEntry();
//
//		}
	}

	static void del(File f) {
		try {
			if (f.isDirectory()) {
				if (f.list().length == 0) {
					f.delete();
				} else {
					for (String t : f.list())
						del(new File(f,t));

					if (f.list().length == 0) {
						f.delete();
					}
				}
			} else {
				f.delete();
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}

final class HotDeploy {
	final String name, scanpackages;
	final File rootDir;
	final Collection<Route> reqPaths = new DoublyLinkedSet<Route>();
	URLClassLoader dynamicClassLoader;

	HotDeploy(String name, String scanpackages, File rootDir) {
		super();
		this.name = name;
		this.scanpackages = scanpackages;
		this.rootDir = rootDir;
	}

}