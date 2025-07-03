package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.aerospike.mapper.GenerationAnnotationTest.GenerationEntity;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.mapper.tools.configuration.ClassConfig;

public class GenerationConfigurationTest extends AeroMapperBaseTest {

    @AerospikeRecord(namespace = NAMESPACE, set = "configGenerationTest")
    public static class ConfigGenerationEntity {
        @AerospikeKey
        private int id;
        private String name;
        private int generation; // No @Generation annotation, will be configured via config

        public ConfigGenerationEntity() {}

        public ConfigGenerationEntity(int id, String name) {
            this.id = id;
            this.name = name;
            this.generation = 0;
        }

        // Getters and setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getGeneration() { return generation; }
        public void setGeneration(int generation) { this.generation = generation; }
    }

    @Test
    public void testGenerationThroughCodeConfiguration() {
        // Configure generation field through code
        ClassConfig config = new ClassConfig.Builder(ConfigGenerationEntity.class)
                .withFieldNamed("generation").asGenerationField()
                .build();
        
        AeroMapper mapper = new AeroMapper.Builder(client)
                .withClassConfigurations(config)
                .build();
        
        // Start with a clean slate
        mapper.delete(ConfigGenerationEntity.class, 1);

        // Create and save entity
        ConfigGenerationEntity entity = new ConfigGenerationEntity(1, "Config Test");
        mapper.save(entity);
        
        // Read back the entity
        ConfigGenerationEntity readEntity = mapper.read(ConfigGenerationEntity.class, 1);
        
        assertNotNull(readEntity);
        assertEquals(1, readEntity.getId());
        assertEquals("Config Test", readEntity.getName());
        assertEquals(1, readEntity.getGeneration()); // Should be 1 after first save
    }

    @Test
    public void testGenerationThroughYamlConfiguration() throws Exception {
        String yamlConfig = "classes:\n" +
                "  - class: com.aerospike.mapper.GenerationConfigurationTest$ConfigGenerationEntity\n" +
                "    bins:\n" +
                "      - field: generation\n" +
                "        generation: true\n";
        
        AeroMapper mapper = new AeroMapper.Builder(client)
                .withConfiguration(yamlConfig)
                .build();
        
        // Start with a clean slate
        mapper.delete(ConfigGenerationEntity.class, 2);

        // Create and save entity
        ConfigGenerationEntity entity = new ConfigGenerationEntity(2, "YAML Config Test");
        mapper.save(entity);
        
        // Read back the entity
        ConfigGenerationEntity readEntity = mapper.read(ConfigGenerationEntity.class, 2);
        
        assertNotNull(readEntity);
        assertEquals(2, readEntity.getId());
        assertEquals("YAML Config Test", readEntity.getName());
        assertEquals(1, readEntity.getGeneration()); // Should be 1 after first save
    }

    @Test
    public void testOptimisticConcurrencyWithConfiguration() {
        // Configure generation field through code
        ClassConfig config = new ClassConfig.Builder(ConfigGenerationEntity.class)
                .withFieldNamed("generation").asGenerationField()
                .build();
        
        AeroMapper mapper = new AeroMapper.Builder(client)
                .withClassConfigurations(config)
                .build();
        
        // Start with a clean slate
        mapper.delete(ConfigGenerationEntity.class, 3);

        // Create and save entity
        ConfigGenerationEntity entity = new ConfigGenerationEntity(3, "Concurrency Config Test");
        mapper.save(entity);
        
        // Read the entity to get the current generation
        ConfigGenerationEntity readEntity1 = mapper.read(ConfigGenerationEntity.class, 3);
        ConfigGenerationEntity readEntity2 = mapper.read(ConfigGenerationEntity.class, 3);
        
        // Both should have the same generation
        assertEquals(readEntity1.getGeneration(), readEntity2.getGeneration());
        
        // Update first entity
        readEntity1.setName("Updated by first");
        mapper.save(readEntity1);
        
        // Try to update second entity with stale generation - should fail
        readEntity2.setName("Updated by second");
        assertThrows(Exception.class, () -> {
            mapper.save(readEntity2);
        });
    }

    @Test
    public void testGenerationIncrementWithConfiguration() {
        // Configure generation field through code
        ClassConfig config = new ClassConfig.Builder(ConfigGenerationEntity.class)
                .withFieldNamed("generation").asGenerationField()
                .build();
        
        AeroMapper mapper = new AeroMapper.Builder(client)
                .withClassConfigurations(config)
                .build();
        
        // Start with a clean slate
        mapper.delete(ConfigGenerationEntity.class, 4);

        // Create and save entity
        ConfigGenerationEntity entity = new ConfigGenerationEntity(4, "Generation Increment Config Test");
        mapper.save(entity);
        
        // Read and verify initial generation
        ConfigGenerationEntity readEntity = mapper.read(ConfigGenerationEntity.class, 4);
        assertEquals(1, readEntity.getGeneration());
        
        // Update and save
        readEntity.setName("Updated Name");
        mapper.save(readEntity);
        
        // Read again and verify generation incremented
        ConfigGenerationEntity updatedEntity = mapper.read(ConfigGenerationEntity.class, 4);
        assertEquals(2, updatedEntity.getGeneration());
        assertEquals("Updated Name", updatedEntity.getName());
    }
} 