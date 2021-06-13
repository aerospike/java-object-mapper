package com.aerospike.mapper.tools;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.tools.mappers.ListMapper;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ReactiveMultiOperation<E> {
    final List<Interactor> interactions;
    int indexToReturn = -1;
    final WritePolicy writePolicy;
    final String binName;
    final ListMapper listMapper;
    Key key;
    final VirtualListInteractors virtualListInteractors;
    final IReactiveAeroMapper reactiveAeroMapper;

    ReactiveMultiOperation(@NotNull WritePolicy writePolicy,
                           String binName,
                           ListMapper listMapper,
                           Key key,
                           VirtualListInteractors virtualListInteractors,
                           IReactiveAeroMapper reactiveAeroMapper) {
        this.interactions = new ArrayList<>();
        this.writePolicy = writePolicy;
        this.binName = binName;
        this.listMapper = listMapper;
        this.key = key;
        this.virtualListInteractors = virtualListInteractors;
        this.reactiveAeroMapper = reactiveAeroMapper;
    }

    public ReactiveMultiOperation<E> append(E item) {
        Object aerospikeItem = listMapper.toAerospikeInstanceFormat(item);
        this.interactions.add(new Interactor(virtualListInteractors.getAppendOperation(aerospikeItem)));
        return this;
    }

    public ReactiveMultiOperation<E> removeByKey(Object key) {
        this.interactions.add(virtualListInteractors.getRemoveKeyInteractor(key));
        return this;
    }

    public ReactiveMultiOperation<E> removeByKeyRange(Object startKey, Object endKey) {
        this.interactions.add(virtualListInteractors.getRemoveKeyRangeInteractor(startKey, endKey));
        return this;
    }

    public ReactiveMultiOperation<E> removeByValueRange(Object startValue, Object endValue) {
        this.interactions.add(virtualListInteractors.getRemoveValueRangeInteractor(startValue, endValue));
        return this;
    }

    public ReactiveMultiOperation<E> getByValueRange(Object startValue, Object endValue) {
        this.interactions.add(virtualListInteractors.getGetByValueRangeInteractor(startValue, endValue));
        return this;
    }

    public ReactiveMultiOperation<E> getByKeyRange(Object startKey, Object endKey) {
        this.interactions.add(virtualListInteractors.getGetByKeyRangeInteractor(startKey, endKey));
        return this;
    }

    public ReactiveMultiOperation<E> get(int index) {
        this.interactions.add(virtualListInteractors.getIndexInteractor(index));
        return this;
    }

    public ReactiveMultiOperation<E> size() {
        this.interactions.add(virtualListInteractors.getSizeInteractor());
        return this;
    }

    public ReactiveMultiOperation<E> clear() {
        this.interactions.add(virtualListInteractors.getClearInteractor());
        return this;
    }

    public ReactiveMultiOperation<E> asResult() {
        return this.asResultOfType(ReturnType.DEFAULT);
    }

    public ReactiveMultiOperation<E> asResultOfType(ReturnType type) {
        if (interactions.isEmpty()) {
            throw new AerospikeException("asResult() cannot mark an item as the function result if there are no items to process");
        }
        else if (this.indexToReturn >= 0) {
            throw new AerospikeException("asResult() can only be called once in a multi operation");
        }
        else {
            this.indexToReturn = this.interactions.size() - 1;
            this.interactions.get(indexToReturn).setNeedsResultOfType(type);
        }
        return this;
    }

    /**
     * Finish the multi operation and process it.
     * @return The multi operation result.
     */
    public <T> Mono<T> end() {
        return end(null);
    }

    /**
     * Finish the multi operation and process it.
     * @param resultType The return type for the result.
     * @return The multi operation result with the given result type.
     */
    public <T> Mono<T> end(Class<T> resultType) {
        if (this.interactions.isEmpty()) {
            return null;
        }
        this.writePolicy.respondAllOps = true;
        Operation[] operations = new Operation[this.interactions.size()];

        int listSize = this.interactions.size();
        if (this.indexToReturn < 0) {
            // Mark the last get operation to return it's value, or the last value if there are no get operations
            for (int i = listSize-1; i >= 0; i--) {
                if (!this.interactions.get(i).isWriteOperation()) {
                    this.indexToReturn = i;
                    this.interactions.get(indexToReturn).setNeedsResultOfType(ReturnType.DEFAULT);
                    break;
                }
            }
        }
        int count = 0;
        for (Interactor thisInteractor : this.interactions) {
            operations[count++] = thisInteractor.getOperation();
        }

        int finalCount = count;
        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, operations)
                .map(keyRecord -> {
                    T result;
                    if(finalCount == 1) {
                        result = (T)this.interactions.get(0).getResult(keyRecord.record.getValue(binName));
                    } else {
                        List<?> resultList = keyRecord.record.getList(binName);
                        if (indexToReturn < 0) {
                            indexToReturn = listSize-1;
                            // Determine the last GET operation
                            for (int i = listSize-1; i >= 0; i--) {
                                if (!this.interactions.get(i).isWriteOperation()) {
                                    indexToReturn = i;
                                    break;
                                }
                            }
                        }
                        result = (T)this.interactions.get(indexToReturn).getResult(resultList.get(indexToReturn));
                    }
                    if (result != null) {
                        Object object = result;
                        if (result instanceof Collection) {
                            Collection<T> collection = (Collection<T>) result;
                            object = collection.isEmpty() ? null : collection.iterator().next();
                        }
                        reactiveAeroMapper.getMappingConverter().resolveDependencies(ClassCache.getInstance().loadClass(object.getClass(), reactiveAeroMapper));
                    }
                    return result;
                });
    }
}
