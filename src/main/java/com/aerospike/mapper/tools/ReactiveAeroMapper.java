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
    private final IAeroMapper aeroMapper;
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
            public ReactiveAeroMapper.Builder forClasses(Class<?>... classes) {
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
        this.aeroMapper = new AeroMapper.Builder(reactorClient.getAerospikeClient()).build();
        this.mappingConverter = new MappingConverter(this, reactorClient.getAerospikeClient());
    }

    /**
     * Save each object in the database. This method will perform a REPLACE on the existing record so any existing
     * data will be overwritten by the data in the passed object. This is a convenience method for
     * <pre>
     * save(A);
     * save(B);
     * save(C);
     * </pre>
     * Not that no transactionality is implied by this method -- if any of the save methods fail, the exception will be
     * thrown without trying the other objects, nor attempting to roll back previously saved objects
     * @param objects One or two objects to save.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    @Override
    public <T> Flux<T> save(@NotNull T... objects) {
        return Flux.fromStream(Arrays.stream(objects))
                .flatMap(this::save);
    }

    /**
     * Save an object in the database. This method will perform a REPLACE on the existing record so any existing
     * data will be overwritten by the data in the passed object
     * @param object The object to save.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    @Override
    public <T> Mono<T> save(@NotNull T object, String... binNames) {
        return save(null, object, RecordExistsAction.REPLACE, binNames);
    }

    /**
     * Save an object in the database with the given WritePolicy. This write policy will override any other set writePolicy so
     * is effectively an upsert operation
     * @param writePolicy The write policy for the save operation.
     * @param object The object to save.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    @Override
    public <T> Mono<T> save(@NotNull WritePolicy writePolicy, @NotNull T object, String... binNames) {
        return save(writePolicy, object, null, binNames);
    }

    /**
     * Updates the object in the database, merging the record with the existing record. This uses the RecordExistsAction
     * of UPDATE. If bins are specified, only bins with the passed names will be updated (or all of them if null is passed)
     * @param object The object to update.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    @Override
    public <T> Mono<T> update(@NotNull T object, String... binNames) {
        return save(null, object, RecordExistsAction.UPDATE, binNames);
    }

    /**
     * Read a record from the repository and map it to an instance of the passed class, by providing a digest.
     * @param clazz - The type of the record.
     * @param digest - The Aerospike digest (Unique server hash value generated from set name and user key).
     * @return The returned mapped record.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
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

    /**
     * Read a record from the repository and map it to an instance of the passed class, by providing a digest.
     * @param readPolicy - The read policy for the read operation.
     * @param clazz - The type of the record.
     * @param digest - The Aerospike digest (Unique server hash value generated from set name and user key).
     * @return The returned mapped record.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
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

    /**
     * Read a record from the repository and map it to an instance of the passed class.
     * @param clazz - The type of the record.
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
     * Read a record from the repository and map it to an instance of the passed class.
     * @param readPolicy - The read policy for the read operation.
     * @param clazz - The type of the record.
     * @param userKey - The key of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return The returned mapped record.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
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
     * Read a batch of records from the repository and map them to an instance of the passed class.
     * @param clazz - The type of the record.
     * @param userKeys - The keys of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return The returned mapped records.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    @Override
    public <T> Flux<T> read(@NotNull Class<T> clazz, @NotNull Object... userKeys) {
        throw new UnsupportedOperationException("Batch reading is not supported in ReactiveAeroMapper yet.");
        //return this.read(null, clazz, userKeys);
    }

    /**
     * Read a batch of records from the repository and map them to an instance of the passed class.
     * @param batchPolicy A given batch policy.
     * @param clazz - The type of the record.
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

    /**
     * Delete a record by specifying a class and a user key.
     * @param clazz - The type of the record.
     * @param userKey - The key of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return whether record existed on server before deletion
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    @Override
    public <T> Mono<Boolean> delete(@NotNull Class<T> clazz, @NotNull Object userKey) {
        return this.delete(null, clazz, userKey);
    }

    /**
     * Delete a record by specifying a write policy, a class and a user key.
     * @param writePolicy - The write policy for the delete operation.
     * @param clazz - The type of the record.
     * @param userKey - The key of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return whether record existed on server before deletion
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
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

    /**
     * Delete a record by specifying an object.
     * @param object The object to delete.
     * @return whether record existed on server before deletion
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    @Override
    public Mono<Boolean> delete(@NotNull Object object) {
        return this.delete((WritePolicy)null, object);
    }

    /**
     * Delete a record by specifying a write policy and an object.
     * @param writePolicy - The write policy for the delete operation.
     * @param object The object to delete.
     * @return whether record existed on server before deletion
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
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

    /**
     * Create a reactive virtual list against an attribute on a class. The list does all operations to the database and does not affect the underlying
     * class, and is useful for situation when operations are needed to affect the database without having to return all the elements on the
     * list each time.
     * <p/>
     * For example, consider a set of transactions associated with a credit card. Common operations might be
     * <ul>
     * 	<li>Return the last N transactions </li>
     * 	<li>insert a new transaction into the list</li>
     * </ul>
     * These operation can all be done without having the full set of transactions
     * @param <T> the type of the elements in the list.
     * @param object The object that will use as a base for the virtual list.
     * @param binName The Aerospike bin name.
     * @param elementClazz The class of the elements in the list.
     * @return A reactive virtual list.
     */
    @Override
    public <T> ReactiveVirtualList<T> asBackedList(@NotNull Object object, @NotNull String binName, Class<T> elementClazz) {
        return new ReactiveVirtualList<>(this, object, binName, elementClazz);
    }

    /**
     * Create a reactive virtual list against an attribute on a class. The list does all operations to the database and does not affect the underlying
     * class, and is useful for situation when operations are needed to affect the database without having to return all the elements on the
     * list each time.
     * <p/>
     * Note that the object being mapped does not need to actually exist in this case. The owning class is used purely for the definitions
     * of how to map the list elements (are they to be mapped in the database as a list or a map, is each element a list or a map, etc), as
     * well as using the namespace / set definition for the location to map into the database.  The
     * passed key is used to map the object to the database.
     * <p/>
     * For example, consider a set of transactions associated with a credit card. Common operations might be
     * <ul>
     * 	<li>Return the last N transactions </li>
     * 	<li>insert a new transaction into the list</li>
     * </ul>
     * These operation can all be done without having the full set of transactions
     * @param <T> the type of the elements in the list.
     * @param owningClazz Used for the definitions of how to map the list elements.
     * @param key The key to map the object to the database.
     * @param binName The Aerospike bin name.
     * @param elementClazz The class of the elements in the list.
     * @return A reactive virtual list.
     */
    @Override
    public <T> ReactiveVirtualList<T> asBackedList(@NotNull Class<?> owningClazz, @NotNull Object key, @NotNull String binName, Class<T> elementClazz) {
        return new ReactiveVirtualList<>(this, owningClazz, key, binName, elementClazz);
    }

    /**
     * Find a record by specifying a class and a Boolean function.
     * @param clazz - The type of the record.
     * @param function a Boolean function.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    @Override
    public <T> Mono<Void> find(@NotNull Class<T> clazz, Function<T, Boolean> function) throws AerospikeException {
        return Mono.fromCallable(() -> {
            asMapper().find(clazz, function);
            return null;
        });
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

    @Override
    public IAeroMapper asMapper() {
        return aeroMapper;
    }

    private Policy getPolicyByClassAndType(Class<?> clazz, ClassCache.PolicyType policyType) {
        ClassCacheEntry<?> entry = ClassCache.getInstance().loadClass(clazz, this);

        switch (policyType) {
            case READ: return entry == null ? reactorClient.getReadPolicyDefault() : entry.getReadPolicy();
            case WRITE: return entry == null ? reactorClient.getWritePolicyDefault() : entry.getWritePolicy();
            case BATCH: return entry == null ? reactorClient.getBatchPolicyDefault() : entry.getBatchPolicy();
            case SCAN: return entry == null ? reactorClient.getScanPolicyDefault() : entry.getScanPolicy();
            case QUERY: return entry == null ? reactorClient.getQueryPolicyDefault() : entry.getQueryPolicy();
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
