package org.arivu.data;

import java.util.Locale;
import java.util.Map;

import org.arivu.datastructure.Amap;
import org.arivu.utils.NullCheck;

/**
 * @author P
 *
 */
public enum CallableType {
	/**
	 * 
	 */
	DML {
		/* (non-Javadoc)
		 * @see org.arivu.data.CallableType#getCallableQuery(javax.sql.DataSource)
		 */
		@Override
		CallableQuery getCallableQuery(javax.sql.DataSource ds) {
			return new DMLQuery(ds);
		}
	},
	/**
	 * 
	 */
	INSERT {
		/* (non-Javadoc)
		 * @see org.arivu.data.CallableType#getCallableQuery(javax.sql.DataSource)
		 */
		@Override
		CallableQuery getCallableQuery(javax.sql.DataSource ds) {
			return new InsertQuery(ds);
		}
		
	},
	/**
	 * 
	 */
	SELECT;

	/**
	 * @param ds
	 * @return
	 */
	CallableQuery getCallableQuery(javax.sql.DataSource ds){
		return new CallableQuery(ds);
	}
	
	/**
	 * 
	 */
	static final Map<String,CallableType> queryTypes = new Amap<String,CallableType>();
	
	/**
	 * @param query
	 * @param ds
	 * @return callableQuery
	 */
	public static CallableQuery getByQuery(String query, final javax.sql.DataSource ds){
		CallableType type = CallableType.SELECT;
		query = query.intern();
		if (!NullCheck.isNullOrEmpty(query)) {
			type = queryTypes.get(query);
			if(type==null){
				type=getByQueryInit(query);
				queryTypes.put(query, type);
			}
		}
		return type.getCallableQuery(ds);
	}

	/**
	 * @param query
	 * @return
	 */
	private static CallableType getByQueryInit(final String query){
		String lowerCaseQry = query.toLowerCase(Locale.ENGLISH);
		if (lowerCaseQry.startsWith("{call ") || lowerCaseQry.startsWith("begin ")) {
			return CallableType.DML;
		} else if (lowerCaseQry.startsWith("insert") || lowerCaseQry.startsWith("update")) {
			return CallableType.INSERT;
		} 
		return CallableType.SELECT;
	}
}
