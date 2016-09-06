package org.arivu.dbds;

public interface ConnectionPoolMXBean {
	
	public int getMaxPoolSize();
	public void setMaxPoolSize(int size);
	
	public int getTimoutCheckInterval();
	public void setTimoutCheckInterval(int interval);
	
	public int getMaxUsedCnt();
	public void setMaxUsedCnt(int cnt);
	
	public int getMaxConnectionReuseTime();
	public void setMaxConnectionReuseTime(int time);
	
	public int getIdleTimeout();
	public void setIdleTimeout(int idleTimeout);
	
	public void recycle();
    public void cleanUp();
    public void gc();
    
//    public void setUser(String user);
    public String getUser();
//	public void setPassword(String password);
	public String getPassword();
//	public void setUrl(String url);
	public String getUrl();
//	public void setDriver(String driver);
	public String getDriver();

//	public boolean isAsynclog();
//	public void setAsynclog(boolean asynclog);
//	public boolean isEnablelog();
//	public void setEnablelog(boolean enablelog);
//	public String getLoglevel();
//	public void setLoglevel(String loglevel);
}
