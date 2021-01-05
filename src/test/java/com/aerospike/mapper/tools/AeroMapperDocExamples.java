package com.aerospike.mapper.tools;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeReference;

public class AeroMapperDocExamples {
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
	
	@AerospikeRecord(namespace = "test", set = "account", mapAll = true)
	public static class Account {
		@AerospikeKey
		public long id;
		public String title;
		public int balance;
	}

	@AerospikeRecord(namespace="test", set="people")
	public static class Person {
		
	    @AerospikeKey
	    @AerospikeBin(name="ssn")
	    public String ssn; 
	    @AerospikeBin
	    public String firstName;
	    
	    @AerospikeBin(name="lastName")
	    public String lastName;
	    
	    @AerospikeBin(name="age")
	    public int age;

		@AerospikeBin(name = "primAcc")
		@AerospikeReference(lazy = true)
		public Account primaryAccount;
		
		@AerospikeBin(name="accts")
		@AerospikeReference(lazy = true)
		public List<Account> accounts;
	}

	private AeroMapper mapper;
	@Before 
	public void setup() {
		mapper = new AeroMapper(client);
		client.truncate(null, NAMESPACE, "people", null);
		client.truncate(null, NAMESPACE, "account", null);
	}
	
	@Test
	public void run() {
		Account account = new Account();
		account.id = 103;
		account.title = "Primary Savings Account";
		account.balance = 137;
		
		Person person = new Person();
		person.ssn = "123-456-7890";
		person.firstName = "John";
		person.lastName = "Doe";
		person.age = 43;
		person.primaryAccount = account;

		Account acc1 = new Account();
		acc1.id = 101;
		acc1.title = "Primary Checking Account";
		acc1.balance = 100;

		Account acc2 = new Account();
		acc2.id = 102;
		acc2.title = "Secondary Savings Account";
		acc2.balance = 200;

		person.accounts = new ArrayList<>();
		person.accounts.add(acc1);
		person.accounts.add(acc2);
		
		mapper.save(account);
		mapper.save(person);
		
		Person loadedPerson = mapper.read(Person.class, "123-456-7890");
		System.out.printf("ssn = %s, name = %s %s, balance = %d",
				loadedPerson.ssn, loadedPerson.firstName, loadedPerson.lastName,
				loadedPerson.primaryAccount.balance);
		
	}

}
