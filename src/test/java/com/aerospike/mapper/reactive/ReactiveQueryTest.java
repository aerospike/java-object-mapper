package com.aerospike.mapper.reactive;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.IndexType;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReactiveQueryTest extends ReactiveAeroMapperBaseTest {
    @AerospikeRecord(namespace = "test", set = "testScan")
    public static class A {
        @AerospikeKey
        private final int id;
        private final String name;
        private final int age;

        public A(@ParamFrom("id") int id, @ParamFrom("name") String name, @ParamFrom("age") int age) {
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

        @Override
        public String toString() {
            return String.format("id:%d, name:%s, age:%d", id, name, age);
        }
    }

    private ReactiveAeroMapper populate() {
        reactorClient.getAerospikeClient().truncate(null, "test", "testScan", null);
        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();
        reactiveMapper.save(new A(1, "Tim", 312),
                new A(2, "Bob", 44),
                new A(3, "Sue", 56),
                new A(4, "Rob", 23),
                new A(5, "Jim", 32),
                new A(6, "Bob", 78),
                new A(7, "Fred", 23),
                new A(8, "Wilma", 11),
                new A(9, "Barney", 54),
                new A(10, "Steve", 72),
                new A(11, "Bam Bam", 19),
                new A(12, "Betty", 34),
                new A(13, "Del", 7),
                new A(14, "Khon", 98),
                new A(15, "Dave", 21),
                new A(16, "Mike", 32),
                new A(17, "Darren", 14),
                new A(18, "Lucy", 45),
                new A(19, "Gertrude", 36),
                new A(20, "Lucinda", 63)).subscribeOn(Schedulers.parallel()).collectList().block();

        try {
            reactorClient.getAerospikeClient().createIndex(null, "test", "testScan", "age_idx", "age", IndexType.NUMERIC).waitTillComplete();
        } catch (AerospikeException ae) {
            // swallow the exception
        }
        return reactiveMapper;
    }

    @Test
    public void queryTest() {
        ReactiveAeroMapper reactiveMapper = populate();
        List<A> results = reactiveMapper.query(A.class, Filter.range("age", 30, 54)).subscribeOn(Schedulers.parallel()).collectList().block();
        assert results != null;
        assertEquals(7, results.size());
    }
}
