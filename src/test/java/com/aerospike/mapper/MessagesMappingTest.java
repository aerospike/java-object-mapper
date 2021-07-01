package com.aerospike.mapper;


import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.aerospike.client.AerospikeException;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeVersion;
import com.aerospike.mapper.tools.AeroMapper;

public class MessagesMappingTest extends AeroMapperBaseTest {
	@AerospikeRecord(namespace = "test", set = "testSet")
	public static class InvalidVerzon {
		@AerospikeKey 
		public int id;
		@AerospikeVersion(max = -3)
		public int versioned;
	}
	
	@Test
	public void testVersion() {
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		InvalidVerzon invalid = new InvalidVerzon();
		invalid.id = 1;
		invalid.versioned = 3;
		try {
			mapper.save(invalid);
			assertTrue(false, "Exception should have been thrown");
		}
		catch (AerospikeException e) {
			assertTrue(e.getMessage().toLowerCase().contains("version"), "Inaccurate error message does not reference version");
		}
	}
}
