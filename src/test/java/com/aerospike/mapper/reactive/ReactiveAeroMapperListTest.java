package com.aerospike.mapper.reactive;

import com.aerospike.mapper.annotations.*;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReactiveAeroMapperListTest extends ReactiveAeroMapperBaseTest {

    private ReactiveAeroMapper reactiveMapper;

    @BeforeEach
    public void setup() {
        reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();
        reactorClient.getAerospikeClient().truncate(null, NAMESPACE, "testSet", null);
    }

    @AerospikeRecord(namespace = NAMESPACE, set = "testSet")
    public static class TestV1 {
        public int a;
        @AerospikeOrdinal()
        public int b;
        public int c;
        public int d;
    }

    // Version 2 of the test record removes c from the database and adds e, leaving a, b, d, e persisted
    @AerospikeRecord(namespace = NAMESPACE, set = "testSet", version = 2)
    public static class TestV2 {
        public int a;
        @AerospikeOrdinal()
        public int b;
        @AerospikeVersion(max = 1)
        public int c;
        public int d;
        @AerospikeVersion(min = 2)
        public int e;
    }

    // Version 3 of the test record removes a, e, from the database and adds f, g, leaving b, d, g, f persisted
    @AerospikeRecord(namespace = NAMESPACE, set = "testSet", version = 3)
    public static class TestV3 {
        @AerospikeVersion(max = 2)
        public int a;
        @AerospikeOrdinal()
        public int b;
        @AerospikeVersion(max = 1)
        public int c;
        public int d;
        @AerospikeVersion(min = 2, max = 2)
        public int e;
        @AerospikeVersion(min = 3)
        public int f;
        @AerospikeVersion(min = 3)
        public int g;
    }

    @AerospikeRecord(namespace = NAMESPACE, set = "testSet")
    public static class TestV1Container {
        @AerospikeKey
        public int id;
        @AerospikeEmbed(type = AerospikeEmbed.EmbedType.LIST)
        public TestV1 value;
    }

    @AerospikeRecord(namespace = NAMESPACE, set = "testSet")
    public static class TestV2Container {
        @AerospikeKey
        public int id;
        @AerospikeEmbed(type = AerospikeEmbed.EmbedType.LIST)
        public TestV2 value;
    }

    @AerospikeRecord(namespace = NAMESPACE, set = "testSet")
    public static class TestV3Container {
        @AerospikeKey
        public int id;
        @AerospikeEmbed(type = AerospikeEmbed.EmbedType.LIST)
        public TestV3 value;
    }

    @Test
    public void testSave() {
        TestV1Container container = new TestV1Container();
        container.id = 1;
        container.value = new TestV1();
        container.value.a = 1;
        container.value.b = 2;
        container.value.c = 3;
        container.value.d = 4;

        reactiveMapper.save(container).subscribeOn(Schedulers.parallel()).block();

        container = reactiveMapper.read(TestV1Container.class, 1).subscribeOn(Schedulers.parallel()).block();
        assert container != null;
        assertEquals(1, container.value.a);
        assertEquals(2, container.value.b);
        assertEquals(3, container.value.c);
        assertEquals(4, container.value.d);
    }

    @Test
    public void testSaveV1LoadV2() {
        TestV1Container container = new TestV1Container();
        container.id = 1;
        container.value = new TestV1();
        container.value.a = 1;
        container.value.b = 2;
        container.value.c = 3;
        container.value.d = 4;

        reactiveMapper.save(container).subscribeOn(Schedulers.parallel()).block();

        TestV2Container container2 = reactiveMapper.read(TestV2Container.class, 1).subscribeOn(Schedulers.parallel()).block();
        assert container2 != null;
        assertEquals(1, container2.value.a);
        assertEquals(2, container2.value.b);
        assertEquals(0, container2.value.c);
        assertEquals(4, container2.value.d);
        assertEquals(0, container2.value.e);
    }

    @Test
    public void testSaveV2LoadV2() {
        TestV2Container container = new TestV2Container();
        container.id = 1;
        container.value = new TestV2();
        container.value.a = 1;
        container.value.b = 2;
        container.value.d = 4;
        container.value.e = 5;

        reactiveMapper.save(container).subscribeOn(Schedulers.parallel()).block();

        TestV2Container container2 = reactiveMapper.read(TestV2Container.class, 1).subscribeOn(Schedulers.parallel()).block();
        assert container2 != null;
        assertEquals(1, container2.value.a);
        assertEquals(2, container2.value.b);
        assertEquals(0, container2.value.c);
        assertEquals(4, container2.value.d);
        assertEquals(5, container2.value.e);
    }

    @Test
    public void testSaveV1LoadV3() {
        TestV1Container container = new TestV1Container();
        container.id = 1;
        container.value = new TestV1();
        container.value.a = 1;
        container.value.b = 2;
        container.value.c = 3;
        container.value.d = 4;

        reactiveMapper.save(container).subscribeOn(Schedulers.parallel()).block();

        TestV3Container container2 = reactiveMapper.read(TestV3Container.class, 1).subscribeOn(Schedulers.parallel()).block();
        assert container2 != null;
        assertEquals(0, container2.value.a);
        assertEquals(2, container2.value.b);
        assertEquals(0, container2.value.c);
        assertEquals(4, container2.value.d);
        assertEquals(0, container2.value.e);
        assertEquals(0, container2.value.f);
        assertEquals(0, container2.value.g);
    }

    @Test
    public void testSaveV2LoadV3() {
        TestV2Container container = new TestV2Container();
        container.id = 1;
        container.value = new TestV2();
        container.value.a = 1;
        container.value.b = 2;
        container.value.d = 3;
        container.value.e = 4;

        reactiveMapper.save(container).subscribeOn(Schedulers.parallel()).block();

        TestV3Container container2 = reactiveMapper.read(TestV3Container.class, 1).subscribeOn(Schedulers.parallel()).block();
        assert container2 != null;
        assertEquals(0, container2.value.a);
        assertEquals(2, container2.value.b);
        assertEquals(0, container2.value.c);
        assertEquals(3, container2.value.d);
        assertEquals(0, container2.value.e);
        assertEquals(0, container2.value.f);
        assertEquals(0, container2.value.g);
    }

    @Test
    public void testSaveV3LoadV3() {
        TestV3Container container = new TestV3Container();
        container.id = 1;
        container.value = new TestV3();
        container.value.b = 100;
        container.value.d = 2;
        container.value.f = 3;
        container.value.g = 4;

        reactiveMapper.save(container).subscribeOn(Schedulers.parallel()).block();

        TestV3Container container2 = reactiveMapper.read(TestV3Container.class, 1).subscribeOn(Schedulers.parallel()).block();
        assert container2 != null;
        assertEquals(0, container2.value.a);
        assertEquals(100, container2.value.b);
        assertEquals(0, container2.value.c);
        assertEquals(2, container2.value.d);
        assertEquals(0, container2.value.e);
        assertEquals(3, container2.value.f);
         assertEquals(4, container2.value.g);
    }

    @Test
    public void testConvenienceMethods() {
        TestV1 value = new TestV1();
        value.a = 1;
        value.b = 2;
        value.c = 3;
        value.d = 4;
        List<Object> list = reactiveMapper.getMappingConverter().convertToList(value);
        assertEquals(4, list.size());
        // Note that "b" is ordinal 1, so it should be first in the list, then a, c, d
        assertEquals(2, list.get(0));
        assertEquals(1, list.get(1));
        assertEquals(3, list.get(2));
        assertEquals(4, list.get(3));

        TestV1 value2 = reactiveMapper.getMappingConverter().convertToObject(TestV1.class, list);
        assertEquals(value.a, value2.a);
        assertEquals(value.b, value2.b);
        assertEquals(value.c, value2.c);
        assertEquals(value.d, value2.d);
    }
}
