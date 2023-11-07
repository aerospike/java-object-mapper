package com.aerospike.mapper.tools.mappers;

import java.math.BigDecimal;

import com.aerospike.mapper.tools.TypeMapper;

public class BigDecimalMapper extends TypeMapper {

    @Override
    public Object toAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        BigDecimal bigInt = (BigDecimal) value;
        return bigInt.toString();
    }

    @Override
    public Object fromAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        return new BigDecimal((String)value);
    }
}
