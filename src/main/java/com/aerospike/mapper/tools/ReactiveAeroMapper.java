package com.aerospike.mapper.tools;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import javax.validation.constraints.NotNull;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Value;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.KeyRecord;
import com.aerospike.client.query.Statement;
import com.aerospike.client.reactor.IAerospikeReactorClient;
import com.aerospike.mapper.tools.converters.MappingConverter;
import com.aerospike.mapper.tools.utils.MapperUtils;
import com.aerospike.mapper.tools.virtuallist.ReactiveVirtualList;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactiveAeroMapper implements IReactiveAeroMapper {

    private final IAerospikeReactorClient reactorClient;
    private final IAeroMapper aeroMapper;
    private final MappingConverter mappingConverter;

    /**
     * Create a new Builder to instantiate the AeroMapper. 
     * @author tfaulkes
     *
     */
    public static class Builder extends AbstractBuilder<ReactiveAeroMapper> {
        public Builder(IAerospikeReactorClient reactorClient) {
            super(new ReactiveAeroMapper(reactorClient));
            ClassCache.getInstance().setReactiveDefaultPolicies(reactorClient);
        }
    }
    
    private ReactiveAeroMapper(@NotNull IAerospikeReactorClient reactorClient) {
        this.reactorClient = reactorClient;
        this.aeroMapper = new AeroMapper.Builder(reactorClient.getAerospikeClient()).build();
        this.mappingConverter = new MappingConverter(this, reactorClient.getAerospikeClient());
    }

    @Override
    public <T> Flux<T> save(@NotNull T... objects) {
        return Flux.fromStream(Arrays.stream(objects))
                .flatMap(this::save);
    }

    @Override
    public <T> Mono<T> save(@NotNull T object, String... binNames) {
        return save(null, object, RecordExistsAction.REPLACE, binNames);
    }

    @Override
    public <T> Mono<T> save(@NotNull WritePolicy writePolicy, @NotNull T object, String... binNames) {
        return save(writePolicy, object, null, binNames);
    }

    @SuppressWarnings("unchecked")
    private <T> Mono<T> save(WritePolicy writePolicy, @NotNull T object, RecordExistsAction recordExistsAction, String[] binNames) {
        Class<T> clazz = (Class<T>) object.getClass();
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        if (writePolicy == null) {
            writePolicy = new WritePolicy(entry.getWritePolicy());
            if (recordExistsAction != null) {
                writePolicy.recordExistsAction = recordExistsAction;
            }
            
            // #132 -- Only override the TTL / send key if the policy was not passed in.
            Integer ttl = entry.getTtl();
            Boolean sendKey = entry.getSendKey();

            if (ttl != null) {
                writePolicy.expiration = ttl;
            }
            if (sendKey != null) {
                writePolicy.sendKey = sendKey;
            }
        }

        String set = entry.getSetName();
        if ("".equals(set)) {
            // Use the null set
            set = null;
        }
        Key key = new Key(entry.getNamespace(), set, Value.get(entry.getKey(object)));

        Bin[] bins = entry.getBins(object, writePolicy.recordExistsAction != RecordExistsAction.REPLACE, binNames);

        return reactorClient
                .put(writePolicy, key, bins)
                .map(docKey -> object);
    }

    @Override
    public <T> Mono<T> update(@NotNull T object, String... binNames) {
        return save(null, object, RecordExistsAction.UPDATE, binNames);
    }

    @Override
    public <T> Mono<T> readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest) {
        return this.readFromDigest(clazz, digest, true);
    }

    @Override
    public <T> Mono<T> readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest, boolean resolveDependencies) throws AerospikeException {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        Key key = new Key(entry.getNamespace(), digest, entry.getSetName(), null);
        return this.read(null, clazz, key, entry, resolveDependencies);
    }

    @Override
    public <T> Mono<T> readFromDigest(Policy readPolicy, @NotNull Class<T> clazz, @NotNull byte[] digest) {
        return this.readFromDigest(readPolicy, clazz, digest, true);
    }

    @Override
    public <T> Mono<T> readFromDigest(Policy readPolicy, @NotNull Class<T> clazz, @NotNull byte[] digest, boolean resolveDependencies) {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        Key key = new Key(entry.getNamespace(), digest, entry.getSetName(), null);
        return this.read(readPolicy, clazz, key, entry, resolveDependencies);
    }

    @Override
    public <T> Mono<T> read(@NotNull Class<T> clazz, @NotNull Object userKey) {
        return this.read(clazz, userKey, true);
    }

    @Override
    public <T> Mono<T> read(@NotNull Class<T> clazz, @NotNull Object userKey, boolean resolveDependencies) {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        String set = entry.getSetName();
        Key key = new Key(entry.getNamespace(), set, Value.get(entry.translateKeyToAerospikeKey(userKey)));
        return read(null, clazz, key, entry, resolveDependencies);
    }

    @Override
    public <T> Mono<T> read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Object userKey) {
        return this.read(readPolicy, clazz, userKey, true);
    }

    @Override
    public <T> Mono<T> read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Object userKey, boolean resolveDependencies) {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        String set = entry.getSetName();
        Key key = new Key(entry.getNamespace(), set, Value.get(entry.translateKeyToAerospikeKey(userKey)));
        return read(readPolicy, clazz, key, entry, resolveDependencies);
    }

    @Override
    public <T> Flux<T> read(@NotNull Class<T> clazz, @NotNull Object[] userKeys) {
        return read(null, clazz, userKeys);
    }

    @Override
    public <T> Flux<T> read(BatchPolicy batchPolicy, @NotNull Class<T> clazz, @NotNull Object[] userKeys) {
        return read(null, clazz, userKeys, (Operation[]) null);
    }

    @Override
    public <T> Flux<T> read(@NotNull Class<T> clazz, @NotNull Object[] userKeys, Operation... operations) {
        return read(null, clazz, userKeys, operations);
    }

    @Override
    public <T> Flux<T> read(BatchPolicy batchPolicy, @NotNull Class<T> clazz, @NotNull Object[] userKeys, Operation... operations) {
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

    private <T> Mono<T> read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Key key, @NotNull ClassCacheEntry<T> entry, boolean resolveDependencies) {
        if (readPolicy == null) {
            readPolicy = entry.getReadPolicy();
        }

        return reactorClient
                .get(readPolicy, key)
                .filter(keyRecord -> Objects.nonNull(keyRecord.record))
                .map(keyRecord -> {
                    try {
                        ThreadLocalKeySaver.save(key);
                        return mappingConverter.convertToObject(clazz, keyRecord.record, entry, resolveDependencies);
                    } catch (ReflectiveOperationException e) {
                        throw new AerospikeException(e);
                    } finally {
                        ThreadLocalKeySaver.clear();
                    }
                });
    }

    private <T> Flux<T> readBatch(BatchPolicy batchPolicy, @NotNull Class<T> clazz, @NotNull Key[] keys,
                                  @NotNull ClassCacheEntry<T> entry, Operation... operations) {
        if (batchPolicy == null) {
            batchPolicy = entry.getBatchPolicy();
        }

        Flux<KeyRecord> keyRecordFlux;

        if (operations != null && operations.length > 0) {
            keyRecordFlux = reactorClient
                    .getFlux(batchPolicy, keys, operations);
        } else {
            keyRecordFlux = reactorClient
                    .getFlux(batchPolicy, keys);
        }

        return keyRecordFlux.filter(keyRecord -> Objects.nonNull(keyRecord.record))
                .map(keyRecord -> {
                    try {
                        ThreadLocalKeySaver.save(keyRecord.key);
                        return mappingConverter.convertToObject(clazz, keyRecord.record, entry, true);
                    } catch (ReflectiveOperationException e) {
                        throw new AerospikeException(e);
                    } finally {
                        ThreadLocalKeySaver.clear();
                    }
                });
    }

    @Override
    public <T> Mono<Boolean> delete(@NotNull Class<T> clazz, @NotNull Object userKey) {
        return delete(null, clazz, userKey);
    }

    @Override
    public <T> Mono<Boolean> delete(WritePolicy writePolicy, @NotNull Class<T> clazz, @NotNull Object userKey) {
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

        return reactorClient
                .delete(writePolicy, key)
                .map(k -> true);
    }

    @Override
    public Mono<Boolean> delete(@NotNull Object object) {
        return this.delete((WritePolicy) null, object);
    }

    @Override
    public Mono<Boolean> delete(WritePolicy writePolicy, @NotNull Object object) {
        ClassCacheEntry<?> entry = MapperUtils.getEntryAndValidateNamespace(object.getClass(), this);
        Key key = new Key(entry.getNamespace(), entry.getSetName(), Value.get(entry.getKey(object)));

        if (writePolicy == null) {
            writePolicy = entry.getWritePolicy();
            if (entry.getDurableDelete() != null) {
                writePolicy = new WritePolicy(writePolicy);
                writePolicy.durableDelete = entry.getDurableDelete();
            }
        }
        return reactorClient
                .delete(writePolicy, key)
                .map(k -> true);
    }

    @Override
    public <T> Mono<Void> find(@NotNull Class<T> clazz, Function<T, Boolean> function) throws AerospikeException {
        return Mono.fromCallable(() -> {
            asMapper().find(clazz, function);
            return null;
        });
    }

    @Override
    public <T> Flux<T> scan(@NotNull Class<T> clazz) {
        return scan(null, clazz);
    }

    @Override
    public <T> Flux<T> scan(ScanPolicy policy, @NotNull Class<T> clazz) {
        return scan(policy, clazz, -1);
    }

    @Override
    public <T> Flux<T> scan(@NotNull Class<T> clazz, int recordsPerSecond) {
        return scan(null, clazz, recordsPerSecond);
    }

    @Override
    public <T> Flux<T> scan(ScanPolicy policy, @NotNull Class<T> clazz, int recordsPerSecond) {
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

        return reactorClient.scanAll(policy, namespace, setName)
                .map(keyRecord -> getMappingConverter().convertToObject(clazz, keyRecord.record));
    }

    @Override
    public <T> Flux<T> query(@NotNull Class<T> clazz, Filter filter) {
        return query(null, clazz, filter);
    }

    @Override
    public <T> Flux<T> query(QueryPolicy policy, @NotNull Class<T> clazz, Filter filter) {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        if (policy == null) {
            policy = entry.getQueryPolicy();
        }
        Statement statement = new Statement();
        statement.setFilter(filter);
        statement.setNamespace(entry.getNamespace());
        statement.setSetName(entry.getSetName());

        return reactorClient.query(policy, statement)
                .map(keyRecord -> getMappingConverter().convertToObject(clazz, keyRecord.record));
    }

    @Override
    public <T> ReactiveVirtualList<T> asBackedList(@NotNull Object object, @NotNull String binName, Class<T> elementClazz) {
        return new ReactiveVirtualList<>(this, object, binName, elementClazz);
    }

    @Override
    public <T> ReactiveVirtualList<T> asBackedList(@NotNull Class<?> owningClazz, @NotNull Object key, @NotNull String binName, Class<T> elementClazz) {
        return new ReactiveVirtualList<>(this, owningClazz, key, binName, elementClazz);
    }

    @Override
    public IAerospikeReactorClient getReactorClient() {
        return reactorClient;
    }

    @Override
    public MappingConverter getMappingConverter() {
        return mappingConverter;
    }

    @Override
    public IAeroMapper asMapper() {
        return aeroMapper;
    }

    @Override
    public Policy getReadPolicy(Class<?> clazz) {
        return getPolicyByClassAndType(clazz, ClassCache.PolicyType.READ);
    }

    @Override
    public WritePolicy getWritePolicy(Class<?> clazz) {
        return (WritePolicy) getPolicyByClassAndType(clazz, ClassCache.PolicyType.WRITE);
    }

    @Override
    public BatchPolicy getBatchPolicy(Class<?> clazz) {
        return (BatchPolicy) getPolicyByClassAndType(clazz, ClassCache.PolicyType.BATCH);
    }

    @Override
    public ScanPolicy getScanPolicy(Class<?> clazz) {
        return (ScanPolicy) getPolicyByClassAndType(clazz, ClassCache.PolicyType.SCAN);
    }

    @Override
    public QueryPolicy getQueryPolicy(Class<?> clazz) {
        return (QueryPolicy) getPolicyByClassAndType(clazz, ClassCache.PolicyType.QUERY);
    }

    private Policy getPolicyByClassAndType(Class<?> clazz, ClassCache.PolicyType policyType) {
        ClassCacheEntry<?> entry = ClassCache.getInstance().loadClass(clazz, this);

        switch (policyType) {
            case READ:
                return entry == null ? reactorClient.getReadPolicyDefault() : entry.getReadPolicy();
            case WRITE:
                return entry == null ? reactorClient.getWritePolicyDefault() : entry.getWritePolicy();
            case BATCH:
                return entry == null ? reactorClient.getBatchPolicyDefault() : entry.getBatchPolicy();
            case SCAN:
                return entry == null ? reactorClient.getScanPolicyDefault() : entry.getScanPolicy();
            case QUERY:
                return entry == null ? reactorClient.getQueryPolicyDefault() : entry.getQueryPolicy();
            default:
                throw new UnsupportedOperationException("Provided unsupported policy.");
        }
    }

    @Override
    public <T> Mono<String> getNamespace(Class<T> clazz) {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        return entry == null ? null : Mono.just(entry.getNamespace());
    }

    @Override
    public <T> Mono<String> getSet(Class<T> clazz) {
        ClassCacheEntry<?> entry = ClassCache.getInstance().loadClass(clazz, this);
        return entry == null ? null : Mono.just(entry.getSetName());
    }

    @Override
    public Mono<Object> getKey(Object obj) {
        ClassCacheEntry<?> entry = ClassCache.getInstance().loadClass(obj.getClass(), this);
        return entry == null ? null : Mono.just(entry.getKey(obj));
    }

    @Override
    public Mono<Key> getRecordKey(Object obj) {
        ClassCacheEntry<?> entry = ClassCache.getInstance().loadClass(obj.getClass(), this);
        return entry == null ? null : Mono.just(new Key(entry.getNamespace(), entry.getSetName(), Value.get(entry.getKey(obj))));
    }
}
