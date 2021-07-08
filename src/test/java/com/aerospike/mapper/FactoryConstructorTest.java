package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aerospike.mapper.annotations.AerospikeExclude;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;

public class FactoryConstructorTest extends AeroMapperBaseTest {
	public static class Factory {
		public static final String VALID = "created from factory";
		public static A createA() {
			A newA = new A();
			newA.factory = VALID;
			return newA;
		}
	}
	@AerospikeRecord(namespace = "test", set = "A", factoryMethod = "createA", factoryClass = "com.aerospike.mapper.FactoryConstructorTest$Factory")
	public static class A {
		public String name;
		public int age;
		@AerospikeKey
		public int id;
		@AerospikeExclude
		public String factory;
		
		A() {}

		public A(String name, int age, int id) {
			super();
			this.name = name;
			this.id = id;
		}
	}
	
	@Test
	public void runTest() {
		A a1 = new A("a", 10, 1);
		
		AeroMapper mapper = new AeroMapper.Builder(client).build();
//		Map<String, Object> mappedObject = mapper.getMappingConverter().convertToMap(a1);
//		A a2 = mapper.getMappingConverter().convertToObject(A.class, mappedObject);
		mapper.save(a1);
		A a2 = mapper.read(A.class, a1.id);

		assertEquals(a1.age, a2.age);
		assertEquals(a1.name, a2.name);
		assertEquals(a1.id, a2.id);
		assertEquals(Factory.VALID, a2.factory);
	}
}