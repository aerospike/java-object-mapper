package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.*;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeGeneration;
import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.mapper.tools.ClassCacheEntry;
import com.aerospike.mapper.tools.utils.MapperUtils;

import java.util.Map;

public class GenerationNotStoredTest extends AeroMapperBaseTest {

    @Setter
    @Getter
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

    }

    @Test
    public void testGenerationFieldNotStoredAsBin() {
        AeroMapper mapper = new AeroMapper.Builder(client).build();
        EntityWithGeneration entity = new EntityWithGeneration(1, "test");
        entity.setGeneration(5); // Set some generation value

        // Get the ClassCacheEntry and the bin map that would be stored
        ClassCacheEntry<EntityWithGeneration> entry = MapperUtils.getEntryAndValidateNamespace(EntityWithGeneration.class, mapper);
        Map<String, Object> binMap = entry.getMap(entity, false);

        // Verify that the generation field should NOT be included
        assertTrue(binMap.containsKey("name"), "Should find 'name' bin");
        assertEquals("test", binMap.get("name"), "name bin should have correct value");
        assertFalse(binMap.containsKey("generation"), "Should NOT find 'generation' bin - it should not be stored");

        // Log what bins we actually found for debugging
        System.out.println("Found " + binMap.size() + " bins:");
        binMap.forEach((k, v) -> System.out.println("  - " + k + " = " + v));
    }
} 