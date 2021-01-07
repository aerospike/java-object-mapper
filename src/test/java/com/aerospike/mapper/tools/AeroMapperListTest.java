package com.aerospike.mapper.tools;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;

public class AeroMapperListTest {
	public static final String NAMESPACE = "test";
	private static IAerospikeClient client;
	@BeforeClass
	public static void setupClass() {
		client = new AerospikeClient("localhost", 3000);
	}
	
	@AfterClass
	public static void cleanupClass() {
		if (client != null) {
			client.close();
		}
	}
	
	private AeroMapper mapper;
	@Before 
	public void setup() {
		mapper = new AeroMapper.Builder(client).build();
		client.truncate(null, NAMESPACE, "testSet", null);
	}
	
	@AerospikeRecord(namespace = NAMESPACE, set = "testSet", mapAll = true)
	public static class TestV1 {
		public int a;
		public int b;
		public int c;
		public int d;
	}

	// Version 2 of the test record removes c from the database and adds e, leaving a, b, d, e persisted
	@AerospikeRecord(namespace = NAMESPACE, set = "testSet", mapAll = true, version = 2)
	public static class TestV2 {
		public int a;
		public int b;
		public int d;
		public int e;
	}

	// Version 3 of the test record removes a, e, from the database and adds f, g, leaving b, d, g, f persisted
	@AerospikeRecord(namespace = NAMESPACE, set = "testSet", mapAll = true, version = 3)
	public static class TestV3 {
		public int b;
		public int d;
		public int f;
		public int g;
	}

	@AerospikeRecord(namespace = NAMESPACE, set = "testSet", mapAll = true)
	public static class TestV1Container {
		@AerospikeKey
		public int id;
		@AerospikeEmbed(type = EmbedType.MAP)
		public TestV1 value;
	}
	
	@AerospikeRecord(namespace = NAMESPACE, set = "testSet", mapAll = true)
	public static class TestV2Container {
		@AerospikeKey
		public int id;
		@AerospikeEmbed(type = EmbedType.MAP)
		public TestV2 value;
	}
	
	@AerospikeRecord(namespace = NAMESPACE, set = "testSet", mapAll = true)
	public static class TestV3Container {
		@AerospikeKey
		public int id;
		@AerospikeEmbed(type = EmbedType.MAP)
		public TestV3 value;
	}
	
	@Test
	public void testSave() {
		TestV1Container container = new TestV1Container();
		container.id = 1;
		container.value = new TestV1();
		container.value.a = 1;
		container.value.b = 2;
		container.value.c = 3;
		container.value.d = 4;
		
		mapper.save(container);
		
		container = mapper.read(TestV1Container.class, 1);
		assertEquals(1, container.value.a);
		assertEquals(2, container.value.b);
		assertEquals(3, container.value.c);
		assertEquals(4, container.value.d);
	}
	@Test
	public void testSaveV1LoadV2() {
		TestV1Container container = new TestV1Container();
		container.id = 1;
		container.value = new TestV1();
		container.value.a = 1;
		container.value.b = 2;
		container.value.c = 3;
		container.value.d = 4;
		
		mapper.save(container);
		
		TestV2Container container2 = mapper.read(TestV2Container.class, 1);
		assertEquals(1, container2.value.a);
		assertEquals(2, container2.value.b);
		assertEquals(4, container2.value.d);
		assertEquals(0, container2.value.e);
	}
	@Test
	public void testSaveV2LoadV2() {
		TestV2Container container = new TestV2Container();
		container.id = 1;
		container.value = new TestV2();
		container.value.a = 1;
		container.value.b = 2;
		container.value.d = 4;
		container.value.e = 5;
		
		mapper.save(container);
		
		TestV2Container container2 = mapper.read(TestV2Container.class, 1);
		assertEquals(1, container2.value.a);
		assertEquals(2, container2.value.b);
		assertEquals(4, container2.value.d);
		assertEquals(5, container2.value.e);
	}
	@Test
	public void testSaveV1LoadV3() {
		TestV1Container container = new TestV1Container();
		container.id = 1;
		container.value = new TestV1();
		container.value.a = 1;
		container.value.b = 2;
		container.value.c = 3;
		container.value.d = 4;
		
		mapper.save(container);
		
		TestV3Container container2 = mapper.read(TestV3Container.class, 1);
		assertEquals(2, container2.value.b);
		assertEquals(4, container2.value.d);
		assertEquals(0, container2.value.f);
		assertEquals(0, container2.value.g);
	}
	@Test
	public void testSaveV2LoadV3() {
		TestV2Container container = new TestV2Container();
		container.id = 1;
		container.value = new TestV2();
		container.value.a = 1;
		container.value.b = 2;
		container.value.d = 3;
		container.value.e = 4;
		
		mapper.save(container);
		
		TestV3Container container2 = mapper.read(TestV3Container.class, 1);
		assertEquals(2, container2.value.b);
		assertEquals(3, container2.value.d);
		assertEquals(0, container2.value.f);
		assertEquals(0, container2.value.g);
	}
	
	@Test
	public void testSaveV3LoadV3() {
		TestV3Container container = new TestV3Container();
		container.id = 1;
		container.value = new TestV3();
		container.value.b = 1;
		container.value.d = 2;
		container.value.f = 3;
		container.value.g = 4;
		
		mapper.save(container);
		
		TestV3Container container2 = mapper.read(TestV3Container.class, 1);
		assertEquals(1, container2.value.b);
		assertEquals(2, container2.value.d);
		assertEquals(3, container2.value.f);
		assertEquals(4, container2.value.g);
	}

	
}
