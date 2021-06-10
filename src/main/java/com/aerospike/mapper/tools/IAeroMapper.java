package com.aerospike.mapper.tools;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;

import javax.validation.constraints.NotNull;
import java.util.function.Function;

public interface IAeroMapper extends IBaseAeroMapper {

    void save(@NotNull Object... objects);

    void save(@NotNull Object object, String... binNames);

    void save(@NotNull WritePolicy writePolicy, @NotNull Object object, String... binNames);

    void update(@NotNull Object object, String... binNames);

    <T> T readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest);

    <T> T readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest, boolean resolveDependencies);

    <T> T readFromDigest(Policy readPolicy, @NotNull Class<T> clazz, @NotNull byte[] digest);

    <T> T readFromDigest(Policy readPolicy, @NotNull Class<T> clazz, @NotNull byte[] digest, boolean resolveDependencies);

    <T> T read(@NotNull Class<T> clazz, @NotNull Object userKey);

    <T> T read(@NotNull Class<T> clazz, @NotNull Object userKey, boolean resolveDependencies);

    <T> T read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Object userKey);

    <T> T read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Object userKey, boolean resolveDependencies);

    <T> T[] read(@NotNull Class<T> clazz, @NotNull Object... userKeys);

    <T> T[] read(BatchPolicy batchPolicy, @NotNull Class<T> clazz, @NotNull Object... userKeys);

    <T> boolean delete(@NotNull Class<T> clazz, @NotNull Object userKey);

    <T> boolean delete(WritePolicy writePolicy, @NotNull Class<T> clazz, @NotNull Object userKey);

    boolean delete(@NotNull Object object);

    boolean delete(WritePolicy writePolicy, @NotNull Object object);

    <T> VirtualList<T> asBackedList(@NotNull Object object, @NotNull String binName, Class<T> elementClazz);

    <T> VirtualList<T> asBackedList(@NotNull Class<?> owningClazz, @NotNull Object key, @NotNull String binName, Class<T> elementClazz);

    <T> void find(@NotNull Class<T> clazz, Function<T, Boolean> function);

    IAerospikeClient getClient();
}
