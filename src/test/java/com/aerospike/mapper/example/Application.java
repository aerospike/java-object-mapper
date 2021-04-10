package com.aerospike.mapper.example;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.aerospike.client.DebugAerospikeClient;
import com.aerospike.client.DebugAerospikeClient.Granularity;
import com.aerospike.client.DebugAerospikeClient.Options;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.Replica;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.example.model.Address;
import com.aerospike.mapper.example.model.Branch;
import com.aerospike.mapper.example.model.Checkbook;
import com.aerospike.mapper.example.model.Customer;
import com.aerospike.mapper.example.model.Property;
import com.aerospike.mapper.example.model.Transaction;
import com.aerospike.mapper.example.model.accounts.Account;
import com.aerospike.mapper.example.model.accounts.LoanAccount;
import com.aerospike.mapper.example.model.accounts.PortfolioAccount;
import com.aerospike.mapper.tools.AeroMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class Application extends ApplicationBase {

	@Test
	public void run() throws JsonProcessingException {
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
				.withReadPolicy(readPolicy).forAll()
				.withReadPolicy(txnReadPolicy).forClasses(Transaction.class)
				.build();
		
		Address mailingAddress = new Address("773 Elm St", "Apt 2", "Grand Junction", "CO", "83451");
		Address billingAddress = new Address("123 Main St", null, "Denver", "CO", "80001");
		Address alternateAddress = new Address("1 Park Road", null, "Miami", "FL", "98531");
				
		Customer customer = createAndPopulateCustomer();
		customer.setMailingAddress(mailingAddress);
		
		/* Create a checking account for this customer */
		Account checkingAccount = createAndPopulateChecking(customer.getCustomerId());
		checkingAccount.setBillingAddress(billingAddress);
		checkingAccount.getAlternateAddresses().add(mailingAddress);
		checkingAccount.getAlternateAddresses().add(alternateAddress);

		Branch issuingBranch = new Branch("Br123", new Address("77 Park Road", null, "Miami", "FL", "98531"), "Miami Central");
		Checkbook checkbook = createAndPopulateCheckbook1(checkingAccount.getAccountId(), issuingBranch);
		checkingAccount.getCheckbooks().put(1, checkbook);
		
		Branch otherIssuingBranch = new Branch("Br567", new Address("129 Bump Drive", null, "New York", "NY", "77777"), "New York City Office");
		Checkbook checkbook2 = createAndPopulateCheckbook2(checkingAccount.getAccountId(), otherIssuingBranch);
		checkingAccount.getCheckbooks().put(2, checkbook2);
		
		mapper.save(checkingAccount);
		customer.getAccounts().add(checkingAccount);

		/* Create a savings account for this customer */
		Account savingsAccount = createAndPopulateSavingsAccount(customer.getCustomerId());
		savingsAccount.setBillingAddress(billingAddress);
		savingsAccount.getAlternateAddresses().add(mailingAddress);

		mapper.save(savingsAccount);
		customer.getAccounts().add(savingsAccount);

		/* Create a porfolio account for this customer */
		Property property1 = createAndPopulateProperty1();
		Property property2 = createAndPopulateProperty2();
		Property property3 = createAndPopulateProperty3();
		mapper.save(property1);
		mapper.save(property2);
		mapper.save(property3);

		PortfolioAccount portfolioAccount = createAndPopulatePortfolioAccount(customer.getCustomerId());
		portfolioAccount.setBillingAddress(billingAddress);
		portfolioAccount.getAlternateAddresses().add(mailingAddress);
		portfolioAccount.addPropertyToPortfolio(property1, 0.0239f);
		portfolioAccount.addPropertyToPortfolio(property2, 0.0299f);
		portfolioAccount.addPropertyToPortfolio(property3, 0.0319f);

		mapper.save(portfolioAccount);
		customer.getAccounts().add(portfolioAccount);

		/* Create a loan account for this customer */
		
		LoanAccount loanAccount = createAndPopulateLoanAccount(customer.getCustomerId());
		loanAccount.setBillingAddress(billingAddress);
		loanAccount.getAlternateAddresses().add(mailingAddress);
		customer.getAccounts().add(loanAccount);

		Property securityProperty = createAndPopulateProperty4();
		loanAccount.setSecurityProperty(securityProperty);

		mapper.save(securityProperty);
		mapper.save(loanAccount);

		mapper.save(issuingBranch);
		mapper.save(otherIssuingBranch);
		mapper.save(customer);
		mapper.save(checkbook);
		mapper.save(checkbook2);
		
		Customer readCustomer = null;
		for (int i = 0; i < 100; i++) {
			long now = System.nanoTime();
			readCustomer = mapper.read(Customer.class, customer.getCustomerId());
			System.out.println(String.format("Customer graph read time: %.3fms", (System.nanoTime() - now)/1000000f));
		}
		ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
		String readString = objectWriter.writeValueAsString(readCustomer);
		System.out.println(readString);
		String originalObject = objectWriter.writeValueAsString(customer);
		assertEquals(originalObject, readString);
	}
}
