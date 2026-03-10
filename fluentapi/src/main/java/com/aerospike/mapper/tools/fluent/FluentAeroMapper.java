package com.aerospike.mapper.tools.fluent;

import com.aerospike.client.fluent.RecordMapper;
import com.aerospike.client.fluent.RecordMappingFactory;
import com.aerospike.client.fluent.Session;
import com.aerospike.mapper.exceptions.AerospikeMapperException;
import com.aerospike.mapper.tools.ClassCache;
import com.aerospike.mapper.tools.ClassCacheEntry;
import com.aerospike.mapper.tools.IObjectMapper;
import com.aerospike.mapper.tools.IRecordConverter;
import com.aerospike.mapper.tools.RecordLoader;
import com.aerospike.mapper.tools.converters.MappingConverter;
import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Entry point for the fluent Aerospike Object Mapper.
 * Implements the fluent client's {@link RecordMappingFactory} so it can be registered
 * directly with the fluent client session.
 *
 * <p>Usage:
 * <pre>
 *   FluentAeroMapper mapper = new FluentAeroMapper(session);
 *   RecordMapper&lt;Customer&gt; customerMapper = mapper.getMapper(Customer.class);
 * </pre>
 */
public class FluentAeroMapper implements RecordMappingFactory, IObjectMapper {

    @Getter
    private final Session session;
    private final FluentRecordLoader recordLoader;
    private final MappingConverter mappingConverter;
    private final ConcurrentHashMap<Class<?>, FluentRecordMapper<?>> mapperCache = new ConcurrentHashMap<>();

    public FluentAeroMapper(Session session) {
        if (session == null) {
            throw new AerospikeMapperException("Session must not be null");
        }
        this.session = session;
        this.recordLoader = new FluentRecordLoader(session);
        this.mappingConverter = new MappingConverter(this, this.recordLoader);
    }

    /**
     * Returns a {@link RecordMapper} for the given class. Implements {@link RecordMappingFactory}.
     */
    @Override
    public <T> RecordMapper<T> getMapper(Class<T> clazz) {
        return getRecordMapper(clazz);
    }

    /**
     * Returns a {@link FluentRecordMapper} for the given class, using the core
     * {@link ClassCacheEntry} for annotation-driven serialization/deserialization.
     * Instances are cached per class.
     */
    @SuppressWarnings("unchecked")
    public <T> FluentRecordMapper<T> getRecordMapper(Class<T> clazz) {
        return (FluentRecordMapper<T>) mapperCache.computeIfAbsent(clazz, c -> {
            ClassCacheEntry<T> entry = ClassCache.getInstance().loadClass(clazz, this);
            return new FluentRecordMapper<>(entry, mappingConverter);
        });
    }

    @Override
    public IRecordConverter getMappingConverter() {
        return mappingConverter;
    }

    @Override
    public RecordLoader getRecordLoader() {
        return recordLoader;
    }

}
