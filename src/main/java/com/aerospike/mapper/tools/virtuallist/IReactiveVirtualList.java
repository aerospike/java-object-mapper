package com.aerospike.mapper.tools.virtuallist;

import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import reactor.core.publisher.Mono;

public interface IReactiveVirtualList<E> {

    Mono<E> getByValueRange(Object startValue, Object endValue, ReturnType returnResultsOfType);

    Mono<E> getByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType);

    Mono<E> getByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType);

    Mono<E> getByKeyRange(WritePolicy writePolicy, Object startKey, Object endKey, ReturnType returnResultsOfType);

    Mono<E> removeByKey(Object key, ReturnType returnResultsOfType);

    Mono<E> removeByKey(WritePolicy writePolicy, Object key, ReturnType returnResultsOfType);

    Mono<E> removeByValueRange(Object startValue, Object endValue, ReturnType returnResultsOfType);

    Mono<E> removeByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType);

    Mono<E> removeByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType);

    Mono<E> removeByKeyRange(WritePolicy writePolicy, Object startKey, Object endKey, ReturnType returnResultsOfType);

    Mono<Long> append(E element);

    Mono<Long> append(WritePolicy writePolicy, E element);

    Mono<E> get(int index);

    Mono<E> get(Policy policy, int index);

    Mono<Long> size(Policy policy);

    Mono<Void> clear();
}
