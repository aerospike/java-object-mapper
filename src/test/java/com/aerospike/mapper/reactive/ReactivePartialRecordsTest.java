package com.aerospike.mapper.reactive;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReactivePartialRecordsTest extends ReactiveAeroMapperBaseTest {
    @AerospikeRecord(namespace = "test", set="testSet")
    public static class DataClass{
        @AerospikeKey
        int a;
        int b;
        int c;
        int d;
        int e;
    }

    @Test
    public void testPartialSave() {
        WritePolicy writePolicy = new WritePolicy(reactorClient.getWritePolicyDefault());
        writePolicy.totalTimeout = 2000;
        writePolicy.socketTimeout = 100;
        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient)
                .withWritePolicy(writePolicy).forClasses(DataClass.class)
                .build();

        DataClass dataClass = new DataClass();
        dataClass.a = 1;
        dataClass.b = 2;
        dataClass.c = 3;
        dataClass.d = 4;
        dataClass.e = 5;

        // Do a full save
        reactiveMapper.save(dataClass).subscribeOn(Schedulers.parallel()).block();

        Key key = new Key("test", "testSet", 1);
        Record record = Objects.requireNonNull(reactorClient.get(null, key).subscribeOn(Schedulers.parallel()).block()).record;
        assertEquals(5, record.bins.size());
        assertEquals(3, record.getInt("c"));

        // Perform a partial save, which should replace the record with just these bins
        dataClass.c = 9;
        dataClass.e = 11;
        reactiveMapper.save(dataClass, "a", "c", "e").subscribeOn(Schedulers.parallel()).block();

        record = Objects.requireNonNull(reactorClient.get(null, key).subscribeOn(Schedulers.parallel()).block()).record;
        assertEquals(3, record.bins.size());
        assertEquals(1, record.getInt("a"));
        assertEquals(9, record.getInt("c"));
        assertEquals(11, record.getInt("e"));

        // Now do an update of bin c and add bin d
        dataClass.c = 99;
        reactiveMapper.update(dataClass, "c", "d").subscribeOn(Schedulers.parallel()).block();

        record = Objects.requireNonNull(reactorClient.get(null, key).subscribeOn(Schedulers.parallel()).block()).record;
        assertEquals(4, record.bins.size());
        assertEquals(1, record.getInt("a"));
        assertEquals(99, record.getInt("c"));
        assertEquals(4, record.getInt("d"));
        assertEquals(11, record.getInt("e"));
    }
}
