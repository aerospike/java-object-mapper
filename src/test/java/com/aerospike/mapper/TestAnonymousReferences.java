package com.aerospike.mapper;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;

public class TestAnonymousReferences extends AeroMapperBaseTest {

	@AerospikeRecord(namespace = "test", set = "A")
	public static class A {
		@AerospikeKey
		public int id;
		public List<B> namedB;
		public List unnamedB;
		public List nonB;
		
		public A() {
			namedB = new ArrayList<>();
			unnamedB = new ArrayList<>();
			nonB = new ArrayList<>();
		}
	}
	
	@AerospikeRecord(namespace = "test", set = "B")
	public static class B {
		@AerospikeKey
		public int id;
		public String name;
	}
	
	@Test
	public void runner() {
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		B b = new B();
		b.id = 2;
		b.name = "test";
		
		mapper.save(b);
		
		A a = new A();
		a.id = 1;
		a.namedB.add(b);
		a.unnamedB.add(b);
		List nonB = new ArrayList();
		nonB.add(2);
		nonB.add("B");
		a.nonB.add(nonB);
		
		mapper.save(a);
		A a2 = mapper.read(A.class, a.id);
		assertEquals(a.id, a2.id);
		assertEquals(a.unnamedB.size(), a2.unnamedB.size());
		assertEquals(((B)a.unnamedB.get(0)).id, ((B)a2.unnamedB.get(0)).id);
	}
}
