package com.aerospike.mapper;

import com.aerospike.mapper.tools.LegacySetterParamTypeResolver;
import com.aerospike.mapper.tools.PropertyDefinition.SetterParamType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LegacySetterParamTypeResolverTest {

    private final LegacySetterParamTypeResolver resolver = LegacySetterParamTypeResolver.INSTANCE;

    @Test
    public void resolvesLegacyKeyType() {
        assertEquals(SetterParamType.KEY, resolver.resolve("com.aerospike.client.Key"));
    }

    @Test
    public void resolvesLegacyValueType() {
        assertEquals(SetterParamType.VALUE, resolver.resolve("com.aerospike.client.Value"));
    }

    @Test
    public void unknownType_returnsNone() {
        assertEquals(SetterParamType.NONE, resolver.resolve("java.lang.String"));
    }

    @Test
    public void nullType_returnsNone() {
        assertEquals(SetterParamType.NONE, resolver.resolve(null));
    }

    @Test
    public void fluentKeyType_returnsNone() {
        assertEquals(SetterParamType.NONE, resolver.resolve("com.aerospike.client.fluent.Key"));
    }

    @Test
    public void singletonInstance_isNotNull() {
        assertNotNull(LegacySetterParamTypeResolver.INSTANCE);
    }
}
