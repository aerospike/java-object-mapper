package com.aerospike.mapper.tools.fluent;

import com.aerospike.client.fluent.Key;
import com.aerospike.client.fluent.RecordMapper;
import com.aerospike.client.fluent.Value;
import com.aerospike.mapper.tools.ClassCacheEntry;
import com.aerospike.mapper.tools.converters.MappingConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implements the fluent client's {@link RecordMapper} using the core {@link ClassCacheEntry}
 * for map-based serialization and deserialization.
 *
 * @param <T> the mapped POJO type
 */
public class FluentRecordMapper<T> implements RecordMapper<T> {

    private final ClassCacheEntry<T> entry;
    private final MappingConverter converter;

    FluentRecordMapper(ClassCacheEntry<T> entry, MappingConverter converter) {
        this.entry = entry;
        this.converter = converter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T fromMap(Map<String, Object> bins, Key key, int generation) {
        Map<String, Object> binsWithKey = bins;
        // Inject key field if stored separately (sendKey scenario)
        String keyFieldName = entry.getKeyFieldName();
        if (keyFieldName != null && !entry.isKeyFieldStoredAsBin() && key.userKey != null) {
            binsWithKey = new HashMap<>(bins);
            binsWithKey.put(keyFieldName, key.userKey.getObject());
        }
        T result = converter.convertToObject((Class<T>) entry.getUnderlyingClass(), binsWithKey);
        entry.setGenerationValue(result, generation);
        return result;
    }

    @Override
    public Map<String, Value> toMap(T obj) {
        Map<String, Object> rawMap = entry.getMap(obj, false);
        return rawMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Value.get(e.getValue())
                ));
    }

    @Override
    public Object id(T obj) {
        return entry.getKey(obj);
    }
}
