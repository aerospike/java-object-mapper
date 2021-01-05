package com.aerospike.mapper.tools;

public interface TypeMapper {
	Object toAerospikeFormat(Object value);
	Object fromAerospikeFormat(Object value);
}
