package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.aerospike.client.Bin;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeGeneration;
import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.mapper.tools.ClassCacheEntry;
import com.aerospike.mapper.tools.utils.MapperUtils;

public class GenerationNotStoredTest extends AeroMapperBaseTest {

    @AerospikeRecord(namespace = NAMESPACE, set = "generationNotStoredTest")
    public static class EntityWithGeneration {
        @AerospikeKey
        private int id;
        private String name;
        @AerospikeGeneration
        private Integer generation = 0;

        public EntityWithGeneration() {}

        public EntityWithGeneration(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getGeneration() {
            return generation;
        }

        public void setGeneration(Integer generation) {
            this.generation = generation;
        }
    }

    @Test
    public void testGenerationFieldNotStoredAsBin() {
        AeroMapper mapper = new AeroMapper.Builder(client).build();
        EntityWithGeneration entity = new EntityWithGeneration(1, "test");
        entity.setGeneration(5); // Set some generation value
        
        // Get the ClassCacheEntry and the bins that would be stored
        ClassCacheEntry<EntityWithGeneration> entry = MapperUtils.getEntryAndValidateNamespace(EntityWithGeneration.class, mapper);
        Bin[] bins = entry.getBins(entity, false, null);
        
        // Verify that the generation field should NOT be included
        // Should only have bins for regular fields (name and potentially id if stored as bin)
        
        boolean foundName = false;
        boolean foundId = false;
        boolean foundGeneration = false;
        
        for (Bin bin : bins) {
            String binName = bin.name;
            if ("name".equals(binName)) {
                foundName = true;
                assertEquals("test", bin.value.getObject());
            } else if ("id".equals(binName)) {
                foundId = true;
                assertEquals(1, bin.value.getObject());
            } else if ("generation".equals(binName)) {
                foundGeneration = true;
            }
        }
        
        assertTrue(foundName, "Should find 'name' bin");
        assertFalse(foundGeneration, "Should NOT find 'generation' bin - it should not be stored");
        
        // Log what bins we actually found for debugging
        System.out.println("Found " + bins.length + " bins:");
        for (Bin bin : bins) {
            System.out.println("  - " + bin.name + " = " + bin.value.getObject());
        }
    }
} 