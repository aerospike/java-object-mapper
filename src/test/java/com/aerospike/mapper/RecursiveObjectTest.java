package com.aerospike.mapper;

import org.junit.jupiter.api.Test;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecursiveObjectTest extends AeroMapperBaseTest {
	@AerospikeRecord(namespace = "test", set = "A")
	public static class A {
		public String name;
		public int age;
		@AerospikeKey
		public int id;
//		@AerospikeReference(batchLoad = false)
		public A a;
		
		A() {}

		public A(String name, int age, int id) {
			super();
			this.name = name;
			this.age = age;
			this.id = id;
		}
	}
	
	@AerospikeRecord(namespace = "test", set = "B")
	public static class B {
		@AerospikeKey
		public int id ;
		public A a;
	}
	
	@Test
	public void runTest() {
		A a1 = new A("a", 10, 1);
		a1.a = a1;
		
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		mapper.save(a1);
		
		A a2 = mapper.read(A.class, a1.id);
		assertEquals(a1.age, a2.age);
		assertEquals(a1.name, a2.name);
		assertEquals(a1.id, a2.id);
	}

	@Test
	public void runMultipleObjectTest() {
		A a1 = new A("a", 10, 1);
		a1.a = a1;
		B b = new B();
		b.id = 10;
		b.a = a1;
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		mapper.save(a1);
		mapper.save(b);
		
		B b2 = mapper.read(B.class, b.id);
		assertEquals(b.id, b2.id);
		assertEquals(b.a.age, b2.a.age);
		assertEquals(b.a.name, b2.a.name);
		assertEquals(b.a.id, b2.a.id);
	}
	
	@Test
	public void runTest2() {
		A a1 = new A("a", 10, 1);
		a1.a = new A("a2", 11, 11);

		AeroMapper mapper = new AeroMapper.Builder(client).build();
		mapper.save(a1, a1.a);
		
		A a2 = mapper.read(A.class, a1.id);
		assertEquals(a1.age, a2.age);
		assertEquals(a1.name, a2.name);
		assertEquals(a1.id, a2.id);
		assertEquals(a1.a.id, a2.a.id);
	}
}