package com.aerospike.mapper.tools.mappers;

import com.aerospike.mapper.tools.TypeMapper;

public class LongMapper implements TypeMapper {

	@Override
	public Object toAerospikeFormat(Object value) {
		return value;
	}

	@Override
	public Object fromAerospikeFormat(Object value) {
		if (value == null) {
			return Long.valueOf(0);
		}
		return value;
	}
}
