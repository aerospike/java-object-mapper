package com.aerospike.mapper.tools;

/**
 * Minimal contract that core-bound classes use to interact with the enclosing mapper.
 * Implemented by both the legacy AeroMapper and the fluent FluentAeroMapper.
 */
public interface IObjectMapper {
    IRecordConverter getMappingConverter();

    RecordLoader getRecordLoader();

    /**
     * Returns a resolver that maps setter parameter type names to {@link PropertyDefinition.SetterParamType} values for
     * the client library used by this mapper.
     */
    default SetterParamTypeResolver getSetterParamTypeResolver() {
        return SetterParamTypeResolver.DEFAULT;
    }
}
