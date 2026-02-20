package com.aerospike.mapper;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.mapper.tools.ClassCache;
import com.aerospike.mapper.tools.ClassCacheEntry;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that verify metadata caching during actual database operations.
 * These tests require a running Aerospike server and verify that ClassCacheEntry
 * instances are properly reused across save() and read() operations.
 */
public class MetadataCachingTests extends AeroMapperBaseTest {

    @Setter
    @Getter
    @AerospikeRecord(namespace = "test", set = "person")
    public static class Person {
        @AerospikeKey
        private String id;
        
        @AerospikeBin(name = "fname")
        private String firstName;
        
        private String lastName;
        private int age;
        private Date birthDate;
        
        public Person() {
        }
        
        public Person(String id, String firstName, String lastName, int age) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.age = age;
            this.birthDate = new Date();
        }

    }

    /**
     * INTEGRATION TEST: Verify cache is reused across mixed read and write operations.
     * This is the primary integration test showing that the same ClassCacheEntry
     * is used for both saves and reads in real database operations.
     */
    @Test
    public void testMetadataCachedAcrossMixedOperations() {
        AeroMapper mapper = new AeroMapper.Builder(client).build();
        
        // Write operation
        Person person1 = new Person("p200", "Charlie", "Davis", 45);
        mapper.save(person1);
        
        ClassCacheEntry<Person> entryAfterWrite = ClassCache.getInstance().loadClass(Person.class, mapper);
        assertNotNull(entryAfterWrite, "Cache entry should exist after write");
        
        // Read operation - should reuse same cache entry
        Person read1 = mapper.read(Person.class, "p200");
        assertNotNull(read1, "Read should succeed");
        
        ClassCacheEntry<Person> entryAfterRead = ClassCache.getInstance().loadClass(Person.class, mapper);
        assertSame(entryAfterWrite, entryAfterRead,
            "Read should reuse the same cache entry created by write");
        
        // Another write - should still reuse same cache entry
        Person person2 = new Person("p201", "Diana", "Evans", 28);
        mapper.save(person2);
        
        ClassCacheEntry<Person> entryAfterSecondWrite = ClassCache.getInstance().loadClass(Person.class, mapper);
        assertSame(entryAfterWrite, entryAfterSecondWrite,
            "Second write should reuse the same cache entry");
        
        // Another read - should still reuse same cache entry
        Person read2 = mapper.read(Person.class, "p201");
        assertNotNull(read2, "Second read should succeed");
        
        ClassCacheEntry<Person> entryAfterSecondRead = ClassCache.getInstance().loadClass(Person.class, mapper);
        assertSame(entryAfterWrite, entryAfterSecondRead,
            "Mixed operations should all use the same cached instance");
        
        // Verify data correctness
        assertEquals("Diana", read2.getFirstName());
        assertEquals("Evans", read2.getLastName());
        assertEquals(28, read2.getAge());
    }

    /**
     * STRESS TEST: Verify cache behavior with high volume database operations.
     * Performs 100 save operations and verifies the same ClassCacheEntry
     * is used throughout. This tests cache stability under load.
     */
    @Test
    public void testCacheWithHighVolumeOperations() {
        AeroMapper mapper = new AeroMapper.Builder(client).build();
        
        // Get the initial cached entry
        ClassCacheEntry<Person> initialEntry = ClassCache.getInstance().loadClass(Person.class, mapper);
        
        // Perform many save operations
        final int iterations = 100;
        for (int i = 0; i < iterations; i++) {
            Person person = new Person("p" + (500 + i), "Person" + i, "Lastname" + i, 20 + (i % 50));
            mapper.save(person);
            
            // Every 10 iterations, verify cache entry is still the same
            if (i % 10 == 0) {
                ClassCacheEntry<Person> currentEntry = ClassCache.getInstance().loadClass(Person.class, mapper);
                assertSame(initialEntry, currentEntry,
                    "Cache entry should remain the same after " + i + " operations");
            }
        }
        
        // Final verification after all operations
        ClassCacheEntry<Person> finalEntry = ClassCache.getInstance().loadClass(Person.class, mapper);
        assertSame(initialEntry, finalEntry,
            "Cache entry should be the same after " + iterations + " operations");
        
        // Verify a read also uses same cached entry
        Person readPerson = mapper.read(Person.class, "p550");
        assertNotNull(readPerson, "Read should succeed after many writes");
        
        ClassCacheEntry<Person> entryAfterRead = ClassCache.getInstance().loadClass(Person.class, mapper);
        assertSame(initialEntry, entryAfterRead,
            "Read after many writes should still use the same cached instance");
    }
}
