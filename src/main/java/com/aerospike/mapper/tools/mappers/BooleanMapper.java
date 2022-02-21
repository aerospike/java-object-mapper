package com.aerospike.mapper.tools.mappers;

import com.aerospike.mapper.tools.TypeMapper;

import static com.aerospike.client.Value.UseBoolBin;

public class BooleanMapper extends TypeMapper {

    @Override
    public Object toAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        if (UseBoolBin) {
            return value;
        }
        return ((Boolean) value) ? 1 : 0;
    }

    @Override
    public Object fromAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        if (UseBoolBin) {
            return value;
        }
        return !Long.valueOf(0).equals(value);
    }
}
