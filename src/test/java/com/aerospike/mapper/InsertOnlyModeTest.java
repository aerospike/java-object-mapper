package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class InsertOnlyModeTest extends AeroMapperBaseTest {
    @AerospikeRecord(namespace = "test", set = "custs")
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Customer {
        @AerospikeKey
        private String name;
        private int age;
    }
    
    @Test
    public void tetDefaultPolicies() {
        WritePolicy writePolicy = new WritePolicy();
        writePolicy.sendKey = true;
        writePolicy.recordExistsAction=RecordExistsAction.CREATE_ONLY;
        
        ClientPolicy policy = new ClientPolicy();
        policy.writePolicyDefault = writePolicy;
        
        AeroMapper mapper = new AeroMapper.Builder(client).withWritePolicy(writePolicy).forAll().build();
        
        Customer customer = new Customer("Tim", 312);
        mapper.delete(customer);
        // First one should succeed.
        mapper.save(customer);
        try {
            mapper.save(customer);
            fail("Expected an exception to be thrown");
        }
        catch (AerospikeException e) {
        }
    }
    @Test
    public void testExplicitPolicies() {
        WritePolicy writePolicy = new WritePolicy();
        writePolicy.sendKey = true;
        writePolicy.recordExistsAction=RecordExistsAction.CREATE_ONLY;
        
        AeroMapper mapper = new AeroMapper.Builder(client).withWritePolicy(writePolicy).forAll().build();
        
        Customer customer = new Customer("Tim", 312);
        mapper.delete(customer);
        // First one should succeed.
        mapper.save(customer);
        try {
            mapper.save(customer);
            fail("Expected an exception to be thrown");
        }
        catch (AerospikeException e) {
        }
    }
}
