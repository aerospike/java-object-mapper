package com.aerospike.mapper.tools.mappers;

import com.aerospike.mapper.tools.TypeMapper;

public class BooleanMapper extends TypeMapper {

    @Override
    public Object toAerospikeFormat(Object value) {
        return value;
    }

    @Override
    public Object fromAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return value;
        }
        // Backward compat: boolean stored as integer (0/1) on older Aerospike servers
        return !Long.valueOf(0).equals(value);
    }
}
