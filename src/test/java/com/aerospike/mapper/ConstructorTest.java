package com.aerospike.mapper;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;

import com.aerospike.mapper.annotations.AerospikeConstructor;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.tools.AeroMapper;

public class ConstructorTest extends AeroMapperBaseTest {

	@AerospikeRecord(namespace = "test", set = "testSet") 
	public static class NoArgConstructorClass {
		@AerospikeKey 
		public final int id;
		public String name;
		
		public NoArgConstructorClass() {
			this.name ="";
			this.id = 0;
		}

		public NoArgConstructorClass(int id, String name) {
			super();
			this.id = id;
			this.name = name;
		}
	}
	
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
	
	@AerospikeRecord(namespace = "test", set = "testSet") 
	public static class ConstructoredClass2 {
		@AerospikeKey
		public final int id;
		public final int a;
		public int b;
		public int c;
		
		public ConstructoredClass2(@ParamFrom("id") int id, @ParamFrom("a") int a) {
			this.id = id;
			this.a = a;
			System.out.println("Using 2 arg");
		}
		
		@AerospikeConstructor
		public ConstructoredClass2(@ParamFrom("id") int id, @ParamFrom("a") int a, @ParamFrom("b") int b) {
			this.id = id;
			this.a = a;
			this.b = b;
			System.out.println("Using 3 arg");
		}
	}
	
	@Test
	public void test2() {
		ConstructoredClass2 data = new ConstructoredClass2(1, 2);
		data.b = 3;
		data.c = 4;
		
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		mapper.save(data);
		System.out.println("reading back");
		ConstructoredClass2 data2 = mapper.read(ConstructoredClass2.class, 1);
		assertEquals(data.id, data2.id);
		assertEquals(data.a, data2.a);
		assertEquals(data.b, data2.b);
		assertEquals(data.c, data2.c);
	}
	
	@Test
	public void test3() {
		NoArgConstructorClass data = new NoArgConstructorClass(12, "tim");
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		mapper.save(data);
		NoArgConstructorClass data2 = mapper.read(NoArgConstructorClass.class, data.id);
		assertEquals(data.id, data2.id);
		assertEquals(data.name, data2.name);
	}
}
