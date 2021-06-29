package com.aerospike.mapper.reactive;

import com.aerospike.client.AerospikeException;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeReference;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ReactiveCompetingAnnotationsTest extends ReactiveAeroMapperBaseTest {

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
        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();

        A a = new A();
        a.a = 2;
        a.b = "hello";

        CompetingReferenceAndEmbedAnnotations b = new CompetingReferenceAndEmbedAnnotations();
        b.key = 1;
        b.a = a;

        try {
            reactiveMapper.save(b).subscribeOn(Schedulers.parallel()).block();
            fail();
        }
        catch (AerospikeException ae) {
            assertTrue(true);
        }
    }

    @Test
    public void testCompetingListReferenceAndEmbedAnnotations() {
        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();

        CompetingListReferenceAndEmbedAnnotations b = new CompetingListReferenceAndEmbedAnnotations();
        b.key = 1;

        try {
            reactiveMapper.save(b).subscribeOn(Schedulers.parallel()).block();
            fail();
        }
        catch (AerospikeException ae) {
            assertTrue(true);
        }
    }

    @Test
    public void testMapCompetingReferenceAndEmbedAnnotations() {
        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();

        CompetingMapReferenceAndEmbedAnnotations b = new CompetingMapReferenceAndEmbedAnnotations();
        b.key = 1;
        try {
            reactiveMapper.save(b).subscribeOn(Schedulers.parallel()).block();
            fail();
        }
        catch (AerospikeException ae) {
            assertTrue(true);
        }
    }

    @Test
    public void testCompetingArrayReferenceAndEmbedAnnotations() {
        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();

        CompetingArrayReferenceAndEmbedAnnotations b = new CompetingArrayReferenceAndEmbedAnnotations();
        b.key = 1;

        try {
            reactiveMapper.save(b).subscribeOn(Schedulers.parallel()).block();
            fail();
        }
        catch (AerospikeException ae) {
            assertTrue(true);
        }
    }
}
