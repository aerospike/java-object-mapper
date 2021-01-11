package com.aerospike.mapper.tools.mappers;

import com.aerospike.mapper.tools.TypeMapper;

public class CharacterMapper implements TypeMapper {

	@Override
	public Object toAerospikeFormat(Object value) {
		if (value == null) {
			return Long.valueOf(0);
		}
		else {
			char c = ((Character)value).charValue();
			return Long.valueOf(c);
		}
	}

	@Override
	public Object fromAerospikeFormat(Object value) {
		if (value == null) {
			return Character.valueOf((char)0);
		}
		long longVal = ((Number)value).longValue();
		return Character.valueOf((char)longVal);
	}
}
