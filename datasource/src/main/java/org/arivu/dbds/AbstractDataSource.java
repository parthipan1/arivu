/**
 * 
 */
package org.arivu.dbds;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;

import org.arivu.datastructure.Amap;
import org.arivu.pool.Pool;
import org.arivu.pool.PoolFactory;
import org.arivu.utils.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base data source class delegates all call to underlying Pool Implementation. 
 * 
 * @author P
 *
 */
abstract class AbstractDataSource implements DataSource {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractDataSource.class);
	
	static interface UsePool{
		Pool<DelegatingConnection> create(PoolFactory<DelegatingConnection> factory);
	}
	
	/**
	 * 
	 */
	public static final LinkedList<AbstractDataSource> instanceQueue = new LinkedList<AbstractDataSource>();
	
	/**
	 * 
	 */
	volatile boolean closed = false;
	/**
	 * 
	 */
	private int timoutCheckInterval = 10000;// unit in millisecs
	
	/**
	 * 
	 */
	private PrintWriter logWriter = null;
	
	/**
	 * 
	 */
	private int loginTimeout = 0,connectionTimeout = 250;
	/**
	 * 
	 */
	private boolean autoCommit = true;
	/**
	 * 
	 */
	private java.util.logging.Logger parentLogger = null;
	/**
	 * 
	 */
	String user = null, password = null, url = null, driver = null, name = null;
	
	/**
	 * 
	 */
	final Pool<DelegatingConnection> pool;
	/**
	 * 
	 */
	final PoolFactory<DelegatingConnection> factory = new PoolFactory<DelegatingConnection>() {
		
		final String getValue(final Object obj,final String defValue){
			if( obj != null && !NullCheck.isNullOrEmpty(obj.toString())){
				return obj.toString();
			}else {
				return defValue;
			}
				
		}
		
		@Override
		public DelegatingConnection create(final Map<String,Object> params) {
			String cuser=AbstractDataSource.this.user, cpassword=AbstractDataSource.this.password, cdriver=AbstractDataSource.this.driver, curl=AbstractDataSource.this.url;
			if(!NullCheck.isNullOrEmpty(params)){
				cuser=getValue(params.get("user"), cuser);
				cpassword=getValue(params.get("password"), cpassword);
				cdriver=getValue(params.get("driver"), cdriver);
				curl=getValue(params.get("url"), curl);
			}
			return new DelegatingConnectionImpl( cf.create(cuser, cpassword, cdriver, curl));
		}

		@Override
		public void close(final DelegatingConnection conn) {
			if(conn!=null){
				try {
					conn.close();
				} catch (SQLException e) {
					logger.error("Failed",e);
				}
			}
		}

		@Override
		public void clear(DelegatingConnection arg0) {
			
		}

	};
	
	private static final ExecutorService networkExe = Executors.newFixedThreadPool(1);
	
	/**
	 * 
	 */
	final ConnectionFactory cf;
	
	/**
	 * 
	 */
	final ConnectionFactory defaultCF  = new ConnectionFactory() {
		
		@Override
		public Connection create(final String cuser, final String cpassword, final String cdriver, final String curl) {
			try {
				Class.forName(cdriver);
				Connection connection = DriverManager.getConnection(curl, cuser, cpassword);
				connection.setAutoCommit(autoCommit);
				connection.setNetworkTimeout(networkExe, connectionTimeout);
				return connection;
			} catch (Exception e) {
				logger.error("Failed",e);
				throw new RuntimeException(e);
			}
		}
	};
	
	final Thread hook = new Thread(new Runnable() {
		@Override
		public void run() {
			close();
		}
	});
	/**
	 * @param cf
	 * @param usePool
	 */
	public AbstractDataSource(final ConnectionFactory cf,final UsePool usePool) {
		super();
		instanceQueue.push(this);
		if(cf == null){
			this.cf = defaultCF;
		}else{
			this.cf = cf;
		}
		this.pool = usePool.create(factory);
		try {
			Runtime.getRuntime().addShutdownHook(hook);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * @throws MalformedObjectNameException 
	 * 
	 */
	/**
	 * @param usePool
	 */
	public AbstractDataSource(final UsePool usePool){
		this(null,usePool);
	}

	/**
	 * 
	 */
	ObjectName mxbeanName = null;
	/**
	 * 
	 */
	int beanInstanceCnt = 0;
	/**
	 * 
	 */
	public void registerMXBean() {
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer(); 
			if( name == null ){
				String name2 = "org.arivu.dbds:type="+getClass().getSimpleName()+"."+(beanInstanceCnt++);
				mxbeanName = new ObjectName(name2);
				logger.debug( "Registered MXBean Bean "+name2);
			}else{
				String name2 = "org.arivu.dbds:type="+getClass().getSimpleName()+"."+name+(beanInstanceCnt++);
				mxbeanName = new ObjectName(name2);
				logger.debug( "Registered MXBean Bean "+name2);
			}
			mbs.registerMBean(getConnectionPoolMXBean() , mxbeanName);
		} catch (InstanceAlreadyExistsException e) {
			logger.error("Failed",e);
			registerMXBean();
		} catch (Exception e) {
			logger.error("Failed",e);
		}
	}

	/**
	 * 
	 */
	void unregisterMXBean(){
		if (mxbeanName!=null) {
			try {
				ManagementFactory.getPlatformMBeanServer().unregisterMBean(mxbeanName);
			} catch (Exception e) {
				logger.error("Failed",e);
			} 
		}
	}


	/* (non-Javadoc)
	 * @see org.arivu.dbds.AbstractDataSource#getConnectionPoolMXBean()
	 */
	/**
	 * @return
	 */
	ConnectionPoolMXBean getConnectionPoolMXBean() {
		final AbstractDataSource that = this;
		return new ConnectionPoolMXBean(){
			@Override
			public int getMaxPoolSize() {
				return pool.getMaxPoolSize();
			}
			@Override
			public void setTimoutCheckInterval(int intreval) {
				if( intreval == 0) throw new IllegalArgumentException("Zero not allowed for timoutCheckInterval!");
				logger.error("Failed","JMX reset intreval value "+intreval+" old value "+that.getInterval());
				that.setInterval(intreval);
			}
			@Override
			public void recycle() {
				logger.error("Failed","JMX invoked recycle!");
				that.recycle();
			}
			@Override
			public void cleanUp() {
				logger.error("Failed","JMX invoked cleanup!");
				that.cleanUp();
			}
			@Override
			public void gc() {
			}
			@Override
			public int getTimoutCheckInterval() {
				return that.getInterval();
			}
			@Override
			public void setMaxPoolSize(int size) {
				if( size == 0) throw new IllegalArgumentException("Zero not allowed for maxPoolSize!");
				logger.info("JMX reset maxPoolSize value "+size+" old value "+pool.getMaxPoolSize());
				that.setMaxPoolSize(size);
			}
			@Override
			public void setMaxUsedCnt(int cnt) {
				if( cnt == 0) throw new IllegalArgumentException("Zero not allowed for maxConnectionReuseCount!");
				logger.info("JMX reset maxConnectionReuseCount value "+cnt+" old value "+pool.getMaxReuseCount());
				pool.setMaxReuseCount(cnt);
			}
			@Override
			public int getMaxUsedCnt() {
				return pool.getMaxReuseCount();
			}
			@Override
			public void setMaxConnectionReuseTime(int time) {
				if( time == 0) throw new IllegalArgumentException("Zero not allowed for maxConnectionReuseTime!");
				logger.info("JMX reset maxConnectionReuseTime value "+time+" old value "+pool.getLifeSpan());
				pool.setLifeSpan(time);
			}
			@Override
			public int getMaxConnectionReuseTime() {
				return pool.getLifeSpan();
			}
//			@Override
//			public void setUser(String user) {
//				that.setUser(user);
//			}
			@Override
			public String getUser() {
				return that.user;
			}
//			@Override
//			public void setPassword(String password) {
//				that.setPassword(password);
//			}
			@Override
			public String getPassword() {
				return that.password;
			}
//			@Override
//			public void setUrl(String url) {
//				that.setUrl(url);
//			}
			@Override
			public String getUrl() {
				return that.url;
			}
//			@Override
//			public void setDriver(String driver) {
//				that.setDriver(driver);
//			}
			@Override
			public String getDriver() {
				return that.driver;
			}
//			@Override
//			public boolean isEnablelog() {
//				return DSLogger.enablelog;
//			}
			@Override
			public int getIdleTimeout() {
				return that.getIdleTimeout();
			}
			@Override
			public void setIdleTimeout(int idleTimeout) {
				that.setIdleTimeout(idleTimeout);
			}
//			@Override
//			public void setEnablelog(boolean enablelog) {
//				DSLogger.enablelog = enablelog;
//			}
//			@Override
//			public String getLoglevel() {
////				return DSLogger.loglevel.getName();
////				return .
//			}
//			@Override
//			public void setLoglevel(String loglevel) {
//				Level parse = Level.parse(loglevel);
//				
//				if(parse!=null)
//					DSLogger.loglevel = parse;
//			}
//			@Override
//			public boolean isAsynclog() {
//				return DSLogger.asynclog;
//			}
//			@Override
//			public void setAsynclog(boolean asynclog) {
//				DSLogger.asynclog = asynclog;
//			}
		};
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sql.CommonDataSource#getLogWriter()
	 */
//	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return this.logWriter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sql.CommonDataSource#getLoginTimeout()
	 */
//	@Override
	public int getLoginTimeout() throws SQLException {
		return this.loginTimeout;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sql.CommonDataSource#getParentLogger()
	 */
//	@Override
	public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return this.parentLogger;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sql.CommonDataSource#setLogWriter(java.io.PrintWriter)
	 */
//	@Override
	public void setLogWriter(PrintWriter writer) throws SQLException {
		this.logWriter = writer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sql.CommonDataSource#setLoginTimeout(int)
	 */
//	@Override
	public void setLoginTimeout(int timeout) throws SQLException {
		this.loginTimeout = timeout;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
	 */
//	@Override
	public boolean isWrapperFor(Class<?> klass) throws SQLException {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.sql.Wrapper#unwrap(java.lang.Class)
	 */
//	@Override
	public <T> T unwrap(Class<T> klass) throws SQLException {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sql.DataSource#getConnection()
	 */
//	@Override
	public Connection getConnection() throws SQLException {
		return getThreadConnection();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.sql.DataSource#getConnection(java.lang.String,
	 * java.lang.String)
	 */
//	@Override
	public Connection getConnection(String user, String password) throws SQLException {
		Map<String,Object> params = new Amap<String, Object>();
		params.put("user", user);
		params.put("password", password);
		return pool.get(params);
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public void setUser(String user) {
		this.user = user;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public void destroy() {
		networkExe.shutdownNow();
		this.closed = true;
		try {
			unregisterMXBean();
		} catch (Exception e1) {
			System.err.println(e1.toString());
		}
		try {
			pool.close();
		} catch (Exception e) {
			logger.error("Failed",e);
		}
		try {
			Runtime.getRuntime().removeShutdownHook(hook);
		} catch (Throwable e) {
//			System.err.println(e.toString());
		}
	}

	public void close() {
		destroy();
	}
//
//	final CloseListener closeListener = new CloseListener() {
//
//		@Override
//		public void close(org.arivu.dbds.DelegatingConnectionImpl conn) {
//			returnBack(conn);
//		}
//	};
//	
//	DelegatingConnectionImpl create() {
//		if (this.closed) 
//			return null;
//		return new DelegatingConnectionImpl(cf.create(), closeListener);
//	}

	int getInterval() {
		return timoutCheckInterval;
	}

	public void setInterval(int interval) {
		this.timoutCheckInterval = interval;
	}


	/* (non-Javadoc)
	 * @see org.arivu.dbds.AbstractDataSource#getThreadConnection()
	 */
	Connection getThreadConnection() {
		return pool.get(null);
	}

	/**
	 * 
	 */
	void cleanUp(){
		pool.clear();
	}

	/**
	 * 
	 */
	void recycle(){
		pool.clear();
	} 

	/**
	 * @return pool.getMaxPoolSize()
	 */
	public int getMaxPoolSize() {
		return pool.getMaxPoolSize();
	}

	/**
	 * @param maxPoolSize
	 */
	public void setMaxPoolSize(int maxPoolSize) {
		pool.setMaxPoolSize(maxPoolSize);
	}

	/**
	 * @return pool.getMaxReuseCount()
	 */
	public int getMaxReuseCount() {
		return pool.getMaxReuseCount();
	}

	/**
	 * @param maxReuseCount
	 */
	public void setMaxReuseCount(int maxReuseCount) {
		pool.setMaxReuseCount(maxReuseCount);
	}

	/**
	 * @return pool.getLifeSpan()
	 */
	public int getMaxReuseTime() {
		return pool.getLifeSpan();
	}

	/**
	 * @param maxReuseTime
	 */
	public void setMaxReuseTime(int maxReuseTime) {
		pool.setLifeSpan(maxReuseTime);
	}
	
	/**
	 * @return pool.getIdleTimeout()
	 */
	public int getIdleTimeout() {
		return pool.getIdleTimeout();
	}

	/**
	 * @param idleTimeout
	 */
	public void setIdleTimeout(int idleTimeout) {
		pool.setIdleTimeout(idleTimeout);
	}

	/**
	 * @return connectionTimeout
	 */
	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	/**
	 * @param connectionTimeout
	 */
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	/**
	 * @return autoCommit
	 */
	public boolean isAutoCommit() {
		return autoCommit;
	}

	/**
	 * @param autoCommit
	 */
	public void setAutoCommit(boolean autoCommit) {
		this.autoCommit = autoCommit;
	}

//	static class LogLevel {
//
//	    /**
//	     * Allowed levels, as an enum. Import using "import [package].LogLevel.Level"
//	     * Every logging implementation has something like this except SLF4J.
//	     */
//
//	    public static enum Level {
//	        TRACE, DEBUG, INFO, WARN, ERROR
//	    }
//
//	    /**
//	     * This class cannot be instantiated, why would you want to?
//	     */
//
//	    private LogLevel() {
//	        // Unreachable
//	    }
//
//	    public static Level log(String logLevel) {
//	    	return Level.valueOf(logLevel);
//	    }
//
//	}
}
