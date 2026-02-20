package com.aerospike.mapper;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.mapper.tools.ClassCache;
import com.aerospike.mapper.tools.ClassCacheEntry;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ClassCache behavior that don't require a running Aerospike server.
 * These tests verify that metadata caching works correctly by checking ClassCacheEntry
 * instance reuse and cache state management.
 * Coverage:
 * - Same instance returned on repeated loads (with stress test)
 * - Cache state tracking (hasClass)
 * - Multiple classes cached simultaneously
 * - Clear behavior forces new reflection
 * - Singleton behavior across mapper instances
 */
public class ClassCacheUnitTests {

    @Setter
    @Getter
    @AerospikeRecord(namespace = "test", set = "testSet")
    public static class TestClass {
        @AerospikeKey
        private String id;
        
        @AerospikeBin(name = "name")
        private String name;
        
        private int value;
        private Date timestamp;

    }

    @Setter
    @Getter
    @AerospikeRecord(namespace = "test", set = "otherclass")
    public static class OtherTestClass {
        @AerospikeKey
        private int id;
        private String data;

    }

    private AeroMapper mapper;

    private static IAerospikeClient createMockClient() {
        IAerospikeClient mockClient = mock(IAerospikeClient.class);
        when(mockClient.getReadPolicyDefault()).thenReturn(new Policy());
        when(mockClient.getWritePolicyDefault()).thenReturn(new WritePolicy());
        when(mockClient.getBatchPolicyDefault()).thenReturn(new BatchPolicy());
        when(mockClient.getQueryPolicyDefault()).thenReturn(new QueryPolicy());
        when(mockClient.getScanPolicyDefault()).thenReturn(new ScanPolicy());
        return mockClient;
    }

    @BeforeEach
    public void setup() {
        ClassCache.getInstance().clear();
        IAerospikeClient mockClient = createMockClient();
        mapper = new AeroMapper.Builder(mockClient).build();
    }

    @AfterEach
    public void cleanup() {
        ClassCache.getInstance().clear();
    }

    /**
     * PRIMARY TEST: Verifies that metadata reflection happens only once.
     * Multiple calls to loadClass() must return the exact same ClassCacheEntry instance.
     * Includes stress test with 100 iterations.
     */
    @Test
    public void testSameInstanceReturnedOnRepeatedLoads() {
        // First load - performs reflection and creates entry
        ClassCacheEntry<TestClass> entry1 = ClassCache.getInstance().loadClass(TestClass.class, mapper);
        assertNotNull(entry1, "First load should return a ClassCacheEntry");

        // Second load - must return exact same instance
        ClassCacheEntry<TestClass> entry2 = ClassCache.getInstance().loadClass(TestClass.class, mapper);
        assertSame(entry1, entry2, "Second load must return the same instance (no new reflection)");

        // Third load - verify consistency
        ClassCacheEntry<TestClass> entry3 = ClassCache.getInstance().loadClass(TestClass.class, mapper);
        assertSame(entry1, entry3, "Third load must return the same instance");

        // Stress test: 100 repeated loads - all must return same instance
        final int iterations = 100;
        for (int i = 0; i < iterations; i++) {
            ClassCacheEntry<TestClass> entryN = ClassCache.getInstance().loadClass(TestClass.class, mapper);
            assertSame(entry1, entryN, "Iteration " + i + " must return the same cached instance");
        }
    }

    /**
     * Test that cache properly tracks which classes have been loaded using hasClass().
     */
    @Test
    public void testHasClassTracksLoadedClasses() {
        // Initially, cache should not have TestClass
        assertFalse(ClassCache.getInstance().hasClass(TestClass.class),
                "Cache should not have TestClass before it's loaded");

        // Load the class
        ClassCacheEntry<TestClass> entry = ClassCache.getInstance().loadClass(TestClass.class, mapper);
        assertNotNull(entry, "Load should succeed");

        // Now cache should have TestClass
        assertTrue(ClassCache.getInstance().hasClass(TestClass.class),
                "Cache should have TestClass after it's loaded");

        // Loading again should not change hasClass result
        ClassCache.getInstance().loadClass(TestClass.class, mapper);
        assertTrue(ClassCache.getInstance().hasClass(TestClass.class),
                "Cache should still have TestClass after repeated load");
    }

