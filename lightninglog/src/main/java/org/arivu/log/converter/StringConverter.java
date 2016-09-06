package org.arivu.log.converter;

import org.arivu.log.Converter;

public class StringConverter implements Converter<String>{

	@Override
	public String convert(String t) {
		return t;
	}

}
