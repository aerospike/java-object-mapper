package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.aerospike.client.AerospikeException;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeVersion;
import com.aerospike.mapper.tools.AeroMapper;

public class MessagesMappingTest extends AeroMapperBaseTest {
    @AerospikeRecord(namespace = "test", set = "testSet")
    public static class InvalidVersion {
        @AerospikeKey
        public int id;
        @AerospikeVersion(max = -3)
        public int versioned;
    }

    @Test
    public void testVersion() {
        AeroMapper mapper = new AeroMapper.Builder(client).build();
        InvalidVersion invalid = new InvalidVersion();
        invalid.id = 1;
        invalid.versioned = 3;
        try {
            mapper.save(invalid);
            fail("Exception should have been thrown");
        } catch (AerospikeException e) {
            assertTrue(e.getMessage().toLowerCase().contains("version"), "Inaccurate error message does not reference version");
        }
    }
}
