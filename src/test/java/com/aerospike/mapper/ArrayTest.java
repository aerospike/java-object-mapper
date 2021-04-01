package com.aerospike.mapper;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeReference;
import com.aerospike.mapper.tools.AeroMapper;

public class ArrayTest extends AeroMapperBaseTest {
	@AerospikeRecord(namespace = "test", set = "A")
	public static class A {
		public String name;
		public int age;
		@AerospikeKey
		public int id;
		
		public A() {}

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
		public int id;
		@AerospikeReference(lazy = true)
		public A[] lazyAs;
		public A[] batchAs;
		@AerospikeReference(batchLoad = false)
		public A[] normalAs;
		
		public A a1;
		public A a2;
		public A a3;
	}
	
	@Test
	public void runTest() {
		A a1 = new A("a", 10, 1);
		A a2 = new A("b", 20, 2);
		A a3 = new A("c", 30, 3);
		A a4 = new A("d", 40, 4);
		A a5 = new A("e", 50, 5);
		A a6 = new A("f", 60, 6);
		A a7 = new A("g", 70, 7);
		A a8 = new A("h", 80, 8);
		A a9 = new A("i", 90, 9);
		
		B b = new B();
		b.id = 1;
		b.lazyAs = new A[] {a1,a2,a3};
		b.batchAs = new A[] {a1,a2,a3,a4,a5,a6,a7,a8,a9};
		b.normalAs = new A[] {a3,a4};
		b.a1 = a1;
		b.a2 = a2;
		b.a3 = a3;
		
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		mapper.save(b);
		mapper.save(a1);
		mapper.save(a2);
		mapper.save(a3);
		mapper.save(a4);
		mapper.save(a5);
		mapper.save(a6);
		mapper.save(a7);
		mapper.save(a8);
		mapper.save(a9);
		B b2 = mapper.read(B.class, 1);
		
		assertEquals(b.id, b2.id);
		assertEquals(b.batchAs.length, b2.batchAs.length);
		assertEquals(b.lazyAs.length, b2.lazyAs.length);
		assertEquals(b.normalAs.length, b2.normalAs.length);

		assertEquals(b.a1.age, b2.a1.age);
		assertEquals(b.a1.id, b2.a1.id);
		assertEquals(b.a1.name, b2.a1.name);

		assertEquals(b.a2.age, b2.a2.age);
		assertEquals(b.a2.id, b2.a2.id);
		assertEquals(b.a2.name, b2.a2.name);

		assertEquals(b.a3.age, b2.a3.age);
		assertEquals(b.a3.id, b2.a3.id);
		assertEquals(b.a3.name, b2.a3.name);

		for (int i = 0; i < b.batchAs.length; i++) {
			assertEquals(b.batchAs[i].age, b2.batchAs[i].age);
			assertEquals(b.batchAs[i].id, b2.batchAs[i].id);
			assertEquals(b.batchAs[i].name, b2.batchAs[i].name);
		}
		for (int i = 0; i < b.normalAs.length; i++) {
			assertEquals(b.normalAs[i].age, b2.normalAs[i].age);
			assertEquals(b.normalAs[i].id, b2.normalAs[i].id);
			assertEquals(b.normalAs[i].name, b2.normalAs[i].name);
		}
		for (int i = 0; i < b.lazyAs.length; i++) {
			assertEquals(0, b2.lazyAs[i].age);
			assertEquals(b.lazyAs[i].id, b2.lazyAs[i].id);
			assertEquals(null, b2.lazyAs[i].name);
		}
	}
}
