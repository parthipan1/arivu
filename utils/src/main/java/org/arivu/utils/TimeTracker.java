package org.arivu.utils;

import java.util.HashMap;

import java.util.Map;

public final class TimeTracker {

	final String name;
	final Map<String, Object> tracks = new HashMap<String, Object>();

	public TimeTracker(String name) {
		super();
		this.name = name;
	}

	public TimeTracker(String name,Map<String, Object> data) {
		this(name);
		this.tracks.putAll(data);
	}
	
	long s = System.currentTimeMillis();

	final long c = s;
	
	public TimeTracker nextTrack(final String tag) {
		return track(tag, s) ;
	}
	
	public TimeTracker totalTrack(final String tag) {
		tracks.put(tag, (System.currentTimeMillis() - c));
		return this ;
	}
	
	public TimeTracker stop() {
		return track("total", c) ;
	}
	
	private TimeTracker track(final String tag,long t) {
		tracks.put(tag, (System.currentTimeMillis() - t));
		s = System.currentTimeMillis();
		return this;
	}

	
	@Override
	public String toString() {
		return name + " tracks=" + tracks;
	}

	public static TimeTracker start(String name) {

		return new TimeTracker(name);

	}

}
