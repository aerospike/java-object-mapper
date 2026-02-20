package com.aerospike.mapper.tools;

/**
 * Minimal contract that core-bound classes use to interact with the enclosing mapper.
 * Implemented by both the legacy AeroMapper and the fluent FluentAeroMapper.
 */
public interface IObjectMapper {
    IRecordConverter getMappingConverter();

    RecordLoader getRecordLoader();
}
