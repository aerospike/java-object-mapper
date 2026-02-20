package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aerospike.client.AerospikeException;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeReference;
import com.aerospike.mapper.tools.AeroMapper;

public class CompetingAnnotationsTest extends AeroMapperBaseTest {

    @AerospikeRecord(namespace = "test", set = "A")
    public static class A {
        @AerospikeKey
        int a;
        String b;
    }

    @AerospikeRecord(namespace = "test", set = "B")
    public static class CompetingReferenceAndEmbedAnnotations {
        @AerospikeKey
        int key;
        @AerospikeEmbed
        @AerospikeReference
        A a;
    }

    @AerospikeRecord(namespace = "test", set = "B")
    public static class CompetingListReferenceAndEmbedAnnotations {
        @AerospikeKey
        int key;
        @AerospikeEmbed
        @AerospikeReference
        List<A> a;
    }

    @AerospikeRecord(namespace = "test", set = "B")
    public static class CompetingMapReferenceAndEmbedAnnotations {
        @AerospikeKey
        int key;
        @AerospikeEmbed
        @AerospikeReference
        Map<Integer, A> a;
    }

    @AerospikeRecord(namespace = "test", set = "B")
    public static class CompetingArrayReferenceAndEmbedAnnotations {
        @AerospikeKey
        int key;
        @AerospikeEmbed
        @AerospikeReference
        A[] a;
    }

    @Test
    public void testCompetingReferenceAndEmbedAnnotations() {
        AeroMapper mapper = new AeroMapper.Builder(client).build();

        A a = new A();
        a.a = 2;
        a.b = "hello";

        CompetingReferenceAndEmbedAnnotations b = new CompetingReferenceAndEmbedAnnotations();
        b.key = 1;
        b.a = a;

        try {
            mapper.save(b);
            fail();
        } catch (AerospikeException ae) {
            assertTrue(true);
        }
    }

    @Test
    public void testCompetingListReferenceAndEmbedAnnotations() {
        AeroMapper mapper = new AeroMapper.Builder(client).build();

        CompetingListReferenceAndEmbedAnnotations b = new CompetingListReferenceAndEmbedAnnotations();
        b.key = 1;

        try {
            mapper.save(b);
            fail();
        } catch (AerospikeException ae) {
            assertTrue(true);
        }
    }

    @Test
    public void testMapCompetingReferenceAndEmbedAnnotations() {
        AeroMapper mapper = new AeroMapper.Builder(client).build();

        CompetingMapReferenceAndEmbedAnnotations b = new CompetingMapReferenceAndEmbedAnnotations();
        b.key = 1;
        try {
            mapper.save(b);
            fail();
        } catch (AerospikeException ae) {
            assertTrue(true);
        }
    }

    @Test
    public void testCompetingArrayReferenceAndEmbedAnnotations() {
        AeroMapper mapper = new AeroMapper.Builder(client).build();

        CompetingArrayReferenceAndEmbedAnnotations b = new CompetingArrayReferenceAndEmbedAnnotations();
        b.key = 1;

        try {
            mapper.save(b);
            fail();
        } catch (AerospikeException ae) {
            assertTrue(true);
        }
    }
}
