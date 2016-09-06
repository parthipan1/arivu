/**
 * 
 */
package org.arivu.dbds;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ThreadLocal datasource creates a connection per thread and keeps it until the
 * thread is active.
 * 
 * @author P
 *
 */
abstract class BaseDataSourceFactory implements ObjectFactory {
	private static final Logger logger = LoggerFactory.getLogger(BaseDataSourceFactory.class);
	
	final AbstractDataSource ds;

	BaseDataSourceFactory(AbstractDataSource ds) {
		super();
		this.ds = ds;
	}

	/**
	 * 
	 */
	public BaseDataSourceFactory() {
		this.ds = new ThreadlocalDataSource();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.naming.spi.ObjectFactory#getObjectInstance(java.lang.Object,
	 * javax.naming.Name, javax.naming.Context, java.util.Hashtable)
	 */
	@Override
	public Object getObjectInstance(Object obj, Name arg1, Context arg2, Hashtable<?, ?> arg3) throws Exception {
		Reference ref = (Reference) obj;
		Enumeration<RefAddr> addrs = ref.getAll();

		// Loop on all the elements in the object.
		while (addrs.hasMoreElements()) {
			RefAddr addr = addrs.nextElement();
			final String nameAddr = addr.getType();
			final String value = (String) addr.getContent();
			logger.info( "datasource nameAddr=" + nameAddr + " value=" + value);
			// Set the property string to the MyProperty object.
			if (nameAddr.equals("username")) {
				ds.setUser(value);
				ds.setName(value);
			} else if (nameAddr.equals("password")) {
				ds.setPassword(value);
			} else if (nameAddr.equals("url")) {
				ds.setUrl(value);
			} else if (nameAddr.equals("driverClassName")) {
				ds.setDriver(value);
			} else if (nameAddr.equals("timoutCheckInterval")) {
				try {
					ds.setInterval(Integer.parseInt(value));
				} catch (Throwable e) {
					logger.error("Failed", e);
				}
			} else if (nameAddr.equals("maxPoolSize")) {
				try {
					ds.setMaxPoolSize(Integer.parseInt(value));
				} catch (Throwable e) {
					logger.error("Failed", e);
				}
			} else if (nameAddr.equals("maxReuseCount")) {
				try {
					ds.setMaxReuseCount(Integer.parseInt(value));
				} catch (Throwable e) {
					logger.error("Failed", e);
				}
			} else if (nameAddr.equals("maxReuseTime")) {
				try {
					ds.setMaxReuseTime(Integer.parseInt(value));
				} catch (Throwable e) {
					logger.error("Failed", e);
				}
			} else if (nameAddr.equals("maxIdle")) {
				try {
					ds.setIdleTimeout(Integer.parseInt(value));
				} catch (Throwable e) {
					logger.error("Failed", e);
				}
			} else if (nameAddr.equals("connectionTimeout")) {
				try {
					ds.setConnectionTimeout(Integer.parseInt(value));
				} catch (Throwable e) {
					logger.error("Failed", e);
				}
			} else if (nameAddr.equals("defaultAutoCommit")) {
				try {
					ds.setAutoCommit(Boolean.parseBoolean(value));
				} catch (Throwable e) {
					logger.error("Failed", e);
				}
			}
		}
		ds.registerMXBean();
		return ds;
	}

}
