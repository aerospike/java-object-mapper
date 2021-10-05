package com.aerospike.mapper.reactive;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReactiveBatchLoadTest extends ReactiveAeroMapperBaseTest {

    @AerospikeRecord(namespace = "test", set = "B")
    public static class B {
        @AerospikeKey
        public int id;
        public String name;

        public B(@ParamFrom("id") int id, @ParamFrom("name") String name) {
            this.id = id;
            this.name = name;
        }
    }

    @AerospikeRecord(namespace = "test", set = "A")
    public static class A {
        @AerospikeKey
        public int id;
        public String name;
        public List<B> data;

        public A(int id, String name) {
            this.id = id;
            this.name = name;
            data = new ArrayList<>();
        }

        public void setBList(List<B> bees) {
            data = bees;
        }

        public A() {
        }
    }

    @Test
    public void testBatchLoad() {
        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();

        B[] bees = new B[100];
        for (int i = 0; i < 100; i++) {
            bees[i] = new B(i, "B-" + i);
        }

        A[] as = new A[10];
        for (int i = 0; i < 10; i++) {
            as[i] = new A(100 + i, "A-" + i);
            as[i].setBList(Arrays.asList(
                    Arrays.copyOfRange(bees, i * 10, (i + 1) * 10)));
        }

        reactiveMapper.save((Object[]) bees).subscribeOn(Schedulers.parallel()).collectList().block();
        reactiveMapper.save((Object[]) as).subscribeOn(Schedulers.parallel()).collectList().block();

        System.out.println("--- Reading single object (bees[1]) ---");
        B resultB = reactiveMapper.read(B.class, bees[1].id).subscribeOn(Schedulers.parallel()).block();
        compare(bees[1], resultB);

        System.out.println("--- Reading single object (a[1]) ---");
        A resultA = reactiveMapper.read(A.class, as[1].id).subscribeOn(Schedulers.parallel()).block();
        compare(as[1], resultA);

        System.out.println("--- Reading batch object with 6 keys ---");
        A[] results = new A[6];
        List<A> resultsObjects = reactiveMapper.read(A.class, as[4].id, as[7].id, as[5].id, as[0].id, as[1].id, 3000)
                .subscribeOn(Schedulers.parallel()).collectList().block();

        assert resultsObjects != null;
        results = resultsObjects.toArray(results);
        compare(results[0], as[4]);
        compare(results[1], as[7]);
        compare(results[2], as[5]);
        compare(results[3], as[0]);
        compare(results[4], as[1]);
        compare(results[5], null);
    }
}
