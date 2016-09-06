/**
 * 
 */
package org.arivu.log;

import java.util.Collections;
import java.util.Map;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.Threadlocal;
import org.slf4j.spi.MDCAdapter;

/**
 * @author P
 *
 */
public class AsyncMDCAdapter implements MDCAdapter {

	final Threadlocal<Map<String, String>> mdc = new Threadlocal<Map<String, String>>(new Threadlocal.Factory<Map<String, String>>() {

		@Override
		public Map<String, String> create(Map<String, Object> arg0) {
			return new Amap<String, String>();
		}
	}, -1);
	
	/* (non-Javadoc)
	 * @see org.slf4j.spi.MDCAdapter#put(java.lang.String, java.lang.String)
	 */
	@Override
	public void put(String key, String val) {
		mdc.get(null).put(key, val);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.spi.MDCAdapter#get(java.lang.String)
	 */
	@Override
	public String get(String key) {
		return mdc.get(null).get(key);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.spi.MDCAdapter#remove(java.lang.String)
	 */
	@Override
	public void remove(String key) {
		mdc.get(null).remove(key);
	}

	/* (non-Javadoc)
	 * @see org.slf4j.spi.MDCAdapter#clear()
	 */
	@Override
	public void clear() {
		mdc.get(null).clear();
	}

	/* (non-Javadoc)
	 * @see org.slf4j.spi.MDCAdapter#getCopyOfContextMap()
	 */
	@Override
	public Map<String, String> getCopyOfContextMap() {
		return Collections.unmodifiableMap(mdc.get(null)) ;
	}

	/* (non-Javadoc)
	 * @see org.slf4j.spi.MDCAdapter#setContextMap(java.util.Map)
	 */
	@Override
	public void setContextMap(Map<String, String> contextMap) {
		
		if( contextMap!=null )
			mdc.get(null).putAll(contextMap);
	}

}
