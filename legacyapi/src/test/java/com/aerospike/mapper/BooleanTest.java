package com.aerospike.mapper;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.Policy;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.aerospike.client.Value.UseBoolBin;

public class BooleanTest extends AeroMapperBaseTest {
    @AfterAll
    public static void resetBooleanEncoding(){
        UseBoolBin = true;
    }


    @AerospikeRecord(namespace = "test", set = "B")
    public static class B {
        @AerospikeKey
        public int id;
        public Boolean boolValue;
    }

    @Test
    public void testNumericEncoding() throws JsonProcessingException {
        UseBoolBin = false;

        B b = new B();
        b.boolValue = true;
        b.id = 1;
        String config =
                "---\n" +
                        "classes:\n" +
                        "  - class: com.aerospike.mapper.BooleanTest$B\n" +
                        "    namespace: test\n" +
                        "    set: B\n" +
                        "    key:\n" +
                        "      field: id\n";

        AeroMapper mapper = new AeroMapper.Builder(client).withConfiguration(config).build();
        mapper.save(b);

        B b2 = mapper.read(B.class, 1);

        assertEquals(b.id, b2.id);
        assertEquals(b.boolValue, b2.boolValue);
        final Record rec = mapper.getClient().get(new Policy(), new Key("test", "B", 1));
        final Object rawRepresentation = rec.bins.get("boolValue");

        assertEquals(Long.class, rawRepresentation.getClass());
    }

    @Test
    public void testObjectEncoding() throws JsonProcessingException {
        UseBoolBin = true;

        B b = new B();
        b.boolValue = true;
        b.id = 1;
        String config =
                "---\n" +
                        "classes:\n" +
                        "  - class: com.aerospike.mapper.BooleanTest$B\n" +
                        "    namespace: test\n" +
                        "    set: B\n" +
                        "    key:\n" +
                        "      field: id\n";

        AeroMapper mapper = new AeroMapper.Builder(client).withConfiguration(config).build();
        mapper.save(b);

        B b2 = mapper.read(B.class, 1);

        assertEquals(b.id, b2.id);
        assertEquals(b.boolValue, b2.boolValue);
        final Record rec = mapper.getClient().get(new Policy(), new Key("test", "B", 1));
        final Object rawRepresentation = rec.bins.get("boolValue");
        assertEquals(Boolean.class, rawRepresentation.getClass());
    }

    @Test
    public void testObjectByDefault() throws JsonProcessingException {
        B b = new B();
        b.boolValue = true;
        b.id = 1;
        String config =
                "---\n" +
                        "classes:\n" +
                        "  - class: com.aerospike.mapper.BooleanTest$B\n" +
                        "    namespace: test\n" +
                        "    set: B\n" +
                        "    key:\n" +
                        "      field: id\n";

        AeroMapper mapper = new AeroMapper.Builder(client).withConfiguration(config).build();
        mapper.save(b);

        B b2 = mapper.read(B.class, 1);

        assertEquals(b.id, b2.id);
        assertEquals(b.boolValue, b2.boolValue);
        final Record rec = mapper.getClient().get(new Policy(), new Key("test", "B", 1));
        final Object rawRepresentation = rec.bins.get("boolValue");
        assertEquals(Boolean.class, rawRepresentation.getClass());
    }

}
