package com.aerospike.mapper.reactive;

import com.aerospike.client.query.KeyRecord;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import com.aerospike.mapper.tools.configuration.ClassConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;

import static org.junit.jupiter.api.Assertions.*;

public class ReactiveUsePKColumnInsteadOfBinForKeyTest extends ReactiveAeroMapperBaseTest {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @AerospikeRecord(namespace = "test", set = "testSet", sendKey = true)
    public static class A {
        @AerospikeKey(storeInPkOnly = true)
        private long key1;
        private String value;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class B {
        private String keyStr;
        private String value;
    }

    @Test
    public void runTest() {
        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();
        A a = new A(1, "test");
        reactiveMapper.save(a).subscribeOn(Schedulers.parallel()).block();

        A readA = reactiveMapper.read(A.class, 1).subscribeOn(Schedulers.parallel()).block();
        assertNotNull(readA);
        assertEquals(1, readA.key1);

        KeyRecord rawObject = reactorClient.get(null, reactiveMapper.getRecordKey(a).block()).block();
        assertNotNull(rawObject);
        assertFalse(rawObject.record.bins.containsKey("key1"));
    }

    @Test
    public void runTestViaYaml() throws Exception {
        String yaml = "---\n" +
                "classes:\n" +
                " - class: com.aerospike.mapper.reactive.ReactiveUsePKColumnInsteadOfBinForKeyTest$B\n" +
                "   namespace: test\n" +
                "   set: testSet\n" +
                "   sendKey: true\n" +
                "   key:\n" +
                "     field: keyStr\n" +
                "     storeInPkOnly: true\n";
        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).withConfiguration(yaml).build();
        B b = new B("key1", "test1");
        reactiveMapper.save(b).subscribeOn(Schedulers.parallel()).block();

        B readB = reactiveMapper.read(B.class, "key1").subscribeOn(Schedulers.parallel()).block();
        assertNotNull(readB);
        assertEquals("key1", readB.keyStr);

        KeyRecord rawObject = reactorClient.get(null, reactiveMapper.getRecordKey(b).block()).block();
        assertNotNull(rawObject);
        assertFalse(rawObject.record.bins.containsKey("keyStr"));
    }

    @Test
    public void runTestViaConfig() {
        ClassConfig classBConfig = new ClassConfig.Builder(B.class)
                .withNamespace("test")
                .withSet("testSet")
                .withSendKey(true)
                .withKeyFieldAndStorePkOnly("keyStr", true).build();
        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).withClassConfigurations(classBConfig).build();
        B b = new B("key2", "test2");
        reactiveMapper.save(b).subscribeOn(Schedulers.parallel()).block();

        B readB = reactiveMapper.read(B.class, "key2").subscribeOn(Schedulers.parallel()).block();
        assertNotNull(readB);
        assertEquals("key2", readB.keyStr);

        KeyRecord rawObject = reactorClient.get(null, reactiveMapper.getRecordKey(b).block()).block();
        assertNotNull(rawObject);
        assertFalse(rawObject.record.bins.containsKey("keyStr"));
    }
}
