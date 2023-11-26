package com.aerospike.mapper.tools.mappers;

import com.aerospike.mapper.tools.TypeMapper;

import java.math.BigDecimal;

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
        return new BigDecimal((String) value);
    }
}
