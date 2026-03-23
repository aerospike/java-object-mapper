package com.aerospike.mapper.tools;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.exceptions.AerospikeMapperException;
import com.aerospike.mapper.tools.configuration.ClassConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for ClassCacheEntry factory method validation (C1 bug fix) and ClassConfig.Builder (C2 bug fix).
 */
public class ClassCacheEntryFactoryTest {

    @SuppressWarnings("unused")
    @AerospikeRecord(namespace = "test", set = "factory_test",
        factoryClass = "com.aerospike.mapper.tools.ClassCacheEntryFactoryTest$TestFactory")
    static class OnlyFactoryClassPojo {

        @AerospikeKey
        int id;
    }

    @SuppressWarnings("unused")
    @AerospikeRecord(namespace = "test", set = "factory_test",
        factoryMethod = "create")
    static class OnlyFactoryMethodPojo {

        @AerospikeKey
        int id;
    }

    @SuppressWarnings("unused")
    static class TestFactory {

        public static OnlyFactoryClassPojo create() {
            return new OnlyFactoryClassPojo();
        }
    }

    private IObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        ClassCache.getInstance().clear();
        mapper = mock(IObjectMapper.class);
    }

    @AfterEach
    public void tearDown() {
        ClassCache.getInstance().clear();
    }

    @Test
    public void missingFactoryMethod_throwsWithMessage() {
        AerospikeMapperException ex = assertThrows(
            AerospikeMapperException.class,
            () -> ClassCache.getInstance().loadClass(OnlyFactoryClassPojo.class, mapper));
        assertTrue(ex.getMessage().contains("Missing factoryMethod"),
            "Expected message about missing factoryMethod, got: " + ex.getMessage());
    }

    @Test
    public void missingFactoryClass_throwsWithMessage() {
        AerospikeMapperException ex = assertThrows(
            AerospikeMapperException.class,
            () -> ClassCache.getInstance().loadClass(OnlyFactoryMethodPojo.class, mapper));
        assertTrue(ex.getMessage().contains("Missing factoryClass"),
            "Expected message about missing factoryClass, got: " + ex.getMessage());
    }

    // ── C2: ClassConfig.Builder.withShortName overload removed ────────

    @Test
    public void builderWithShortName_setsShortName() {
        @SuppressWarnings("unused")
        @AerospikeRecord(namespace = "test", set = "test")
        class SimpleClass {

            @AerospikeKey
            int id;
        }

        ClassConfig config = new ClassConfig.Builder(SimpleClass.class)
            .withNamespace("test")
            .withSet("test")
            .withShortName("MyShort")
            .build();
        assertEquals("MyShort", config.getShortName());
    }

    @Test
    public void builderWithSendKey_doesNotAffectShortName() {
        @SuppressWarnings("unused")
        @AerospikeRecord(namespace = "test", set = "test")
        class SimpleClass {

            @AerospikeKey
            int id;
        }

        ClassConfig config = new ClassConfig.Builder(SimpleClass.class)
            .withNamespace("test")
            .withSet("test")
            .withSendKey(true)
            .withShortName("Alias")
            .build();
        assertTrue(config.getSendKey());
        assertEquals("Alias", config.getShortName());
    }
}
