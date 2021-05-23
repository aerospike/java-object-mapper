package com.aerospike.mapper.reactive;

import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import org.junit.Test;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ReactiveSubclassListTest extends ReactiveAeroMapperBaseTest {
    @AerospikeRecord(namespace = "test", set = "account")
    public static class Account {
        @AerospikeKey
        public int id;
        public String name;
    }

    @AerospikeRecord(namespace = "test", set = "account", shortName = "PFOLIO")
    public static class Portfolio extends Account {
        public List<String> subAccounts;
        public Portfolio() {
            subAccounts = new ArrayList<>();
        }
    }

    @AerospikeRecord(shortName = "SVNG")
    public static class Savings extends Account {
        public int balance;
    }

    @AerospikeRecord(namespace = "test", set = "accounts")
    public static class Container {
        @AerospikeKey
        public int id;
        @AerospikeEmbed(type = AerospikeEmbed.EmbedType.LIST)
        public Account account;
        @AerospikeEmbed(type = AerospikeEmbed.EmbedType.LIST, elementType = AerospikeEmbed.EmbedType.LIST)
        public List<Account> accounts;

        public Container() {
            this.accounts = new ArrayList<>();
        }
    }

    @Test
    public void test() {
        Container container = new Container();
        container.id = 1;

        Portfolio portfolio = new Portfolio();
        portfolio.subAccounts.add("sub-1");
        portfolio.subAccounts.add("sub-2");
        portfolio.id = 222;
        container.account = portfolio;
        container.accounts.add(portfolio);
        Savings savings = new Savings();
        savings.balance = 100;
        savings.name = "Bob";
        savings.id = 33;

        container.accounts.add(savings);

        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();
        reactiveMapper.save(container).subscribeOn(Schedulers.parallel()).block();

        Container object = reactiveMapper.read(Container.class, 1).subscribeOn(Schedulers.parallel()).block();
        assert object != null;
        assertEquals(container.id, object.id);
        assertEquals(container.account.id, object.account.id);
        assertEquals(container.account.name, object.account.name);
        assertEquals(((Portfolio)container.account).subAccounts.get(0), ((Portfolio)object.account).subAccounts.get(0));
        assertEquals(((Portfolio)container.account).subAccounts.get(1), ((Portfolio)object.account).subAccounts.get(1));

        assertEquals(container.accounts.get(0).id, object.accounts.get(0).id);
        assertEquals(container.accounts.get(0).name, object.accounts.get(0).name);
        assertEquals(((Portfolio)container.accounts.get(0)).subAccounts.get(0), ((Portfolio)object.accounts.get(0)).subAccounts.get(0));
        assertEquals(((Portfolio)container.accounts.get(0)).subAccounts.get(1), ((Portfolio)object.accounts.get(0)).subAccounts.get(1));

        assertEquals(container.accounts.get(1).id, object.accounts.get(1).id);
        assertEquals(container.accounts.get(1).name, object.accounts.get(1).name);
        assertEquals(((Savings)container.accounts.get(1)).balance, ((Savings)object.accounts.get(1)).balance);
    }
}
