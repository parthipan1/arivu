package org.arivu.data;

/**
 * @author P
 *
 */
public class Parameter {
	/**
	 * 
	 */
	final Object value;
	/**
	 * 
	 */
	final int sqlType;
	/**
	 * 
	 */
	final IOType ioType;

	/**
	 * @param value
	 * @param sqlType
	 */
	public Parameter(Object value, int sqlType){
		this(value, sqlType, null);
	}
	
	/**
	 * @param value
	 * @param sqlType
	 * @param ioType
	 */
	public Parameter(Object value, int sqlType, IOType ioType){
		super();
		this.value = value;
		this.sqlType = sqlType;
		if (ioType == null)
			this.ioType = IOType.IN;
		else
			this.ioType = ioType;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Parameter [value=" + value + ", sqlType=" + sqlType + ", ioType=" + ioType + "]";
	}
	
}
