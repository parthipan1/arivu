/**
 * 
 */
package org.slf4j.impl;

import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MarkerFactoryBinder;

/**
 * @author P
 *
 */
public class StaticMarkerBinder implements MarkerFactoryBinder {

	/**
	 * The unique instance of this class.
	 */
	public static final StaticMarkerBinder SINGLETON = new StaticMarkerBinder();

	final IMarkerFactory markerFactory = new BasicMarkerFactory();

	private StaticMarkerBinder() {
	}

	/**
	 * Currently this method always returns an instance of
	 * {@link BasicMarkerFactory}.
	 */
	public IMarkerFactory getMarkerFactory() {
		return markerFactory;
	}

	/**
	 * Currently, this method returns the class name of
	 * {@link BasicMarkerFactory}.
	 */
	public String getMarkerFactoryClassStr() {
		return BasicMarkerFactory.class.getName();
	}

}