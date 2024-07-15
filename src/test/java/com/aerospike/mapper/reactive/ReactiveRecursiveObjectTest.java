package com.aerospike.mapper.reactive;

import com.aerospike.mapper.RecursiveObjectTest;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ReactiveRecursiveObjectTest extends ReactiveAeroMapperBaseTest {

    @Test
    public void runTest() {
        RecursiveObjectTest.A a1 = new RecursiveObjectTest.A("a", 10, 1);
        a1.a = a1;

        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();
        reactiveMapper.save(a1).block();

        RecursiveObjectTest.A a2 = reactiveMapper.read(RecursiveObjectTest.A.class, a1.id).block();
        assertNotNull(a2);
        assertEquals(a1.age, a2.age);
        assertEquals(a1.name, a2.name);
        assertEquals(a1.id, a2.id);
    }

    @Test
    public void runMultipleObjectTest() {
        RecursiveObjectTest.A a1 = new RecursiveObjectTest.A("a", 10, 1);
        a1.a = a1;
        RecursiveObjectTest.B b = new RecursiveObjectTest.B();
        b.id = 10;
        b.a = a1;

        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();
        reactiveMapper.save(a1).block();
        reactiveMapper.save(b).block();

        RecursiveObjectTest.B b2 = reactiveMapper.read(RecursiveObjectTest.B.class, b.id).block();
        assertNotNull(b2);
        assertEquals(b.id, b2.id);
        assertEquals(b.a.age, b2.a.age);
        assertEquals(b.a.name, b2.a.name);
        assertEquals(b.a.id, b2.a.id);
    }

    @Test
    public void runTest2() {
        RecursiveObjectTest.A a1 = new RecursiveObjectTest.A("a", 10, 1);
        a1.a = new RecursiveObjectTest.A("a2", 11, 11);

        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();
        reactiveMapper.save(a1, a1.a).blockLast();

        RecursiveObjectTest.A a2 = reactiveMapper.read(RecursiveObjectTest.A.class, a1.id).block();
        assertNotNull(a2);
        assertEquals(a1.age, a2.age);
        assertEquals(a1.name, a2.name);
        assertEquals(a1.id, a2.id);
        assertEquals(a1.a.id, a2.a.id);
    }
}
