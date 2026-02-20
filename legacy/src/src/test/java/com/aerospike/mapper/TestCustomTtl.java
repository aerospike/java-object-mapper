package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;

public class TestCustomTtl extends AeroMapperBaseTest {
	
	@AerospikeRecord(namespace = "test", set = "classWithTtl", ttl=300)
	public static class ClassWithTtl {
		@AerospikeKey
		public int id;
		public String name;
	}

	@AerospikeRecord(namespace = "test", set = "classWithTtl")	
	public static class ClassWithTtlViaPolicy {
		@AerospikeKey
		public int id;
		public String name;
	}
	
	@Test
	public void testTtl() {
		AeroMapper mapper = new AeroMapper.Builder(client)
				.build();
		
		ClassWithTtl myRecord = new ClassWithTtl();
		myRecord.id = 1;
		myRecord.name = "not overridden ttl";
		mapper.save(myRecord);
		
		WritePolicy customWritePolicy = new WritePolicy(mapper.getWritePolicy(ClassWithTtl.class));
		customWritePolicy.expiration = 100;
		myRecord.id = 2;
		myRecord.name = "overridden ttl";
		mapper.save(customWritePolicy, myRecord);
		
		// To validate the TTL, read the records as raw records rather than via the mapper interface
		Record readClient1 = client.get(null, new Key("test", "classWithTtl", 1));
		Record readClient2 = client.get(null, new Key("test", "classWithTtl", 2));
		
		assertTrue(readClient1.getTimeToLive() > 290 && readClient1.getTimeToLive() <= 300);
		assertTrue(readClient2.getTimeToLive() > 90 && readClient2.getTimeToLive() <= 100);
	}
	
	@Test
	public void testTtlViaPolicy() {
		WritePolicy writePolicy = new WritePolicy();
		writePolicy.expiration = 300;
		AeroMapper mapper = new AeroMapper.Builder(client)
				.withWritePolicy(writePolicy).forClasses(ClassWithTtlViaPolicy.class)
				.build();
		
		ClassWithTtlViaPolicy myRecord = new ClassWithTtlViaPolicy();
		myRecord.id = 1;
		myRecord.name = "not overridden ttl";
		mapper.save(myRecord);
		
		WritePolicy customWritePolicy = new WritePolicy(mapper.getWritePolicy(ClassWithTtlViaPolicy.class));
		customWritePolicy.expiration = 100;
		myRecord.id = 2;
		myRecord.name = "overridden ttl";
		mapper.save(customWritePolicy, myRecord);
		
		// To validate the TTL, read the records as raw records rather than via the mapper interface
		Record readClient1 = client.get(null, new Key("test", "classWithTtl", 1));
		Record readClient2 = client.get(null, new Key("test", "classWithTtl", 2));
		
		assertTrue(readClient1.getTimeToLive() > 290 && readClient1.getTimeToLive() <= 300);
		assertTrue(readClient2.getTimeToLive() > 90 && readClient2.getTimeToLive() <= 100);
	}
}
