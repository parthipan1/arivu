package org.arivu.data;

import java.lang.reflect.InvocationTargetException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.arivu.datastructure.Amap;
import org.arivu.utils.TimeTracker;

/**
 * @author P
 *
 */
public class DMLQuery extends CallableQuery {

	/**
	 * @param dataSource
	 */
	public DMLQuery(DataSource dataSource) {
		super(dataSource);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.arivu.data.CallableQuery#call(java.lang.String, java.util.List,
	 * org.arivu.utils.TimeTracker)
	 */
	@Override
	Collection<Map<String, Object>> call(final String query, final List<Parameter> params, final TimeTracker tt)
			throws IllegalAccessException, InvocationTargetException, SQLException {
		final Collection<Map<String, Object>> retList = new ArrayList<Map<String, Object>>();
		final Map<String, Object> data = new Amap<String, Object>();
		tt.nextTrack("buildparams");
		final Connection dbConnection = getConnection();
		try {
			tt.nextTrack("getconn");
			final CallableStatement callableStatement = dbConnection.prepareCall(query);
			try {
				tt.nextTrack("statement");
				setValue(params, callableStatement);
				tt.nextTrack("setval");
				boolean execute = callableStatement.execute();
				tt.nextTrack("query");
				getValue(data, params, callableStatement);
				tt.nextTrack("getval");
				logger.info(this.getClass().getName(),
						"After Executing query(" + tt.stop() + "): " + query + " exe:" + execute);
			} finally {
				callableStatement.close();
			}
		} finally {
			dbConnection.close();
		}
		retList.add(Collections.unmodifiableMap(data));
		return Collections.unmodifiableCollection(retList);
	}

}
