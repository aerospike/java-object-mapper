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
		public static final String NO_ARG_FACTORY = "Created from factory";
		public static final String MAP_ARG_FACTORY = "Map arg factory";
		public static final String CLASS_ARG_FACTORY = "Class arg factory";
		public static final String CLASS_MAP_ARG_FACTORY = "Class and map arg factory";

		public static A createA() {
			A newA = new A();
			newA.factory = NO_ARG_FACTORY;
			newA.factoryDetails = "";
			return newA;
		}

		public static A createA1(Map<String, Object> data) {
			A newA = new A();
			newA.factory = MAP_ARG_FACTORY;
			newA.factoryDetails = "" + data.size();
			return newA;
		}

		public static A createA2(Class<?> desiredType) {
			A newA = new A();
			newA.factory = CLASS_ARG_FACTORY;
			newA.factoryDetails = desiredType.getSimpleName();
			return newA;
		}

		public static A createA3(Class<?> desiredType, Map<String, Object> data) {
			A newA = new A();
			newA.factory = CLASS_MAP_ARG_FACTORY;
			newA.factoryDetails = desiredType.getSimpleName() + " " + data.size();
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
		@AerospikeExclude
		public String factoryDetails;
		
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
		mapper.save(a1);
		A a2 = mapper.read(A.class, a1.id);

		assertEquals(a1.age, a2.age);
		assertEquals(a1.name, a2.name);
		assertEquals(a1.id, a2.id);
		assertEquals(Factory.NO_ARG_FACTORY, a2.factory);
	}

	@Test
	public void runTest2() throws Exception {
		A a1 = new A("a", 10, 1);

		// Override the config to use a different factory method
		String yaml = "---\n"
				+ "classes:\n"
				+ "- class: com.aerospike.mapper.FactoryConstructorTest$A\n"
				+ "  factoryMethod: createA1\n"
				+ "  factoryClass: com.aerospike.mapper.FactoryConstructorTest$Factory";
				
		AeroMapper mapper = new AeroMapper.Builder(client).withConfiguration(yaml).build();
		mapper.save(a1);
		A a2 = mapper.read(A.class, a1.id);

		assertEquals(a1.age, a2.age);
		assertEquals(a1.name, a2.name);
		assertEquals(a1.id, a2.id);
		assertEquals(Factory.MAP_ARG_FACTORY, a2.factory);
		assertEquals("3", a2.factoryDetails);
	}
	
	@Test
	public void runTest3() throws Exception {
		A a1 = new A("a", 10, 1);

		// Override the config to use a different factory method
		String yaml = "---\n"
				+ "classes:\n"
				+ "- class: com.aerospike.mapper.FactoryConstructorTest$A\n"
				+ "  factoryMethod: createA2\n"
				+ "  factoryClass: com.aerospike.mapper.FactoryConstructorTest$Factory";
				
		AeroMapper mapper = new AeroMapper.Builder(client).withConfiguration(yaml).build();
		mapper.save(a1);
		A a2 = mapper.read(A.class, a1.id);

		assertEquals(a1.age, a2.age);
		assertEquals(a1.name, a2.name);
		assertEquals(a1.id, a2.id);
		assertEquals(Factory.CLASS_ARG_FACTORY, a2.factory);
		assertEquals("A", a2.factoryDetails);
	}

	@Test
	public void runTest4() throws Exception {
		A a1 = new A("a", 10, 1);

		// Override the config to use a different factory method
		String yaml = "---\n"
				+ "classes:\n"
				+ "- class: com.aerospike.mapper.FactoryConstructorTest$A\n"
				+ "  factoryMethod: createA3\n"
				+ "  factoryClass: com.aerospike.mapper.FactoryConstructorTest$Factory";
				
		AeroMapper mapper = new AeroMapper.Builder(client).withConfiguration(yaml).build();
		mapper.save(a1);
		A a2 = mapper.read(A.class, a1.id);

		assertEquals(a1.age, a2.age);
		assertEquals(a1.name, a2.name);
		assertEquals(a1.id, a2.id);
		assertEquals(Factory.CLASS_MAP_ARG_FACTORY, a2.factory);
		assertEquals("A 3", a2.factoryDetails);
	}
}