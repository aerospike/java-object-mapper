package com.aerospike.mapper.tools.virtuallist;

import com.aerospike.client.Key;
import com.aerospike.client.Value;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.tools.IReactiveAeroMapper;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.List;

public class ReactiveVirtualList<E> extends BaseVirtualList<E> implements IReactiveVirtualList<E> {

    private final IReactiveAeroMapper reactiveAeroMapper;

    public ReactiveVirtualList(@NotNull IReactiveAeroMapper reactiveAeroMapper, @NotNull Class<?> owningClazz,
                               @NotNull Object key, @NotNull String binName, @NotNull Class<E> clazz) {
        super(reactiveAeroMapper, null, owningClazz, key, binName, clazz);
        this.reactiveAeroMapper = reactiveAeroMapper;
    }

    public ReactiveVirtualList(@NotNull IReactiveAeroMapper reactiveAeroMapper, @NotNull Object object,
                               @NotNull String binName, @NotNull Class<E> clazz) {
        super(reactiveAeroMapper, object, null, null, binName, clazz);
        this.reactiveAeroMapper = reactiveAeroMapper;
    }

    public ReactiveVirtualList<E> changeKey(Object newKey) {
        String set = alignedSet();
        this.key = new Key(owningEntry.getNamespace(), set, Value.get(owningEntry.translateKeyToAerospikeKey(key)));
        return this;
    }

    public ReactiveMultiOperation<E> beginMultiOperation() {
        return this.beginMulti(null);
    }

