package com.aerospike.mapper.reactive;

import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;

public class ReactiveObjectReferencesTest extends ReactiveAeroMapperBaseTest {

    @AerospikeRecord(namespace = "test", set = "parent")
    public static class Parent {
        @AerospikeKey
        private int id;
        @AerospikeEmbed(type = AerospikeEmbed.EmbedType.LIST)
        private Child child;
    }

    @AerospikeRecord(namespace = "test", set = "child")
    public static class Child {
        @AerospikeKey
        private String key;
        private int age;
        private String name;
    }

    @Test
    public void test() {
        Child child = new Child();
        child.key = "child1";
        child.age = 17;
        child.name = "bob";

        Parent parent = new Parent();
        parent.id = 1;
        parent.child = child;

        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();
        reactiveMapper.save(parent).subscribeOn(Schedulers.parallel()).block();
        reactiveMapper.save(child).subscribeOn(Schedulers.parallel()).block();
    }
}
