package com.aerospike.mapper.example;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.aerospike.client.DebugAerospikeClient;
import com.aerospike.client.DebugAerospikeClient.Granularity;
import com.aerospike.client.DebugAerospikeClient.Options;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.Replica;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.example.model.Account;
import com.aerospike.mapper.example.model.Address;
import com.aerospike.mapper.example.model.Branch;
import com.aerospike.mapper.example.model.Checkbook;
import com.aerospike.mapper.example.model.Customer;
import com.aerospike.mapper.example.model.Transaction;
import com.aerospike.mapper.tools.AeroMapper;

public class Application {

	@Test
	public void run() {
		Policy readPolicy = new Policy();
		readPolicy.maxRetries = 4;
		readPolicy.replica = Replica.SEQUENCE;
		readPolicy.sleepBetweenRetries = 50;
		readPolicy.socketTimeout = 250;
		readPolicy.totalTimeout = 2000;
		
		Policy txnReadPolicy = new Policy();
		txnReadPolicy.maxRetries = 3;
		txnReadPolicy.replica = Replica.SEQUENCE;
		txnReadPolicy.sleepBetweenRetries = 10;
		txnReadPolicy.socketTimeout = 50;
		txnReadPolicy.totalTimeout = 250;
		
		WritePolicy writePolicy = new WritePolicy();
		writePolicy.durableDelete = true;
		writePolicy.sendKey = true;	// For ease of debugging for now
		
		IAerospikeClient client = new DebugAerospikeClient(null, "127.0.0.1", 3000, new Options(Granularity.EVERY_CALL));
		
		AeroMapper mapper = new AeroMapper.Builder(client)
				.withWritePolicy(writePolicy).forAll()
				.withReadPolicy(txnReadPolicy).forAll()
				.withReadPolicy(txnReadPolicy).forClasses(Transaction.class)
				.build();
		
		Address mailingAddress = new Address("773 Elm St", "Apt 2", "Grand Junction", new char[] {'C', 'O'}, "83451");
		Address billingAddress = new Address("123 Main St", null, "Denver", new char[] {'C', 'O'}, "80001");
		Address alternateAddress = new Address("1 Park Road", null, "Miami", new char[] {'F', 'L'}, "98531");
				
		Customer customer = new Customer("cust1", "Bob", "Smith");
		customer.setDateOfBirth(new Date(new Date().getTime() - TimeUnit.MILLISECONDS.convert(30*365, TimeUnit.DAYS)));
		customer.setPhone("(555)555-1234");
		customer.setMailingAddress(mailingAddress);
		
		Account testAccount = new Account("ACC-1234", customer.getCustomerId(), "Test Account");
		testAccount.setBalance(100000);
		testAccount.setCard(true);
		testAccount.setBillingAddress(billingAddress);
		testAccount.getAlternateAddresses().add(mailingAddress);
		testAccount.getAlternateAddresses().add(alternateAddress);
		testAccount.setRouting("123456789");
		testAccount.setPaperless(true);
		testAccount.setOverdraftProtection(false);
		testAccount.setOnlineUserName("beesmith");
		testAccount.setLastLogin(new Date());

		Branch issuingBranch = new Branch("Br123", new Address("77 Park Road", null, "Miami", new char[] {'F', 'L'}, "98531"), "Miami Central");
		Checkbook checkbook = new Checkbook(testAccount.getAccountId(), 1, 100, new Date());
		checkbook.setIssuer(issuingBranch);
		checkbook.setRecalled(false);
		testAccount.getCheckbooks().put(1, checkbook);
		
		Branch otherIssuingBranch = new Branch("Br567", new Address("129 Bump Drive", null, "New York", new char[] {'N', 'Y'}, "77777"), "New York City Office");
		Checkbook checkbook2 = new Checkbook(testAccount.getAccountId(), 101, 600, new Date());
		checkbook2.setIssuer(otherIssuingBranch);
		checkbook2.setRecalled(false);
		testAccount.getCheckbooks().put(2, checkbook2);
		
		customer.getAccounts().add(testAccount);
		
		mapper.save(issuingBranch);
		mapper.save(otherIssuingBranch);
		mapper.save(testAccount);
		mapper.save(customer);
		mapper.save(checkbook);
		mapper.save(checkbook2);
		
		for (int i = 0; i < 100; i++) {
			long now =System.nanoTime();
			Customer readCustomer = mapper.read(Customer.class, customer.getCustomerId());
			System.out.println((System.nanoTime() - now)/1000);
		}
	}
}
