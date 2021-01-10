package com.aerospike.mapper.tools.mappers;

import com.aerospike.mapper.tools.TypeMapper;

public class ShortMapper implements TypeMapper {

	@Override
	public Object toAerospikeFormat(Object value) {
		return Long.valueOf(((Number)value).longValue());
	}

	@Override
	public Object fromAerospikeFormat(Object value) {
		if (value == null) {
			return Short.valueOf((short)0);
		}
		return Short.valueOf(((Number)value).shortValue());
	}
}
