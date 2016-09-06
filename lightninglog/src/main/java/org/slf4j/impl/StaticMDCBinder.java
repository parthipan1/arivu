/**
 * 
 */
package org.slf4j.impl;

import org.arivu.log.AsyncMDCAdapter;
import org.slf4j.spi.MDCAdapter;

/**
 * @author P
 *
 */
public class StaticMDCBinder {

	  
	  /**
	   * The unique instance of this class.
	   */
	  public static final StaticMDCBinder SINGLETON = new StaticMDCBinder();

	  private StaticMDCBinder() {
	  }
	  
	  /**
	   * Currently this method always returns an instance of 
	   * {@link StaticMDCBinder}.
	   */
	  public MDCAdapter getMDCA() {
	     return new AsyncMDCAdapter();
	  }
	  
	  public String  getMDCAdapterClassStr() {
	    return AsyncMDCAdapter.class.getName();
	  }
	}
