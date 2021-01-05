package com.aerospike.mapper.tools.mappers;

import com.aerospike.mapper.tools.TypeMapper;

public class BooleanMapper implements TypeMapper {

	@Override
	public Object toAerospikeFormat(Object value) {
		if (value == null) {
			return 0;
		}
		return ((Boolean)value) ? 1 : 0;
	}

	@Override
	public Object fromAerospikeFormat(Object value) {
		if (value == null) {
			return false;
		}
		return !Long.valueOf(0).equals(value);
	}
}
