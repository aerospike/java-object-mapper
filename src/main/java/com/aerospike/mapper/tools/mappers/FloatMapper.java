package com.aerospike.mapper.tools.mappers;

import com.aerospike.mapper.tools.TypeMapper;

public class FloatMapper implements TypeMapper {

	@Override
	public Object toAerospikeFormat(Object value) {
		return value;
	}

	@Override
	public Object fromAerospikeFormat(Object value) {
		if (value == null) {
			return Float.valueOf(0f);
		}
		double doubleValue = (Double)value;
		return Float.valueOf((float)doubleValue);
	}
}
