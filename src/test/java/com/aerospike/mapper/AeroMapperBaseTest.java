package com.aerospike.mapper;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.aerospike.client.DebugAerospikeClient;
import com.aerospike.client.DebugAerospikeClient.Granularity;
import com.aerospike.client.DebugAerospikeClient.Options;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.mapper.tools.ClassCache;

public abstract class AeroMapperBaseTest {

    public static final String NAMESPACE = "test";
    protected static IAerospikeClient client;

    @BeforeClass
    public static void setupClass() {
//        client = new AerospikeClient("localhost", 3000);
    	client = new DebugAerospikeClient("localhost", 3000, new Options(Granularity.EVERY_CALL));
    }

    @AfterClass
    public static void cleanupClass() {
        if (client != null) {
            client.close();
        }
    }
    
    @Before
    public void clearCache() {
		ClassCache.getInstance().clear();

    }
}
