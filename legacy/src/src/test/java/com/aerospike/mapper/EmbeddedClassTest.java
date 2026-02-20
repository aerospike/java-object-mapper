package com.aerospike.mapper;

import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EmbeddedClassTest extends AeroMapperBaseTest {

    @Test
    void testEmbed() {
        Embed2 embed2 = new Embed2(Collections.singletonList(new Embed3("s3", "s4")));
        Embed1 record = new Embed1(Collections.singletonList(embed2), new Embed3("s1", "s2"), "id");
        AeroMapper mapper = new AeroMapper.Builder(client).build();

        mapper.save(record);
        Embed1 read = mapper.read(Embed1.class, record.getId());
        assertEquals(record, read);
    }

    @Test
    void testDerived() {
        Derived derived = new Derived("str1", 1);
        AeroMapper mapper = new AeroMapper.Builder(client).build();

        mapper.save(derived);
        Derived read = mapper.read(Derived.class, derived.getStr());
        assertEquals(derived, read);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @AerospikeRecord(namespace = "test", set = "embed")
    private static class Embed1 {
        @AerospikeEmbed
        public List<Embed2> bList;
        @AerospikeEmbed
        private Embed3 embed3;
        @AerospikeKey
        private String id;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @AerospikeRecord // test backward
    private static class Embed2 {
        @AerospikeEmbed
        public List<Embed3> cList;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Embed3 {
        public String s1;
        public String s2;
    }

    @EqualsAndHashCode(callSuper = true)
    @Getter
    @NoArgsConstructor
    @AerospikeRecord(namespace = "test", set = "embed")
    private static class Derived extends Base {
        @AerospikeKey
        private String str;

        Derived(String str, int i1) {
            super(i1);
            this.str = str;
        }
    }

    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Base {
        private int i1;
    }
}
