package com.aerospike.mapper.tools.mappers;

import java.util.Date;

import com.aerospike.mapper.tools.TypeMapper;

public class DateMapper implements TypeMapper {

	@Override
	public Object toAerospikeFormat(Object value) {
		if (value == null) {
			return null;
		}
		return ((Date)value).getTime();
	}

	@Override
	public Object fromAerospikeFormat(Object value) {
		if (value == null) {
			return null;
		}
		long longValue = (Long)value;
		return new Date(longValue);
	}

}
