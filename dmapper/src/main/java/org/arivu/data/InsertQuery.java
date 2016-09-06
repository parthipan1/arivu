package org.arivu.data;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
public class InsertQuery extends CallableQuery {

	/**
	 * @param dataSource
	 */
	public InsertQuery(DataSource dataSource) {
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
		tt.nextTrack("buildparams");
		final Connection dbConnection = getConnection();
		try {
			tt.nextTrack("getconn");
			final PreparedStatement preparedStatement = dbConnection.prepareStatement(query);
			try {
				tt.nextTrack("statement");
				setValue(params, preparedStatement);
				tt.nextTrack("setval");
				int executeUpdate = preparedStatement.executeUpdate();
				tt.nextTrack("query");
				Collection<Map<String, Object>> retList = new ArrayList<Map<String, Object>>();
				Map<String, Object> ret = new Amap<String, Object>();
				ret.put("rows", executeUpdate);
				retList.add(Collections.unmodifiableMap(ret));
				tt.nextTrack("getval");
				logger.info(this.getClass().getName(),
						"After Executing query(" + tt.stop() + "): " + query + " exe:" + retList);
				return Collections.unmodifiableCollection(retList);
			} finally {
				preparedStatement.close();
			}
		} finally {
			dbConnection.close();
		}
	}

}
