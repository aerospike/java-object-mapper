package com.aerospike.mapper.reactive;

import com.aerospike.client.Key;
import com.aerospike.client.Value;
import com.aerospike.mapper.annotations.*;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReactiveMultipleParameterSetterTest extends ReactiveAeroMapperBaseTest {

    @AerospikeRecord(namespace = "test", set = "A", mapAll = false)
    public static class A {
        private String key;
        private String value1;
        private long value2;

        @AerospikeKey
        public String getKey() {
            return key;
        }

        @AerospikeKey(setter = true)
        public void setKey(String key) {
            this.key = key;
        }

        @AerospikeGetter(name = "v1")
        public String getValue1() {
            return value1;
        }

        @AerospikeSetter(name = "v1")
        public void setValue1(String value1, Value owningKey) {
            assertEquals("B-1", owningKey.getObject());
            this.value1 = value1;
        }

        @AerospikeGetter(name = "v2")
        public long getValue2() {
            return value2;
        }

        @AerospikeSetter(name = "v2")
        public void setValue2(long value2, Key key) {
            assertEquals("test", key.namespace);
            assertEquals("B", key.setName);
            assertEquals("B-1", key.userKey.getObject());
            this.value2 = value2;
        }
    }

    @AerospikeRecord(namespace = "test", set = "B")
    public static class B {
        @AerospikeKey
        private String key;
        @AerospikeEmbed
        private A a;
    }

    @Test
    public void test() {
        A a = new A();
        a.key = "A-1";
        a.value1 = "value1";
        a.value2 = 1000;

        B b = new B();
        b.key = "B-1";
        b.a = a;

        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();
        reactiveMapper.save(b).subscribeOn(Schedulers.parallel()).block();
        B b2 = reactiveMapper.read(B.class, b.key).subscribeOn(Schedulers.parallel()).block();

        compare(b, b2);
    }
}
