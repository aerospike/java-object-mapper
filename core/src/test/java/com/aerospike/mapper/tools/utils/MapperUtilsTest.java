package com.aerospike.mapper.tools.utils;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.exceptions.AerospikeMapperException;
import com.aerospike.mapper.tools.ClassCache;
import com.aerospike.mapper.tools.ClassCacheEntry;
import com.aerospike.mapper.tools.IObjectMapper;
import com.aerospike.mapper.tools.IRecordConverter;
import com.aerospike.mapper.tools.RecordLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link MapperUtils}.
 */
public class MapperUtilsTest {

    /** A correctly configured class - has a namespace. */
    @AerospikeRecord(namespace = "test", set = "mu_valid")
    private static class ValidClass {
        @AerospikeKey
        int id;
    }

    /** A misconfigured class - namespace is intentionally blank. */
    @AerospikeRecord(set = "mu_bad")
    private static class BlankNamespaceClass {
        @AerospikeKey
        int id;
    }

    private IObjectMapper mockMapper;

    @BeforeEach
    public void setUp() {
        ClassCache.getInstance().clear();
        mockMapper = new IObjectMapper() {
            @Override public IRecordConverter getMappingConverter() { return mock(IRecordConverter.class); }
            @Override public RecordLoader getRecordLoader() { return mock(RecordLoader.class); }
        };
    }

    @AfterEach
    public void tearDown() {
        ClassCache.getInstance().clear();
    }

    @Test
    public void validClass_returnsEntry() {
        ClassCacheEntry<ValidClass> entry = MapperUtils.getEntryAndValidateNamespace(ValidClass.class, mockMapper);
        assertNotNull(entry);
        assertEquals("test", entry.getNamespace());
    }

    @Test
    public void blankNamespace_throwsAerospikeMapperException() {
        AerospikeMapperException ex = assertThrows(
                AerospikeMapperException.class,
                () -> MapperUtils.getEntryAndValidateNamespace(BlankNamespaceClass.class, mockMapper));
        assertTrue(ex.getMessage().contains("Namespace not specified"));
    }

    @Test
    public void exceptionMessage_containsClassName() {
        AerospikeMapperException ex = assertThrows(
                AerospikeMapperException.class,
                () -> MapperUtils.getEntryAndValidateNamespace(BlankNamespaceClass.class, mockMapper));
        assertTrue(ex.getMessage().contains(BlankNamespaceClass.class.getName()));
    }
}
