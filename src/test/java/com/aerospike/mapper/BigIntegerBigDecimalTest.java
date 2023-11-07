package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class BigIntegerBigDecimalTest extends AeroMapperBaseTest {
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @AerospikeRecord(namespace = "test", set = "bigType")
    public static class BigTypes {
        public String name;
        public BigDecimal bigD;
        public BigInteger bigI;
        @AerospikeKey
        public int id;
    }

    @Test
    public void runTest() {
        AeroMapper mapper = new AeroMapper.Builder(client).build();
        BigTypes types = new BigTypes("test", new BigDecimal("123456789.123456789"), new BigInteger("12345678901234567890"), 1);
        mapper.save(types);
        BigTypes readTypes = mapper.read(BigTypes.class, 1); 
        assertEquals(types, readTypes);
    }
}
