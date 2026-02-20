package com.aerospike.mapper.reactive;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

public class ReactiveAeroMapperObjectTransformTest extends ReactiveAeroMapperBaseTest {

    @AerospikeRecord()
    public static class Transaction {
        public String name;
        public int value;
        @AerospikeKey
        public long time;

        public Transaction() {
        }

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

        @AerospikeEmbed(type = AerospikeEmbed.EmbedType.MAP, elementType = AerospikeEmbed.EmbedType.LIST)
        public List<Transaction> txns;

        @AerospikeEmbed(type = AerospikeEmbed.EmbedType.LIST, elementType = AerospikeEmbed.EmbedType.LIST)
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

        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();
        reactiveMapper.save(account).subscribeOn(Schedulers.parallel()).block();

        Account account2 = reactiveMapper.read(Account.class, 1).subscribeOn(Schedulers.parallel()).block();
        System.out.println(account2);
    }
}
