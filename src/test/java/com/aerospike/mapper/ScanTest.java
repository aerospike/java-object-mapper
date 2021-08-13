package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.aerospike.client.exp.Exp;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.tools.AeroMapper;

public class ScanTest extends AeroMapperBaseTest {
	@AerospikeRecord(namespace = "test", set = "testScan")
	public static class Person {
		@AerospikeKey
		private final int id;
		private final String name;
		private final int age;
		
		public Person(@ParamFrom("id") int id, @ParamFrom("name") String name, @ParamFrom("age") int age) {
			super();
			this.id = id;
			this.name = name;
			this.age = age;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public int getAge() {
			return age;
		}
	}
	
	private AeroMapper populate() {
		client.truncate(null, "test", "testScan", null);
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		mapper.save(new Person(1, "Tim", 312),
					new Person(2, "Bob", 44),
					new Person(3, "Sue", 56),
					new Person(4, "Rob", 23),
					new Person(5, "Jim", 32),
					new Person(6, "Bob", 78));
		return mapper;
	}

	@Test
	public void scanTest() {
		AeroMapper mapper = populate();
		AtomicInteger counter = new AtomicInteger(0);
		mapper.query(Person.class, (a) -> {
			counter.incrementAndGet();
			return true;
		});
		assertEquals(6, counter.get());
	}

	@Test
	public void scanTestWithFilter() {
		AeroMapper mapper = populate();
		AtomicInteger counter = new AtomicInteger(0);
		QueryPolicy queryPolicy = new QueryPolicy(mapper.getQueryPolicy(Person.class));
		queryPolicy.filterExp = Exp.build(Exp.eq(Exp.stringBin("name"), Exp.val("Bob")));
		mapper.query(queryPolicy, Person.class, (a) -> {
			counter.incrementAndGet();
			return true;
		});
		assertEquals(2, counter.get());
	}

	@Test
	public void scanTestWithAbort() {
		AeroMapper mapper = populate();
		QueryPolicy queryPolicy = new QueryPolicy(mapper.getQueryPolicy(Person.class));
		queryPolicy.maxConcurrentNodes = 1;
		AtomicInteger counter = new AtomicInteger(0);
		mapper.query(queryPolicy, Person.class, (a) -> {
			counter.incrementAndGet();
			return false;
		});
		assertEquals(1, counter.get());
	}
}
