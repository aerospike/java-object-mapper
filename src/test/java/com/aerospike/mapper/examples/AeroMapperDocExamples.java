package com.aerospike.mapper.examples;

import com.aerospike.client.AerospikeException;
import com.aerospike.mapper.AeroMapperBaseTest;
import com.aerospike.mapper.annotations.*;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.annotations.AerospikeReference.ReferenceType;
import com.aerospike.mapper.tools.AeroMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.fail;

public class AeroMapperDocExamples extends AeroMapperBaseTest {

    @AerospikeRecord(namespace = "test", set = "product")
    public static class Product {
        public String productId;
        public int version;
        public String name;
        public Date createdDate;
    }

    @AerospikeRecord(namespace = "test", set = "account")
    public static class Account {
        @AerospikeKey
        public long id;
        public String title;
        public int balance;
        @AerospikeEmbed(type = EmbedType.LIST)
        public Product product;
    }

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

    @AerospikeRecord(namespace = "test", set = "people", mapAll = false)
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
        @AerospikeReference(type = ReferenceType.DIGEST)
        public Account primaryAccount;

        @AerospikeBin(name = "accts")
        @AerospikeReference(lazy = true)
        public List<Account> accounts;
    }

    private AeroMapper mapper;

    //@BeforeEach
    public void setup() {
        mapper = new AeroMapper.Builder(client).build();
        client.truncate(null, NAMESPACE, "people", null);
        client.truncate(null, NAMESPACE, "account", null);
    }

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
    
    @AerospikeRecord(namespace = "test", set = "testSet")
    public static class IntContainer {
    	public int a;
    	public int b;
    	public int c;
    }
    
    @AerospikeRecord(namespace = "test", set = "testSet") 
    public static class IntMapper {
    	@AerospikeKey
    	public int id;
    	@AerospikeEmbed(type = EmbedType.LIST)
    	public IntContainer container;
    }

    @AerospikeRecord(namespace = "test", set = "testSet", version = 2)
    public static class IntContainerV2 {
    	@AerospikeVersion(max = 1)
    	public int a;
    	public int b;
    	public int c;
    	@AerospikeVersion(min = 2)
    	public int d;
    }
    
    @AerospikeRecord(namespace = "test", set = "testSet") 
    public static class IntMapperV2 {
    	@AerospikeKey
    	public int id;
    	@AerospikeEmbed(type = EmbedType.LIST)
    	public IntContainerV2 container;
    }

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
    
    public enum AccountType {
    	SAVINGS, CHEQUING
    }

    @AerospikeRecord(namespace = "test", set = "accounts") 
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
    
    @AerospikeRecord(namespace = "test", set = "txns")
    public static class Transactions {
    	public String txnId;
    	@AerospikeOrdinal()
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
    
    @AerospikeRecord(namespace = "test", set = "parent")
    public static class Parent {
    	@AerospikeKey
    	int id;
    	String name;
    	
    	@AerospikeEmbed(type = EmbedType.MAP)
    	public Child mapEmbedChild;
    	
    	@AerospikeEmbed(type = EmbedType.LIST)
    	public Child listEmbedChild;

    	@AerospikeReference(type = ReferenceType.DIGEST)
    	public Child refChild;

		public Parent(@ParamFrom("id") int id, @ParamFrom("name") String name, @ParamFrom("refChild") Child child) {
			super();
			this.id = id;
			this.name = name;
			this.mapEmbedChild = child;
			this.listEmbedChild = child;
			this.refChild = child;
		}
    }
    
    @AerospikeRecord(namespace = "test", set = "parent")
    public static class ParentWithBadChild {
    	@AerospikeKey
    	int id;
    	String name;
    	
    	@AerospikeEmbed(type = EmbedType.MAP)
    	public Child mapEmbedChild;
    	
    	@AerospikeEmbed(type = EmbedType.LIST)
    	public Child listEmbedChild;

    	@AerospikeReference(type = ReferenceType.DIGEST, lazy = true)
    	public Child refChild;

		public ParentWithBadChild(@ParamFrom("id") int id, @ParamFrom("name") String name, @ParamFrom("refChild") Child child) {
			super();
			this.id = id;
			this.name = name;
			this.mapEmbedChild = child;
			this.listEmbedChild = child;
			this.refChild = child;
		}
    }
    
    @AerospikeRecord(namespace = "test", set = "child")
    public static class Child {
    	@AerospikeKey
    	int id;
    	String name;
    	Date date;

    	public Child(@ParamFrom("id") int id, @ParamFrom("name")String name, @ParamFrom("date") Date date) {
			super();
			this.id = id;
			this.name = name;
			this.date = date;
		}
    }

    public void showParentWithChildren() {
    	Child child = new Child(1, "child", new Date());
    	Parent parent = new Parent(10, "parent", child);
    	mapper.save(parent);
    	
    	ParentWithBadChild parent2 = new ParentWithBadChild(11, "parent2", child);
    	try {
    		mapper.save(parent2);
    		fail();
    	}
    	catch (AerospikeException ae) {
    		// Expected 
    	}
    	
    	// Since the child is referenced, it needs to be saved explicitly in the database
    	// If it were only embedded, it would not be necessary to save explicitly.
    	mapper.save(child);
    	// TODO: Validate the results
    	System.out.println("done");
    }
}
