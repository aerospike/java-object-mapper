package com.aerospike.mapper.tools.converters;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.mapper.tools.*;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Deprecated
public class ReactiveMappingConverter {

    private final IReactiveAeroMapper reactiveMapper;

    public ReactiveMappingConverter(IReactiveAeroMapper reactiveMapper) {
        this.reactiveMapper = reactiveMapper;
    }

    /**
     * Translate a Java object to an Aerospike format object. Note that this could potentially have performance issues as
     * the type information of the passed object must be determined on every call.
     * @param obj A given Java object.
     * @return An Aerospike format object.
     */
    public Object translateToAerospike(Object obj) {
        if (obj == null) {
            return null;
        }
        TypeMapper thisMapper = TypeUtils.getMapper(obj.getClass(), TypeUtils.AnnotatedType.getDefaultAnnotateType(), reactiveMapper);
        return thisMapper == null ? obj : thisMapper.toAerospikeFormat(obj);
    }

    /**
     * Translate an Aerospike object to a Java object. Note that this could potentially have performance issues as
     * the type information of the passed object must be determined on every call.
     * @param obj A given Java object.
     * @return An Aerospike format object.
     */
    @SuppressWarnings("unchecked")
    public <T> T translateFromAerospike(@NotNull Object obj, @NotNull Class<T> expectedClazz) {
        TypeMapper thisMapper = TypeUtils.getMapper(expectedClazz, TypeUtils.AnnotatedType.getDefaultAnnotateType(), reactiveMapper);
        T result = (T)(thisMapper == null ? obj : thisMapper.fromAerospikeFormat(obj));
        resolveDependenciesReactively(ClassCache.getInstance().loadClass(expectedClazz, reactiveMapper));
        return result;
    }

    // --------------------------------------------------------------------------------------------------
    // The following are convenience methods to convert objects to / from lists / maps / records in case
    // it is needed to perform this operation manually. They will not be needed in most use cases.
    // --------------------------------------------------------------------------------------------------
    /**
     * Given a record loaded from Aerospike and a class type, attempt to convert the record to
     * an instance of the passed class.
     * @param clazz The class type to convert the Aerospike record to.
     * @param record The Aerospike record to convert.
     * @return A virtual list.
     * @throws AerospikeException an AerospikeException will be thrown in case of an encountering a ReflectiveOperationException.
     */
    public <T> T convertToObject(Class<T> clazz, Record record) {
        try {
            return convertToObject(clazz, record, null);
        } catch (ReflectiveOperationException e) {
            throw new AerospikeException(e);
        }
    }

    public <T> T convertToObject(Class<T> clazz, Record record, ClassCacheEntry<T> entry) throws ReflectiveOperationException {
        return this.convertToObject(clazz, record, entry, true);
    }

    /**
     * This method should not be used, it is public only to allow mappers to see it.
     */
    public <T> T convertToObject(Class<T> clazz, Record record, ClassCacheEntry<T> entry, boolean resolveDependencies) throws ReflectiveOperationException {
        if (entry == null) {
            entry = ClassCache.getInstance().loadClass(clazz, reactiveMapper);
        }
        T result = entry.constructAndHydrate(record);
        if (resolveDependencies) {
            resolveDependenciesReactively(entry);
        }
        return result;
    }

    public <T> T convertToObject(Class<T> clazz, List<Object> record) {
        return this.convertToObject(clazz, record, true);
    }

    /**
     * This method should not be used, it is public only to allow mappers to see it.
     */
    public <T> T convertToObject(Class<T> clazz, List<Object> record, boolean resolveDependencies) {
        try {
            ClassCacheEntry<T> entry = ClassCache.getInstance().loadClass(clazz, reactiveMapper);
            T result;
            result = clazz.getConstructor().newInstance();
            entry.hydrateFromList(record, result);
            if (resolveDependencies) {
                resolveDependenciesReactively(entry);
            }
            return result;
        } catch (ReflectiveOperationException e) {
            throw new AerospikeException(e);
        }
    }

    public <T> List<Object> convertToList(@NotNull T instance) {
        ClassCacheEntry<T> entry = (ClassCacheEntry<T>) ClassCache.getInstance().loadClass(instance.getClass(), reactiveMapper);
        return entry.getList(instance, false, false);
    }

