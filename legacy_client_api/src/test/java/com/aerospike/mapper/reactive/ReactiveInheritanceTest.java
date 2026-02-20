package com.aerospike.mapper.reactive;

import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;

public class ReactiveInheritanceTest extends ReactiveAeroMapperBaseTest {
    @AerospikeRecord(namespace = "test", set = "A")
    public static class A {
        @AerospikeKey
        public int id;
        public String name;
        @AerospikeEmbed
        public B topB;
        @AerospikeEmbed
        public B cAsAb;
        @AerospikeEmbed
        public B dAsAb;
    }

    @AerospikeRecord(namespace = "test", set = "B")
    public static class B {
        @AerospikeKey
        public int id;
        public String name;
    }

    // Inherit namespace and set from B
    @AerospikeRecord
    public static class C extends B {
        public String otherName;
    }

    // Map into a separate table
    @AerospikeRecord(namespace = "test", set = "D")
    public static class D extends B {
        public int age;
    }

    @Test
    public void run() {
        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();
        A a = new A();
        a.id = 1;
        a.name = "test";

        B b = new B();
        b.id = 2;
        b.name = "b";

        C c = new C();
        c.id = 3;
        c.name = "c";
        c.otherName = "masquerading as a B";

        D d = new D();
        d.id = 4;
        d.name = "d";
        d.age = 312;
        a.topB = b;
        a.cAsAb = c;
        a.dAsAb = d;

        reactiveMapper.save(a, b, c, d).subscribeOn(Schedulers.parallel()).collectList().block();
        A a2 = reactiveMapper.read(A.class, a.id).subscribeOn(Schedulers.parallel()).block();
        compare(a, a2);
    }
}
