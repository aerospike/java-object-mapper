package com.aerospike.mapper.example;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.DebugAerospikeClient;
import com.aerospike.client.DebugAerospikeClient.Granularity;
import com.aerospike.client.DebugAerospikeClient.Options;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.mapper.example.model.Customer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class NonJavaMapperApplication extends ApplicationBase {
	
	private void save(IAerospikeClient client, Customer customer) {
		Key key = new Key("test", "customer", customer.getCustomerId());
		
		Bin customerId = new Bin("id", customer.getCustomerId());
		Bin firstName = new Bin("firstName", customer.getFirstName());
		Bin lasttName = new Bin("lastName", customer.getFirstName());
		Bin dateOfBirth = new Bin("dob", customer.getDateOfBirth() == null ? null : customer.getDateOfBirth().getTime());
		Bin phone = new Bin("phone", customer.getPhone());
		Bin joinedBank = new Bin("joinedBank", customer.getJoinedBank() == null ? null : customer.getJoinedBank().getTime());
		Bin vip = new Bin("vip", customer.isVip());
		Bin salutation = new Bin("greet", customer.getPreferredSaluation());
		
		client.put(null, key, customerId, firstName, lasttName, dateOfBirth, phone, joinedBank, vip, salutation);
	}

	private Customer read(IAerospikeClient client, String id) {
		Key key = new Key("test", "customer", id);
		Record record = client.get(null, key);
		
		Customer result = new Customer(record.getString("id"), 
				record.getString("firstName"),
				record.getString("lastName"));
		
		Long date = record.getLong("dob");
		result.setDateOfBirth(date == null ? null : new Date(date));
		result.setPhone(record.getString("phone"));
		date = record.getLong("joinedBank");
		result.setJoinedBank(date == null ? null : new Date(date));
		result.setVip(record.getBoolean("vip"));
		result.setPreferredSaluation(record.getString("greet"));
		
		return result;
	}
	
	@Test
	public void run() throws JsonProcessingException {
		IAerospikeClient client = new DebugAerospikeClient(null, "127.0.0.1", 3000, new Options(Granularity.EVERY_CALL));
		
		Customer customer = createAndPopulateCustomer();
		save(client, customer);
		Customer customer2 = null;
		for (int i = 0; i < 100; i++) {
			long now = System.nanoTime();
			customer2 = read(client, customer.getCustomerId());
			System.out.println(String.format("Customer graph read time: %.3fms", (System.nanoTime() - now)/1000000f));
		}
		
		ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
		String readString = objectWriter.writeValueAsString(customer2);
		System.out.println(readString);
		String originalObject = objectWriter.writeValueAsString(customer);
		System.out.println(originalObject);
		assertEquals(originalObject, readString);

		client.close();
	}
}
