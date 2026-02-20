package com.aerospike.mapper;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InsertOnlyTest extends AeroMapperBaseTest {

    @AerospikeRecord(namespace = "test", set = "testSet")
    public static class DataClass {
        @AerospikeKey
        int a;
        int b;
        int c;
        int d;
        int e;
    }

    @BeforeEach
    public void setup() {
        client.delete(null, new Key("test", "testSet", 1));
    }

    @Test
    public void testInsertOnly() {
        WritePolicy writePolicy = new WritePolicy(client.getWritePolicyDefault());
        writePolicy.totalTimeout = 2000;
        writePolicy.socketTimeout = 100;
        AeroMapper mapper = new AeroMapper.Builder(client)
                .withWritePolicy(writePolicy).forClasses(DataClass.class)
                .build();

        DataClass dataClass = new DataClass();
        dataClass.a = 1;
        dataClass.b = 2;
        dataClass.c = 3;
        dataClass.d = 4;
        dataClass.e = 5;

        // Insert
        mapper.insert(dataClass);

        Key key = new Key("test", "testSet", 1);
        Record record = client.get(null, key);
        assertEquals(5, record.bins.size());
        assertEquals(3, record.getInt("c"));

        // Try to insert again and get an exception
        dataClass.c = 9;
        dataClass.e = 11;
        assertThrows(AerospikeException.class, () -> mapper.insert(dataClass, "a", "c", "e"));
    }
}
