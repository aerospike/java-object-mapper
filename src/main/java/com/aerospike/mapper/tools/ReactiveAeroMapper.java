package com.aerospike.mapper.tools;

import com.aerospike.client.*;
import com.aerospike.client.policy.*;
import com.aerospike.client.reactor.IAerospikeReactorClient;
import com.aerospike.mapper.tools.configuration.ClassConfig;
import com.aerospike.mapper.tools.configuration.Configuration;
import com.aerospike.mapper.tools.converters.MappingConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class ReactiveAeroMapper implements IReactiveAeroMapper {

    private final IAerospikeReactorClient reactorClient;
    private final MappingConverter mappingConverter;

    public static class Builder {
        private final ReactiveAeroMapper reactorMapper;
        private List<Class<?>> classesToPreload = null;

        public Builder(IAerospikeReactorClient reactorClient) {
            this.reactorMapper = new ReactiveAeroMapper(reactorClient);
            ClassCache.getInstance().setReactiveDefaultPolicies(reactorClient);
        }

        /**
         * Add in a custom type converter. The converter must have methods which implement the ToAerospike and FromAerospike annotation.
         * @param converter The custom converter
         * @return this object
         */
        public ReactiveAeroMapper.Builder addConverter(Object converter) {
            GenericTypeMapper mapper = new GenericTypeMapper(converter);
            TypeUtils.addTypeMapper(mapper.getMappedClass(), mapper);

            return this;
        }

        public ReactiveAeroMapper.Builder preLoadClass(Class<?> clazz) {
            if (classesToPreload == null) {
                classesToPreload = new ArrayList<>();
            }
            classesToPreload.add(clazz);
            return this;
        }

        public ReactiveAeroMapper.Builder withConfigurationFile(File file) throws IOException {
            return this.withConfigurationFile(file, false);
        }

        public ReactiveAeroMapper.Builder withConfigurationFile(File file, boolean allowsInvalid) throws IOException {
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            Configuration configuration = objectMapper.readValue(file, Configuration.class);
            this.loadConfiguration(configuration, allowsInvalid);
            return this;
        }

        public ReactiveAeroMapper.Builder withConfiguration(String configurationYaml) throws JsonProcessingException {
            return this.withConfiguration(configurationYaml, false);
        }

        public ReactiveAeroMapper.Builder withConfiguration(String configurationYaml, boolean allowsInvalid) throws JsonProcessingException {
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            Configuration configuration = objectMapper.readValue(configurationYaml, Configuration.class);
            this.loadConfiguration(configuration, allowsInvalid);
            return this;
        }

        private void loadConfiguration(@NotNull Configuration configuration, boolean allowsInvalid) {
            for (ClassConfig config : configuration.getClasses()) {
                try {
                    String name = config.getClassName();
                    if (StringUtils.isBlank(name)) {
                        throw new AerospikeException("Class with blank name in configuration file");
                    }
                    else {
                        try {
                            Class.forName(config.getClassName());
                        } catch (ClassNotFoundException e) {
                            throw new AerospikeException("Cannot find a class with name " + name);
                        }
                    }
                }
                catch (RuntimeException re) {
                    if (allowsInvalid) {
                        System.err.println("Ignoring issue with configuration: " + re.getMessage());
                    }
                    else {
                        throw re;
                    }
                }
            }
            ClassCache.getInstance().addConfiguration(configuration);
        }

        public static class ReactiveAeroPolicyMapper {
            private final ReactiveAeroMapper.Builder builder;
            private final Policy policy;
            private final ClassCache.PolicyType policyType;

            public ReactiveAeroPolicyMapper(ReactiveAeroMapper.Builder builder, ClassCache.PolicyType policyType, Policy policy) {
                this.builder = builder;
                this.policyType = policyType;
                this.policy = policy;
            }
            public ReactiveAeroMapper.Builder forClasses(Class<?> ... classes) {
                for (Class<?> thisClass : classes) {
                    ClassCache.getInstance().setSpecificPolicy(policyType, thisClass, policy);
                }
                return builder;
            }
            public ReactiveAeroMapper.Builder forThisOrChildrenOf(Class<?> clazz) {
                ClassCache.getInstance().setChildrenPolicy(this.policyType, clazz, this.policy);
                return builder;
            }
            public ReactiveAeroMapper.Builder forAll() {
                ClassCache.getInstance().setDefaultPolicy(policyType, policy);
                return builder;
            }
        }

        public ReactiveAeroPolicyMapper withReadPolicy(Policy policy) {
            return new ReactiveAeroPolicyMapper(this, ClassCache.PolicyType.READ, policy);
        }
        public ReactiveAeroPolicyMapper withWritePolicy(Policy policy) {
            return new ReactiveAeroPolicyMapper(this, ClassCache.PolicyType.WRITE, policy);
        }
        public ReactiveAeroPolicyMapper withBatchPolicy(BatchPolicy policy) {
            return new ReactiveAeroPolicyMapper(this, ClassCache.PolicyType.BATCH, policy);
        }
        public ReactiveAeroPolicyMapper withScanPolicy(ScanPolicy policy) {
            return new ReactiveAeroPolicyMapper(this, ClassCache.PolicyType.SCAN, policy);
        }
        public ReactiveAeroPolicyMapper withQueryPolicy(QueryPolicy policy) {
            return new ReactiveAeroPolicyMapper(this, ClassCache.PolicyType.QUERY, policy);
        }

        public ReactiveAeroMapper build() {
            if (classesToPreload != null) {
                for (Class<?> clazz : classesToPreload) {
                    ClassCache.getInstance().loadClass(clazz, reactorMapper);
                }
            }
            return this.reactorMapper;
        }
    }

    private ReactiveAeroMapper(@NotNull IAerospikeReactorClient reactorClient) {
        this.reactorClient = reactorClient;
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

    @Override
    public <T> Mono<T> update(@NotNull T object, String... binNames) {
        return save(null, object, RecordExistsAction.UPDATE, binNames);
    }

    @Override
    public <T> Mono<T> readFromDigest(Policy readPolicy, @NotNull Class<T> clazz, @NotNull byte[] digest) {
        return this.readFromDigest(readPolicy, clazz, digest, true);
    }

    /**
     * This method should not be used except by mappers
     */
    @Override
    public <T> Mono<T> readFromDigest(Policy readPolicy, @NotNull Class<T> clazz, @NotNull byte[] digest, boolean resolveDependencies) {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        Key key = new Key(entry.getNamespace(), digest, entry.getSetName(), null);
        return this.read(readPolicy, clazz, key, entry, resolveDependencies);
    }

    @Override
    public <T> Mono<T> readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest) {
        return this.readFromDigest(clazz, digest, true);
    }

    /**
     * This method should not be used except by mappers
     */
    @Override
    public <T> Mono<T> readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest, boolean resolveDependencies) throws AerospikeException {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        Key key = new Key(entry.getNamespace(), digest, entry.getSetName(), null);
        return this.read(null, clazz, key, entry, resolveDependencies);
    }

    @Override
    public <T> Mono<T> read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Object userKey) {
        return this.read(readPolicy, clazz, userKey, true);
    }

    /**
     * This method should not be used except by mappers
     */
    @Override
    public <T> Mono<T> read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Object userKey, boolean resolveDependencies) {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        String set = entry.getSetName();
        Key key = new Key(entry.getNamespace(), set, Value.get(entry.translateKeyToAerospikeKey(userKey)));
        return read(readPolicy, clazz, key, entry, resolveDependencies);
    }

    /**
     * Read a record from the repository and map it to an instance of the passed class.
     * @param clazz - The type of be returned.
     * @param userKey - The key of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return The returned mapped record.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    @Override
    public <T> Mono<T> read(@NotNull Class<T> clazz, @NotNull Object userKey) {
        return this.read(clazz, userKey, true);
    }

    /**
     * This method should not be used: It is used by mappers to correctly resolved dependencies. Use read(clazz, userKey) instead
     */
    @Override
    public <T> Mono<T> read(@NotNull Class<T> clazz, @NotNull Object userKey, boolean resolveDependencies) {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        String set = entry.getSetName();
        Key key = new Key(entry.getNamespace(), set, Value.get(entry.translateKeyToAerospikeKey(userKey)));
        return read(null, clazz, key, entry, resolveDependencies);
    }

    /**
     * Read a batch of records from the repository and map them to an instance of the passed class.
     * @param clazz - The type of be returned.
     * @param userKeys - The keys of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return The returned mapped records.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    @Override
    public <T> Flux<T> read(@NotNull Class<T> clazz, @NotNull Object... userKeys) {
        return this.read(null, clazz, userKeys);
    }

    /**
     * Read a batch of records from the repository and map them to an instance of the passed class.
     * @param batchPolicy A given batch policy.
     * @param clazz - The type of be returned.
     * @param userKeys - The keys of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return The returned mapped records.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    @Override
    public <T> Flux<T> read(BatchPolicy batchPolicy, @NotNull Class<T> clazz, @NotNull Object... userKeys) {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        String set = entry.getSetName();
        Key[] keys = new Key[userKeys.length];
        for (int i = 0; i < userKeys.length; i++) {
            if (userKeys[i] == null) {
                throw new AerospikeException("Cannot pass null to object " + i + " in multi-read call");
            }
            else {
                keys[i] = new Key(entry.getNamespace(), set, Value.get(entry.translateKeyToAerospikeKey(userKeys[i])));
            }
        }

        return readBatch(batchPolicy, clazz, keys, entry);
    }

    @Override
    public <T> Mono<Boolean> delete(@NotNull Class<T> clazz, @NotNull Object userKey) {
        return this.delete(null, clazz, userKey);
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
        return this.delete((WritePolicy)null, object);
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
    public <T> ReactiveVirtualList<T> asBackedList(@NotNull Object object, @NotNull String binName, Class<T> elementClazz) {
        return new ReactiveVirtualList<>(this, object, binName, elementClazz);
    }

    @Override
    public <T> ReactiveVirtualList<T> asBackedList(@NotNull Class<?> owningClazz, @NotNull Object key, @NotNull String binName, Class<T> elementClazz) {
        return new ReactiveVirtualList<>(this, owningClazz, key, binName, elementClazz);
    }

    @Override
    public <T> Mono<T> find(@NotNull Class<T> clazz, Function<T, Boolean> function) throws AerospikeException {
        /*
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);

        Statement statement = new Statement();
        statement.setNamespace(entry.getNamespace());
        statement.setSetName(entry.getSetName());

        try {
            // TODO: set the policy (If this statement is thought to be useful, which is dubious)
            Flux<KeyRecord> recordSet = reactorClient
                    .query(null, statement)
                    .next()
                    .map(x -> {
                        T result = clazz.getConstructor().newInstance();
                        entry.hydrateFromRecord(x.record, result);
                        if (!function.apply(result)) {

                        }
                    });

        } catch (ReflectiveOperationException e) {
            throw new AerospikeException(e);
        }
         */
        throw new UnsupportedOperationException("Method not supported yet.");
    }

    @Override
    public IAerospikeReactorClient getReactorClient() {
        return this.reactorClient;
    }

    @Override
    public MappingConverter getMappingConverter() {
        return this.mappingConverter;
    }

    /**
     * Return the read policy to be used for the passed class. This is a convenience method only and should rarely be needed
     * @param clazz - the class to return the read policy for.
     * @return - the appropriate read policy. If none is set, the client's readPolicyDefault is returned.
     */
    @Override
    public Policy getReadPolicy(Class<?> clazz) {
        return getPolicyByClassAndType(clazz, ClassCache.PolicyType.READ);
    }

    /**
     * Return the write policy to be used for the passed class. This is a convenience method only and should rarely be needed
     * @param clazz - the class to return the write policy for.
     * @return - the appropriate write policy. If none is set, the client's writePolicyDefault is returned.
     */
    @Override
    public WritePolicy getWritePolicy(Class<?> clazz) {
        return (WritePolicy) getPolicyByClassAndType(clazz, ClassCache.PolicyType.WRITE);
    }

    /**
     * Return the batch policy to be used for the passed class. This is a convenience method only and should rarely be needed
     * @param clazz - the class to return the batch policy for.
     * @return - the appropriate batch policy. If none is set, the client's batchPolicyDefault is returned.
     */
    @Override
    public BatchPolicy getBatchPolicy(Class<?> clazz) {
        return (BatchPolicy) getPolicyByClassAndType(clazz, ClassCache.PolicyType.BATCH);
    }

    /**
     * Return the scan policy to be used for the passed class. This is a convenience method only and should rarely be needed
     * @param clazz - the class to return the scan policy for.
     * @return - the appropriate scan policy. If none is set, the client's scanPolicyDefault is returned.
     */
    @Override
    public ScanPolicy getScanPolicy(Class<?> clazz) {
        return (ScanPolicy) getPolicyByClassAndType(clazz, ClassCache.PolicyType.SCAN);
    }

    /**
     * Return the query policy to be used for the passed class. This is a convenience method only and should rarely be needed
     * @param clazz - the class to return the query policy for.
     * @return - the appropriate query policy. If none is set, the client's queryPolicyDefault is returned.
     */
    @Override
    public Policy getQueryPolicy(Class<?> clazz) {
        return getPolicyByClassAndType(clazz, ClassCache.PolicyType.QUERY);
    }

    /**
     * Used for ObjectReferenceMapper only
     */
    public <T> T readFromDigestSync(@NotNull Class<T> clazz, @NotNull byte[] digest, boolean resolveDependencies) throws AerospikeException {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        Key key = new Key(entry.getNamespace(), digest, entry.getSetName(), null);
        return this.readSync(null, clazz, key, entry, resolveDependencies);
    }

    /**
     * Used for ObjectReferenceMapper only
     */
    public <T> T readSync(@NotNull Class<T> clazz, @NotNull Object userKey, boolean resolveDependencies) throws AerospikeException {
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        String set = entry.getSetName();
        Key key = new Key(entry.getNamespace(), set, Value.get(entry.translateKeyToAerospikeKey(userKey)));
        return readSync(null, clazz, key, entry, resolveDependencies);
    }

    /**
     * Used for ObjectReferenceMapper only
     */
    private <T> T readSync(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Key key, @NotNull ClassCacheEntry<T> entry, boolean resolveDependencies) {
        if (readPolicy == null) {
            readPolicy = entry.getReadPolicy();
        }
        Record record = reactorClient.getAerospikeClient().get(readPolicy, key);

        if (record == null) {
            return null;
        } else {
            try {
                ThreadLocalKeySaver.save(key);
                return mappingConverter.convertToObject(clazz, record, entry, resolveDependencies);
            } catch (ReflectiveOperationException e) {
                throw new AerospikeException(e);
            }
            finally {
                ThreadLocalKeySaver.clear();
            }
        }
    }

    private Policy getPolicyByClassAndType(Class<?> clazz, ClassCache.PolicyType policyType) {
        ClassCacheEntry<?> entry = ClassCache.getInstance().loadClass(clazz, this);

        switch (policyType) {
            case READ: return entry == null ? reactorClient.getReadPolicyDefault() : entry.getReadPolicy();
            case WRITE: return entry == null ? reactorClient.getWritePolicyDefault() : entry.getReadPolicy();
            case BATCH: return entry == null ? reactorClient.getBatchPolicyDefault() : entry.getReadPolicy();
            case SCAN: return entry == null ? reactorClient.getScanPolicyDefault() : entry.getReadPolicy();
            case QUERY: return entry == null ? reactorClient.getQueryPolicyDefault() : entry.getReadPolicy();
            default: throw new UnsupportedOperationException("Provided unsupported policy.");
        }
    }

    private <T> Mono<T> save(WritePolicy writePolicy, @NotNull T object, RecordExistsAction recordExistsAction, String[] binNames) {
        Class<T> clazz = (Class<T>) object.getClass();
        ClassCacheEntry<T> entry = MapperUtils.getEntryAndValidateNamespace(clazz, this);
        if (writePolicy == null) {
            writePolicy = new WritePolicy(entry.getWritePolicy());
            if (recordExistsAction != null) {
                writePolicy.recordExistsAction = recordExistsAction;
            }
        }

        String set = entry.getSetName();
        if ("".equals(set)) {
            // Use the null set
            set = null;
        }
        Integer ttl = entry.getTtl();
        Boolean sendKey = entry.getSendKey();

        if (ttl != null) {
            writePolicy.expiration = ttl;
        }
        if (sendKey != null) {
            writePolicy.sendKey = sendKey;
        }
        Key key = new Key(entry.getNamespace(), set, Value.get(entry.getKey(object)));

        Bin[] bins = entry.getBins(object, writePolicy.recordExistsAction != RecordExistsAction.REPLACE, binNames);

        return reactorClient
                .put(writePolicy, key, bins)
                .map(docKey -> object)
                .onErrorMap(this::translateError);
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

    private <T> Flux<T> readBatch(BatchPolicy batchPolicy, @NotNull Class<T> clazz, @NotNull Key[] keys, @NotNull ClassCacheEntry<T> entry) {
        if (batchPolicy == null) {
            batchPolicy = entry.getBatchPolicy();
        }

        Flux<T> results = reactorClient
                .getFlux(batchPolicy, keys)
                .filter(keyRecord -> Objects.nonNull(keyRecord.record))
                .map(keyRecord -> {
                        try {
                            ThreadLocalKeySaver.save(keyRecord.key);
                            return mappingConverter.convertToObject(clazz, keyRecord.record, entry, false);
                        } catch (ReflectiveOperationException e) {
                            throw new AerospikeException(e);
                        } finally {
                            ThreadLocalKeySaver.clear();
                        }
                });

        mappingConverter.resolveDependencies(entry);
        return results;
    }

    private Throwable translateError(Throwable e) {
        if (e instanceof AerospikeException) {
            return translateError((AerospikeException) e);
        }
        return e;
    }
}