    /**
     * Test that cache maintains separate entries for multiple classes simultaneously
     * and that reloading returns the same cached instances.
     */
    @Test
    public void testMultipleClassesCachedSimultaneously() {
        // Load first class
        ClassCacheEntry<TestClass> testEntry1 = ClassCache.getInstance().loadClass(TestClass.class, mapper);
        assertNotNull(testEntry1, "TestClass should load");

        // Load second class
        ClassCacheEntry<OtherTestClass> otherEntry1 = ClassCache.getInstance().loadClass(OtherTestClass.class, mapper);
        assertNotNull(otherEntry1, "OtherTestClass should load");

        // Verify no mixup: each entry must reflect its own class metadata
        assertEquals(TestClass.class, testEntry1.getUnderlyingClass(),
                "TestClass entry must map to TestClass");
        assertEquals(OtherTestClass.class, otherEntry1.getUnderlyingClass(),
                "OtherTestClass entry must map to OtherTestClass");
        assertEquals("testSet", testEntry1.getSetName(),
                "TestClass entry must have set 'testSet'");
        assertEquals("otherclass", otherEntry1.getSetName(),
                "OtherTestClass entry must have set 'otherclass'");

        // Reload first class - should get same instance
        ClassCacheEntry<TestClass> testEntry2 = ClassCache.getInstance().loadClass(TestClass.class, mapper);
        assertSame(testEntry1, testEntry2, "TestClass should return same cached instance");

        // Reload second class - should get same instance
        ClassCacheEntry<OtherTestClass> otherEntry2 = ClassCache.getInstance().loadClass(OtherTestClass.class, mapper);
        assertSame(otherEntry1, otherEntry2, "OtherTestClass should return same cached instance");

        // Both should be in cache
        assertTrue(ClassCache.getInstance().hasClass(TestClass.class),
                "TestClass should be in cache");
        assertTrue(ClassCache.getInstance().hasClass(OtherTestClass.class),
                "OtherTestClass should be in cache");
    }

    /**
     * Test that clear() removes all entries and forces new reflection.
     * After clear, loadClass must create a NEW ClassCacheEntry instance.
     */
    @Test
    public void testClearRemovesAllEntries() {
        // Load a class
        ClassCacheEntry<TestClass> entry1 = ClassCache.getInstance().loadClass(TestClass.class, mapper);
        assertNotNull(entry1, "Initial load should succeed");
        assertTrue(ClassCache.getInstance().hasClass(TestClass.class),
                "Class should be in cache after load");

        // Clear the cache
        ClassCache.getInstance().clear();

        // Class should no longer be in cache
        assertFalse(ClassCache.getInstance().hasClass(TestClass.class),
                "Class should not be in cache after clear");

        // Loading again should create a new entry
        ClassCacheEntry<TestClass> entry2 = ClassCache.getInstance().loadClass(TestClass.class, mapper);
        assertNotNull(entry2, "Load after clear should succeed");

        // Must be a different instance (new reflection occurred)
        assertNotSame(entry1, entry2,
                "Load after clear must create a new instance (reflection happens again)");
    }

    /**
     * Test that cache is singleton - shared across different AeroMapper instances.
     * Different mapper instances should get the same cached ClassCacheEntry.
     */
    @Test
    public void testCacheSharedAcrossMapperInstances() {
        // Create first mapper and load a class
        AeroMapper mapper1 = new AeroMapper.Builder(createMockClient()).build();
        ClassCacheEntry<TestClass> entry1 = ClassCache.getInstance().loadClass(TestClass.class, mapper1);
        assertNotNull(entry1, "Load with mapper1 should succeed");

        // Create second mapper and load the same class
        AeroMapper mapper2 = new AeroMapper.Builder(createMockClient()).build();
        ClassCacheEntry<TestClass> entry2 = ClassCache.getInstance().loadClass(TestClass.class, mapper2);
        assertNotNull(entry2, "Load with mapper2 should succeed");

        // Must be the exact same instance (cache is singleton)
        assertSame(entry1, entry2,
                "Different mapper instances should share the same cached ClassCacheEntry");

        // Cache state should be consistent
        assertTrue(ClassCache.getInstance().hasClass(TestClass.class),
                "Cache state should be consistent across mapper instances");
    }
}
