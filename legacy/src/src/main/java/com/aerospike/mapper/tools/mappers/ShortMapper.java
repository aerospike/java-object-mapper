package com.aerospike.mapper.tools.mappers;

import com.aerospike.mapper.tools.TypeMapper;

public class ShortMapper extends TypeMapper {

    @Override
    public Object toAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).longValue();
    }

    @Override
    public Object fromAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).shortValue();
    }
}
