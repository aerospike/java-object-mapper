package com.aerospike.mapper.tools.utils;

import com.aerospike.client.AerospikeException;
import com.aerospike.mapper.tools.ClassCache;
import com.aerospike.mapper.tools.ClassCacheEntry;
import com.aerospike.mapper.tools.IBaseAeroMapper;
import org.apache.commons.lang3.StringUtils;

public class MapperUtils {
    public static <T> ClassCacheEntry<T> getEntryAndValidateNamespace(Class<T> clazz, IBaseAeroMapper mapper) {
        ClassCacheEntry<T> entry = ClassCache.getInstance().loadClass(clazz, mapper);
        String namespace = null;
        if (entry != null) {
            namespace = entry.getNamespace();
        }
        if (StringUtils.isBlank(namespace)) {
            throw new AerospikeException("Namespace not specified to perform database operation on a record of type " + clazz.getName());
        }
        return entry;
    }
}
