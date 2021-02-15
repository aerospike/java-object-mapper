package com.aerospike.mapper;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.tools.AeroMapper;

public class ConstructorTest extends AeroMapperBaseTest {

	@AerospikeRecord(namespace = "test", set = "testSet")
	public static class ConstructoredClass {
		@AerospikeKey
		public final int id;
		public final int age;
		public final String name;
		public final Date date;
		
		
		public ConstructoredClass(@ParamFrom("id") int id, @ParamFrom("age") int age, @ParamFrom("name") String name, @ParamFrom("date")Date date) {
			super();
			this.id = id;
			this.age = age;
			this.name = name;
			this.date = date;
		}
	}
	
	@Test
	public void test() {
		ConstructoredClass data = new ConstructoredClass(1, 19, "jane", new Date());
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		mapper.save(data);
		ConstructoredClass data2 = mapper.read(ConstructoredClass.class, 1);
		assertEquals(data.id, data2.id);
		assertEquals(data.age, data2.age);
		assertEquals(data.name, data2.name);
		assertEquals(data.date, data2.date);
		
	}
}
