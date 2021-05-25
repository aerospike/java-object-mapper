package com.aerospike.mapper.tools;

import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.reactor.IAerospikeReactorClient;
import com.aerospike.mapper.tools.converters.MappingConverter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.function.Function;

public interface IReactiveAeroMapper extends IBaseAeroMapper {

    <T> Flux<T> save(@NotNull T ... objects);

    <T> Mono<T> save(@NotNull T object, String ...binNames);

    <T> Mono<T> save(@NotNull WritePolicy writePolicy, @NotNull T object, String ...binNames);

    <T> Mono<T> update(@NotNull T object, String ... binNames);

    <T> Mono<T> readFromDigest(Policy readPolicy, @NotNull Class<T> clazz, @NotNull byte[] digest);

    /**
     * This method should not be used except by mappers
     */
    <T> Mono<T> readFromDigest(Policy readPolicy, @NotNull Class<T> clazz, @NotNull byte[] digest, boolean resolveDependencies);

    <T> Mono<T> readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest);

    /**
     * This method should not be used except by mappers
     */
    <T> Mono<T> readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest, boolean resolveDependencies);

    <T> Mono<T> read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Object userKey);

    /**
     * This method should not be used except by mappers
     */
    <T> Mono<T> read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Object userKey, boolean resolveDependencies);

    <T> Mono<T> read(@NotNull Class<T> clazz, @NotNull Object userKey);

    <T> Mono<T> read(@NotNull Class<T> clazz, @NotNull Object userKey, boolean resolveDependencies);

    <T> Flux<T> read(@NotNull Class<T> clazz, @NotNull Object ... userKeys);

    <T> Flux<T> read(BatchPolicy batchPolicy, @NotNull Class<T> clazz, @NotNull Object ... userKeys);

    <T> Mono<Boolean> delete(@NotNull Class<T> clazz, @NotNull Object userKey);

    <T> Mono<Boolean> delete(WritePolicy writePolicy, @NotNull Class<T> clazz, @NotNull Object userKey);

    Mono<Boolean> delete(@NotNull Object object);

    Mono<Boolean> delete(WritePolicy writePolicy, @NotNull Object object);

    <T> ReactiveVirtualList<T> asBackedList(@NotNull Object object, @NotNull String binName, Class<T> elementClazz);

    <T> ReactiveVirtualList<T> asBackedList(@NotNull Class<?> owningClazz, @NotNull Object key, @NotNull String binName, Class<T> elementClazz);

    <T> Mono<Void> find(@NotNull Class<T> clazz, Function<T, Boolean> function);

    IAerospikeReactorClient getReactorClient();

    MappingConverter getMappingConverter();
}
