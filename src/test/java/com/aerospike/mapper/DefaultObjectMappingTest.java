package com.aerospike.mapper;

import org.junit.Test;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;

public class DefaultObjectMappingTest extends AeroMapperBaseTest {
	@AerospikeRecord(namespace = "test", set = "testSet")
	public static class Child {
		public int age;
		@AerospikeKey
		public int id;
		public String name;
	}
	@AerospikeRecord(namespace = "test", set = "testSet")
	public static class Parent {
		@AerospikeKey
		public int id;
		public Child child;
	}
	
	@Test
	public void test() {
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		Child child = new Child();
		child.age = 17;
		child.id = 3;
		child.name = "bob";
		
		Parent parent = new Parent();
		parent.id = 1;
		parent.child = child;
		
		mapper.save(parent);
	}
}
