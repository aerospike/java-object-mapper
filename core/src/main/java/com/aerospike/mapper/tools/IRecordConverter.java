package com.aerospike.mapper.tools;

import java.util.List;
import java.util.Map;

/**
 * Core-level abstraction for converting Aerospike bin data to Java objects.
 * Implemented by MappingConverter in the legacy module.
 */
public interface IRecordConverter {
    <T> T convertToObject(Class<T> clazz, Map<String, Object> record);
    <T> T convertToObject(Class<T> clazz, List<Object> record);
}
