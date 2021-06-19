package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;

public class MapTest extends AeroMapperBaseTest {
	@AerospikeRecord(namespace = "test", set = "A")
	public static class A {
		public String name;
		public int age;
		@AerospikeKey
		public int id;
		
		A() {}

		public A(String name, int age, int id) {
			super();
			this.name = name;
			this.id = id;
		}
	}
	
	@AerospikeRecord(namespace = "test", set = "B")
	public static class B {
		@AerospikeKey
		public int id;
		public Map<Integer, A> batchAs;
	}
	
	@Test
	public void runTest() {
		A a1 = new A("a", 10, 1);
		A a2 = new A("b", 20, 2);
		A a3 = new A("c", 30, 3);
		A a4 = new A("d", 40, 4);
		A a5 = new A("e", 50, 5);
		A a6 = new A("f", 60, 6);
		
		B b = new B();
		b.batchAs = new HashMap<>();
		b.id = 1;
		
		b.batchAs.put(10, a1);
		b.batchAs.put(11, a2);
		b.batchAs.put(12, a3);
		b.batchAs.put(13, a4);
		b.batchAs.put(14, a5);
		b.batchAs.put(15, a6);
		
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		mapper.save(b);
		mapper.save(a1);
		mapper.save(a2);
		mapper.save(a3);
		mapper.save(a4);
		mapper.save(a5);
		mapper.save(a6);

		B b2 = mapper.read(B.class, 1);
		
		assertEquals(b.id, b2.id);
		assertEquals(b.batchAs.size(), b2.batchAs.size());
		for (int i = 10; i <= 15; i++) {
			assertEquals(b.batchAs.get(i).age, b2.batchAs.get(i).age);
			assertEquals(b.batchAs.get(i).id, b2.batchAs.get(i).id);
			assertEquals(b.batchAs.get(i).name, b2.batchAs.get(i).name);
		}
	}
}
