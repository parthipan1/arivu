/**
 * 
 */
package org.arivu.dbds;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * @author P
 *
 */
public interface DelegatingConnection extends Connection {
	Connection getInnermostDelegate();
}

/**
 * @author P
 *
 */
final class DelegatingConnectionImpl implements DelegatingConnection {
	
	/**
	 * 
	 */
	final Connection delegate;
	/**
	 * 
	 */
//	final CloseListener closeListener;
	// keep track of time created / issued / closed
//	/**
//	 * 
//	 */
//	volatile long time = System.currentTimeMillis();
//	/**
//	 * 
//	 */
//	final long createTime = System.currentTimeMillis();
//	/**
//	 * 
//	 */
//	int usedCnt = 0;
	/**
	 * @param connection
	 */
	public DelegatingConnectionImpl(Connection connection) {
		super();
		if(connection instanceof DelegatingConnectionImpl){
			this.delegate =  ((DelegatingConnectionImpl) connection).getInnermostDelegate();
		}else{
			this.delegate = connection;
		}
//		this.closeListener = closeListener;
//		DSLogger.log(Level.INFO,"Creating database connection " + this.delegate);
	}

//	/**
//	 * @return
//	 */
//	DelegatingConnectionImpl setTime() {
//		this.time = System.currentTimeMillis();
//		return this;
//	}
	
	/* (non-Javadoc)
	 * @see org.arivu.dbds.DelegatingConnection#getInnermostDelegate()
	 */
	@Override
	public Connection getInnermostDelegate(){
		if(this.delegate instanceof DelegatingConnectionImpl){
			return ((DelegatingConnectionImpl) this.delegate).getInnermostDelegate();
		}else
			return this.delegate;
	}

//	/**
//	 * @throws SQLException
//	 */
//	void remove() throws SQLException {
//		if (this.delegate!=null && !this.delegate.isClosed()) {
//			this.delegate.close();
////			DSLogger.log(Level.INFO,"Closing database connection " + delegate);
//		}
//	}

