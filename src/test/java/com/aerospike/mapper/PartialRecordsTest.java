package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;

public class PartialRecordsTest extends AeroMapperBaseTest {

    @AerospikeRecord(namespace = "test", set = "testSet")
    public static class DataClass {
        @AerospikeKey
        int a;
        int b;
        int c;
        int d;
        int e;
    }

    @Test
    public void testPartialSave() {
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

        // Do a full save
        mapper.save(dataClass);

        Key key = new Key("test", "testSet", 1);
        Record record = client.get(null, key);
        assertEquals(5, record.bins.size());
        assertEquals(3, record.getInt("c"));

        // Perform a partial save, which should replace the record with just these bins
        dataClass.c = 9;
        dataClass.e = 11;
        mapper.save(dataClass, "a", "c", "e");

        record = client.get(null, key);
        assertEquals(3, record.bins.size());
        assertEquals(1, record.getInt("a"));
        assertEquals(9, record.getInt("c"));
        assertEquals(11, record.getInt("e"));

        // Now do an update of bin c and add bin d
        dataClass.c = 99;
        mapper.update(dataClass, "c", "d");

        record = client.get(null, key);
        assertEquals(4, record.bins.size());
        assertEquals(1, record.getInt("a"));
        assertEquals(99, record.getInt("c"));
        assertEquals(4, record.getInt("d"));
        assertEquals(11, record.getInt("e"));
    }
}
