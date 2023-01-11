package com.aerospike.mapper.tools.mappers;

import java.time.LocalDate;

import com.aerospike.mapper.tools.TypeMapper;

public class LocalDateMapper extends TypeMapper {

    @Override
    public Object toAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        return ((LocalDate)value).toEpochDay();
    }

    @Override
    public Object fromAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        return LocalDate.ofEpochDay((Long) value);
    } 
}
