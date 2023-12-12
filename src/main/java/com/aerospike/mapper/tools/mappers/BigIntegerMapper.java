package com.aerospike.mapper.tools.mappers;

import com.aerospike.mapper.tools.TypeMapper;

import java.math.BigInteger;

public class BigIntegerMapper extends TypeMapper {

    @Override
    public Object toAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        BigInteger bigInt = (BigInteger) value;
        return bigInt.toString();
    }

    @Override
    public Object fromAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        return new BigInteger((String) value);
    }
}
