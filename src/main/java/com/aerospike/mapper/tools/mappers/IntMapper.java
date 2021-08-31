package com.aerospike.mapper.tools.mappers;

import com.aerospike.mapper.tools.TypeMapper;

public class IntMapper extends TypeMapper {

    @Override
    public Object toAerospikeFormat(Object value) {
        return value;
    }

    @Override
    public Object fromAerospikeFormat(Object value) {
        if (value == null) {
            return Integer.valueOf(0);
        }
        return Integer.valueOf(((Number) value).intValue());
    }
}
