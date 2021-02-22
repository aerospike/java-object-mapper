package com.aerospike.mapper.tools.mappers;

import com.aerospike.mapper.tools.TypeMapper;

public class ByteMapper extends TypeMapper {

	@Override
	public Object toAerospikeFormat(Object value) {
		return Long.valueOf(((Number)value).longValue());
	}

	@Override
	public Object fromAerospikeFormat(Object value) {
		if (value == null) {
			return Byte.valueOf((byte)0);
		}
		return Byte.valueOf(((Number)value).byteValue());
	}
}
