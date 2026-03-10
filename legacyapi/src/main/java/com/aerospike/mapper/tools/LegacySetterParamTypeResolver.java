package com.aerospike.mapper.tools;

import com.aerospike.mapper.tools.PropertyDefinition.SetterParamType;

/**
 * Recognizes the legacy Aerospike Java client's Key and Value types as valid second-parameter types for 2-argument
 * property setters.
 */
public class LegacySetterParamTypeResolver implements SetterParamTypeResolver {

    public static final LegacySetterParamTypeResolver INSTANCE = new LegacySetterParamTypeResolver();

    private static final String LEGACY_KEY = "com.aerospike.client.Key";
    private static final String LEGACY_VALUE = "com.aerospike.client.Value";

    @Override
    public SetterParamType resolve(String paramTypeName) {
        if (LEGACY_KEY.equals(paramTypeName)) {
            return SetterParamType.KEY;
        } else if (LEGACY_VALUE.equals(paramTypeName)) {
            return SetterParamType.VALUE;
        }
        return SetterParamType.NONE;
    }
}
