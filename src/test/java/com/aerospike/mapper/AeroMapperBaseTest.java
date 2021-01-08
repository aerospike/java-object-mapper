package com.aerospike.mapper;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.IAerospikeClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public abstract class AeroMapperBaseTest {

    public static final String NAMESPACE = "test";
    protected static IAerospikeClient client;

    @BeforeClass
    public static void setupClass() {
        client = new AerospikeClient("localhost", 3000);
    }

    @AfterClass
    public static void cleanupClass() {
        if (client != null) {
            client.close();
        }
    }
}
