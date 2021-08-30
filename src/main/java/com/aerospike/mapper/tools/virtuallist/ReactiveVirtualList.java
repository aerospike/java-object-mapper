package com.aerospike.mapper.tools.virtuallist;

import com.aerospike.client.Key;
import com.aerospike.client.Value;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.KeyRecord;
import com.aerospike.mapper.tools.ClassCache;
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

    @Override
    public Mono<E> getByValue(Object value, ReturnType returnResultsOfType) {
        return this.getByValue(null, value, returnResultsOfType);
    }

    @Override
    public Mono<E> getByValue(WritePolicy writePolicy, Object value, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByValueInteractor(value);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> getByValueRange(Object startValue, Object endValue, ReturnType returnResultsOfType) {
        return this.getByValueRange(null, startValue, endValue, returnResultsOfType);
    }

    @Override
    public Mono<E> getByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByValueRangeInteractor(startValue, endValue);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> getByValueList(List<Object> values, ReturnType returnResultsOfType) {
        return this.getByValueList(null, values, returnResultsOfType);
    }

    @Override
    public Mono<E> getByValueList(WritePolicy writePolicy, List<Object> values, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByValueListInteractor(values);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> getByValueRelativeRankRange(Object value, int rank, ReturnType returnResultsOfType) {
        return this.getByValueRelativeRankRange(null, value, rank, returnResultsOfType);
    }

    @Override
    public Mono<E> getByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByValueRelativeRankRangeInteractor(value, rank);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> getByValueRelativeRankRange(Object value, int rank, int count, ReturnType returnResultsOfType) {
        return this.getByValueRelativeRankRange(null, value, rank, count, returnResultsOfType);
    }

    @Override
    public Mono<E> getByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, int count, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByValueRelativeRankRangeInteractor(value, rank, count);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> getByIndexRange(int index, ReturnType returnResultsOfType) {
        return this.getByIndexRange(null, index, returnResultsOfType);
    }

    @Override
    public Mono<E> getByIndexRange(WritePolicy writePolicy, int index, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByIndexRangeInteractor(index);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> getByIndexRange(int index, int count, ReturnType returnResultsOfType) {
        return this.getByIndexRange(null, index, count, returnResultsOfType);
    }

    @Override
    public Mono<E> getByIndexRange(WritePolicy writePolicy, int index, int count, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByIndexRangeInteractor(index, count);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> getByRank(int rank, ReturnType returnResultsOfType) {
        return this.getByRank(null, rank, returnResultsOfType);
    }

    @Override
    public Mono<E> getByRank(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByRankInteractor(rank);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> getByRankRange(int rank, ReturnType returnResultsOfType) {
        return this.getByRankRange(null, rank, returnResultsOfType);
    }

    @Override
    public Mono<E> getByRankRange(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByRankRangeInteractor(rank);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> getByRankRange(int rank, int count, ReturnType returnResultsOfType) {
        return this.getByRankRange(null, rank, count, returnResultsOfType);
    }

    @Override
    public Mono<E> getByRankRange(WritePolicy writePolicy, int rank, int count, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByRankRangeInteractor(rank, count);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> getByKey(Object key, ReturnType returnResultsOfType) {
        return getByKey(null, key, returnResultsOfType);
    }

    @Override
    public Mono<E> getByKey(WritePolicy writePolicy, Object key, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByKeyInteractor(key);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, this.key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> getByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType) {
        return getByKeyRange(null, startKey, endKey, returnResultsOfType);
    }

    @Override
    public Mono<E> getByKeyRange(WritePolicy writePolicy, Object startKey, Object endKey, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getGetByKeyRangeInteractor(startKey, endKey);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, this.key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> removeByKey(Object key, ReturnType returnResultsOfType) {
        return removeByKey(null, key, returnResultsOfType);
    }

    @Override
    public Mono<E> removeByKey(WritePolicy writePolicy, Object key, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveKeyInteractor(key);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, this.key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> removeByValue(Object value, ReturnType returnResultsOfType) {
        return this.removeByValue(null, value, returnResultsOfType);
    }

    @Override
    public Mono<E> removeByValue(WritePolicy writePolicy, Object value, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByValueInteractor(value);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> removeByValueList(List<Object> values, ReturnType returnResultsOfType) {
        return this.removeByValueList(null, values, returnResultsOfType);
    }

    @Override
    public Mono<E> removeByValueList(WritePolicy writePolicy, List<Object> values, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByValueListInteractor(values);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> removeByValueRange(Object startValue, Object endValue, ReturnType returnResultsOfType) {
        return this.removeByValueRange(null, startValue, endValue, returnResultsOfType);
    }

    @Override
    public Mono<E> removeByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByValueRangeInteractor(startValue, endValue);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> removeByValueRelativeRankRange(Object value, int rank, ReturnType returnResultsOfType) {
        return this.removeByValueRelativeRankRange(null, value, rank, returnResultsOfType);
    }

    @Override
    public Mono<E> removeByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByValueRelativeRankRangeInteractor(value, rank);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> removeByValueRelativeRankRange(Object value, int rank, int count, ReturnType returnResultsOfType) {
        return this.removeByValueRelativeRankRange(null, value, rank, count, returnResultsOfType);
    }

    @Override
    public Mono<E> removeByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, int count, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByValueRelativeRankRangeInteractor(value, rank, count);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> removeByIndex(int index, ReturnType returnResultsOfType) {
        return this.removeByIndex(null, index, returnResultsOfType);
    }

    @Override
    public Mono<E> removeByIndex(WritePolicy writePolicy, int index, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByIndexInteractor(index);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> removeByIndexRange(int index, ReturnType returnResultsOfType) {
        return this.removeByIndexRange(null, index, returnResultsOfType);
    }

    @Override
    public Mono<E> removeByIndexRange(WritePolicy writePolicy, int index, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByIndexRangeInteractor(index);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> removeByIndexRange(int index, int count, ReturnType returnResultsOfType) {
        return this.removeByIndexRange(null, index, count, returnResultsOfType);
    }

    @Override
    public Mono<E> removeByIndexRange(WritePolicy writePolicy, int index, int count, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByIndexRangeInteractor(index, count);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> removeByRank(int rank, ReturnType returnResultsOfType) {
        return this.removeByRank(null, rank, returnResultsOfType);
    }

    @Override
    public Mono<E> removeByRank(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByRankInteractor(rank);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> removeByRankRange(int rank, ReturnType returnResultsOfType) {
        return this.removeByRankRange(null, rank, returnResultsOfType);
    }

    @Override
    public Mono<E> removeByRankRange(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByRankRangeInteractor(rank);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> removeByRankRange(int rank, int count, ReturnType returnResultsOfType) {
        return this.removeByRankRange(null, rank, count, returnResultsOfType);
    }

    @Override
    public Mono<E> removeByRankRange(WritePolicy writePolicy, int rank, int count, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveByRankRangeInteractor(rank, count);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<E> removeByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType) {
        return this.removeByKeyRange(null, startKey, endKey, returnResultsOfType);
    }

    @Override
    public Mono<E> removeByKeyRange(WritePolicy writePolicy, Object startKey, Object endKey, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveKeyRangeInteractor(startKey, endKey);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<Long> append(E element) {
        return this.append(null, element);
    }

    @Override
    public Mono<Long> append(WritePolicy writePolicy, E element) {
        Object result = listMapper.toAerospikeInstanceFormat(element);
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, virtualListInteractors.getAppendOperation(result))
                .map(keyRecord -> keyRecord == null ? -1L : keyRecord.record.getLong(binName));
    }

    @Override
    public Mono<E> get(int index) {
        return get(null, index);
    }

    @Override
    public Mono<E> get(Policy policy, int index) {
        Interactor interactor = virtualListInteractors.getByIndexInteractor(index);
        return reactiveAeroMapper.getReactorClient()
                .operate(getWritePolicy(policy), key, interactor.getOperation())
                .map(keyRecord -> getResultsWithDependencies(keyRecord, interactor));
    }

    @Override
    public Mono<Long> size(Policy policy) {
        Interactor interactor = virtualListInteractors.getSizeInteractor();
        return reactiveAeroMapper.getReactorClient()
                .operate(getWritePolicy(policy), key, interactor.getOperation())
                .map(keyRecord -> keyRecord == null ? -1L : keyRecord.record.getLong(binName));
    }

    @Override
    public Mono<Void> clear() {
        Interactor interactor = virtualListInteractors.getClearInteractor();
        return reactiveAeroMapper.getReactorClient()
                .operate(null, key, interactor.getOperation()).then();
    }

    @SuppressWarnings("unchecked")
    private E getResultsWithDependencies(KeyRecord keyRecord, Interactor interactor) {
        E result = keyRecord == null ? null : (E) interactor.getResult(keyRecord.record.getList(binName));
        if (result != null) {
            reactiveAeroMapper.getMappingConverter().resolveDependencies(ClassCache.getInstance().loadClass(result.getClass(), reactiveAeroMapper));
        }
        return result;
    }
}
