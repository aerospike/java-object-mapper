package com.aerospike.mapper.tools.virtuallist;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.tools.ClassCache;
import com.aerospike.mapper.tools.IAeroMapper;
import com.aerospike.mapper.tools.mappers.ListMapper;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MultiOperation<E> {
    final List<Interactor> interactions;
    int indexToReturn = -1;
    final WritePolicy writePolicy;
    final String binName;
    final ListMapper listMapper;
    Key key;
    final VirtualListInteractors virtualListInteractors;
    final IAeroMapper mapper;

    MultiOperation(@NotNull WritePolicy writePolicy,
                   String binName,
                   ListMapper listMapper,
                   Key key,
                   VirtualListInteractors virtualListInteractors,
                   IAeroMapper mapper) {
        this.interactions = new ArrayList<>();
        this.writePolicy = writePolicy;
        this.binName = binName;
        this.listMapper = listMapper;
        this.key = key;
        this.virtualListInteractors = virtualListInteractors;
        this.mapper = mapper;
    }

    public MultiOperation<E> append(E item) {
        Object aerospikeItem = listMapper.toAerospikeInstanceFormat(item);
        this.interactions.add(new Interactor(virtualListInteractors.getAppendOperation(aerospikeItem)));
        return this;
    }

    public MultiOperation<E> removeByKey(Object key) {
        this.interactions.add(virtualListInteractors.getRemoveKeyInteractor(key));
        return this;
    }

    public MultiOperation<E> removeByKeyRange(Object startKey, Object endKey) {
        this.interactions.add(virtualListInteractors.getRemoveKeyRangeInteractor(startKey, endKey));
        return this;
    }

    public MultiOperation<E> removeByValueRange(Object startValue, Object endValue) {
        this.interactions.add(virtualListInteractors.getRemoveValueRangeInteractor(startValue, endValue));
        return this;
    }

    public MultiOperation<E> getByValueRange(Object startValue, Object endValue) {
        this.interactions.add(virtualListInteractors.getGetByValueRangeInteractor(startValue, endValue));
        return this;
    }

    public MultiOperation<E> getByKeyRange(Object startKey, Object endKey) {
        this.interactions.add(virtualListInteractors.getGetByKeyRangeInteractor(startKey, endKey));
        return this;
    }

    public MultiOperation<E> get(int index) {
        this.interactions.add(virtualListInteractors.getIndexInteractor(index));
        return this;
    }

    public MultiOperation<E> size() {
        this.interactions.add(virtualListInteractors.getSizeInteractor());
        return this;
    }

    public MultiOperation<E> clear() {
        this.interactions.add(virtualListInteractors.getClearInteractor());
        return this;
    }

    public MultiOperation<E> asResult() {
        return this.asResultOfType(ReturnType.DEFAULT);
    }

    public MultiOperation<E> asResultOfType(ReturnType type) {
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
    public Object end() {
        return end(null);
    }

    /**
     * Finish the multi operation and process it.
     * @param resultType The return type for the result.
     * @return The multi operation result with the given result type.
     */
    public <T> T end(Class<T> resultType) {
        if (interactions.isEmpty()) {
            return null;
        }
        writePolicy.respondAllOps = true;
        Operation[] operations = new Operation[interactions.size()];

        int listSize = interactions.size();
        if (this.indexToReturn < 0) {
            // Mark the last get operation to return it's value, or the last value if there are no get operations
            for (int i = listSize-1; i >= 0; i--) {
                if (!interactions.get(i).isWriteOperation()) {
                    indexToReturn = i;
                    interactions.get(indexToReturn).setNeedsResultOfType(ReturnType.DEFAULT);
                    break;
                }
            }
        }
        int count = 0;
        for (Interactor thisInteractor : interactions) {
            operations[count++] = thisInteractor.getOperation();
        }

        Record record = mapper.asMapper().getClient().operate(writePolicy, key, operations);

        T result;
        if (count == 1) {
            Object resultObj = record.getValue(binName);
            result = (T)interactions.get(0).getResult(resultObj);
        }
        else {
            List<?> resultList = record.getList(binName);
            if (indexToReturn < 0) {
                indexToReturn = listSize-1;
                // Determine the last GET operation
                for (int i = listSize-1; i >= 0; i--) {
                    if (!interactions.get(i).isWriteOperation()) {
                        indexToReturn = i;
                        break;
                    }
                }
            }
            result = (T)interactions.get(indexToReturn).getResult(resultList.get(indexToReturn));
        }
        if (result != null) {
            Object object = result;
            if (result instanceof Collection) {
                Collection<T> collection = (Collection<T>)result;
                object = collection.isEmpty() ? null : collection.iterator().next();
            }
            mapper.getMappingConverter().resolveDependencies(ClassCache.getInstance().loadClass(object.getClass(), mapper));
        }
        return result;
    }
}
