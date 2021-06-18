package com.aerospike.mapper.tools.virtuallist;

import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;

import java.util.List;

public interface IVirtualList<E> {

    List<E> getByValueRange(Object startValue, Object endValue, ReturnType returnResultsOfType);

    List<E> getByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType);

    List<E> getByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType);

    List<E> getByKeyRange(WritePolicy writePolicy, Object startKey, Object endKey, ReturnType returnResultsOfType);

    List<E> removeByKey(Object key, ReturnType returnResultsOfType);

    List<E> removeByKey(WritePolicy writePolicy, Object key, ReturnType returnResultsOfType);

    List<E> removeByValueRange(Object startValue, Object endValue, ReturnType returnResultsOfType);

    List<E> removeByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType);

    List<E> removeByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType);

    List<E> removeByKeyRange(WritePolicy writePolicy, Object startKey, Object endKey, ReturnType returnResultsOfType);

    long append(E element);

    long append(WritePolicy writePolicy, E element);

    E get(int index);

    E get(Policy policy, int index);

    long size(Policy policy);

    void clear();
}
