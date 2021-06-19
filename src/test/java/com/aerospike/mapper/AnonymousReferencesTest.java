package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;

public class AnonymousReferencesTest extends AeroMapperBaseTest {

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
		b.name = "a B";
		mapper.save(b);

		B b1 = new B();
		b1.id = 3;
		b1.name = "another B";
		mapper.save(b1);

		A a = new A();
		a.id = 1;
		a.namedB.add(b);
		a.namedB.add(b1);

		a.unnamedB.add(b);
		a.unnamedB.add(b1);

		List nonB = new ArrayList();
		nonB.add(2L);
		nonB.add("B");
		a.nonB.add(nonB);

		mapper.save(a);
		A a2 = mapper.read(A.class, a.id);
		assertEquals(a.id, a2.id);
		assertEquals(a.unnamedB.size(), a2.unnamedB.size());
		assertEquals(((B)a.unnamedB.get(0)).id, ((B)a2.unnamedB.get(0)).id);
		assertEquals(((B)a.unnamedB.get(0)).name, ((B)a2.unnamedB.get(0)).name);
		assertEquals(((B)a.unnamedB.get(1)).id, ((B)a2.unnamedB.get(1)).id);
		assertEquals(((B)a.unnamedB.get(1)).name, ((B)a2.unnamedB.get(1)).name);

		assertEquals(a.namedB.size(), a2.namedB.size());
		assertEquals(a.namedB.get(0).id, a2.namedB.get(0).id);
		assertEquals(a.namedB.get(0).name, a2.namedB.get(0).name);
		assertEquals(a.namedB.get(1).id, a2.namedB.get(1).id);
		assertEquals(a.namedB.get(1).name, a2.namedB.get(1).name);

		assertEquals(a.nonB.size(), a2.nonB.size());
		assertEquals(((List)a.nonB.get(0)).get(0), ((List)a2.nonB.get(0)).get(0));
		assertEquals(((List)a.nonB.get(0)).get(1), ((List)a2.nonB.get(0)).get(1));
	}
}
