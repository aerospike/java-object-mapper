package com.aerospike.mapper;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BigIntegerBigDecimalTest extends AeroMapperBaseTest {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @AerospikeRecord(namespace = "test", set = "bigType")
    public static class BigTypes {
        @AerospikeKey
        public int id;
        public String name;
        public BigDecimal bigD;
        public BigInteger bigI;
    }

    @Test
    public void runTest() {
        AeroMapper mapper = new AeroMapper.Builder(client).build();
        BigTypes types = new BigTypes(1, "test", new BigDecimal("123456789.123456789"), new BigInteger("12345678901234567890"));
        mapper.save(types);
        BigTypes readTypes = mapper.read(BigTypes.class, 1);
        assertEquals(types, readTypes);
    }
}