    public ReactiveMultiOperation<E> beginMulti(WritePolicy writePolicy) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        return new ReactiveMultiOperation<>(writePolicy, binName, listMapper, key, virtualListInteractors, reactiveAeroMapper);
    }

    /**
     * Get items from the list matching the specified value.
     * @param value The value to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given value.
     */
    public Mono<E> getByValue(Object value, ReturnType returnResultsOfType) {
        return this.getByValue(null, value, returnResultsOfType);
    }

    /**
     * Get items from the list matching the specified value.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param value The value to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given value.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> getByValue(WritePolicy writePolicy, Object value, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByValueInteractor(value);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Get items from the list matching the specified value. If the list is mapped to a MAP in Aerospike,
     * the start value and end value will dictate the range of values to get,
     * inclusive of the start, exclusive of the end.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to get from the list.
     * <p/>
     * @param startValue Start value of the range to get.
     * @param endValue End value of the range to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given value range.
     */
    public Mono<E> getByValueRange(Object startValue, Object endValue, ReturnType returnResultsOfType) {
        return this.getByValueRange(null, startValue, endValue, returnResultsOfType);
    }

    /**
     * Get items from the list matching the specified value. If the list is mapped to a MAP in Aerospike,
     * the start value and end value will dictate the range of values to get,
     * inclusive of the start, exclusive of the end.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to get from the list.
     * <p/>
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param startValue Start value of the range to get.
     * @param endValue End value of the range to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given value range.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> getByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByValueRangeInteractor(startValue, endValue);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Get items from the list matching the specified list of values.
     * @param values The list of values to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given list of values.
     */
    public Mono<E> getByValueList(List<Object> values, ReturnType returnResultsOfType) {
        return this.getByValueList(null, values, returnResultsOfType);
    }

    /**
     * Get items from the list matching the specified list of values.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param values The list of values to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given list of values.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> getByValueList(WritePolicy writePolicy, List<Object> values, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByValueListInteractor(values);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Get items nearest to value and greater by relative rank.
     * @param value The value to base the relative rank range calculation on.
     * @param rank The relative rank.
     * @param returnResultsOfType Type to return.
     * @return A list of records that matches the given value and rank.
     */
    public Mono<E> getByValueRelativeRankRange(Object value, int rank, ReturnType returnResultsOfType) {
        return this.getByValueRelativeRankRange(null, value, rank, returnResultsOfType);
    }

    /**
     * Get items nearest to value and greater by relative rank.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param value The value to base the relative rank range calculation on.
     * @param rank The relative rank.
     * @param returnResultsOfType Type to return.
     * @return A list of records that matches the given value and rank.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> getByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByValueRelativeRankRangeInteractor(value, rank);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Get items nearest to value and greater by relative rank with a count limit.
     * @param value The value to base the relative rank range calculation on.
     * @param rank The relative rank.
     * @param count The count limit.
     * @param returnResultsOfType Type to return.
     * @return A list of records that matches the given value, rank and count.
     */
    public Mono<E> getByValueRelativeRankRange(Object value, int rank, int count, ReturnType returnResultsOfType) {
        return this.getByValueRelativeRankRange(null, value, rank, count, returnResultsOfType);
    }

    /**
     * Get items nearest to value and greater by relative rank with a count limit.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param value The value to base the relative rank range calculation on.
     * @param rank The relative rank.
     * @param count The count limit.
     * @param returnResultsOfType Type to return.
     * @return A list of records that matches the given value, rank and count.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> getByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, int count, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByValueRelativeRankRangeInteractor(value, rank, count);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Get items starting at specified index to the end of virtual list.
     * @param index The start index to get items from to the end of the virtual list.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given index.
     */
    public Mono<E> getByIndexRange(int index, ReturnType returnResultsOfType) {
        return this.getByIndexRange(null, index, returnResultsOfType);
    }

    /**
     * Get items starting at specified index to the end of virtual list.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param index The start index to get items from to the end of the virtual list.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given index.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> getByIndexRange(WritePolicy writePolicy, int index, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByIndexRangeInteractor(index);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Get "count" items starting at specified index.
     * @param index The start index to get the "count" items from.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given index and count.
     */
    public Mono<E> getByIndexRange(int index, int count, ReturnType returnResultsOfType) {
        return this.getByIndexRange(null, index, count, returnResultsOfType);
    }

    /**
     * Get "count" items starting at specified index.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param index The start index to get the "count" items from.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given index and count.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> getByIndexRange(WritePolicy writePolicy, int index, int count, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByIndexRangeInteractor(index, count);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Get items identified by rank.
     * @param rank The rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given rank.
     */
    public Mono<E> getByRank(int rank, ReturnType returnResultsOfType) {
        return this.getByRank(null, rank, returnResultsOfType);
    }

    /**
     * Get items identified by rank.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param rank The rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given rank.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> getByRank(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByRankInteractor(rank);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Get items starting at specified rank to the last ranked item.
     * @param rank The starting rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given rank.
     */
    public Mono<E> getByRankRange(int rank, ReturnType returnResultsOfType) {
        return this.getByRankRange(null, rank, returnResultsOfType);
    }

    /**
     * Get items starting at specified rank to the last ranked item.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param rank The starting rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given rank.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> getByRankRange(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByRankRangeInteractor(rank);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Get "count" items starting at specified rank to the last ranked item.
     * @param rank The starting rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given rank and count.
     */
    public Mono<E> getByRankRange(int rank, int count, ReturnType returnResultsOfType) {
        return this.getByRankRange(null, rank, count, returnResultsOfType);
    }

    /**
     * Get "count" items starting at specified rank to the last ranked item.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param rank The starting rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given rank and count.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> getByRankRange(WritePolicy writePolicy, int rank, int count, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByRankRangeInteractor(rank, count);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Get items from the list matching the specified key. If the list is mapped to a MAP in Aerospike,
     * It will get the matching key.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, it will get the matching value.
     * <p/>
     * @param key Key to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given key range.
     */
    public Mono<E> getByKey(Object key, ReturnType returnResultsOfType) {
        return getByKey(null, key, returnResultsOfType);
    }

    /**
     * Get items from the list matching the specified key. If the list is mapped to a MAP in Aerospike,
     * It will get the matching key.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, it will get the matching value.
     * <p/>
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param key Key to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given key range.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> getByKey(WritePolicy writePolicy, Object key, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByKeyInteractor(key);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, this.key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Get items from the list matching the specified key range. If the list is mapped to a MAP in Aerospike,
     * the start key and end key will dictate the range of keys to get,
     * inclusive of the start, exclusive of the end.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to get from the list.
     * <p/>
     * @param startKey Start key of the range to get.
     * @param endKey End key of the range to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given key range.
     */
    public Mono<E> getByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType) {
        return getByKeyRange(null, startKey, endKey, returnResultsOfType);
    }

    /**
     * Get items from the list matching the specified key range. If the list is mapped to a MAP in Aerospike,
     * the start key and end key will dictate the range of keys to get,
     * inclusive of the start, exclusive of the end.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to get from the list.
     * <p/>
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param startKey Start key of the range to get.
     * @param endKey End key of the range to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given key range.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> getByKeyRange(WritePolicy writePolicy, Object startKey, Object endKey, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByKeyRangeInteractor(startKey, endKey);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, this.key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Remove items from the list matching the specified key. If the list is mapped to a MAP in Aerospike,
     * the key will dictate the map key to be removed.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the given key will use as the value to remove from the list.
     * <p/>
     * @param key Key to remove.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    public Mono<E> removeByKey(Object key, ReturnType returnResultsOfType) {
        return removeByKey(null, key, returnResultsOfType);
    }

    /**
     * Remove items from the list matching the specified key. If the list is mapped to a MAP in Aerospike,
     * the key will dictate the map key to be removed.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the given key will use as the value to remove from the list.
     * <p/>
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param key Key to remove.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> removeByKey(WritePolicy writePolicy, Object key, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveKeyInteractor(key);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, this.key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Remove items identified by value and returns the removed data.
     * @param value The value to base the items to remove on.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    public Mono<E> removeByValue(Object value, ReturnType returnResultsOfType) {
        return this.removeByValue(null, value, returnResultsOfType);
    }

    /**
     * Remove items identified by value and returns the removed data.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param value The value to base the items to remove on.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> removeByValue(WritePolicy writePolicy, Object value, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByValueInteractor(value);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Remove items identified by list of values and returns the removed data.
     * @param values The list of values to base the items to remove on.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    public Mono<E> removeByValueList(List<Object> values, ReturnType returnResultsOfType) {
        return this.removeByValueList(null, values, returnResultsOfType);
    }

    /**
     * Remove items identified by list of values and returns the removed data.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param values The list of values to base the items to remove on.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> removeByValueList(WritePolicy writePolicy, List<Object> values, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByValueListInteractor(values);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Remove items from the list matching the specified value. If the list is mapped to a MAP in Aerospike,
     * the start value and end value will dictate the range of values to be removed,
     * inclusive of the start, exclusive of the end.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to removed from the list.
     * <p/>
     * @param startValue Start value of the range to remove.
     * @param endValue End value of the range to remove.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    public Mono<E> removeByValueRange(Object startValue, Object endValue, ReturnType returnResultsOfType) {
        return this.removeByValueRange(null, startValue, endValue, returnResultsOfType);
    }

    /**
     * Remove items from the list matching the specified value. If the list is mapped to a MAP in Aerospike,
     * the start value and end value will dictate the range of values to be removed,
     * inclusive of the start, exclusive of the end.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to be removed from the list.
     * <p/>
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param startValue Start value of the range to remove.
     * @param endValue End value of the range to remove.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> removeByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByValueRangeInteractor(startValue, endValue);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Remove items nearest to value and greater by relative rank.
     * @param value The value to base the items to remove on.
     * @param rank The rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    public Mono<E> removeByValueRelativeRankRange(Object value, int rank, ReturnType returnResultsOfType) {
        return this.removeByValueRelativeRankRange(null, value, rank, returnResultsOfType);
    }

    /**
     * Remove items nearest to value and greater by relative rank.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param value The value to base the items to remove on.
     * @param rank The rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> removeByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByValueRelativeRankRangeInteractor(value, rank);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Remove items nearest to value and greater by relative rank with a count limit.
     * @param value The value to base the items to remove on.
     * @param rank The rank.
     * @param count The count limit.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    public Mono<E> removeByValueRelativeRankRange(Object value, int rank, int count, ReturnType returnResultsOfType) {
        return this.removeByValueRelativeRankRange(null, value, rank, count, returnResultsOfType);
    }

    /**
     * Remove items nearest to value and greater by relative rank with a count limit.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param value The value to base the items to remove on.
     * @param rank The rank.
     * @param count The count limit.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> removeByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, int count, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByValueRelativeRankRangeInteractor(value, rank, count);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Remove item identified by index and returns removed data.
     * @param index The index to remove the item from.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    public Mono<E> removeByIndex(int index, ReturnType returnResultsOfType) {
        return this.removeByIndex(null, index, returnResultsOfType);
    }

    /**
     * Remove item identified by index and returns removed data.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param index The index to remove the item from.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> removeByIndex(WritePolicy writePolicy, int index, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByIndexInteractor(index);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Remove items starting at specified index to the end of list and returns removed data.
     * @param index The start index to remove the item from.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    public Mono<E> removeByIndexRange(int index, ReturnType returnResultsOfType) {
        return this.removeByIndexRange(null, index, returnResultsOfType);
    }

    /**
     * Remove items starting at specified index to the end of list and returns removed data.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param index The start index to remove the item from.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> removeByIndexRange(WritePolicy writePolicy, int index, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByIndexRangeInteractor(index);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Remove "count" items starting at specified index and returns removed data.
     * @param index The start index to remove the item from.
     * @param count The count limit.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    public Mono<E> removeByIndexRange(int index, int count, ReturnType returnResultsOfType) {
        return this.removeByIndexRange(null, index, count, returnResultsOfType);
    }

    /**
     * Remove "count" items starting at specified index and returns removed data.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param index The start index to remove the item from.
     * @param count The count limit.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> removeByIndexRange(WritePolicy writePolicy, int index, int count, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByIndexRangeInteractor(index, count);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Remove item identified by rank and returns removed data.
     * @param rank The rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    public Mono<E> removeByRank(int rank, ReturnType returnResultsOfType) {
        return this.removeByRank(null, rank, returnResultsOfType);
    }

    /**
     * Remove item identified by rank and returns removed data.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param rank The rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> removeByRank(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByRankInteractor(rank);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Remove items starting at specified rank to the last ranked item and returns removed data.
     * @param rank The starting rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    public Mono<E> removeByRankRange(int rank, ReturnType returnResultsOfType) {
        return this.removeByRankRange(null, rank, returnResultsOfType);
    }

    /**
     * Remove items starting at specified rank to the last ranked item and returns removed data.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param rank The starting rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> removeByRankRange(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByRankRangeInteractor(rank);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Remove "count" items starting at specified rank and returns removed data.
     * @param rank The starting rank.
     * @param count The count limit.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    public Mono<E> removeByRankRange(int rank, int count, ReturnType returnResultsOfType) {
        return this.removeByRankRange(null, rank, count, returnResultsOfType);
    }

    /**
     * Remove "count" items starting at specified rank and returns removed data.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param rank The starting rank.
     * @param count The count limit.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> removeByRankRange(WritePolicy writePolicy, int rank, int count, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByRankRangeInteractor(rank, count);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Remove items from the list matching the specified key. If the list is mapped to a MAP in Aerospike,
     * the start key and end key will dictate the range of keys to be removed,
     * inclusive of the start, exclusive of the end.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to be removed from the list.
     * <p/>
     * @param startKey Start key of the range to remove.
     * @param endKey End key of the range to remove.
     * @param returnResultsOfType Type to return.
     * @return The result of the method is a list of the records which have been removed from the database if
     * returnResults is true, null otherwise.
     */
    public Mono<E> removeByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType) {
        return this.removeByKeyRange(null, startKey, endKey, returnResultsOfType);
    }

    /**
     * Remove items from the list matching the specified key. If the list is mapped to a MAP in Aerospike,
     * the start key and end key will dictate the range of keys to be removed,
     * inclusive of the start, exclusive of the end.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to be removed from the list.
     * <p/>
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param startKey Start key of the range to remove.
     * @param endKey End key of the range to remove.
     * @param returnResultsOfType Type to return.
     * @return The result of the method is a list of the records which have been removed from the database if
     * returnResults is true, null otherwise.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> removeByKeyRange(WritePolicy writePolicy, Object startKey, Object endKey, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveKeyRangeInteractor(startKey, endKey);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Append a new element at the end of the virtual list.
     * @param element The given element to append.
     * @return The list size.
     */
    public Mono<Long> append(E element) {
        return this.append(null, element);
    }

    /**
     * Append a new element at the end of the virtual list.
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param element The given element to append.
     * @return The size of the list.
     */
    public Mono<Long> append(WritePolicy writePolicy, E element) {
        Object result = listMapper.toAerospikeInstanceFormat(element);
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, virtualListInteractors.getAppendOperation(result))
                .map(keyRecord -> keyRecord.record.getLong(binName));
    }

    /**
     * Get an element from the virtual list at a specific index.
     * @param index The index to get the item from.
     * @return The element to get from the virtual list.
     */
    public Mono<E> get(int index) {
        return get(null, index);
    }

    /**
     * Get an element from the virtual list at a specific index.
     * @param policy - The policy to use for the operate() operation.
     * @param index The index to get the item from.
     * @return The element to get from the virtual list.
     */
    @SuppressWarnings("unchecked")
    public Mono<E> get(Policy policy, int index) {
        Interactor interactor = virtualListInteractors.getByIndexInteractor(index);
        return reactiveAeroMapper.getReactorClient()
                .operate(getWritePolicy(policy), key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Get the size of the virtual list (number of elements)
     * @param policy - The policy to use for the operate() operation.
     * @return The size of the list.
     */
    public Mono<Long> size(Policy policy) {
        Interactor interactor = virtualListInteractors.getSizeInteractor();
        return reactiveAeroMapper.getReactorClient()
                .operate(getWritePolicy(policy), key, interactor.getOperation())
                .map(keyRecord -> keyRecord.record.getLong(binName));
    }

    /**
     * Remove all the items in the virtual list.
     */
    public Mono<Void> clear() {
        Interactor interactor = virtualListInteractors.getClearInteractor();
        return reactiveAeroMapper.getReactorClient()
                .operate(null, key, interactor.getOperation()).then();
    }
}
