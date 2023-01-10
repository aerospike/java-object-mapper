package com.aerospike.mapper.tools.mappers;

import java.sql.Date;
import java.time.LocalDate;

import com.aerospike.mapper.tools.TypeMapper;

public class LocalDateMapper extends TypeMapper {

    @Override
    public Object toAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        return Date.valueOf((LocalDate) value).getTime();
    }

    @Override
    public Object fromAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        long longValue = (Long) value;
        return new Date(longValue).toLocalDate();
    } 
}
