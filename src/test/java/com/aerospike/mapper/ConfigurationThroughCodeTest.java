package com.aerospike.mapper;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;

import lombok.Data;

public class ConfigurationThroughCodeTest extends AeroMapperBaseTest {

    @Data
    @AerospikeRecord(namespace = "test")
    public static class A {
        @AerospikeKey
        private long id;
        @AerospikeEmbed(type = AerospikeEmbed.EmbedType.LIST)
        private List<B> b;
        private String aData;
    }
    
    @Data
    public static class B {
        private C c;
        private String bData;
    }
    
    @Data
    public static class C {
        private String id;
        private String cData;
    }
    
    private A setupA() {
        A a = new A();
        a.id = 1;
        a.aData = "aData1";
        
        C c1 = new C();
        c1.id = "c1";
        c1.cData = "cData-1";
        C c2 = new C();
        c2.id = "c2";
        c2.cData = "cData-2";
        
        B b1 = new B();
        b1.bData = "bData-b1";
        b1.c = c1;
        
        B b2 = new B();
        b2.bData = "bData-b2";
        b2.c = c2;
        
        a.b = new ArrayList<>();
        a.b.add(b1);
        a.b.add(b2);
        return a;
    }
    
    @Test
    public void testNoConfiguration() {
        AeroMapper mapper = new AeroMapper.Builder(client).build();
        A a = setupA();
        mapper.delete(a);
        
        try {
            mapper.save(a);
            
            mapper.read(A.class, 1);
            Assertions.fail("Expected NotSerializableException to be thrown");
        }
        catch (AerospikeException.Serialize expected) {
        }
    }
    
    @Test
    public void testWithInvalidConfiguarion() {
        try {
            new AeroMapper.Builder(new AerospikeClient("172.17.0.2", 3000))
                    .withConfigurationForClass(C.class)
                        .withKeyField("id1")
                    .end()
                    .withConfigurationForClass(B.class) 
                        .withFieldNamed("c").beingEmbeddedAs(AerospikeEmbed.EmbedType.MAP)
                    .end()
                .build();
            Assertions.fail("Excpected invalid configuration to throw an error");
        }
        catch (AerospikeException ignore) {
        }
    }
    
    @Test
    public void testWithConfiguration() {
        AeroMapper mapper = new AeroMapper.Builder(client)
                .withConfigurationForClass(C.class)
                    .withKeyField("id")
                .end()
                .withConfigurationForClass(B.class) 
                    .withFieldNamed("c").beingEmbeddedAs(AerospikeEmbed.EmbedType.MAP)
                .end()
            .build();
        A a = setupA();
        mapper.delete(a);
        
        mapper.save(a);
            
        A readA = mapper.read(A.class, 1);
        Assertions.assertEquals(a, readA);
    }
}
