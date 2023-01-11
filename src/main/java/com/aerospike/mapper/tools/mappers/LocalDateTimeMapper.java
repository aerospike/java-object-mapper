package com.aerospike.mapper.tools.mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import com.aerospike.mapper.tools.TypeMapper;

/**
 * Map a java.time.LocalDateTime to Aerospike.
 * <p/> 
 * If we store the data in a single long we can only store to the millisecond
 * precision, like:
 * <pre>
 * return Date.from(((LocalDateTime) value).toInstant(ZoneOffset.UTC)).getTime();
 * </pre>
 * Whereas LocalDateTime can store down to the nanosecond precision. To store this properly
 * we will split it into date and time components and store both in a list.
 * *
 * @author tfaulkes
 *
 */
public class LocalDateTimeMapper extends TypeMapper {

    @Override
    public Object toAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        LocalDateTime dateTime = (LocalDateTime)value;
        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();
        return Arrays.asList(date.toEpochDay(), time.toNanoOfDay());
    }

    @Override
    public Object fromAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        List<Long> values = (List<Long>)value;
        LocalDate date = LocalDate.ofEpochDay(values.get(0));
        LocalTime time = LocalTime.ofNanoOfDay(values.get(1));
        return LocalDateTime.of(date, time);
    }
}