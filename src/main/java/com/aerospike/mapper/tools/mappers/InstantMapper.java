package com.aerospike.mapper.tools.mappers;

import java.time.Instant;

import com.aerospike.mapper.tools.TypeMapper;

public class InstantMapper extends TypeMapper {

	@Override
	public Object toAerospikeFormat(Object value) {
		if (value == null) {
			return null;
		}
		Instant instant = (Instant)value;
		return instant.getEpochSecond() * 1_000_000_000 + instant.getNano();
	}

	@Override
	public Object fromAerospikeFormat(Object value) {
		if (value == null) {
			return null;
		}
		long longValue = (Long)value;
		return Instant.ofEpochSecond(longValue/1_000_000_000, longValue % 1_000_000_000);
	}
}