	/**
	 * @return
	 */
//	public long getCreateTime() {
//		return this.createTime;
//	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#abort(java.util.concurrent.Executor)
	 */
	@Override
	public void abort(Executor executor) throws SQLException {
		this.delegate.abort(executor);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#clearWarnings()
	 */
	@Override
	public void clearWarnings() throws SQLException {
		this.delegate.clearWarnings();
	}

	/**
	 * @return
	 */
//	public int getUsedCnt() {
//		return this.usedCnt;
//	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#close()
	 */
	@Override
	public void close() throws SQLException {
		this.delegate.close();
//		this.usedCnt++;
//		this.setTime();
		// DSLogger.log(Level.INFO,"Close called for "+connection);
//		if (this.closeListener != null) {
//			this.closeListener.close(this);
//		}
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#commit()
	 */
	@Override
	public void commit() throws SQLException {
		this.delegate.commit();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#createArrayOf(java.lang.String, java.lang.Object[])
	 */
	@Override
	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		return this.delegate.createArrayOf(typeName, elements);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#createBlob()
	 */
	@Override
	public Blob createBlob() throws SQLException {
		return this.delegate.createBlob();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#createClob()
	 */
	@Override
	public Clob createClob() throws SQLException {
		return this.delegate.createClob();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#createNClob()
	 */
	@Override
	public NClob createNClob() throws SQLException {
		return this.delegate.createNClob();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#createSQLXML()
	 */
	@Override
	public SQLXML createSQLXML() throws SQLException {
		return this.delegate.createSQLXML();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#createStatement()
	 */
	@Override
	public Statement createStatement() throws SQLException {
		return this.delegate.createStatement();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#createStatement(int, int, int)
	 */
	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		return this.delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#createStatement(int, int)
	 */
	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		return this.delegate.createStatement(resultSetType, resultSetConcurrency);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#createStruct(java.lang.String, java.lang.Object[])
	 */
	@Override
	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		return this.delegate.createStruct(typeName, attributes);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getAutoCommit()
	 */
	@Override
	public boolean getAutoCommit() throws SQLException {
		return this.delegate.getAutoCommit();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getCatalog()
	 */
	@Override
	public String getCatalog() throws SQLException {
		return this.delegate.getCatalog();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getClientInfo()
	 */
	@Override
	public Properties getClientInfo() throws SQLException {
		return this.delegate.getClientInfo();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getClientInfo(java.lang.String)
	 */
	@Override
	public String getClientInfo(String name) throws SQLException {
		return this.delegate.getClientInfo(name);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getHoldability()
	 */
	@Override
	public int getHoldability() throws SQLException {
		return this.delegate.getHoldability();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getMetaData()
	 */
	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		return this.delegate.getMetaData();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getNetworkTimeout()
	 */
	@Override
	public int getNetworkTimeout() throws SQLException {
		return this.delegate.getNetworkTimeout();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getSchema()
	 */
	@Override
	public String getSchema() throws SQLException {
		return this.delegate.getSchema();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getTransactionIsolation()
	 */
	@Override
	public int getTransactionIsolation() throws SQLException {
		return this.delegate.getTransactionIsolation();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getTypeMap()
	 */
	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return this.delegate.getTypeMap();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#getWarnings()
	 */
	@Override
	public SQLWarning getWarnings() throws SQLException {
		return this.delegate.getWarnings();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#isClosed()
	 */
	@Override
	public boolean isClosed() throws SQLException {
		return this.delegate.isClosed();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#isReadOnly()
	 */
	@Override
	public boolean isReadOnly() throws SQLException {
		return this.delegate.isReadOnly();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#isValid(int)
	 */
	@Override
	public boolean isValid(int timeout) throws SQLException {
		return this.delegate.isValid(timeout);
	}

	/* (non-Javadoc)
	 * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
	 */
	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return this.delegate.isWrapperFor(iface);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#nativeSQL(java.lang.String)
	 */
	@Override
	public String nativeSQL(String sql) throws SQLException {
		return this.delegate.nativeSQL(sql);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#prepareCall(java.lang.String, int, int, int)
	 */
	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		return this.delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#prepareCall(java.lang.String, int, int)
	 */
	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		return this.delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#prepareCall(java.lang.String)
	 */
	@Override
	public CallableStatement prepareCall(String sql) throws SQLException {
		return this.delegate.prepareCall(sql);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int, int, int)
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		return this.delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int, int)
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		return this.delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int)
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		return this.delegate.prepareStatement(sql, autoGeneratedKeys);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int[])
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		return this.delegate.prepareStatement(sql, columnIndexes);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#prepareStatement(java.lang.String, java.lang.String[])
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		return this.delegate.prepareStatement(sql, columnNames);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#prepareStatement(java.lang.String)
	 */
	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return this.delegate.prepareStatement(sql);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#releaseSavepoint(java.sql.Savepoint)
	 */
	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		this.delegate.releaseSavepoint(savepoint);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#rollback()
	 */
	@Override
	public void rollback() throws SQLException {
		this.delegate.rollback();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#rollback(java.sql.Savepoint)
	 */
	@Override
	public void rollback(Savepoint savepoint) throws SQLException {
		this.delegate.rollback(savepoint);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setAutoCommit(boolean)
	 */
	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		this.delegate.setAutoCommit(autoCommit);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setCatalog(java.lang.String)
	 */
	@Override
	public void setCatalog(String catalog) throws SQLException {
		this.delegate.setCatalog(catalog);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setClientInfo(java.util.Properties)
	 */
	@Override
	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		this.delegate.setClientInfo(properties);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setClientInfo(java.lang.String, java.lang.String)
	 */
	@Override
	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		this.delegate.setClientInfo(name, value);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setHoldability(int)
	 */
	@Override
	public void setHoldability(int holdability) throws SQLException {
		this.delegate.setHoldability(holdability);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setNetworkTimeout(java.util.concurrent.Executor, int)
	 */
	@Override
	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		this.delegate.setNetworkTimeout(executor, milliseconds);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setReadOnly(boolean)
	 */
	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
		this.delegate.setReadOnly(readOnly);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setSavepoint()
	 */
	@Override
	public Savepoint setSavepoint() throws SQLException {
		return this.delegate.setSavepoint();
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setSavepoint(java.lang.String)
	 */
	@Override
	public Savepoint setSavepoint(String name) throws SQLException {
		return this.delegate.setSavepoint(name);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setSchema(java.lang.String)
	 */
	@Override
	public void setSchema(String schema) throws SQLException {
		this.delegate.setSchema(schema);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setTransactionIsolation(int)
	 */
	@Override
	public void setTransactionIsolation(int level) throws SQLException {
		this.delegate.setTransactionIsolation(level);
	}

	/* (non-Javadoc)
	 * @see java.sql.Connection#setTypeMap(java.util.Map)
	 */
	@Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		this.delegate.setTypeMap(map);
	}

	/* (non-Javadoc)
	 * @see java.sql.Wrapper#unwrap(java.lang.Class)
	 */
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return this.delegate.unwrap(iface);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.delegate.toString();
	}

	@Override
	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
		return delegate.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DelegatingConnectionImpl other = (DelegatingConnectionImpl) obj;
		if (delegate == null) {
			if (other.delegate != null)
				return false;
		} else if (!delegate.equals(other.delegate))
			return false;
		return true;
	}

//	/* (non-Javadoc)
//	 * @see java.lang.Object#hashCode()
//	 */
//	@Override
//	public int hashCode() {
//		return 31 * 1 + (int) (this.createTime ^ (this.createTime >>> 32));
//	}
//	
//	private static final Connection THAT_CLOSED_ONE = getProxy();
//	
//	private static final Connection getProxy(){
//		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class[] { Connection.class },
//				new InvocationHandler() {
//			@Override
//			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//				final String methodName = method.getName();
//				if ("abort".equals(methodName)) {
//					return Void.TYPE;
//				} else if ("isValid".equals(methodName)) {
//					return Boolean.FALSE;
//				} else if ("toString".equals(methodName)) {
//					return DelegatingConnectionImpl.class.getCanonicalName();
//				} else if ("isClosed".equals(methodName)) {
//					return Boolean.TRUE;
//				}
//				throw new SQLException("Connection is closed");
//			}
//		});
//	}
}