package org.arivu.nioserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.utils.NullCheck;
import org.arivu.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RequestUtil {

	private static final String CLOSE_CHAIN_BRKT = "}";

	private static final String OPEN_CHAIN_BRKT = "{";

	private static final String URI_PATH_DIVIDER = "/";

	private static final String DEFAULT_PROTOCOL = "http";

	private static final String BOUNDARY = "boundary";

	private static final String CONTENT_DISPOSITION = "Content-Disposition";

	private static final String MULTIPART_FORM_DATA = "multipart/form-data";

	private static final String CONTENT_TYPE = "Content-Type";

	private static final String SCANPACKAGES_TOKEN = "scanpackages";

	private static final Logger logger = LoggerFactory.getLogger(RequestUtil.class);

	static final String ENC_UTF_8 = "UTF-8";
	static final byte BYTE_13 = (byte) 13;
	static final byte BYTE_10 = (byte) 10;
	static final String divider = System.lineSeparator() + System.lineSeparator();

	final static int BYTE_SEARCH_DEFLT = Integer.MIN_VALUE;

	private static final String LINE_SEPARATOR = System.lineSeparator();

//	final static DateFormat dateFormat = new SimpleDateFormat("EEE MMM d hh:mm:ss.SSS yyyy");
	
	final static DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
	
	static final Pattern validUrl = Pattern.compile("^[a-zA-Z0-9-_]*$");
	
	static int searchPattern(byte[] content, byte[] pattern, int start, int disp) {
		int mi = disp;
		for (int i = start; i < content.length; i++) {
			if (content[i] == pattern[mi]) {
				mi++;
				if (mi == pattern.length) {
					// logger.debug(" searchPattern 1 "+(i + 1 - mi));
					return i + 1 - mi;
				}
			} else {
				mi = 0;
			}
		}

		if (mi != 0) {
			// logger.debug(" searchPattern 2 "+(-1 * mi)+"
			// c%"+content[content.length-1]+"% p%"+pattern[0]+"% ap%"+new
			// String(pattern)+"%");
			return -1 * mi;
		} else
			return BYTE_SEARCH_DEFLT;
	}

	static MultiPart parseAsMultiPart(List<ByteData> list) {
		List<ByteData> body = new DoublyLinkedList<>();
		StringBuffer headers = new StringBuffer();
		for (ByteData bb : list) {
			byte[] content = bb.array();
			if (body.isEmpty()) {
				int headerIndex = RequestUtil.getHeaderIndex(content, RequestUtil.BYTE_13, RequestUtil.BYTE_10, 2);
				if (headerIndex == -1) {
					headers.append(new String(content));
				} else {
					headers.append(new String(Arrays.copyOfRange(content, 0, headerIndex - 1)));
					body.add(ByteData.wrap(Arrays.copyOfRange(content, headerIndex + 1, content.length)));
				}
			} else {
				body.add(bb);
			}
		}

		Map<String, String> unmodifiableMap = Utils.unmodifiableMap(parseMultipartHeader(headers.toString()));

		String name = null, filename = null, contentType = null, contentDisposition = null;
		contentType = unmodifiableMap.get(CONTENT_TYPE);

		String cd = unmodifiableMap.get(CONTENT_DISPOSITION);

		if (!NullCheck.isNullOrEmpty(cd)) {
			String[] split = cd.split(";");
			contentDisposition = split[0];
			if (split.length > 1) {
				for (int i = 1; i < split.length; i++) {
					if (split[i].indexOf("filename=") != -1) {
						String v = Utils.replaceAll(split[i], "filename=", "");
						v = Utils.replaceAll(v, "\"", "").trim();
						filename = v;
					} else if (split[i].indexOf("name=") != -1) {
						String v = Utils.replaceAll(split[i], "name=", "");
						v = Utils.replaceAll(v, "\"", "").trim();
						name = v;
					}
				}
			}
		}

		return new MultiPart(unmodifiableMap, Utils.unmodifiableList(body), name, filename, contentType,
				contentDisposition);
	}

	static Map<String, String> parseMultipartHeader(String metadata) {
		// System.out.println(" parseMultipartHeader :: "+metadata);
		String[] split = metadata.split(System.lineSeparator());
		Map<String, String> tempheaders = new Amap<String, String>();
		for (int i = 0; i < split.length; i++) {
			String h = split[i];
			int indexOf2 = h.indexOf(":");
			if (indexOf2 != -1) {
				// tempheaders.put(h, "");
//			} else {
				tempheaders.put(h.substring(0, indexOf2), h.substring(indexOf2 + 1).trim());
			}
		}
		return tempheaders;
	}

	static RequestImpl parseRequest(final List<ByteData> messages) {
		String metadata = convert(messages);
		String[] split = metadata.split(System.lineSeparator());
		String[] split2 = split[0].split(" ");

		HttpMethod valueOf = HttpMethod.valueOf(split2[0]);
		if (valueOf == null)
			throw new IllegalArgumentException("Unknown RequestImpl " + metadata);
		String uriWithParams = split2[1];
		String protocol = split2[2];

		Map<String, List<Object>> tempheaders = new Amap<String, List<Object>>();
		for (int i = 1; i < split.length; i++) {
			String h = split[i];
			int indexOf2 = h.indexOf(":");
			if (indexOf2 == -1) {
				String key = h;
				List<Object> list = tempheaders.get(key);
				if (list == null) {
					list = new DoublyLinkedList<>();
					tempheaders.put(key, list);
				}
			} else {
				String key = h.substring(0, indexOf2);
				String value = h.substring(indexOf2 + 1).trim();
				List<Object> list = tempheaders.get(key);
				if (list == null) {
					list = new DoublyLinkedList<>();
					tempheaders.put(key, list);
				}
				list.add(value);
			}
		}

		int indexOf3 = uriWithParams.indexOf("?");
		String uri = uriWithParams;
		Map<String, Collection<String>> tempparams = null;
		if (indexOf3 > 0) {
			uri = uriWithParams.substring(0, indexOf3);
			tempparams = parseParams(uriWithParams.substring(indexOf3 + 1));
		}

		RequestImpl requestImpl = new RequestImpl(valueOf, uri, uriWithParams, protocol, tempparams,
				Utils.unmodifiableMap(tempheaders));
		String contType = null;
		List<Object> list = requestImpl.getHeaders().get(CONTENT_TYPE);
		if (!NullCheck.isNullOrEmpty(list)) {
			contType = list.get(0).toString();
		}
		if (!NullCheck.isNullOrEmpty(contType)) {
			requestImpl.isMultipart = contType.contains(MULTIPART_FORM_DATA);
			if (requestImpl.isMultipart) {
				contType = Utils.replaceAll(contType, MULTIPART_FORM_DATA+";", "").trim();
				contType = Utils.replaceAll(contType, BOUNDARY+"=", "").trim();
				requestImpl.boundary = ("--" + contType).getBytes();
			}
		}

		return requestImpl;
	}

	public static String convert(final List<ByteData> messages) {
		StringBuffer metadataBuf = new StringBuffer();
		for (ByteData bb : messages) {
			metadataBuf.append(convert(bb));
		}

		String metadata = metadataBuf.toString();
		return metadata;
	}

	static String convert(ByteData bb) {
		return new String(bb.array());
	}

	static int getHeaderIndex(byte[] bytes, byte first, byte second, final int cnt) {
		int indexOf = -1;
		if (cnt == 2) {
			int inc = 1;
			for (int i = 3; i < bytes.length; i+=inc) {
				if (bytes[i] == bytes[i - 2] && bytes[i] == second ) {
					if(bytes[i - 1] == bytes[i - 3]
							&& bytes[i - 1] == first){
						indexOf = i;
						break;
					}
					inc = 2;
				}else{
					inc = 1;
				}
				
			}
		} else if (cnt == 1) {
			for (int i = 1; i < bytes.length; i++) {
				if (bytes[i] == second && bytes[i - 1] == first) {
					indexOf = i;
					break;
				}
			}
		}
		return indexOf;
	}

	public static Map<String, Collection<String>> parseParams(String uriparams) {
		Map<String, Collection<String>> tempparams = new Amap<String, Collection<String>>();
		String[] split3 = uriparams.split("&");
		for (String p : split3) {
			int indexOf2 = p.indexOf("=");
			if (indexOf2 == -1) {
				Collection<String> collection = tempparams.get(p);
				if (collection == null) {
					collection = new DoublyLinkedList<String>();
				}
				try {
					tempparams.put(URLDecoder.decode(p, ENC_UTF_8), collection);
				} catch (UnsupportedEncodingException e1) {
					logger.error("Error on parseParams :: ", e1);
				}
			} else {
				String key = p.substring(0, indexOf2);
				String value = p.substring(indexOf2 + 1);
				try {
					String decodeKey = URLDecoder.decode(key, ENC_UTF_8);
					Collection<String> collection = tempparams.get(decodeKey);
					if (collection == null) {
						collection = new DoublyLinkedList<String>();
						tempparams.put(decodeKey, collection);
					}
					collection.add(URLDecoder.decode(value, ENC_UTF_8));
				} catch (UnsupportedEncodingException e1) {
					logger.error("Error on parseParams :: ", e1);
				}
			}

		}
		for (Entry<String, Collection<String>> e : tempparams.entrySet()) {
			e.setValue(Collections.unmodifiableCollection(e.getValue()));
		}
		return Utils.unmodifiableMap(tempparams);
	}

	static String getStackTrace(final Throwable throwable) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw, true);
		throwable.printStackTrace(pw);
		return sw.getBuffer().toString();
	}

	static MethodInvoker getMethodInvoker(Method method) {
		int parameterCount = method.getParameterCount();
		if (parameterCount == 0)
			return MethodInvoker.none;
		else if (parameterCount == 1) {
			Class<?>[] parameterTypes = method.getParameterTypes();
			if (parameterTypes[0].isAssignableFrom(Response.class)) {
				return MethodInvoker.onlyRes;
			} else if (parameterTypes[0].isAssignableFrom(Request.class)) {
				return MethodInvoker.onlyReq;
			} else {
				throw new IllegalStateException("Method signature not in line with @Path! for method " + method);
			}
		} else if (parameterCount == 2) {
			Class<?>[] parameterTypes = method.getParameterTypes();
			if (parameterTypes[0].isAssignableFrom(Response.class)
					&& parameterTypes[1].isAssignableFrom(Request.class)) {
				return MethodInvoker.reverDef;
			} else if (parameterTypes[0].isAssignableFrom(Request.class)
					&& parameterTypes[1].isAssignableFrom(Response.class)) {
				return MethodInvoker.defalt;
			} else {
				throw new IllegalStateException("Method signature not in line with @Path! for method " + method);
			}
		} else {
			return MethodInvoker.variable;
		}
	}

	static boolean validateRouteUri(final String uri) {
		if (NullCheck.isNullOrEmpty(uri))
			return false;
		else if (!uri.startsWith(URI_PATH_DIVIDER))
			return false;
		String[] split = uri.split(URI_PATH_DIVIDER);
		for (int i = 1; i < split.length; i++) {
			String uritkn = split[i];
			int si = uritkn.indexOf(OPEN_CHAIN_BRKT);
			if (si == -1) {
				if (!validUrl.matcher(uritkn).matches())
					return false;
			} else {
				int ei = uritkn.indexOf(CLOSE_CHAIN_BRKT);
				String paramName = uritkn.substring(1, uritkn.length() - 1);
				if ( si != 0 && ei != uritkn.length() - 1 || NullCheck.isNullOrEmpty(paramName)
						|| !validUrl.matcher(paramName).matches()) {
					return false;
				}
			}
		}
		return true;
	}

	static Route getMatchingRoute(Collection<Route> paths, final String uri, final HttpMethod httpMethod,
			final boolean retNull) {

		final Route in = new Route(uri, httpMethod);
		for (Route route : paths) {
			if (!route.active) {
				continue;
			} else if (route.rut == null) {
				if (in.equals(route))
					return route;
				else if (route.httpMethod == HttpMethod.ALL) {
					if (route.uri.equals(uri))
						return route;
					else if (route instanceof ProxyRoute && uri.startsWith(route.uri))
						return route;
				} else if (route instanceof ProxyRoute && uri.startsWith(route.uri) && route.httpMethod == httpMethod) {
					return route;
				}
			} else {
				if (route.httpMethod == HttpMethod.ALL || route.httpMethod == httpMethod) {
					String[] split = uri.split(URI_PATH_DIVIDER);
					if (route.rut.uriTokens.length == split.length) {
						boolean match = true;
						for (int i = 0; i < split.length; i++) {
							if (route.rut.paramIdx[i] == -1 && !route.rut.uriTokens[i].equals(split[i])) {
								match = false;
								break;
							}
						}
						if (match)
							return route;
					}
				}
			}
		}
		if (retNull)
			return null;
		else
			return Configuration.defaultRoute;
	}

	static RequestUriTokens parseRequestUriTokens(String uri, Method method) {
		RequestUriTokens rut = new RequestUriTokens();
		rut.uriTokens = uri.split(URI_PATH_DIVIDER);
		rut.paramIdx = new int[rut.uriTokens.length];

		Parameter[] parameters = method.getParameters();
		Class<?>[] parameterTypes = method.getParameterTypes();

		int as = 0;
		for (int i = 0; i < parameters.length; i++) {
			if (rut.resIdx == -1 && parameterTypes[i].isAssignableFrom(Response.class)) {
				rut.resIdx = i;
				as++;
			}
			if (rut.reqIdx == -1 && parameterTypes[i].isAssignableFrom(Request.class)) {
				rut.reqIdx = i;
				as++;
			}
		}

		rut.argsIdx = new int[parameters.length - as];
		int arid = 0;

		for (int i = 0; i < rut.uriTokens.length; i++) {
			String uritkn = rut.uriTokens[i];
			int si = uritkn.indexOf(OPEN_CHAIN_BRKT);
			int ei = uritkn.indexOf(CLOSE_CHAIN_BRKT);
			if (si != -1 && ei != -1) {
				rut.paramIdx[i] = i;
				rut.uriTokens[i] = uritkn.substring(si + 1, ei);
				for (int j = 0; j < parameters.length; j++) {
					Parameter parameter = parameters[j];
					if (parameter.getName().equals(rut.uriTokens[i])) {
						rut.argsIdx[arid++] = j;
						break;
					}
				}
			} else {
				rut.paramIdx[i] = -1;
			}
		}

		return rut;
	}

	static void accessLog(int responseCode, String uri, long start, long end, long size,
			SocketAddress remoteSocketAddress, HttpMethod method) {
		if (!uri.equals(Configuration.stopUri)) {
			StringBuffer access = new StringBuffer();
			access.append("[").append(dateFormat.format(new Date(start))).append("] ").append(uri).append(" ")
					.append(method).append(" ").append(responseCode).append(" ").append(size).append(" [")
					.append((end - start)).append("] ").append(remoteSocketAddress.toString());
			Server.accessLog.append(access.toString());
		}
	}

	static Ref getResponseBytes(Request request, Response response) {
		Ref responseBytes = RequestUtil.getResponseBytes(response.getResponseCode(), response.getHeaders(),
				response.getOut(), request.getProtocol(), request.getUri(), response.getContentLength(),
				request.getMethod());
		if (response instanceof ResponseImpl) {
			ResponseImpl im = (ResponseImpl) response;
			im.out.clear();
			im.headers.clear();
		}
		return responseBytes;
	}

	static Ref getResponseBytes(int responseCode, Map<String, List<Object>> headers, Collection<ByteData> out,
			String protocol, String uri, long contentLen, HttpMethod method) {
		final StringBuffer responseBody = new StringBuffer();

		Object rescodetxt = null;
		if (!NullCheck.isNullOrEmpty(Configuration.defaultResponseCodes)) {
			rescodetxt = Configuration.defaultResponseCodes.get(String.valueOf(responseCode));
		}

		if (rescodetxt == null)
			responseBody.append(protocol).append(" ").append(responseCode).append(" ").append(LINE_SEPARATOR);
		else
			responseBody.append(protocol).append(" ").append(responseCode).append(" ").append(rescodetxt)
					.append(LINE_SEPARATOR);

//		Date endtime = new Date();
		long enddate = System.currentTimeMillis();
		responseBody.append("Date: ").append(dateFormat.format(enddate)).append(LINE_SEPARATOR);

		for (Entry<String, List<Object>> e : headers.entrySet()) {
			List<Object> value = e.getValue();
			if (!NullCheck.isNullOrEmpty(value)) {
				for (Object ov : value) {
					responseBody.append(e.getKey()).append(": ").append(ov).append(LINE_SEPARATOR);
				}
			}
		}
		responseBody.append(LINE_SEPARATOR);

		Ref ref = new Ref();
		ref.rc = responseCode;
		ref.uri = uri;
		ref.method = method;
		ref.endtime = enddate;
		
		ref.queue.add(new ByteData(responseBody.toString().getBytes()));
		ref.queue.addAll(out);
		ref.cl = contentLen;
		return ref;
	}

	static void stopRemote() {
		String url = DEFAULT_PROTOCOL+"://" + Server.DEFAULT_HOST + ":" + Server.DEFAULT_PORT + Configuration.stopUri;
		BufferedReader in = null;
		try {
			final HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
			int responseCode = con.getResponseCode();
			final StringBuffer response = new StringBuffer();
			in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			if (responseCode == 200) {
				System.out.println("Server stopped");
			}
		} catch (Throwable e) {
			logger.error("Failed on stop::", e);
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					logger.error("Failed on stop::", e);
				}
		}

	}

	static URL[] toArray(List<URL> urls) {
		Object[] objarr = urls.toArray();
		URL[] array = new URL[objarr.length];
		for (int i = 0; i < objarr.length; i++)
			array[i] = (URL) objarr[i];
		return array;
	}

	static void allUrls(File root, List<URL> urls) throws MalformedURLException {
		File[] list = root.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				if (NullCheck.isNullOrEmpty(name))
					return false;
				final String ln = name.toLowerCase(Locale.getDefault());
				return ln.endsWith(".jar") || ln.endsWith(".properties") || ln.endsWith(".xml") || ln.endsWith(".json");
			}
		});
		if (NullCheck.isNullOrEmpty(list))
			return;

		for (File f : list) {
			if (f.isDirectory()) {
				allUrls(f, urls);
			} else {
				urls.add(f.toURI().toURL());
				logger.info("Hotdeploy :: Added file {}", f.getAbsoluteFile());
			}
		}
	}

	static void unzip(File destinationFolder, File zipFile) throws IOException, InterruptedException {

		if (!destinationFolder.exists())
			destinationFolder.mkdirs();

		byte[] buffer = new byte[2048];

		try (FileInputStream fInput = new FileInputStream(zipFile);
				ZipInputStream zipInput = new ZipInputStream(fInput);) {

			ZipEntry entry = zipInput.getNextEntry();

			while (entry != null) {
				String entryName = entry.getName();
				File file = new File(destinationFolder.getAbsolutePath() + File.separator + entryName);

				logger.info("Hotdeploy :: Unzip file {} to {}", entryName, file.getAbsolutePath());

				// create the directories of the zip directory
				if (entry.isDirectory()) {
					File newDir = new File(file.getAbsolutePath());
					if (!newDir.exists()) {
						boolean success = newDir.mkdirs();
						if (success == false) {
							logger.info("Problem creating Folder");
						}
					}
				} else {
					FileOutputStream fOutput = new FileOutputStream(file);
					int count = 0;
					while ((count = zipInput.read(buffer)) > 0) {
						// write 'count' bytes to the file output stream
						fOutput.write(buffer, 0, count);
					}
					fOutput.close();
				}
				// close ZipEntry and take the next one
				zipInput.closeEntry();
				entry = zipInput.getNextEntry();
			}

			// close the last ZipEntry
			zipInput.closeEntry();

		}
	}

	static void del(File f) {
		try {
			if (f.isDirectory()) {
				if (f.list().length == 0) {
					f.delete();
				} else {
					for (String t : f.list())
						del(new File(f, t));

					if (f.list().length == 0) {
						f.delete();
					}
				}
			} else {
				f.delete();
			}
		} catch (Throwable e) {
			logger.error("Failed in delete file :: ", e);
		}
	}

	public static byte[] read(File file) throws IOException {
		if (file == null)
			return null;
		else if (!file.exists())
			return null;

		ByteBuffer bb = readBB(file);
		byte[] data = new byte[bb.remaining()];
		bb.get(data, 0, data.length);
		return data;
	}

	public static MappedByteBuffer readBB(File file) throws IOException {
		if (file == null)
			return null;
		else if (!file.exists())
			return null;
		RandomAccessFile randomAccessFile = null;
		try {
			randomAccessFile = new RandomAccessFile(file, "r");
			final FileChannel fileChannel = randomAccessFile.getChannel();
			return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
		} finally {
			if (randomAccessFile != null) {
				randomAccessFile.close();
			}
		}
	}

	static void scanApps(File root) {
		File[] list = root.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		if (NullCheck.isNullOrEmpty(list))
			return;

		for (File f : list) {
			try {
				File scanpackagesFile = new File(f, SCANPACKAGES_TOKEN);
				if( scanpackagesFile.exists() )
					new App(f.getName(), new String(read(scanpackagesFile))).deploy();
				else{
					del(f);
					logger.info("Invalid folder " + f.getAbsolutePath()+" removed!");
				}
			} catch (Exception e) {
				logger.error("Failed in scan Apps :: ", e);
			}
		}
		logger.info("Discovered Apps :: " + Utils.toString(Admin.allHotDeployedArtifacts.keySet()) );
	}

	static void addProxyRouteRuntime(String name, String method, String location, String proxyPass, String dir, Collection<Route> rts, Map<String, List<Object>> header) {
		HttpMethod httpMethod = HttpMethod.ALL;
		if (!NullCheck.isNullOrEmpty(method))
			httpMethod = HttpMethod.valueOf(method);

		if (!RequestUtil.validateRouteUri(location))
			throw new IllegalArgumentException("Illegal location(" + location + ") specified!");

		String proxy_pass = proxyPass;
		boolean notNullProxy = !NullCheck.isNullOrEmpty(proxy_pass);
		boolean notNullDir = !NullCheck.isNullOrEmpty(dir);
		if (notNullProxy && notNullDir)
			throw new IllegalArgumentException("Illegal proxy_pass(" + proxyPass + ") and dir(" + dir + ") specified!");
		else if(!notNullProxy && !notNullDir)
			throw new IllegalArgumentException("Illegal proxy_pass(" + proxyPass + ") and dir(" + dir + ") specified!");
		
		if (notNullProxy) {
			proxy_pass = Utils.replaceAll(proxy_pass, "$host", Server.DEFAULT_HOST);
			proxy_pass = Utils.replaceAll(proxy_pass, "$port", String.valueOf(Server.DEFAULT_PORT));
		}
		if (notNullDir) {
			dir = Utils.replaceAll(dir, "$home", new File(".").getAbsolutePath());
		}
		ProxyRoute prp = new ProxyRoute(name, proxy_pass, dir, location, httpMethod, null, null, false, header);
//		Collection<Route> rts = Configuration.routes;
		for (Route rt : rts) {
			if (rt instanceof ProxyRoute) {
				ProxyRoute prt = (ProxyRoute) rt;
				if (prt.uri.equals(location) && (httpMethod == prt.httpMethod || prt.httpMethod == HttpMethod.ALL)) {
					if (NullCheck.isNullOrEmpty(prt.dir) && !notNullDir && proxy_pass.equals(prt.proxy_pass))
						throw new IllegalArgumentException(
								"Duplicate proxy proxy_pass(" + proxyPass + ") and dir(" + dir + ") specified!");
					else if (NullCheck.isNullOrEmpty(prt.proxy_pass) && !notNullProxy && dir.equals(prt.dir))
						throw new IllegalArgumentException(
								"Duplicate proxy proxy_pass(" + proxyPass + ") and dir(" + dir + ") specified!");
				}
			}
		}
		rts.add(prp);
		logger.info("Discovered Proxy setting :: {}", prp.toString());
	}

	static Map<String, List<Object>> transform(Map<String, Object> in) {
		Map<String, List<Object>> out = new Amap<>();
		if (!NullCheck.isNullOrEmpty(in)) {
			for (Entry<String, Object> e : in.entrySet()) {
				List<Object> list = out.get(e.getKey());
				if (list == null) {
					list = new DoublyLinkedList<>();
					out.put(e.getKey(), list);
				}
				list.add(e.getValue());
			} 
		}
		return out;
	}
}

final class Ref {
	String uri = null;
	int rc;
	long cl = 0;
	long endtime;
	HttpMethod method = null;
	Queue<ByteData> queue = new DoublyLinkedList<>();
}

final class RequestUriTokens {
	String[] uriTokens;
	int[] paramIdx;
	int[] argsIdx;
	int reqIdx = -1, resIdx = -1;
}
