package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aerospike.client.async.NioEventLoops;
import com.aerospike.client.policy.ClientPolicy;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.mapper.tools.ClassCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public abstract class AeroMapperBaseTest {

    public static final String NAMESPACE = "test";
    protected static IAerospikeClient client;

    @BeforeAll
    public static void setupClass() {
        ClientPolicy policy = new ClientPolicy();
        // Set event loops to use in asynchronous commands.
        policy.eventLoops = new NioEventLoops(1);
        Host[] hosts = Host.parseHosts(System.getProperty("test.host", "localhost:3000"), 3000);
        client = new AerospikeClient(policy, hosts);
    }

    @AfterAll
    public static void cleanupClass() {
        if (client != null) {
            client.close();
        }
    }

    @BeforeEach
    public void clearCache() {
        ClassCache.getInstance().clear();
    }

    public void compare(Object original, Object read) {
        this.compare(original, read, false);
    }

    public void compare(Object original, Object read, boolean showObjects) {
        try {
            ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
            String readString = objectWriter.writeValueAsString(read);
            if (showObjects) {
                System.out.println("------ Read Data -----\n" + readString);
            }
            String originalObject = objectWriter.writeValueAsString(original);
            if (showObjects) {
                System.out.println("------ Original Data -----\n" + originalObject);
            }
            assertEquals(originalObject, readString);
        } catch (JsonProcessingException jpe) {
            throw new RuntimeException(jpe);
        }
    }
}
