package com.aerospike.mapper.tools;

import com.aerospike.mapper.tools.PropertyDefinition.SetterParamType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SetterParamTypeResolverTest {

    @Test
    public void defaultResolver_alwaysReturnsNone() {
        assertEquals(SetterParamType.NONE, SetterParamTypeResolver.DEFAULT.resolve("any.type"));
        assertEquals(SetterParamType.NONE, SetterParamTypeResolver.DEFAULT.resolve(null));
        assertEquals(SetterParamType.NONE, SetterParamTypeResolver.DEFAULT.resolve(""));
    }

    @Test
    public void customResolver_canReturnKey() {
        SetterParamTypeResolver custom = name ->
            "my.Key".equals(name) ? SetterParamType.KEY : SetterParamType.NONE;
        assertEquals(SetterParamType.KEY, custom.resolve("my.Key"));
        assertEquals(SetterParamType.NONE, custom.resolve("other"));
    }

    @Test
    public void customResolver_canReturnValue() {
        SetterParamTypeResolver custom = name ->
            "my.Value".equals(name) ? SetterParamType.VALUE : SetterParamType.NONE;
        assertEquals(SetterParamType.VALUE, custom.resolve("my.Value"));
        assertEquals(SetterParamType.NONE, custom.resolve("other"));
    }
}
