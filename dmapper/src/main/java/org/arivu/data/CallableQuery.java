package org.arivu.data;

import java.lang.reflect.InvocationTargetException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.sql.DataSource;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.utils.NullCheck;
import org.arivu.utils.TimeTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author P
 *
 */
public class CallableQuery {
	/**
	 * 
	 */
	static final Logger logger = LoggerFactory.getLogger(CallableQuery.class);

	/**
	 * 
	 */
	private final DataSource dataSource;

	/**
	 * @param dataSource
	 */
	public CallableQuery(DataSource dataSource) {
		super();
		this.dataSource = dataSource;
	}

	/**
	 * @param queryResultSet
	 * @return
	 * @throws SQLException
	 */
	Collection<Map<String, Object>> extractData(final ResultSet queryResultSet) throws SQLException {
		if (queryResultSet == null)
			return null;

		final TimeTracker tt = new TimeTracker("extractData");

		final ResultSetMetaData metaData = queryResultSet.getMetaData();
		final Collection<Map<String, Object>> resultSet = new DoublyLinkedList<Map<String, Object>>();
		final int columnCount = metaData.getColumnCount();
		tt.nextTrack("metadata");
		int rowcnt = 0;
		while (queryResultSet.next()) {
			final Map<String, Object> row = new Amap<String, Object>();

			for (int i = 1; i <= columnCount; i++)
				row.put(metaData.getColumnName(i).toLowerCase(Locale.ENGLISH), queryResultSet.getObject(i));

			tt.nextTrack("row" + (rowcnt++));
			resultSet.add(Collections.unmodifiableMap(row));
		}
		logger.info(this.getClass().getName(), "extractData :: " + tt.stop());
		return Collections.unmodifiableCollection(resultSet);
	}

	/**
	 * @param query
	 * @param parameters
	 * @return
	 * @throws SQLException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public Collection<Map<String, Object>> call(final String query, final List<Parameter> parameters)
			throws SQLException, IllegalAccessException, InvocationTargetException {
		final TimeTracker tt = getTimer();
		return call(query, parameters, tt);
	}

	/**
	 * @return
	 */
	TimeTracker getTimer() {
		return new TimeTracker(getClass().getSimpleName());
	}

	/**
	 * @param query
	 * @param params
	 * @param tt
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws SQLException
	 */
	Collection<Map<String, Object>> call(final String query, final List<Parameter> params, final TimeTracker tt)
			throws IllegalAccessException, InvocationTargetException, SQLException {
		tt.nextTrack("buildparams");
		final Connection dbConnection = getConnection();
		try {
			tt.nextTrack("getconn");
			final PreparedStatement preparedStatement = dbConnection.prepareStatement(query);
			try {
				tt.nextTrack("statement");
				setValue(params, preparedStatement);
				tt.nextTrack("setval");
				final ResultSet executeQuery = preparedStatement.executeQuery();
				try {
					tt.nextTrack("query");
					Collection<Map<String, Object>> extractData = extractData(executeQuery);
					tt.nextTrack("getval");
					logger.info(this.getClass().getName(),
							"After Executing query(" + tt.stop() + "): " + query + " : " + extractData);
					return extractData;
				} finally {
					executeQuery.close();
				}
			} finally {
				preparedStatement.close();
			}
		} finally {
			dbConnection.close();
		}
	}

	/**
	 * @return
	 * @throws SQLException
	 */
	Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	/**
	 * @param callableStatement
	 * @param parameterIndex
	 * @param sqlType
	 * @param data
	 * @throws SQLException
	 */
	void getValue(final CallableStatement callableStatement, final int parameterIndex, final int sqlType,
			final Map<String, Object> data) throws SQLException {
		final Object object = callableStatement.getObject(parameterIndex);
		String columnName = callableStatement.getMetaData().getColumnName(parameterIndex);
		if (object != null) {
			if (sqlType == -10) {
				final ResultSet resultSet = (ResultSet) object;
				try {
					final Collection<Map<String, Object>> extractData = extractData(resultSet);
					data.put(columnName, extractData);
					logger.debug(this.getClass().getName(), " extractData " + columnName + " " + extractData);
				}finally{
					resultSet.close();
				}
			} else {
				data.put(columnName, object);
				logger.debug(this.getClass().getName(), " extractData " + columnName + " " + object);
			}
		} else {
			logger.debug(this.getClass().getName(), " extractData " + columnName + " null");
		}
	}

	/**
	 * @param data
	 * @param params
	 * @param callableStatement
	 * @throws SQLException
	 */
	void getValue(final Map<String, Object> data, final List<Parameter> params,
			final CallableStatement callableStatement) throws SQLException {
		int idx = 0;
		for (Parameter p : params) {
			++idx;
			if (p.ioType != IOType.IN) {
				getValue(callableStatement, idx, p.sqlType, data);
			}
		}
	}

	/**
	 * @param params
	 * @param callableStatement
	 * @throws SQLException
	 */
	void setValue(final List<Parameter> params, final PreparedStatement callableStatement) throws SQLException {
		int idx = 0;
		if (!NullCheck.isNullOrEmpty(params)) {
			// set parameters.
			for (Parameter p : params)
				p.ioType.setValue(callableStatement, ++idx, p.value, p.sqlType);
		}
	}

}