    public <T> T convertToObject(Class<T> clazz, Map<String,Object> record) {
        try {
            ClassCacheEntry<T> entry = ClassCache.getInstance().loadClass(clazz, reactiveMapper);
            T result = clazz.getConstructor().newInstance();
            entry.hydrateFromMap(record, result);
            return result;
        } catch (ReflectiveOperationException e) {
            throw new AerospikeException(e);
        }
    }

    public <T> Map<String, Object> convertToMap(@NotNull T instance) {
        ClassCacheEntry<T> entry = (ClassCacheEntry<T>) ClassCache.getInstance().loadClass(instance.getClass(), reactiveMapper);
        return entry.getMap(instance, false);
    }

    /**
     * If an object refers to other objects (eg A has a list of B via references), then reading the object will populate the
     * ids. If configured to do so, these objects can be loaded via a batch load and populated back into the references which
     * contain them. This method performs this batch load, translating the records to objects and mapping them back to the
     * references.
     * <p/>
     * These loaded child objects can themselves have other references to other objects, so we iterate through this until
     * the list of deferred objects is empty. The deferred objects are stored in a <pre>ThreadLocalData<pre> list, so are thread safe
     * @param parentEntity - the ClassCacheEntry of the parent entity. This is used to get the batch policy to use.
     */
    public void resolveDependenciesReactively(ClassCacheEntry<?> parentEntity) {
        List<DeferredObjectLoader.DeferredObjectSetter> deferredObjects = DeferredObjectLoader.getAndClear();

        if (deferredObjects.size() == 0) {
            return;
        }

        BatchPolicy batchPolicy = parentEntity == null ? reactiveMapper.getReactorClient().getBatchPolicyDefault() : parentEntity.getBatchPolicy();
        BatchPolicy batchPolicyClone = new BatchPolicy(batchPolicy);

        while (!deferredObjects.isEmpty()) {
            int size = deferredObjects.size();

            ClassCacheEntry<?>[] classCaches = new ClassCacheEntry<?>[size];
            Key[] keys = new Key[size];

            for (int i = 0; i < size; i++) {
                DeferredObjectLoader.DeferredObjectSetter thisObjectSetter = deferredObjects.get(i);
                DeferredObjectLoader.DeferredObject deferredObject = thisObjectSetter.getObject();
                Class<?> clazz = deferredObject.getType();
                ClassCacheEntry<?> entry = MapperUtils.getEntryAndValidateNamespace(clazz, reactiveMapper);
                classCaches[i] = entry;

                if (deferredObject.isDigest()) {
                    keys[i] = new Key(entry.getNamespace(), (byte[])deferredObject.getKey(), entry.getSetName(), null);
                }
                else {
                    keys[i] = new Key(entry.getNamespace(), entry.getSetName(), Value.get(entry.translateKeyToAerospikeKey(deferredObject.getKey())));
                }
            }

            // Load the data
            if (keys.length <= 2) {
                // Just single-thread these keys for speed
                batchPolicyClone.maxConcurrentThreads = 1;
            }
            else {
                batchPolicyClone.maxConcurrentThreads = batchPolicy.maxConcurrentThreads;
            }

            AtomicInteger i = new AtomicInteger(0);
            List<DeferredObjectLoader.DeferredObjectSetter> finalDeferredObjects = deferredObjects;

            reactiveMapper.getReactorClient()
                    .getFlux(batchPolicyClone, keys)
                    .filter(Objects::nonNull)
                    .map(keyRecord -> {
                        try {
                            DeferredObjectLoader.DeferredObjectSetter thisObjectSetter = finalDeferredObjects.get(i.get());
                            ThreadLocalKeySaver.save(keyRecord.key);
                            Object result = keyRecord.record == null ? null : convertToObject((Class)thisObjectSetter.getObject().getType(), keyRecord.record, classCaches[i.get()], false);
                            thisObjectSetter.getSetter().setValue(result);
                            return null;
                        } catch (ReflectiveOperationException e) {
                            throw new AerospikeException(e);
                        } finally {
                            ThreadLocalKeySaver.clear();
                        }
                    });

            deferredObjects = DeferredObjectLoader.getAndClear();
        }
    }
}
