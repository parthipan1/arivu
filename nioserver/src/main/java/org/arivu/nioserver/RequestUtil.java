package org.arivu.nioserver;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestUtil {

	private static final Logger logger = LoggerFactory.getLogger(RequestUtil.class);

	private static final String ENC_UTF_8 = "UTF-8";
	private static final byte BYTE_13 = (byte) 13;
	private static final byte BYTE_10 = (byte) 10;
	static final String divider = System.lineSeparator() + System.lineSeparator();
	
	static Request parse(final StringBuffer buffer) {
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

//		System.out.println("REQ METHOD :: "+split2[0]);
		logger.debug("Parsing RequestImpl :: "+content);
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
		return new RequestImpl(valueOf, uri, uriWithParams, protocol, tempparams, Utils.unmodifiableMap(tempheaders), body);
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
}
