package com.aerospike.mapper;

import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultFieldValuesTest extends AeroMapperBaseTest {

    @AerospikeRecord(namespace = "test", set = "testSet")
    public static class DefaultFieldsClass {
        @AerospikeKey
        String key;
        Integer i;
        int i2;
        Long l;
        long l2;
        Short s;
        short s2;
        Float f;
        float f2;
        Double d;
        double d2;
        Character c;
        char c2;
        Byte b;
        byte b2;
        Boolean bool;
        boolean bool2;
    }

    @Test
    public void testPartialSave() {
        WritePolicy writePolicy = new WritePolicy(client.getWritePolicyDefault());
        writePolicy.totalTimeout = 2000;
        writePolicy.socketTimeout = 100;
        AeroMapper mapper = new AeroMapper.Builder(client)
                .withWritePolicy(writePolicy).forClasses(PartialRecordsTest.DataClass.class)
                .build();

        DefaultFieldsClass obj = new DefaultFieldsClass();
        obj.key = "dfc";
        mapper.save(obj);

        DefaultFieldsClass dfc = mapper.read(DefaultFieldsClass.class, "dfc");
        assertNull(dfc.i);
        assertEquals(0, dfc.i2);
        assertNull(dfc.l);
        assertEquals(0, dfc.l2);
        assertNull(dfc.s);
        assertEquals(0, dfc.s2);
        assertNull(dfc.f);
        assertEquals(0, dfc.f2);
        assertNull(dfc.d);
        assertEquals(0, dfc.d2);
        assertNull(dfc.c);
        assertEquals(0, dfc.c2);
        assertNull(dfc.b);
        assertEquals(0, dfc.b2);
        assertNull(dfc.bool);
        assertFalse(dfc.bool2);
    }
}
