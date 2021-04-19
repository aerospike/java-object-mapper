package com.aerospike.mapper;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.mapper.tools.ClassCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

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
    
    @Before
    public void clearCache() {
		ClassCache.getInstance().clear();

    }
    
    public void compare(Object original, Object read) {
    	try {
			ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
			String readString = objectWriter.writeValueAsString(read);
			System.out.println(readString);
			String originalObject = objectWriter.writeValueAsString(original);
			assertEquals(originalObject, readString);
    	} catch (JsonProcessingException jpe) {
    		throw new RuntimeException(jpe);
    	}
    }
}
