package com.aerospike.mapper.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeGetter;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.model.Person;
import com.aerospike.mapper.tools.model.PersonDifferentNames;

public class AeroMapperTest {
	
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
		mapper = new AeroMapper(client);
		client.truncate(null, NAMESPACE, "people", null);
		client.truncate(null, NAMESPACE, "account", null);
		client.truncate(null, NAMESPACE, "product", null);
	}
	
	@Test
	public void testSave() {
		Person p = new Person();
		p.setFirstName("John");
		p.setLastName("Doe");
		p.setAge(17);
		p.setBalance(123.10f);
		p.setDateOfBirth(new Date());
		p.setHeight(1.93);
		p.setPhoto("Photo bytes".getBytes());
		p.setSsn("123-456-7890");
		p.setValid(true);
		
		mapper.save(p);
		Person person2 = mapper.read(Person.class, p.getSsn());
		assertEquals(p.toString(), person2.toString());
		
		// Check the column names
		Record record = client.get(null, new Key(NAMESPACE, "people", p.getSsn()));
		assertEquals(record.getString("ssn"), p.getSsn());
		assertEquals(record.getInt("age"), p.getAge());
	}
	@Test
	public void testSaveDifferentNames() {
		PersonDifferentNames p = new PersonDifferentNames();
		p.setFirstName("John");
		p.setLastName("Doe");
		p.setAge(17);
		p.setBalance(123.10f);
		p.setDateOfBirth(new Date());
		p.setHeight(1.93);
		p.setPhoto("Photo bytes".getBytes());
		p.setSsn("123-456-7890");
		p.setValid(true);
		
		mapper.save(p);
		PersonDifferentNames person2 = mapper.read(PersonDifferentNames.class, p.getSsn());
		assertEquals(p.toString(), person2.toString());
		
		// Check the column names
		Record record = client.get(null, new Key(NAMESPACE, "people", p.getSsn()));
		assertEquals(record.getString("s"), p.getSsn());
		assertEquals(record.getInt("a"), p.getAge());
	}
	
	@AerospikeRecord(namespace = "test", set = "none")
	public static class PropertyWithNoSetter {
		@AerospikeBin(name="dummy")
		private int f1;
		@AerospikeGetter(name = "dummy")
		public int getDummy() { return 1; }
		public void setDummy(int dummy) { }
	}

	@AerospikeRecord(namespace = "test", set = "none")
	public static class DuplicateKeyClass {
		@AerospikeBin(name="dummy")
		private int f1;
		@AerospikeGetter(name = "dummy")
		public int getDummy() { return 1; }
		public void setDummy(int dummy) { }
	}
	

	@Test
	public void testDuplicateName() {
		try {
			mapper.preLoadClass(DuplicateKeyClass.class);
			fail();
		}
		catch (Exception e) {
		}
	}
	@Test
	public void testMissingSetter() {
		try {
			mapper.preLoadClass(PropertyWithNoSetter.class);
			fail();
		}
		catch (Exception e) {
		}
	}
}
