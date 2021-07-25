package com.aerospike.mapper.examples;

import com.aerospike.client.*;
import com.aerospike.mapper.examples.model.Customer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NonJavaMapperApplication extends ApplicationBase {
	
	private void save(IAerospikeClient client, Customer customer) {
		Key key = new Key("test", "customer", customer.getCustomerId());
		
		Bin customerId = new Bin("id", customer.getCustomerId());
		Bin firstName = new Bin("firstName", customer.getFirstName());
		Bin lastName = new Bin("lastName", customer.getLastName());
		Bin dateOfBirth = new Bin("dob", customer.getDateOfBirth() == null ? null : customer.getDateOfBirth().getTime());
		Bin phone = new Bin("phone", customer.getPhone());
		Bin joinedBank = new Bin("joinedBank", customer.getJoinedBank() == null ? null : customer.getJoinedBank().getTime());
		Bin vip = new Bin("vip", customer.isVip());
		Bin salutation = new Bin("greet", customer.getPreferredSalutation());
		
		client.put(null, key, customerId, firstName, lastName, dateOfBirth, phone, joinedBank, vip, salutation);
	}

	private Customer read(IAerospikeClient client, String id) {
		Key key = new Key("test", "customer", id);
		Record record = client.get(null, key);
		
		Customer result = new Customer(record.getString("id"), 
				record.getString("firstName"),
				record.getString("lastName"));
		
		long date = record.getLong("dob");
		result.setDateOfBirth(new Date(date));
		result.setPhone(record.getString("phone"));
		date = record.getLong("joinedBank");
		result.setJoinedBank(new Date(date));
		result.setVip(record.getBoolean("vip"));
		result.setPreferredSalutation(record.getString("greet"));
		
		return result;
	}

	public void run() throws JsonProcessingException {
		IAerospikeClient client = new AerospikeClient(null, "127.0.0.1", 3000);
		
		Customer customer = createAndPopulateCustomer();
		save(client, customer);
		Customer customer2 = null;
		for (int i = 0; i < 100; i++) {
			long now = System.nanoTime();
			customer2 = read(client, customer.getCustomerId());
			System.out.printf("Customer graph read time: %.3fms%n", (System.nanoTime() - now)/1000000f);
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
