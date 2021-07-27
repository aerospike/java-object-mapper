package com.aerospike.mapper.tools.virtuallist;

import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;

import java.util.List;

public interface IVirtualList<E> {

    List<E> getByValue(Object value, ReturnType returnResultsOfType);

    List<E> getByValue(WritePolicy writePolicy, Object value, ReturnType returnResultsOfType);

    List<E> getByValueRange(Object startValue, Object endValue, ReturnType returnResultsOfType);

    List<E> getByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType);

    List<E> getByValueList(List<Object> values, ReturnType returnResultsOfType);

    List<E> getByValueList(WritePolicy writePolicy, List<Object> values, ReturnType returnResultsOfType);

    List<E> getByValueRelativeRankRange(Object value, int rank, ReturnType returnResultsOfType);

    List<E> getByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, ReturnType returnResultsOfType);

    List<E> getByValueRelativeRankRange(Object value, int rank, int count, ReturnType returnResultsOfType);

    List<E> getByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, int count, ReturnType returnResultsOfType);

    List<E> getByIndexRange(int index, ReturnType returnResultsOfType);

    List<E> getByIndexRange(WritePolicy writePolicy, int index, ReturnType returnResultsOfType);

    List<E> getByIndexRange(int index, int count, ReturnType returnResultsOfType);

    List<E> getByIndexRange(WritePolicy writePolicy, int index, int count, ReturnType returnResultsOfType);

    List<E> getByRank(int rank, ReturnType returnResultsOfType);

    List<E> getByRank(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType);

    List<E> getByRankRange(int rank, ReturnType returnResultsOfType);

    List<E> getByRankRange(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType);

    List<E> getByRankRange(int rank, int count, ReturnType returnResultsOfType);

    List<E> getByRankRange(WritePolicy writePolicy, int rank, int count, ReturnType returnResultsOfType);

    List<E> getByKey(Object key, ReturnType returnResultsOfType);

    List<E> getByKey(WritePolicy writePolicy, Object key, ReturnType returnResultsOfType);

    List<E> getByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType);

    List<E> getByKeyRange(WritePolicy writePolicy, Object startKey, Object endKey, ReturnType returnResultsOfType);

    List<E> removeByKey(Object key, ReturnType returnResultsOfType);

    List<E> removeByKey(WritePolicy writePolicy, Object key, ReturnType returnResultsOfType);

    List<E> removeByValue(Object value, ReturnType returnResultsOfType);

    List<E> removeByValue(WritePolicy writePolicy, Object value, ReturnType returnResultsOfType);

    List<E> removeByValueList(List<Object> values, ReturnType returnResultsOfType);

    List<E> removeByValueList(WritePolicy writePolicy, List<Object> values, ReturnType returnResultsOfType);

    List<E> removeByValueRange(Object startValue, Object endValue, ReturnType returnResultsOfType);

    List<E> removeByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType);

    List<E> removeByValueRelativeRankRange(Object value, int rank, ReturnType returnResultsOfType);

    List<E> removeByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, ReturnType returnResultsOfType);

    List<E> removeByValueRelativeRankRange(Object value, int rank, int count, ReturnType returnResultsOfType);

    List<E> removeByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, int count, ReturnType returnResultsOfType);

    List<E> removeByIndex(int index, ReturnType returnResultsOfType);

    List<E> removeByIndex(WritePolicy writePolicy, int index, ReturnType returnResultsOfType);

    List<E> removeByIndexRange(int index, ReturnType returnResultsOfType);

    List<E> removeByIndexRange(WritePolicy writePolicy, int index, ReturnType returnResultsOfType);

    List<E> removeByIndexRange(int index, int count, ReturnType returnResultsOfType);

    List<E> removeByIndexRange(WritePolicy writePolicy, int index, int count, ReturnType returnResultsOfType);

    List<E> removeByRank(int rank, ReturnType returnResultsOfType);

    List<E> removeByRank(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType);

    List<E> removeByRankRange(int rank, ReturnType returnResultsOfType);

    List<E> removeByRankRange(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType);

    List<E> removeByRankRange(int rank, int count, ReturnType returnResultsOfType);

    List<E> removeByRankRange(WritePolicy writePolicy, int rank, int count, ReturnType returnResultsOfType);

    List<E> removeByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType);

    List<E> removeByKeyRange(WritePolicy writePolicy, Object startKey, Object endKey, ReturnType returnResultsOfType);

    long append(E element);

    long append(WritePolicy writePolicy, E element);

    E get(int index);

    E get(Policy policy, int index);

    long size(Policy policy);

    void clear();
}
