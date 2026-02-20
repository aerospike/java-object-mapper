package com.aerospike.mapper.tools.utils;

import com.aerospike.mapper.exceptions.AerospikeMapperException;
import com.aerospike.mapper.tools.ClassCache;
import com.aerospike.mapper.tools.ClassCacheEntry;
import com.aerospike.mapper.tools.IObjectMapper;

import org.apache.commons.lang3.StringUtils;

public class MapperUtils {

    private MapperUtils() {
    }

    public static <T> ClassCacheEntry<T> getEntryAndValidateNamespace(Class<T> clazz, IObjectMapper mapper) {
        ClassCacheEntry<T> entry = ClassCache.getInstance().loadClass(clazz, mapper);
        String namespace = null;
        if (entry != null) {
            namespace = entry.getNamespace();
        }
        if (StringUtils.isBlank(namespace)) {
            throw new AerospikeMapperException("Namespace not specified to perform database operation on a record of type " + clazz.getName());
        }
        return entry;
    }
}
