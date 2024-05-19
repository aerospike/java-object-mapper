package com.aerospike.mapper.reactive;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.ReactiveAeroMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ReactiveInsertOnlyModeTest extends ReactiveAeroMapperBaseTest {
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
        
        ReactiveAeroMapper mapper = new ReactiveAeroMapper.Builder(reactorClient).withWritePolicy(writePolicy).forAll().build();
        
        Customer customer = new Customer("Tim", 312);
        mapper.delete(customer);
        // First one should succeed.
        mapper.save(customer).doOnError(c -> fail("Expected an succcess"));
        mapper.save(customer).doOnSuccess(c -> {
            fail("Expected an exception to be thrown");
        });
    }
    @Test
    public void testExplicitPolicies() {
        WritePolicy writePolicy = new WritePolicy();
        writePolicy.sendKey = true;
        writePolicy.recordExistsAction=RecordExistsAction.CREATE_ONLY;
        
        ReactiveAeroMapper mapper = new ReactiveAeroMapper.Builder(reactorClient).withWritePolicy(writePolicy).forAll().build();
        
        Customer customer = new Customer("Tim", 312);
        mapper.delete(customer);
        // First one should succeed.
        mapper.save(customer).doOnError(c -> fail("Expected an succcess"));
        mapper.save(customer).doOnSuccess(c -> {
            fail("Expected an exception to be thrown");
        });
    }
}
