package org.arivu.nioserver;

import java.util.Map;

import org.arivu.utils.Ason;
import org.arivu.utils.Utils;

public final class Configuration {

	static final String stopUri = "/zzxyz";
	static final Map<String, Object> defaultResponseHeader;
	static final Map<String, Object> defaultResponseCodes;
	static final int defaultResCode;
	private static final String CONFIGURATION_FILE = "arivu.nioserver.json";
	static {
		final Map<String, Object> json = Ason.loadProperties(CONFIGURATION_FILE);
		
		defaultResponseHeader = Utils.unmodifiableMap((Map<String, Object>)Ason.getObj(json, "response.header", null));
		defaultResponseCodes = Utils.unmodifiableMap((Map<String, Object>)Ason.getObj(json, "response.codes", null));
		defaultResCode = Ason.getNumber(json, "response.defaultcode", 200).intValue();
	}

}
