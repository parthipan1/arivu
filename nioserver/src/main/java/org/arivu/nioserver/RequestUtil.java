package org.arivu.nioserver;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.HttpURLConnection;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.utils.NullCheck;
import org.arivu.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestUtil {

	private static final Logger logger = LoggerFactory.getLogger(RequestUtil.class);

	static final String ENC_UTF_8 = "UTF-8";
	private static final byte BYTE_13 = (byte) 13;
	static final byte BYTE_10 = (byte) 10;
	static final String divider = System.lineSeparator() + System.lineSeparator();

	static Request parseRequest(final StringBuffer buffer) {
		String content = buffer.toString();
		byte[] bytes = content.getBytes();
		int indexOf = -1;
		for (int i = 3; i < bytes.length; i++) {
			if (bytes[i] == bytes[i - 2] && bytes[i] == BYTE_10 && bytes[i - 1] == bytes[i - 3]
					&& bytes[i - 1] == BYTE_13) {
				indexOf = i;
				break;
			}
		}

		String metadata = null;
		String body = null;

		if (indexOf == -1) {
			metadata = content;
		} else {
			metadata = content.substring(0, indexOf - 1);
			body = content.substring(indexOf);
		}

		String[] split = metadata.split(System.lineSeparator());

		String[] split2 = split[0].split(" ");

		// System.out.println("REQ METHOD :: "+split2[0]);
		logger.debug("Parsing RequestImpl :: " + content);
		HttpMethod valueOf = HttpMethod.valueOf(split2[0]);
		if (valueOf == null)
			throw new IllegalArgumentException("Unknown RequestImpl " + metadata);
		String uriWithParams = split2[1];
		String protocol = split2[2];

		Map<String, String> tempheaders = new Amap<String, String>();
		for (int i = 1; i < split.length; i++) {
			String h = split[i];
			int indexOf2 = h.indexOf(": ");
			if (indexOf2 == -1) {
				tempheaders.put(h, "");
			} else {
				tempheaders.put(h.substring(0, indexOf2), h.substring(indexOf2 + 2));
			}
		}

		int indexOf3 = uriWithParams.indexOf("?");
		String uri = uriWithParams;
		Map<String, Collection<String>> tempparams = null;
		if (indexOf3 > 0) {
			uri = uriWithParams.substring(0, indexOf3);
			tempparams = parseParams(uriWithParams.substring(indexOf3 + 1));
		}
		return new RequestImpl(valueOf, uri, uriWithParams, protocol, tempparams, Utils.unmodifiableMap(tempheaders),
				body);
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
			// throw new IllegalStateException("Method signature not in line
			// with @Path! for method "+method);
			return MethodInvoker.variable;
		}
	}

	static final Pattern validUrl = Pattern.compile("^[a-zA-Z0-9-_]*$");//"[a-zA-Z0-9_-]"
//	static final Pattern validName = Pattern.compile("[a-zA-Z0-9_]");

	static boolean validateRouteUri(final String uri) {
		if( NullCheck.isNullOrEmpty(uri) ) return false;
		else if( !uri.startsWith("/") ) return false;
		String[] split = uri.split("/");
		for (int i=1;i<split.length;i++) {
			String uritkn = split[i];
			int si = uritkn.indexOf("{");
			if (si == -1) {
				if(!validUrl.matcher(uritkn).matches())
					return false;
			} else {
				int ei = uritkn.indexOf("}");
				String paramName = uritkn.substring(1, uritkn.length() - 1);
				if (ei < si || si != 0 && ei != uritkn.length() - 1 
						|| NullCheck.isNullOrEmpty(paramName)
						|| !validUrl.matcher(paramName).matches()) {
					return false;
				}
			}
		}
		return true;
	}

	static Route getMatchingRoute(Collection<Route> paths, final String uri, final HttpMethod httpMethod,
			final boolean retNull) {
		Route df = null;
		final Route in = new Route(uri, httpMethod);
		for (Route rq : paths) {
			if (rq.rut == null) {
				if (in.equals(rq))
					return rq;
				else if (rq.httpMethod == HttpMethod.ALL) {
					if (rq.uri.equals("/*"))
						df = rq;
					else if (rq.uri.equals(uri))
						return rq;
					else if (rq instanceof ProxyRoute && uri.startsWith(rq.uri))
						return rq;
				} else if (rq instanceof ProxyRoute && uri.startsWith(rq.uri)) {
					return rq;
				}
			} else {
				if (rq.httpMethod == HttpMethod.ALL || rq.httpMethod == httpMethod) {
					String[] split = uri.split("/");
					if (rq.rut.uriTokens.length == split.length) {
						boolean match = true;
						for (int i = 0; i < split.length; i++) {
							if (rq.rut.paramIdx[i] == -1) {
								if (!rq.rut.uriTokens[i].equals(split[i])) {
									match = false;
									break;
								}
							}
						}
						if (match)
							return rq;
					}
				}
			}
		}
		if (retNull)
			return null;
		else
			return df;
	}

	static RequestUriTokens parseRequestUriTokens(String uri, Method method) {
		RequestUriTokens rut = new RequestUriTokens();
		rut.uriTokens = uri.split("/");
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
			int si = uritkn.indexOf("{");
			int ei = uritkn.indexOf("}");
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

	private static final String LINE_SEPARATOR = System.lineSeparator();

	static void accessLog(int responseCode, String uri, long start, long end, int size,
			SocketAddress remoteSocketAddress) {
		if (!uri.equals(Configuration.stopUri)) {
			StringBuffer access = new StringBuffer();
			access.append("[").append(dateFormat.format(new Date(start))).append("] ").append(uri).append(" ")
					.append(responseCode).append(" ").append(size).append(" [").append((end - start)).append("] ")
					.append(remoteSocketAddress.toString());
			Server.accessLog.append(access.toString());
		}
	}

	static Ref getResponseBytes(Request request, Response response) {
		return RequestUtil.getResponseBytes(response.getResponseCode(), response.getHeaders(), response.getOut(),
				request.getProtocol(), request.getUri());
	}

	static Ref getResponseBytes(int responseCode, Map<String, Object> headers, ByteArrayOutputStream out,
			String protocol, String uri) {
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

		Date enddate = new Date();
		responseBody.append("Date: ").append(enddate.toString()).append(LINE_SEPARATOR);

		for (Entry<String, Object> e : headers.entrySet()) {
			responseBody.append(e.getKey()).append(": ").append(e.getValue()).append(LINE_SEPARATOR);
		}
		responseBody.append(LINE_SEPARATOR);

		Ref ref = new Ref();
		ref.rc = responseCode;
		ref.uri = uri;
		ref.headerBytes = responseBody.toString().getBytes();
		ref.bodyBytes = out.toByteArray();

		// ByteBuffer resBytes =
		// ByteBuffer.allocate(headerBytes.length+bodyBytes.length);
		// resBytes.put(headerBytes);
		// resBytes.put(bodyBytes);
		// ref.buf = resBytes;
		// ref.len = bodyBytes.length;
		return ref;
	}

	final static DateFormat dateFormat = new SimpleDateFormat("EEE MMM d hh:mm:ss.SSS yyyy");

	static void stopRemote() {
		String url = "http://" + Server.DEFAULT_HOST + ":" + Server.DEFAULT_PORT + Configuration.stopUri;
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
}

class Ref {
	String uri = null;
	int rc;
	byte[] headerBytes;
	byte[] bodyBytes;
}

final class RequestUriTokens {
	String[] uriTokens;
	int[] paramIdx;
	int[] argsIdx;
	int reqIdx = -1, resIdx = -1;
}
