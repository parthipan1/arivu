package org.arivu.log.appender;

public final class AppenderProperties {
	public static long FILE_THRESHOLD_LIMIT = 50 * 1024 * 1024;
	public static String FILE_EXT_FORMAT = "yyyy-MM-dd'T'HH:mm:ss:SSS'Z'Z";
	public static final String separator;
	static{
		separator = System.getProperty( "line.separator" );
	}
}
