package com.aerospike.mapper;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;

public class SubclassListTest extends AeroMapperBaseTest {
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
		@AerospikeEmbed(type = EmbedType.LIST)
		public Account account;
		@AerospikeEmbed(type = EmbedType.LIST, elementType = EmbedType.LIST)
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
		
		AeroMapper mapper = new AeroMapper.Builder(client).build();
//		mapper.save(container.account);
		mapper.save(container);
		
		Container object = mapper.read(Container.class, 1);
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
