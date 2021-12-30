package com.aerospike.mapper;

import com.aerospike.client.AerospikeException;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class NamespacePlaceHolderTest extends AeroMapperBaseTest {

    @AerospikeRecord(namespace = "${namespace.name:default}", set = "set1")
    public static class PlaceHolderModel {

        @AerospikeKey
        String id;
        String name;
        String email;
    }

    @Test
    public void shouldSave() {
        Properties props = System.getProperties();
        props.setProperty("namespace.name", "test");

        PlaceHolderModel placeHolderModel1 = new PlaceHolderModel();
        placeHolderModel1.id = "id1";
        placeHolderModel1.name = "name1";
        placeHolderModel1.email = "name1@gmail.com";

        AeroMapper mapper = new AeroMapper.Builder(client).build();
        mapper.save(placeHolderModel1);

        PlaceHolderModel placeHolderModelRead = mapper.read(PlaceHolderModel.class, placeHolderModel1.id);
        assertEquals(placeHolderModel1.email, placeHolderModelRead.email);
    }

    @Test
    public void shouldFailOnNonExistingNamespace() {
        Properties props = System.getProperties();
        props.setProperty("namespace.name", "non-existing-namespace");

        PlaceHolderModel placeHolderModel1 = new PlaceHolderModel();
        placeHolderModel1.id = "id1";
        placeHolderModel1.name = "name1";
        placeHolderModel1.email = "name1@gmail.com";

        AeroMapper mapper = new AeroMapper.Builder(client).build();

        try {
            mapper.save(placeHolderModel1);
            fail();
        } catch (AerospikeException ae) {
            assertTrue(true);
        }
    }
}
