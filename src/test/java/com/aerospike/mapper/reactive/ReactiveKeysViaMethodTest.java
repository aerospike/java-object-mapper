package com.aerospike.mapper.reactive;

import com.aerospike.client.Key;
import com.aerospike.client.Value;
import com.aerospike.mapper.annotations.*;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReactiveKeysViaMethodTest extends ReactiveAeroMapperBaseTest {

    private static final long epochTime = new GregorianCalendar(2020, Calendar.JANUARY, 1, 0, 0, 0).getTimeInMillis();

    @AerospikeRecord(namespace = "test", set = "testSet", sendKey = true)
    public static class BasicClass {
        private int id;
        private String name;

        public BasicClass(int id, String name) {
            super();
            this.id = id;
            this.name = name;
        }

        public BasicClass() {
        }

        @AerospikeKey
        public String getKey() {
            return name + ":" + id;
        }

        @AerospikeKey(setter = true)
        public void setKey(String key, Key recordKey) {
            int index = key.indexOf(":");
            this.name = key.substring(0, index);
            this.id = Integer.valueOf(key.substring(index+1));
        }
    }

    @AerospikeRecord(namespace = "test", set = "testSet", sendKey = true)
    public static class Container {
        @AerospikeKey
        private int id;
        @AerospikeEmbed(elementType = AerospikeEmbed.EmbedType.LIST, type= AerospikeEmbed.EmbedType.MAP)
        private List<BasicClass> basicList;
    }

    @Test
    public void basicTest() {
        BasicClass basicClass = new BasicClass(7, "tim");
        Container container = new Container();
        container.id = 1;
        container.basicList = new ArrayList<>();
        container.basicList.add(basicClass);

        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();
        reactiveMapper.save(container).subscribeOn(Schedulers.parallel()).block();
        Container container2 = reactiveMapper.read(Container.class, 1).subscribeOn(Schedulers.parallel()).block();
        assert container2 != null;
        assertEquals(7, container2.basicList.get(0).id);
        assertEquals("tim", container2.basicList.get(0).name);
    }

    // Since this record is only ever embedded, we do not need to specify a namespace or set (it would
    // not hurt anything if we did so though)
    @AerospikeRecord
    public static class Transaction {
        @AerospikeExclude
        private Date date;
        private long amount;
        private String description;

        public Transaction() {
        }

        public Transaction(Date date, long amount, String description) {
            super();
            this.date = date;
            this.amount = amount;
            this.description = description;
        }

        @AerospikeKey
        public long getTimeSinceDayStart() {
            return (date.getTime() - epochTime) % TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS);
        }

        public long getDaysSinceEpoch() {
            return (date.getTime() - epochTime) / TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS);
        }

        @AerospikeKey(setter = true)
        public void setDate(long millisSinceDayStart, Value key) {
            String keyAsString = (String) key.getObject();
            String daysAsString = keyAsString.substring(keyAsString.lastIndexOf("-")+1);
            long epochDays = Long.parseLong(daysAsString);
            long millis = epochTime + epochDays * TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS) + millisSinceDayStart;
            this.date = new Date(millis);
        }
    }

    @AerospikeRecord(namespace = "test", set = "account", sendKey = true, mapAll = false)
    public static class AccountTxnContainer {
        private int daySinceEpoch;
        private String accountName;

        @AerospikeBin
        @AerospikeEmbed(elementType = AerospikeEmbed.EmbedType.LIST, type= AerospikeEmbed.EmbedType.MAP)
        private final List<Transaction> txnList;

        public AccountTxnContainer() {
            this.txnList = new ArrayList<>();
        }

        @AerospikeKey
        public String getKey() {
            return accountName + "-" +daySinceEpoch;
        }
    }

    @Test
    public void advancedTest() {
        AccountTxnContainer account = new AccountTxnContainer();
        account.daySinceEpoch = 1;
        account.accountName = "Tim";

        account.txnList.add(new Transaction(new Date(epochTime + account.daySinceEpoch * TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS) +10000), 1111, "Test Transaction 1"));
        account.txnList.add(new Transaction(new Date(epochTime + account.daySinceEpoch * TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS) +75000), 2222, "Test Transaction 2"));
        account.txnList.add(new Transaction(new Date(epochTime + account.daySinceEpoch * TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS) +13 * 60* 60* 1000), 3333, "Test Transaction"));

        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();
        reactiveMapper.save(account).subscribeOn(Schedulers.parallel()).block();

        AccountTxnContainer result = reactiveMapper.read(AccountTxnContainer.class, "Tim-1").subscribeOn(Schedulers.parallel()).block();
        assert result != null;
        assertEquals(account.txnList.size(), result.txnList.size());
        for (int i = 0; i < account.txnList.size(); i++) {
            assertEquals(account.txnList.get(i).amount, result.txnList.get(i).amount);
            assertEquals(account.txnList.get(i).date, result.txnList.get(i).date);
            assertEquals(account.txnList.get(i).description, result.txnList.get(i).description);
        }
    }
}
