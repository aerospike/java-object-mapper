package com.aerospike.mapper.reactive;

import com.aerospike.client.exp.Exp;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReactiveScanTest extends ReactiveAeroMapperBaseTest {
    @AerospikeRecord(namespace = "test", set = "testScan")
    public static class Person {
        @AerospikeKey
        private final int id;
        private final String name;
        private final int age;

        public Person(@ParamFrom("id") int id, @ParamFrom("name") String name, @ParamFrom("age") int age) {
            super();
            this.id = id;
            this.name = name;
            this.age = age;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }

    private ReactiveAeroMapper populate() {
        reactorClient.getAerospikeClient().truncate(null, "test", "testScan", null);
        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();
        reactiveMapper.save(new Person(1, "Tim", 312),
                new Person(2, "Bob", 44),
                new Person(3, "Sue", 56),
                new Person(4, "Rob", 23),
                new Person(5, "Jim", 32),
                new Person(6, "Bob", 78)).subscribeOn(Schedulers.parallel()).collectList().block();
        return reactiveMapper;
    }

    @Test
    public void scanTest() {
        ReactiveAeroMapper reactiveMapper = populate();
        AtomicInteger counter = new AtomicInteger(0);
        reactiveMapper.scan(Person.class, (a) -> {
            counter.incrementAndGet();
            return true;
        }).subscribeOn(Schedulers.parallel()).collectList().block();
        assertEquals(6, counter.get());
    }

    @Test
    public void scanTestWithFilter() {
        ReactiveAeroMapper reactiveMapper = populate();
        AtomicInteger counter = new AtomicInteger(0);
        ScanPolicy scanPolicy = new ScanPolicy(reactiveMapper.getScanPolicy(Person.class));
        scanPolicy.filterExp = Exp.build(Exp.eq(Exp.stringBin("name"), Exp.val("Bob")));
        reactiveMapper.scan(scanPolicy, Person.class, (a) -> {
            counter.incrementAndGet();
            return true;
        }).subscribeOn(Schedulers.parallel()).collectList().block();
        assertEquals(2, counter.get());
    }

    @Test
    public void scanTestWithAbort() {
        ReactiveAeroMapper reactiveMapper = populate();
        ScanPolicy scanPolicy = new ScanPolicy(reactiveMapper.getScanPolicy(Person.class));
        scanPolicy.maxConcurrentNodes = 1;
        AtomicInteger counter = new AtomicInteger(0);
        reactiveMapper.scan(scanPolicy, Person.class, (a) -> {
            counter.incrementAndGet();
            return false;
        }).subscribeOn(Schedulers.parallel()).collectList().block();
        assertEquals(1, counter.get());
    }
}
