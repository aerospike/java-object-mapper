package com.aerospike.mapper.tools.converters;

import com.aerospike.mapper.exceptions.AerospikeMapperException;
import com.aerospike.mapper.tools.ClassCache;
import com.aerospike.mapper.tools.ClassCacheEntry;
import com.aerospike.mapper.tools.DeferredObjectLoader;
import com.aerospike.mapper.tools.DeferredObjectLoader.DeferredObjectSetter;
import com.aerospike.mapper.tools.IObjectMapper;
import com.aerospike.mapper.tools.IRecordConverter;
import com.aerospike.mapper.tools.LoadedObjectResolver;
import com.aerospike.mapper.tools.RecordKey;
import com.aerospike.mapper.tools.RecordLoader;
import com.aerospike.mapper.tools.ThreadLocalKeySaver;
import com.aerospike.mapper.tools.TypeMapper;
import com.aerospike.mapper.tools.utils.MapperUtils;
import com.aerospike.mapper.tools.utils.TypeUtils;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MappingConverter implements IRecordConverter {

    private final IObjectMapper mapper;
    private final RecordLoader recordLoader;

    public MappingConverter(IObjectMapper mapper, RecordLoader recordLoader) {
        this.mapper = mapper;
        this.recordLoader = recordLoader;
    }

    /**
     * Translate a Java object to an Aerospike format object. Note that this could potentially have performance issues as
     * the type information of the passed object must be determined on every call.
     *
     * @param obj A given Java object.
     * @return An Aerospike format object.
     */
    public Object translateToAerospike(Object obj) {
        if (obj == null) {
            return null;
        }
        TypeMapper thisMapper = TypeUtils.getMapper(obj.getClass(), TypeUtils.AnnotatedType.getDefaultAnnotateType(), mapper);
        return thisMapper == null ? obj : thisMapper.toAerospikeFormat(obj);
    }

    /**
     * Translate an Aerospike object to a Java object. Note that this could potentially have performance issues as
     * the type information of the passed object must be determined on every call.
     *
     * @param obj A given Java object.
     * @return An Aerospike format object.
     */
    @SuppressWarnings("unchecked")
    public <T> T translateFromAerospike(@NotNull Object obj, @NotNull Class<T> expectedClazz) {
        TypeMapper thisMapper = TypeUtils.getMapper(expectedClazz, TypeUtils.AnnotatedType.getDefaultAnnotateType(), mapper);
        T result = (T) (thisMapper == null ? obj : thisMapper.fromAerospikeFormat(obj));
        resolveDependencies(ClassCache.getInstance().loadClass(expectedClazz, mapper));
        return result;
    }

    // --------------------------------------------------------------------------------------------------
    // The following are convenience methods to convert objects to / from lists / maps / records in case
    // it is needed to perform this operation manually. They will not be needed in most use cases.
    // --------------------------------------------------------------------------------------------------

    /**
     * Given a map of records loaded from Aerospike and a class type, attempt to convert the records to
     * an instance of the passed class.
     *
     * @param clazz  The class type to convert the Aerospike record to.
     * @param record The Aerospike records to convert.
     * @return A virtual list.
     */
    @Override
    public <T> T convertToObject(Class<T> clazz, Map<String, Object> record) {
        return this.convertToObject(clazz, record, true);
    }

    /**
     * This method should not be used; it is public only to allow mappers to see it.
     */
    public <T> T convertToObject(Class<T> clazz, Map<String, Object> record, boolean resolveDependencies) {
        ClassCacheEntry<T> entry = ClassCache.getInstance().loadClass(clazz, mapper);
        T result = entry.constructAndHydrate(record);
        if (resolveDependencies) {
            resolveDependencies(entry);
        }
        return result;
    }

    /**
     * Given a list of records loaded from Aerospike and a class type, attempt to convert the records to
     * an instance of the passed class.
     *
     * @param clazz  The class type to convert the Aerospike record to.
     * @param record The Aerospike records to convert.
     * @return A virtual list.
     * @throws AerospikeMapperException an AerospikeMapperException will be thrown in case of an encountering
     * a ReflectiveOperationException.
     */
    @Override
    public <T> T convertToObject(Class<T> clazz, List<Object> record) {
        return this.convertToObject(clazz, record, true);
    }

    /**
     * This method should not be used; it is public only to allow mappers to see it.
     */
    public <T> T convertToObject(Class<T> clazz, List<Object> record, boolean resolveDependencies) {
        try {
            ClassCacheEntry<T> entry = ClassCache.getInstance().loadClass(clazz, mapper);
            T result = clazz.getConstructor().newInstance();
            entry.hydrateFromList(record, result);
            if (resolveDependencies) {
                resolveDependencies(entry);
            }
            return result;
        } catch (ReflectiveOperationException e) {
            throw new AerospikeMapperException(e);
        }
    }

    /**
     * Given an instance of a class (of any type), convert its properties to a list
     *
     * @param instance The instance of a class (of any type).
     * @return a List of the properties of the given instance.
     */
    @SuppressWarnings("unchecked")
    public <T> List<Object> convertToList(@NotNull T instance) {
        ClassCacheEntry<T> entry = (ClassCacheEntry<T>) ClassCache.getInstance().loadClass(instance.getClass(), mapper);
        return entry.getList(instance, false, false);
    }

    /**
     * Given an instance of a class (of any type), convert its properties to a map, properties names will use as the
     * key and properties values will be the values.
     *
     * @param instance The instance of a class (of any type).
     * @return the properties {@link Map} of the given instance.
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, Object> convertToMap(@NotNull T instance) {
        ClassCacheEntry<T> entry = (ClassCacheEntry<T>) ClassCache.getInstance().loadClass(instance.getClass(), mapper);
        return entry.getMap(instance, false);
    }

    private RecordKey toRecordKey(ClassCacheEntry<?> entry, DeferredObjectLoader.DeferredObject deferredObject) {
        if (deferredObject.isDigest()) {
            return new RecordKey(entry.getNamespace(), entry.getSetName(), (byte[]) deferredObject.getKey());
        } else {
            Object translatedKey = entry.translateKeyToAerospikeKey(deferredObject.getKey());
            return new RecordKey(entry.getNamespace(), entry.getSetName(), translatedKey);
        }
    }

    /**
     * If an object refers to other objects via @AerospikeReference, reading the object will populate the ids.
     * This method batch-loads those referenced objects and wires them back into the parent.
     * @param parentEntity the ClassCacheEntry of the parent entity (may be null).
     */
    @SuppressWarnings("unchecked")
    public void resolveDependencies(ClassCacheEntry<?> parentEntity) {
        List<DeferredObjectLoader.DeferredObjectSetter> deferredObjects = DeferredObjectLoader.getAndClear();

        if (deferredObjects.isEmpty()) {
            return;
        }

        while (!deferredObjects.isEmpty()) {
            List<RecordKey> recordKeyList = new ArrayList<>();
            List<ClassCacheEntry<?>> classCacheEntryList = new ArrayList<>();

            // Resolve any objects which have been loaded before (cache hit)
            for (Iterator<DeferredObjectSetter> iterator = deferredObjects.iterator(); iterator.hasNext(); ) {
                DeferredObjectSetter thisObjectSetter = iterator.next();
                DeferredObjectLoader.DeferredObject deferredObject = thisObjectSetter.getObject();
                Class<?> clazz = deferredObject.getType();
                ClassCacheEntry<?> entry = MapperUtils.getEntryAndValidateNamespace(clazz, mapper);

                RecordKey rk = toRecordKey(entry, deferredObject);
                Object result = LoadedObjectResolver.get(rk);
                if (result != null) {
                    thisObjectSetter.getSetter().setValue(result);
                    iterator.remove();
                } else {
                    recordKeyList.add(rk);
                    classCacheEntryList.add(entry);
                }
            }

            int size = recordKeyList.size();
            if (size > 0) {
                List<Map<String, Object>> maps = recordLoader.getBatchRecords(recordKeyList);

                for (int i = 0; i < size; i++) {
                    DeferredObjectLoader.DeferredObjectSetter thisObjectSetter = deferredObjects.get(i);
                    ClassCacheEntry<?> entry = classCacheEntryList.get(i);
                    Map<String, Object> bins = maps.get(i);

                    // Inject user key into map if key is not stored as a bin (sendKey scenario)
                    RecordKey rk = recordKeyList.get(i);
                    if (bins != null && !entry.isKeyFieldStoredAsBin()) {
                        String keyFieldName = entry.getKeyFieldName();
                        if (keyFieldName != null && rk.keyValue != null) {
                            bins = new HashMap<>(bins);
                            bins.put(keyFieldName, rk.keyValue);
                        }
                    }

                    try {
                        ThreadLocalKeySaver.save(rk, rk.keyValue);
                        Object obj = bins == null ? null : convertToObject(
                                (Class<Object>) thisObjectSetter.getObject().getType(), bins, false);
                        thisObjectSetter.getSetter().setValue(obj);
                    } finally {
                        ThreadLocalKeySaver.clear();
                    }
                }
            }
            deferredObjects = DeferredObjectLoader.getAndClear();
        }
    }
}
