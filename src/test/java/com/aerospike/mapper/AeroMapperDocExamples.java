package com.aerospike.mapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeOrdinal;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeReference;
import com.aerospike.mapper.annotations.AerospikeVersion;
import com.aerospike.mapper.tools.AeroMapper;

public class AeroMapperDocExamples extends AeroMapperBaseTest {

    @AerospikeRecord(namespace = "test", set = "product", mapAll = true)
    public static class Product {
        public String productId;
        public int version;
        public String name;
        public Date createdDate;
    }

    @AerospikeRecord(namespace = "test", set = "account", mapAll = true)
    public static class Account {
        @AerospikeKey
        public long id;
        public String title;
        public int balance;
        @AerospikeEmbed(type = EmbedType.LIST)
        public Product product;
    }


    @Test
    public void runEmbed() {
        Product product = new Product();
        product.createdDate = new Date();
        product.name = "Sample Product";
        product.productId = "SP-1";
        product.version = 1;

        Account account = new Account();
        account.id = 123;
        account.title = "Test Account";
        account.balance = 111;
        account.product = product;

        mapper.save(account);
    }

    @AerospikeRecord(namespace = "test", set = "people")
    public static class Person {

        @AerospikeKey
        @AerospikeBin(name = "ssn")
        public String ssn;
        @AerospikeBin
        public String firstName;

        @AerospikeBin(name = "lastName")
        public String lastName;

        @AerospikeBin(name = "age")
        public int age;

        @AerospikeBin(name = "primAcc")
        @AerospikeReference(lazy = true)
        public Account primaryAccount;

        @AerospikeBin(name = "accts")
        @AerospikeReference(lazy = true)
        public List<Account> accounts;
    }

    private AeroMapper mapper;

    @Before
    public void setup() {
        mapper = new AeroMapper.Builder(client).build();
        client.truncate(null, NAMESPACE, "people", null);
        client.truncate(null, NAMESPACE, "account", null);
    }

    //	@Test
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
    
    @AerospikeRecord(namespace = "test", set = "testSet", mapAll = true)
    public static class IntContainer {
    	public int a;
    	public int b;
    	public int c;
    }
    
    @AerospikeRecord(namespace = "test", set = "testSet", mapAll = true) 
    public static class IntMapper {
    	@AerospikeKey
    	public int id;
    	@AerospikeEmbed(type = EmbedType.LIST)
    	public IntContainer container;
    }

    @AerospikeRecord(namespace = "test", set = "testSet", mapAll = true, version = 2)
    public static class IntContainerV2 {
    	@AerospikeVersion(max = 1)
    	public int a;
    	public int b;
    	public int c;
    	@AerospikeVersion(min = 2)
    	public int d;
    }
    
    @AerospikeRecord(namespace = "test", set = "testSet", mapAll = true) 
    public static class IntMapperV2 {
    	@AerospikeKey
    	public int id;
    	@AerospikeEmbed(type = EmbedType.LIST)
    	public IntContainerV2 container;
    }
    
    @Test
    public void testListOrdering() {
    	IntContainer container = new IntContainer();
    	container.a = 1;
    	container.b = 2;
    	container.c = 3;
    	IntMapper mapper2 = new IntMapper();
    	mapper2.id = 1;
    	mapper2.container = container;
    	mapper.save(mapper2);
    	
    	IntMapperV2 v2 = mapper.read(IntMapperV2.class, 1);
    	
    	IntContainerV2 containerV2 = new IntContainerV2();
    	containerV2.b = 2;
    	containerV2.c = 3;
    	containerV2.d = 4;
    	IntMapperV2 mapperV2 = new IntMapperV2();
    	mapperV2.id = 2;
    	mapperV2.container = containerV2;
    	mapper.save(mapperV2);
    	
    	
    	for (int i = 1; i <= 2; i++) {
    		mapperV2 = mapper.read(mapperV2.getClass(), i);
    		System.out.println("Read " + i);
    	}
    }
    
    public static enum AccountType {
    	SAVINGS, CHEQUING
    }
    @AerospikeRecord(namespace = "test", set = "accounts", mapAll = true) 
    public static class Accounts {
    	@AerospikeKey
    	public int id;
    	public String name;
    	public AccountType type;
    	@AerospikeEmbed(elementType = EmbedType.LIST)
    	public Map<String, Transactions> transactions;
    	
    	public Accounts() {
    		this.transactions = new HashMap<>();
    	}
		public Accounts(int id, String name, AccountType type) {
			this();
			this.id = id;
			this.name = name;
			this.type = type;
		}
    }
    
    @AerospikeRecord(namespace = "test", set = "txns", mapAll = true)
    public static class Transactions {
    	public String txnId;
    	@AerospikeOrdinal(value = 1)
    	public Instant date;
    	public double amt;
    	public String merchant;
    	public Transactions() {}
		public Transactions(String txnId, Instant date, double amt, String merchant) {
			super();
			this.txnId = txnId;
			this.date = date;
			this.amt = amt;
			this.merchant = merchant;
		}
    }
    
    @Test
    public void testAccounts() {
    	Accounts account = new Accounts(1, "Savings Account", AccountType.SAVINGS);
    	Transactions txn1 = new Transactions("Txn1", Instant.now(), 100.0, "Bob's store");
    	Transactions txn2 = new Transactions("Txn2", Instant.now().minus(Duration.ofHours(8)), 134.99, "Kim's store");
    	Transactions txn3 = new Transactions("Txn3", Instant.now().minus(Duration.ofHours(20)), 75.43, "Sue's store");
    	
    	account.transactions.put(txn1.txnId, txn1);
    	account.transactions.put(txn2.txnId, txn2);
    	account.transactions.put(txn3.txnId, txn3);
    	
    	mapper.save(account);
    	System.out.println("done");
    }
}
