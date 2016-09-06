package org.arivu.data;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.arivu.datastructure.Amap;
import org.arivu.utils.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author P
 *
 */
public enum IOType {
	/**
	 * 
	 */
	IN {
		/* (non-Javadoc)
		 * @see org.arivu.data.IOType#setValue(java.sql.PreparedStatement, int, java.lang.Object, int)
		 */
		@Override
		public void setValue(final PreparedStatement callableStatement, final int parameterIndex, final Object value,
				final int sqlType) throws SQLException {
			String msg = " setValue " + parameterIndex + " " + value+" sqlType "+sqlType;
			logger.debug(this.getClass().getName()+"#"+name(), msg);
			if( value == null )
				callableStatement.setNull(parameterIndex, sqlType);
			else
				callableStatement.setObject(parameterIndex, value);
		}

	},
	/**
	 * 
	 */
	OUT {
		/* (non-Javadoc)
		 * @see org.arivu.data.IOType#setValue(java.sql.PreparedStatement, int, java.lang.Object, int)
		 */
		@Override
		public void setValue(final PreparedStatement callableStatement, final int parameterIndex, final Object value,
				final int sqlType) throws SQLException {
			String msg = " registerOutParameter " + parameterIndex + " sqlType "+sqlType;
			logger.debug(this.getClass().getName()+"#"+name(), msg);
			if( callableStatement instanceof CallableStatement )
				((CallableStatement)callableStatement).registerOutParameter(parameterIndex, sqlType);
		}

	},
	/**
	 * 
	 */
	INOUT;

	/**
	 * 
	 */
	static final Logger logger = LoggerFactory.getLogger(IOType.class);
	
	/**
	 * @param callableStatement
	 * @param parameterIndex
	 * @param value
	 * @param sqlType
	 * @throws SQLException
	 */
	public void setValue(final PreparedStatement callableStatement, final int parameterIndex, final Object value,
			final int sqlType) throws SQLException {
		IN.setValue(callableStatement, parameterIndex, value, sqlType);
		OUT.setValue(callableStatement, parameterIndex, value, sqlType);
	}

	/**
	 * 
	 */
	private static final Map<String,IOType> ioTypeMap;
	
	static{
		final Map<String,IOType> ioTypeMapTemp = new Amap<String, IOType>();
		
		for (IOType pt : values()) 
			ioTypeMapTemp.put(pt.name().toUpperCase(Locale.ENGLISH), pt);
		
		ioTypeMap = Collections.unmodifiableMap(ioTypeMapTemp);
	}
	
	/**
	 * @param type
	 * @return
	 */
	public static IOType get(final String type) {
		if( NullCheck.isNullOrEmpty(type) ) return null;
		else return ioTypeMap.get(type.toUpperCase(Locale.ENGLISH));
	}
}
