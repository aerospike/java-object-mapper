package com.aerospike.mapper;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;

public class BatchLoadTest extends AeroMapperBaseTest {
	@AerospikeRecord(namespace = "test", set = "C")
	public static class C {
		@AerospikeKey
		public int id;
		public String name;
		
		public C(@ParamFrom("id") int id, @ParamFrom("name") String name) {
			super();
			this.id = id;
			this.name = name;
		}
	}
	
	@AerospikeRecord(namespace = "test", set = "B")
	public static class B {
		@AerospikeKey
		public int id;
	}
}
