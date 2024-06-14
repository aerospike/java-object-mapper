package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.aerospike.client.Record;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.mapper.tools.configuration.ClassConfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


public class UsePKColumnInsteadOfBinForKeyTest extends AeroMapperBaseTest {
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
        AeroMapper mapper = new AeroMapper.Builder(client).build();
        A a = new A(1, "test");
        mapper.save(a);
        
        A readA = mapper.read(A.class,1);
        assertTrue(readA.key1 == 1);
        
        Record rawObject = client.get(null, mapper.getRecordKey(a));
        assertFalse(rawObject.bins.containsKey("key1"));
    }
    
    @Test
    public void runTestViaYaml() throws Exception {
        String yaml = "---\n" +
                "classes:\n" +
                " - class: com.aerospike.mapper.UsePKColumnInsteadOfBinForKeyTest$B\n" +
                "   namespace: test\n" + 
                "   set: testSet\n" + 
                "   sendKey: true\n" + 
                "   key:\n" +
                "     field: keyStr\n" + 
                "     storeInPkOnly: true\n";
        AeroMapper mapper = new AeroMapper.Builder(client).withConfiguration(yaml).build();
        B b = new B("key1", "test1");
        mapper.save(b);
        
        B readB = mapper.read(B.class,"key1");
        assertTrue("key1".equals(readB.keyStr));
        
        Record rawObject = client.get(null, mapper.getRecordKey(b));
        assertFalse(rawObject.bins.containsKey("keyStr"));
    }

    @Test
    public void runTestViaConfig() throws Exception {
        ClassConfig classBConfig = new ClassConfig.Builder(B.class)
                .withNamespace("test")
                .withSet("testSet")
                .withSendKey(true)
                .withKeyFieldAndStorePkOnly("keyStr", true).build();
        AeroMapper mapper = new AeroMapper.Builder(client).withClassConfigurations(classBConfig).build();
        B b = new B("key2", "test2");
        mapper.save(b);
        
        B readB = mapper.read(B.class,"key2");
        assertTrue("key2".equals(readB.keyStr));
        
        Record rawObject = client.get(null, mapper.getRecordKey(b));
        assertFalse(rawObject.bins.containsKey("keyStr"));
    }
}
