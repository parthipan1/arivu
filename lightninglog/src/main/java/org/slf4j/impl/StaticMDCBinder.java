/**
 * 
 */
package org.slf4j.impl;

import org.arivu.log.LightningMDCAdapter;
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
	     return new LightningMDCAdapter();
	  }
	  
	  public String  getMDCAdapterClassStr() {
	    return LightningMDCAdapter.class.getName();
	  }
	}
