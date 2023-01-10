package com.aerospike.mapper.tools.mappers;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.aerospike.mapper.tools.TypeMapper;

public class LocalDateTimeMapper extends TypeMapper {

    @Override
    public Object toAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        return Date.from(((LocalDateTime) value).toInstant(ZoneOffset.UTC)).getTime();
    }

    @Override
    public Object fromAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        long longValue = (Long) value;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(longValue), ZoneOffset.UTC);
    }
}