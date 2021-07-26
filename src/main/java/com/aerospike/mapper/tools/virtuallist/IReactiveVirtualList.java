package com.aerospike.mapper.tools.virtuallist;

import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IReactiveVirtualList<E> {

    Mono<E> getByValue(Object value, ReturnType returnResultsOfType);

    Mono<E> getByValue(WritePolicy writePolicy, Object value, ReturnType returnResultsOfType);

    Mono<E> getByValueRange(Object startValue, Object endValue, ReturnType returnResultsOfType);

    Mono<E> getByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType);

    Mono<E> getByValueList(List<Object> values, ReturnType returnResultsOfType);

    Mono<E> getByValueList(WritePolicy writePolicy, List<Object> values, ReturnType returnResultsOfType);

    Mono<E> getByValueRelativeRankRange(Object value, int rank, ReturnType returnResultsOfType);

    Mono<E> getByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, ReturnType returnResultsOfType);

    Mono<E> getByValueRelativeRankRange(Object value, int rank, int count, ReturnType returnResultsOfType);

    Mono<E> getByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, int count, ReturnType returnResultsOfType);

    Mono<E> getByIndexRange(int index, ReturnType returnResultsOfType);

    Mono<E> getByIndexRange(WritePolicy writePolicy, int index, ReturnType returnResultsOfType);

    Mono<E> getByIndexRange(int index, int count, ReturnType returnResultsOfType);

    Mono<E> getByIndexRange(WritePolicy writePolicy, int index, int count, ReturnType returnResultsOfType);

    Mono<E> getByRank(int rank, ReturnType returnResultsOfType);

    Mono<E> getByRank(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType);

    Mono<E> getByRankRange(int rank, ReturnType returnResultsOfType);

    Mono<E> getByRankRange(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType);

    Mono<E> getByRankRange(int rank, int count, ReturnType returnResultsOfType);

    Mono<E> getByRankRange(WritePolicy writePolicy, int rank, int count, ReturnType returnResultsOfType);

    Mono<E> getByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType);

    Mono<E> getByKeyRange(WritePolicy writePolicy, Object startKey, Object endKey, ReturnType returnResultsOfType);

    Mono<E> removeByKey(Object key, ReturnType returnResultsOfType);

    Mono<E> removeByKey(WritePolicy writePolicy, Object key, ReturnType returnResultsOfType);

    Mono<E> removeByValue(Object value, ReturnType returnResultsOfType);

    Mono<E> removeByValue(WritePolicy writePolicy, Object value, ReturnType returnResultsOfType);

    Mono<E> removeByValueList(List<Object> values, ReturnType returnResultsOfType);

    Mono<E> removeByValueList(WritePolicy writePolicy, List<Object> values, ReturnType returnResultsOfType);

    Mono<E> removeByValueRange(Object startValue, Object endValue, ReturnType returnResultsOfType);

    Mono<E> removeByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType);

    Mono<E> removeByValueRelativeRankRange(Object value, int rank, ReturnType returnResultsOfType);

    Mono<E> removeByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, ReturnType returnResultsOfType);

    Mono<E> removeByValueRelativeRankRange(Object value, int rank, int count, ReturnType returnResultsOfType);

    Mono<E> removeByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, int count, ReturnType returnResultsOfType);

    Mono<E> removeByIndex(int index, ReturnType returnResultsOfType);

    Mono<E> removeByIndex(WritePolicy writePolicy, int index, ReturnType returnResultsOfType);

    Mono<E> removeByIndexRange(int index, ReturnType returnResultsOfType);

    Mono<E> removeByIndexRange(WritePolicy writePolicy, int index, ReturnType returnResultsOfType);

    Mono<E> removeByIndexRange(int index, int count, ReturnType returnResultsOfType);

    Mono<E> removeByIndexRange(WritePolicy writePolicy, int index, int count, ReturnType returnResultsOfType);

    Mono<E> removeByRank(int rank, ReturnType returnResultsOfType);

    Mono<E> removeByRank(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType);

    Mono<E> removeByRankRange(int rank, ReturnType returnResultsOfType);

    Mono<E> removeByRankRange(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType);

    Mono<E> removeByRankRange(int rank, int count, ReturnType returnResultsOfType);

    Mono<E> removeByRankRange(WritePolicy writePolicy, int rank, int count, ReturnType returnResultsOfType);

    Mono<E> removeByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType);

    Mono<E> removeByKeyRange(WritePolicy writePolicy, Object startKey, Object endKey, ReturnType returnResultsOfType);

    Mono<Long> append(E element);

    Mono<Long> append(WritePolicy writePolicy, E element);

    Mono<E> get(int index);

    Mono<E> get(Policy policy, int index);

    Mono<Long> size(Policy policy);

    Mono<Void> clear();
}
