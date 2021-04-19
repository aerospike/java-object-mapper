package com.aerospike.mapper;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;

public class AeroMapperObjectTransformTest extends AeroMapperBaseTest {
	@AerospikeRecord()
	public static class Transaction {
		public String name;
		public int value;
		@AerospikeKey
		public long time;
		
		public Transaction() {}

		public Transaction(String name, int value, long time) {
			super();
			this.name = name;
			this.value = value;
			this.time = time;
		}
	}
	
	@AerospikeRecord(namespace = "test", set = "account")
	public static class Account {
		@AerospikeKey
		@AerospikeBin(name = "id")
		public int accountId;
	
		@AerospikeEmbed(type = EmbedType.MAP, elementType = EmbedType.LIST)
		public List<Transaction> txns;
		
		@AerospikeEmbed(type = EmbedType.LIST, elementType = EmbedType.LIST)
		public List<Transaction> txns2;
		
		public Account() {
			this.txns = new ArrayList<>();
			this.txns2 = new ArrayList<>();
		}
	}
	
	@Test
	public void saveTest() {
		Account account = new Account();
		account.accountId = 1;
		account.txns.add(new Transaction("details1", 100, 101));
		account.txns.add(new Transaction("details2", 200, 99));
		account.txns.add(new Transaction("details3", 300, 1010));
		
		account.txns2.add(new Transaction("details1", 100, 101));
		account.txns2.add(new Transaction("details2", 200, 99));
		account.txns2.add(new Transaction("details3", 300, 1010));
		
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		mapper.save(account);
		
		Account account2 = mapper.read(Account.class, 1);
		System.out.println(account2);
	}
}
