package com.aerospike.mapper.tools;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.AerospikeException.ScanTerminated;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import com.aerospike.mapper.tools.ClassCache.PolicyType;
import com.aerospike.mapper.tools.converters.MappingConverter;
import com.aerospike.mapper.tools.utils.MapperUtils;
import com.aerospike.mapper.tools.virtuallist.VirtualList;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class AeroMapper implements IAeroMapper {

    private final IAerospikeClient mClient;
    private final MappingConverter mappingConverter;

    private AeroMapper(@NotNull IAerospikeClient client) {
        this.mClient = client;
        this.mappingConverter = new MappingConverter(this, mClient);
    }

    /**
     * Create a new Builder to instantiate the AeroMapper.
     * @author tfaulkes
     *
     */
    public static class Builder extends AbstractBuilder<AeroMapper> {
        public Builder(IAerospikeClient client) {
            super(new AeroMapper(client));
            ClassCache.getInstance().setDefaultPolicies(client);

        }
    }

    @Override
    public <T> void save(@NotNull T... objects) throws AerospikeException {
        for (T thisObject : objects) {
            this.save(thisObject);
        }
    }

    @Override
    public <T> void save(@NotNull T object, String... binNames) throws AerospikeException {
        WritePolicy writePolicy = generateWritePolicyFromObject(object);
        writePolicy.recordExistsAction = RecordExistsAction.REPLACE;
        save(writePolicy, object, binNames);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void save(@NotNull WritePolicy writePolicy, @NotNull T object, String... binNames)
            throws AerospikeException {
        Class<T> clazz = (Class<T>) object.getClass();
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);

        if (writePolicy == null) {
            writePolicy = generateWritePolicyFromObject(object);
        }

        String set = entry.getSetName();
        if ("".equals(set)) {
            // Use the null set
            set = null;
        }
        Key key = new Key(entry.getNamespace(), set, Value.get(entry.getKey(object)));

        Bin[] bins = entry.getBins(object, writePolicy.recordExistsAction != RecordExistsAction.REPLACE, binNames);

        mClient.put(writePolicy, key, bins);
    }

    @Override
    public <T> void insert(@NotNull T object, String... binNames) {
        WritePolicy writePolicy = generateWritePolicyFromObject(object);
        writePolicy.recordExistsAction = RecordExistsAction.CREATE_ONLY;
        save(writePolicy, object, binNames);
    }

    @Override
    public <T> void update(@NotNull T object, String... binNames) throws AerospikeException {
        WritePolicy writePolicy = generateWritePolicyFromObject(object);
        writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        save(writePolicy, object, binNames);
    }

    @SuppressWarnings("unchecked")
    private <T> WritePolicy generateWritePolicyFromObject(T object) {
        Class<T> clazz = (Class<T>) object.getClass();
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);

        WritePolicy writePolicy = new WritePolicy(entry.getWritePolicy());

        // #132 -- Ensure that if an overriding TTL / sendKey is passed in the policy it
        // is NOT overwritten. Hence, only if the policy is null do we override these settings.
        Integer ttl = entry.getTtl();
        Boolean sendKey = entry.getSendKey();

        if (ttl != null) {
            writePolicy.expiration = ttl;
        }
        if (sendKey != null) {
            writePolicy.sendKey = sendKey;
        }
        return writePolicy;
    }

    @Override
    public <T> T readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest) throws AerospikeException {
        return this.readFromDigest(clazz, digest, true);
    }

    @Override
    public <T> T readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest, boolean resolveDependencies)
            throws AerospikeException {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        Key key = new Key(entry.getNamespace(), digest, entry.getSetName(), null);
        return this.read(null, clazz, key, entry, resolveDependencies);
    }

    @Override
    public <T> T readFromDigest(Policy readPolicy, @NotNull Class<T> clazz, @NotNull byte[] digest)
            throws AerospikeException {
        return this.readFromDigest(readPolicy, clazz, digest, true);
    }

    @Override
    public <T> T readFromDigest(Policy readPolicy, @NotNull Class<T> clazz, @NotNull byte[] digest,
            boolean resolveDependencies) throws AerospikeException {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        Key key = new Key(entry.getNamespace(), digest, entry.getSetName(), null);
        return this.read(readPolicy, clazz, key, entry, resolveDependencies);
    }

    @Override
    public <T> T read(@NotNull Class<T> clazz, @NotNull Object userKey) throws AerospikeException {
        return this.read(clazz, userKey, true);
    }

    @Override
    public <T> T read(@NotNull Class<T> clazz, @NotNull Object userKey, boolean resolveDependencies)
            throws AerospikeException {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        String set = entry.getSetName();
        Key key = new Key(entry.getNamespace(), set, Value.get(entry.translateKeyToAerospikeKey(userKey)));
        return read(null, clazz, key, entry, resolveDependencies);
    }

    @Override
    public <T> T read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Object userKey) throws AerospikeException {
        return this.read(readPolicy, clazz, userKey, true);
    }

    @Override
    public <T> T read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Object userKey, boolean resolveDependencies)
            throws AerospikeException {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        String set = entry.getSetName();
        Key key = new Key(entry.getNamespace(), set, Value.get(entry.translateKeyToAerospikeKey(userKey)));
        return read(readPolicy, clazz, key, entry, resolveDependencies);
    }

    @Override
    public <T> T[] read(@NotNull Class<T> clazz, @NotNull Object[] userKeys) throws AerospikeException {
        return read(null, clazz, userKeys);
    }

    @Override
    public <T> T[] read(BatchPolicy batchPolicy, @NotNull Class<T> clazz, @NotNull Object[] userKeys)
            throws AerospikeException {
        return read(batchPolicy, clazz, userKeys, (Operation[]) null);
    }

    @Override
    public <T> T[] read(@NotNull Class<T> clazz, @NotNull Object[] userKeys, Operation... operations) {
        return read(null, clazz, userKeys, operations);
    }

    @Override
    public <T> T[] read(BatchPolicy batchPolicy, @NotNull Class<T> clazz, @NotNull Object[] userKeys,
            Operation... operations) {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        String set = entry.getSetName();
        Key[] keys = new Key[userKeys.length];
        for (int i = 0; i < userKeys.length; i++) {
            if (userKeys[i] == null) {
                throw new AerospikeException("Cannot pass null to object " + i + " in multi-read call");
            } else {
                keys[i] = new Key(entry.getNamespace(), set, Value.get(entry.translateKeyToAerospikeKey(userKeys[i])));
            }
        }

        return readBatch(batchPolicy, clazz, keys, entry, operations);
    }

    @SuppressWarnings({ "unchecked" })
    private <T> T read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Key key, @NotNull ClassCacheEntry<T> entry,
            boolean resolveDependencies) {
        if (readPolicy == null || readPolicy.filterExp == null) {
            Object objectForKey = LoadedObjectResolver.get(key);
            if (objectForKey != null) {
                return (T) objectForKey;
            }
        }
        if (readPolicy == null) {
            readPolicy = entry.getReadPolicy();
        }
        Record record = mClient.get(readPolicy, key);

        if (record == null) {
            return null;
        } else {
            try {
                ThreadLocalKeySaver.save(key);
                return mappingConverter.convertToObject(clazz, key, record, entry, resolveDependencies);
            } finally {
                ThreadLocalKeySaver.clear();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T[] readBatch(BatchPolicy batchPolicy, @NotNull Class<T> clazz, @NotNull Key[] keys,
            @NotNull ClassCacheEntry<T> entry, Operation... operations) {
        if (batchPolicy == null) {
            batchPolicy = entry.getBatchPolicy();
        }

        Record[] records;
        if (operations != null && operations.length > 0) {
            records = mClient.get(batchPolicy, keys, operations);
        } else {
            records = mClient.get(batchPolicy, keys);
        }

        T[] results = (T[]) Array.newInstance(clazz, records.length);
        for (int i = 0; i < records.length; i++) {
            if (records[i] == null) {
                results[i] = null;
            } else {
                try {
                    ThreadLocalKeySaver.save(keys[i]);
                    T result = mappingConverter.convertToObject(clazz, keys[i], records[i], entry, false);
                    results[i] = result;
                } finally {
                    ThreadLocalKeySaver.clear();
                }
            }
        }
        mappingConverter.resolveDependencies(entry);
        return results;
    }

    @Override
    public <T> boolean delete(@NotNull Class<T> clazz, @NotNull Object userKey) throws AerospikeException {
        return this.delete(null, clazz, userKey);
    }

    @Override
    public <T> boolean delete(WritePolicy writePolicy, @NotNull Class<T> clazz, @NotNull Object userKey)
            throws AerospikeException {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        Object asKey = entry.translateKeyToAerospikeKey(userKey);

        if (writePolicy == null) {
            writePolicy = entry.getWritePolicy();
            if (entry.getDurableDelete() != null) {
                // Clone the write policy so we're not changing the original one
                writePolicy = new WritePolicy(writePolicy);
                writePolicy.durableDelete = entry.getDurableDelete();
            }
        }
        Key key = new Key(entry.getNamespace(), entry.getSetName(), Value.get(asKey));

        return mClient.delete(writePolicy, key);
    }

    @Override
    public boolean delete(@NotNull Object object) throws AerospikeException {
        return this.delete((WritePolicy) null, object);
    }

    @Override
    public boolean delete(WritePolicy writePolicy, @NotNull Object object) throws AerospikeException {
        ClassCacheEntry<?> entry = MapperUtils.getEntryAndValidateNamespace(object.getClass(), this);
        Key key = new Key(entry.getNamespace(), entry.getSetName(), Value.get(entry.getKey(object)));

        if (writePolicy == null) {
            writePolicy = entry.getWritePolicy();
            if (entry.getDurableDelete() != null) {
                writePolicy = new WritePolicy(writePolicy);
                writePolicy.durableDelete = entry.getDurableDelete();
            }
        }
        return mClient.delete(writePolicy, key);
    }

    @Override
    public <T> void find(@NotNull Class<T> clazz, Function<T, Boolean> function) throws AerospikeException {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);

        Statement statement = new Statement();
        statement.setNamespace(entry.getNamespace());
        statement.setSetName(entry.getSetName());

        RecordSet recordSet = null;
        try {
            // TODO: set the policy (If this statement is thought to be useful, which is dubious)
            recordSet = mClient.query(null, statement);
            T result;
            while (recordSet.next()) {
                result = clazz.getConstructor().newInstance();
                entry.hydrateFromRecord(recordSet.getRecord(), result);
                if (!function.apply(result)) {
                    break;
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new AerospikeException(e);
        } finally {
            if (recordSet != null) {
                recordSet.close();
            }
        }
    }

    @Override
    public <T> void scan(@NotNull Class<T> clazz, @NotNull Processor<T> processor) {
        scan(null, clazz, processor);
    }

    @Override
    public <T> void scan(ScanPolicy policy, @NotNull Class<T> clazz, @NotNull Processor<T> processor) {
        scan(policy, clazz, processor, -1);
    }

    @Override
    public <T> void scan(@NotNull Class<T> clazz, @NotNull Processor<T> processor, int recordsPerSecond) {
        scan(null, clazz, processor, recordsPerSecond);
    }

    @Override
    public <T> void scan(ScanPolicy policy, @NotNull Class<T> clazz, @NotNull Processor<T> processor,
            int recordsPerSecond) {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        if (policy == null) {
            policy = entry.getScanPolicy();
        }
        if (recordsPerSecond >= 0) {
            // Ensure the underlying rate on the policy does not change
            policy = new ScanPolicy(policy);
            policy.recordsPerSecond = recordsPerSecond;
        }
        String namespace = entry.getNamespace();
        String setName = entry.getSetName();

        AtomicBoolean userTerminated = new AtomicBoolean(false);
        try {
            mClient.scanAll(policy, namespace, setName, (key, record) -> {
                T object = this.getMappingConverter().convertToObject(clazz, key, record);
                if (!processor.process(object)) {
                    userTerminated.set(true);
                    throw new AerospikeException.ScanTerminated();
                }
            });
        } catch (ScanTerminated st) {
            if (!userTerminated.get()) {
                throw st;
            }
        }
    }

    @Override
    public <T> List<T> scan(@NotNull Class<T> clazz) {
        return scan(null, clazz);
    }

    @Override
    public <T> List<T> scan(ScanPolicy policy, @NotNull Class<T> clazz) {
        List<T> result = new ArrayList<>();
        Processor<T> resultProcessor = record -> {
            synchronized(result) {
                result.add(record);
            }
            return true;
        };
        scan(policy, clazz, resultProcessor);
        return result;
    }

    @Override
    public <T> void query(@NotNull Class<T> clazz, @NotNull Processor<T> processor, Filter filter) {
        query(null, clazz, processor, filter);
    }

    @Override
    public <T> void query(QueryPolicy policy, @NotNull Class<T> clazz, @NotNull Processor<T> processor, Filter filter) {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        if (policy == null) {
            policy = entry.getQueryPolicy();
        }
        Statement statement = new Statement();
        statement.setFilter(filter);
        statement.setNamespace(entry.getNamespace());
        statement.setSetName(entry.getSetName());

        RecordSet recordSet = mClient.query(policy, statement);
        try {
            while (recordSet.next()) {
                T object = this.getMappingConverter().convertToObject(clazz, recordSet.getKey(), recordSet.getRecord());
                if (!processor.process(object)) {
                    break;
                }
            }
        } finally {
            recordSet.close();
        }
    }

    @Override
    public <T> List<T> query(Class<T> clazz, Filter filter) {
        return query(null, clazz, filter);
    }

    @Override
    public <T> List<T> query(QueryPolicy policy, Class<T> clazz, Filter filter) {
        List<T> result = new ArrayList<>();
        Processor<T> resultProcessor = record -> {
            result.add(record);
            return true;
        };
        query(policy, clazz, resultProcessor, filter);
        return result;
    }

    @Override
    public <T> VirtualList<T> asBackedList(@NotNull Object object, @NotNull String binName, Class<T> elementClazz) {
        return new VirtualList<>(this, object, binName, elementClazz);
    }

    @Override
    public <T> VirtualList<T> asBackedList(@NotNull Class<?> owningClazz, @NotNull Object key, @NotNull String binName,
            Class<T> elementClazz) {
        return new VirtualList<>(this, owningClazz, key, binName, elementClazz);
    }

    @Override
    public IAerospikeClient getClient() {
        return this.mClient;
    }

    @Override
    public MappingConverter getMappingConverter() {
        return this.mappingConverter;
    }

    @Override
    public IAeroMapper asMapper() {
        return this;
    }

    @Override
    public Policy getReadPolicy(Class<?> clazz) {
        return getPolicyByClassAndType(clazz, PolicyType.READ);
    }

    @Override
    public WritePolicy getWritePolicy(Class<?> clazz) {
        return (WritePolicy) getPolicyByClassAndType(clazz, PolicyType.WRITE);
    }

    @Override
    public BatchPolicy getBatchPolicy(Class<?> clazz) {
        return (BatchPolicy) getPolicyByClassAndType(clazz, PolicyType.BATCH);
    }

    @Override
    public ScanPolicy getScanPolicy(Class<?> clazz) {
        return (ScanPolicy) getPolicyByClassAndType(clazz, PolicyType.SCAN);
    }

    @Override
    public QueryPolicy getQueryPolicy(Class<?> clazz) {
        return (QueryPolicy) getPolicyByClassAndType(clazz, PolicyType.QUERY);
    }

    private Policy getPolicyByClassAndType(Class<?> clazz, PolicyType policyType) {
        ClassCacheEntry<?> entry = ClassCache.getInstance().loadClass(clazz, this);

        switch (policyType) {
        case READ:
            return entry == null ? mClient.getReadPolicyDefault() : entry.getReadPolicy();
        case WRITE:
            return entry == null ? mClient.getWritePolicyDefault() : entry.getWritePolicy();
        case BATCH:
            return entry == null ? mClient.getBatchPolicyDefault() : entry.getBatchPolicy();
        case SCAN:
            return entry == null ? mClient.getScanPolicyDefault() : entry.getScanPolicy();
        case QUERY:
            return entry == null ? mClient.getQueryPolicyDefault() : entry.getQueryPolicy();
        default:
            throw new UnsupportedOperationException("Provided unsupported policy type: " + policyType);
        }
    }

    @Override
    public String getNamespace(Class<?> clazz) {
        ClassCacheEntry<?> entry = ClassCache.getInstance().loadClass(clazz, this);
        return entry == null ? null : entry.getNamespace();
    }

    @Override
    public String getSet(Class<?> clazz) {
        ClassCacheEntry<?> entry = ClassCache.getInstance().loadClass(clazz, this);
        return entry == null ? null : entry.getSetName();
    }

    @Override
    public Object getKey(Object obj) {
        ClassCacheEntry<?> entry = ClassCache.getInstance().loadClass(obj.getClass(), this);
        return entry == null ? null : entry.getKey(obj);
    }

    @Override
    public Key getRecordKey(Object obj) {
        ClassCacheEntry<?> entry = ClassCache.getInstance().loadClass(obj.getClass(), this);
        return entry == null ? null : new Key(entry.getNamespace(), entry.getSetName(), Value.get(entry.getKey(obj)));
    }
}
