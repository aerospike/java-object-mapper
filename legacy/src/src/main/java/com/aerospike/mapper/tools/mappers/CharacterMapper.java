package com.aerospike.mapper.tools.mappers;

import com.aerospike.mapper.tools.TypeMapper;

public class CharacterMapper extends TypeMapper {

    @Override
    public Object toAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        } else {
            char c = (Character) value;
            return (long) c;
        }
    }

    @Override
    public Object fromAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        long longVal = ((Number) value).longValue();
        return (char) longVal;
    }
}
