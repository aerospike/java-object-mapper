package com.aerospike.mapper;

import org.junit.jupiter.api.Test;

import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.tools.AeroMapper;

public class ObjectReferencesTest extends AeroMapperBaseTest {
    @AerospikeRecord(namespace = "test", set = "parent")
    public static class Parent {
        @AerospikeKey
        private int id;
        @AerospikeEmbed(type = EmbedType.LIST)
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

        AeroMapper mapper = new AeroMapper.Builder(client).build();
        mapper.save(parent);
        mapper.save(child);
    }
}
