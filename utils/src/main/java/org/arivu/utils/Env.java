package org.arivu.utils;

public final class Env {

	public static String getEnv(String key, String dvalue) {
		return System.getProperty(key, (System.getenv().get(key) == null ? dvalue : System.getenv().get(key)));
	}
}
