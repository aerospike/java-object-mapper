package com.aerospike.mapper.tools.mappers;

import java.time.LocalTime;

import com.aerospike.mapper.tools.TypeMapper;

public class LocalTimeMapper extends TypeMapper {

    @Override
    public Object toAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        return ((LocalTime)value).toNanoOfDay();
    }

    @Override
    public Object fromAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        return LocalTime.ofNanoOfDay((Long) value);
    } 
}
