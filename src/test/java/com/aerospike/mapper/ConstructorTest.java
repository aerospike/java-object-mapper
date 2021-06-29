package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aerospike.mapper.annotations.AerospikeConstructor;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
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
	public static class ConstructedClass {
		@AerospikeKey
		public final int id;
		public final int age;
		public final String name;
		public final Date date;

		public ConstructedClass(@ParamFrom("id") int id, @ParamFrom("age") int age, @ParamFrom("name") String name, @ParamFrom("date")Date date) {
			super();
			this.id = id;
			this.age = age;
			this.name = name;
			this.date = date;
		}
	}
	
	@Test
	public void test() {
		ConstructedClass data = new ConstructedClass(1, 19, "jane", new Date());
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		mapper.save(data);
		ConstructedClass data2 = mapper.read(ConstructedClass.class, 1);
		assertEquals(data.id, data2.id);
		assertEquals(data.age, data2.age);
		assertEquals(data.name, data2.name);
		assertEquals(data.date, data2.date);		
	}
	
	@AerospikeRecord(namespace = "test", set = "testSet") 
	public static class ConstructedClass2 {
		@AerospikeKey
		public final int id;
		public final int a;
		public int b;
		public int c;
		
		public ConstructedClass2(@ParamFrom("id") int id, @ParamFrom("a") int a) {
			this.id = id;
			this.a = a;
			System.out.println("Using 2 arg");
		}
		
		@AerospikeConstructor
		public ConstructedClass2(@ParamFrom("id") int id, @ParamFrom("a") int a, @ParamFrom("b") int b) {
			this.id = id;
			this.a = a;
			this.b = b;
			System.out.println("Using 3 arg");
		}
	}
	
	@Test
	public void test2() {
		ConstructedClass2 data = new ConstructedClass2(1, 2);
		data.b = 3;
		data.c = 4;
		
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		mapper.save(data);
		System.out.println("reading back");
		ConstructedClass2 data2 = mapper.read(ConstructedClass2.class, 1);
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
	
	@AerospikeRecord 
	public static class ConstructedClass3 {
		@AerospikeKey
		public final String id;
		public final int a;
		public final int b;
		public final int c;
		
		@AerospikeConstructor
		public ConstructedClass3(@ParamFrom("id") String id, @ParamFrom("a") int a, @ParamFrom("b") int b, @ParamFrom("c") int c) {
			this.id = id;
			this.a = a;
			this.b = b;
			this.c = c;
		}
	}
	
	@AerospikeRecord(namespace = "test", set = "testSet1") 
	public static class ConstructorContainerClass {
		@AerospikeKey
		public int id;
		@AerospikeEmbed(type = EmbedType.MAP, elementType = EmbedType.LIST)
		public List<ConstructedClass3> list;
	}
	
	@Test
	public void test4() {
		ConstructorContainerClass ccc = new ConstructorContainerClass();
		ccc.id = 1;
		ccc.list = new ArrayList<>();
		ccc.list.add(new ConstructedClass3("a", 1, 1, 4));
		ccc.list.add(new ConstructedClass3("b", 2, 2, 3));
		ccc.list.add(new ConstructedClass3("c", 3, 4, 2));
		ccc.list.add(new ConstructedClass3("d", 4, 3, 1));
		
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		mapper.save(ccc);
		ConstructorContainerClass ccc2 = mapper.read(ConstructorContainerClass.class, 1);
		assertEquals(ccc.id, ccc2.id);
		assertEquals(ccc.list.size(), ccc2.list.size());
		assertEquals(ccc.list.get(0).id, ccc2.list.get(0).id);
		assertEquals(ccc.list.get(0).a, ccc2.list.get(0).a);
		assertEquals(ccc.list.get(0).b, ccc2.list.get(0).b);
		assertEquals(ccc.list.get(0).c, ccc2.list.get(0).c);
	}
}
