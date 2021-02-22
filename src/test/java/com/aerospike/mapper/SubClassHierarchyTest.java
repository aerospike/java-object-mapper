package com.aerospike.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeConstructor;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeReference;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.mapper.tools.ClassCache;
import com.aerospike.mapper.tools.ClassCacheEntry;

public class SubClassHierarchyTest extends AeroMapperBaseTest {
	@AerospikeRecord
	public static class BaseClass {
		private Date lastReadTime;
		private final Date creationTime;
		
		public BaseClass() {
			this.creationTime = new Date();
		}
	}
	
	@AerospikeRecord(set = "customer", namespace = "test")
	public static class Customer extends BaseClass {
		@AerospikeKey
		@AerospikeBin(name = "id")
		private final String customerId;
		private final String name;
		@AerospikeBin(name = "date")
		private Date dateJoined;
		@AerospikeReference
		private List<Account> accounts;
		private Map<String, Account> accountMap;

		private Account primaryAcc;
		private Account secondaryAcc;
		private Savings savingsAcc;
		private Checking checkAcc;
		private Portfolio pfolioAcc;
		
		public Customer(String customerId, String name) {
			this(customerId, name, new Date());
		}
		
		@AerospikeConstructor
		public Customer(@ParamFrom("id") String customerId, @ParamFrom("name") String name, @ParamFrom("date") Date dateJoined) {
			this.customerId = customerId;
			this.name = name;
			this.dateJoined = dateJoined;
			this.accounts = new ArrayList<>();
			this.accountMap = new HashMap<>();
		}

		public Date getDateJoined() {
			return dateJoined;
		}

		public void setDateJoined(Date dateJoined) {
			this.dateJoined = dateJoined;
		}

		public List<Account> getAccounts() {
			return accounts;
		}

		public void setAccounts(List<Account> accounts) {
			this.accounts = accounts;
		}

		public String getCustomerId() {
			return customerId;
		}

		public String getName() {
			return name;
		}
	}
	
	@AerospikeRecord(namespace = "test", set = "subaccs", ttl=3600, durableDelete = true, sendKey = true)
	public static class Account extends BaseClass {
		@AerospikeKey
		protected String id;
		protected long balance;
	}

	// Don't need the parameters here, this is just for testing
	@AerospikeRecord(namespace = "test", set = "subaccs", shortName = "SVG")
	public static class Savings extends Account {
		private long numDeposits;
		private float interestRate;
	}
	
	@AerospikeRecord(shortName = "CHK")
	public static class Checking extends Account {
		private int checksWritten;
	}
	
	@AerospikeRecord(namespace = "test", set = "pfolio")
	public static class Portfolio extends Account {
		private List<String> subAccounts;
	}
	
	private void dumpClassCacheEntry(ClassCacheEntry<?> entry) {
		System.out.println(entry.getUnderlyingClass().getName());
		System.out.println("\tNamespace = " + entry.getNamespace());
		System.out.println("\tSet = " + entry.getSetName());
		System.out.println("\tShortened Name = " + entry.getShortenedClassName());
		System.out.println("\tIs Child class = " + entry.isChildClass());
		System.out.println("\tTTL = " + entry.getTtl());
		System.out.println("\tDurable Deletes = " + entry.getDurableDelete());
	}
	
	@Test
	public void runTest() {
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		
		ClassCacheEntry<BaseClass> baseClass = ClassCache.getInstance().loadClass(BaseClass.class, mapper);
		ClassCacheEntry<Customer> customerClass = ClassCache.getInstance().loadClass(Customer.class, mapper);
		ClassCacheEntry<Account> accountClass = ClassCache.getInstance().loadClass(Account.class, mapper);
		ClassCacheEntry<Savings> savingsClass = ClassCache.getInstance().loadClass(Savings.class, mapper);
		ClassCacheEntry<Checking> checkingClass = ClassCache.getInstance().loadClass(Checking.class, mapper);
		ClassCacheEntry<Portfolio> portfolioClass = ClassCache.getInstance().loadClass(Portfolio.class, mapper);
		
		dumpClassCacheEntry(baseClass);
		dumpClassCacheEntry(customerClass);
		dumpClassCacheEntry(accountClass);
		dumpClassCacheEntry(savingsClass);
		dumpClassCacheEntry(checkingClass);
		dumpClassCacheEntry(portfolioClass);
		
		Customer customer = new Customer("cust1", "Tim");

		Savings savingsAccount = new Savings();
		savingsAccount.interestRate = 0.03f;
		savingsAccount.numDeposits = 17;
		savingsAccount.id = "SVNG1";
		savingsAccount.balance = 200;
		customer.accounts.add(savingsAccount);
		mapper.save(savingsAccount);
		
		Checking checkingAccount = new Checking();
		checkingAccount.checksWritten = 4;
		checkingAccount.id = "CHK2";
		checkingAccount.balance = 600;
		customer.accounts.add(checkingAccount);
		mapper.save(checkingAccount);
		
		Portfolio portfolio = new Portfolio();
		portfolio.subAccounts = Arrays.asList(new String[] {"sub-1", "sub-2", "sub-3"});
		portfolio.id = "PF1";
		portfolio.balance = 1000;
		customer.accounts.add(portfolio);
		mapper.save(portfolio);
		
		customer.pfolioAcc = portfolio;
		customer.checkAcc = checkingAccount;
		customer.savingsAcc = savingsAccount;
		customer.primaryAcc = checkingAccount;
		customer.secondaryAcc = portfolio;
		
		customer.accountMap.put("A", portfolio);
		customer.accountMap.put("B", savingsAccount);
		customer.accountMap.put("C", checkingAccount);

		mapper.save(customer);
		
		Customer customer2 = mapper.read(Customer.class, customer.getCustomerId());
		// TODO: validate the 2 object graphs are equivalent
		System.out.println(customer2);
	}
}
