package com.aerospike.mapper.reactive;

import com.aerospike.client.reactor.AerospikeReactorClient;
import com.aerospike.client.reactor.IAerospikeReactorClient;
import com.aerospike.mapper.AeroMapperBaseTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import reactor.blockhound.BlockHound;

import java.io.IOException;

public class ReactiveAeroMapperBaseTest extends AeroMapperBaseTest {
    protected static IAerospikeReactorClient reactorClient;

    @BeforeClass
    public static void setupReactorClass() {
        reactorClient = new AerospikeReactorClient(client);
    }

    @AfterClass
    public static void cleanupReactorClass() throws IOException {
        if (reactorClient != null) {
            reactorClient.close();
        }
    }

    @BeforeAll
    public static void installBlockHound() {
        BlockHound.install();
    }
}
