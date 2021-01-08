package com.aerospike.mapper;

import com.aerospike.mapper.annotations.*;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.tools.AeroMapper;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

}
