package com.aerospike.mapper.tools.utils;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TypeUtils} static helpers and the inner {@link TypeUtils.AnnotatedType}.
 */
public class TypeUtilsTest {

    // ── isVoidType ────────────────────────────────────────────────────────────

    @Test
    public void isVoidType_null_returnsTrue() {
        assertTrue(TypeUtils.isVoidType(null));
    }

    @Test
    public void isVoidType_voidClass_returnsTrue() {
        assertTrue(TypeUtils.isVoidType(Void.class));
    }

    @Test
    public void isVoidType_voidPrimitive_returnsTrue() {
        assertTrue(TypeUtils.isVoidType(void.class));
    }

    @Test
    public void isVoidType_string_returnsFalse() {
        assertFalse(TypeUtils.isVoidType(String.class));
    }

    @Test
    public void isVoidType_int_returnsFalse() {
        assertFalse(TypeUtils.isVoidType(int.class));
    }

    @Test
    public void isVoidType_object_returnsFalse() {
        assertFalse(TypeUtils.isVoidType(Object.class));
    }

    // ── isByteType ────────────────────────────────────────────────────────────

    @Test
    public void isByteType_byteBoxed_returnsTrue() {
        assertTrue(TypeUtils.isByteType(Byte.class));
    }

    @Test
    public void isByteType_bytePrimitive_returnsTrue() {
        assertTrue(TypeUtils.isByteType(byte.class));
    }

    @Test
    public void isByteType_integer_returnsFalse() {
        assertFalse(TypeUtils.isByteType(Integer.class));
    }

    @Test
    public void isByteType_string_returnsFalse() {
        assertFalse(TypeUtils.isByteType(String.class));
    }

    @Test
    public void isByteType_null_returnsFalse() {
        assertFalse(TypeUtils.isByteType(null));
    }

    // ── isAerospikeNativeType ─────────────────────────────────────────────────

    @Test
    public void isAerospikeNativeType_null_returnsFalse() {
        assertFalse(TypeUtils.isAerospikeNativeType(null));
    }

    @Test
    public void isAerospikeNativeType_longBoxed_returnsTrue() {
        assertTrue(TypeUtils.isAerospikeNativeType(Long.class));
    }

    @Test
    public void isAerospikeNativeType_longPrimitive_returnsTrue() {
        assertTrue(TypeUtils.isAerospikeNativeType(long.class));
    }

    @Test
    public void isAerospikeNativeType_doubleBoxed_returnsTrue() {
        assertTrue(TypeUtils.isAerospikeNativeType(Double.class));
    }

    @Test
    public void isAerospikeNativeType_doublePrimitive_returnsTrue() {
        assertTrue(TypeUtils.isAerospikeNativeType(double.class));
    }

    @Test
    public void isAerospikeNativeType_string_returnsTrue() {
        assertTrue(TypeUtils.isAerospikeNativeType(String.class));
    }

    @Test
    public void isAerospikeNativeType_integer_returnsFalse() {
        assertFalse(TypeUtils.isAerospikeNativeType(Integer.class));
    }

    @Test
    public void isAerospikeNativeType_bigDecimal_returnsFalse() {
        assertFalse(TypeUtils.isAerospikeNativeType(BigDecimal.class));
    }

    // ── TypeUtils.AnnotatedType ───────────────────────────────────────────────

    @AerospikeRecord(namespace = "test", set = "tut")
    private static class SampleClass {
        @AerospikeKey
        int id;

        @AerospikeEmbed
        @AerospikeBin(name = "embeddedField")
        SampleClass embedded;

        String plain;
    }

    @Test
    public void annotatedType_defaultInstance_isNotParameterized() {
        TypeUtils.AnnotatedType at = TypeUtils.AnnotatedType.getDefaultAnnotateType();
        assertFalse(at.isParameterizedType());
        assertNull(at.getParameterizedType());
        assertNull(at.getBinConfig());
    }

    @Test
    public void annotatedType_fromField_withAnnotations() throws NoSuchFieldException {
        Field field = SampleClass.class.getDeclaredField("embedded");
        TypeUtils.AnnotatedType at = new TypeUtils.AnnotatedType(null, field);
        // Field has @AerospikeEmbed and @AerospikeBin annotations
        assertNotNull(at.getAnnotations());
        assertTrue(at.getAnnotations().length >= 2);
        assertNotNull(at.getAnnotation(AerospikeEmbed.class));
        assertNotNull(at.getAnnotation(AerospikeBin.class));
    }

    @Test
    public void annotatedType_fromField_noAnnotations() throws NoSuchFieldException {
        Field field = SampleClass.class.getDeclaredField("plain");
        TypeUtils.AnnotatedType at = new TypeUtils.AnnotatedType(null, field);
        // No specific annotation to retrieve
        assertNull(at.getAnnotation(AerospikeEmbed.class));
    }

    @Test
    public void annotatedType_getAnnotation_unknownReturnsNull() throws NoSuchFieldException {
        Field field = SampleClass.class.getDeclaredField("embedded");
        TypeUtils.AnnotatedType at = new TypeUtils.AnnotatedType(null, field);
        // AerospikeKey is not present on this field
        assertNull(at.getAnnotation(AerospikeKey.class));
    }

    @Test
    public void annotatedType_defaultInstance_getAnnotationReturnsNull() {
        TypeUtils.AnnotatedType at = TypeUtils.AnnotatedType.getDefaultAnnotateType();
        assertNull(at.getAnnotation(AerospikeEmbed.class));
    }
}
