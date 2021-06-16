package com.aerospike.mapper.tools.virtuallist;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Value;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.tools.*;
import com.aerospike.mapper.tools.mappers.ListMapper;
import com.aerospike.mapper.tools.utils.TypeUtils;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ReactiveVirtualList<E> {
    private final IReactiveAeroMapper reactiveAeroMapper;
    private final ClassCacheEntry<?> owningEntry;
    private final String binName;
    private final ListMapper listMapper;
    private Key key;
    private final VirtualListInteractors virtualListInteractors;

    public ReactiveVirtualList(@NotNull IReactiveAeroMapper reactiveAeroMapper, @NotNull Class<?> owningClazz, @NotNull Object key, @NotNull String binName, @NotNull Class<E> clazz) {
        this(reactiveAeroMapper, null, owningClazz, key, binName, clazz);
    }

    public ReactiveVirtualList(@NotNull IReactiveAeroMapper reactiveAeroMapper, @NotNull Object object, @NotNull String binName, @NotNull Class<E> clazz) {
        this(reactiveAeroMapper, object, null, null, binName, clazz);
    }

    private ReactiveVirtualList(@NotNull IReactiveAeroMapper reactiveAeroMapper, Object object, Class<?> owningClazz, Object key, @NotNull String binName, @NotNull Class<E> clazz) {
        if (object != null) {
            owningClazz = object.getClass();
        }
        this.owningEntry = ClassCache.getInstance().loadClass(owningClazz, reactiveAeroMapper);
        Object aerospikeKey;
        if (key == null) {
            aerospikeKey = owningEntry.getKey(object);
        } else {
            aerospikeKey = owningEntry.translateKeyToAerospikeKey(key);
        }
        ClassCacheEntry<?> elementEntry = ClassCache.getInstance().loadClass(clazz, reactiveAeroMapper);
        this.reactiveAeroMapper = reactiveAeroMapper;
        this.binName = binName;
        ValueType value = owningEntry.getValueFromBinName(binName);
        if (value == null) {
            throw new AerospikeException(String.format("Class %s has no bin called %s", clazz.getSimpleName(), binName));
        }
        String set = owningEntry.getSetName();
        if ("".equals(set)) {
            // Use the null set
            set = null;
        }
        this.key = new Key(owningEntry.getNamespace(), set, Value.get(aerospikeKey));

        TypeUtils.AnnotatedType annotatedType = value.getAnnotatedType();
        AerospikeEmbed embed = annotatedType.getAnnotation(AerospikeEmbed.class);
        if (embed == null) {
            throw new AerospikeException(String.format("Bin %s on class %s is not specified as a embedded", binName, clazz.getSimpleName()));
        }
        AerospikeEmbed.EmbedType listType = embed.type() == AerospikeEmbed.EmbedType.DEFAULT ? AerospikeEmbed.EmbedType.LIST : embed.type();
        AerospikeEmbed.EmbedType elementType = embed.elementType() == AerospikeEmbed.EmbedType.DEFAULT ? AerospikeEmbed.EmbedType.MAP : embed.elementType();
        Class<?> binClazz = value.getType();
        if (!(binClazz.isArray() || (Map.class.isAssignableFrom(binClazz)) || List.class.isAssignableFrom(binClazz))) {
            throw new AerospikeException(String.format("Bin %s on class %s is not a collection class", binName, clazz.getSimpleName()));
        }

        TypeMapper typeMapper = value.getTypeMapper();
        if (typeMapper instanceof ListMapper) {
            listMapper = ((ListMapper) typeMapper);
        } else {
            throw new AerospikeException(String.format("Bin %s on class %s is not mapped via a listMapper. This is unexpected", binName, clazz.getSimpleName()));
        }
        Function<Object, Object> instanceMapper = listMapper::fromAerospikeInstanceFormat;
        this.virtualListInteractors = new VirtualListInteractors(binName, listType, elementEntry, instanceMapper, reactiveAeroMapper);
    }

    public ReactiveVirtualList<E> changeKey(Object newKey) {
        String set = owningEntry.getSetName();
        if ("".equals(set)) {
            // Use the null set
            set = null;
        }
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
     * Get items from the list matching the specified value. If the list is mapped to a MAP in Aerospike, the start value and end value will dictate the range of values to get,
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
     * Get items from the list matching the specified value. If the list is mapped to a MAP in Aerospike, the start value and end value will dictate the range of values to get,
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
     *
     * @param startKey
     * @param endKey
     * @param returnResultsOfType
     * @return
     */
    public Mono<E> getByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType) {
        return getByKeyRange(null, startKey, endKey, returnResultsOfType);
    }

    /**
     *
     * @param writePolicy
     * @param startKey
     * @param endKey
     * @param returnResultsOfType
     * @return
     */
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
     *
     * @param key
     * @param returnResultsOfType
     * @return
     */
    public Mono<E> removeByKey(Object key, ReturnType returnResultsOfType) {
        return removeByKey(null, key, returnResultsOfType);
    }

    /**
     *
     * @param writePolicy
     * @param key
     * @param returnResultsOfType
     * @return
     */
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
     * Remove items from the list matching the specified value. If the list is mapped to a MAP in Aerospike, the start value and end value will dictate the range of values to be removed,
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
     * Remove items from the list matching the specified value. If the list is mapped to a MAP in Aerospike, the start value and end value will dictate the range of values to be removed,
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
    public Mono<E> removeByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = virtualListInteractors.getRemoveValueRangeInteractor(startValue, endValue);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Remove items from the list matching the specified key. If the list is mapped to a MAP in Aerospike, the start key and end key will dictate the range of keys to be removed,
     * inclusive of the start, exclusive of the end.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to be removed from the list.
     * <p/>
     * @param startKey Start key of the range to remove.
     * @param endKey End key of the range to remove.
     * @param returnResultsOfType Type to return.
     * @return The result of the method is a list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    public Mono<E> removeByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType) {
        return this.removeByKeyRange(null, startKey, endKey, returnResultsOfType);
    }

    /**
     * Remove items from the list matching the specified key. If the list is mapped to a MAP in Aerospike, the start key and end key will dictate the range of keys to be removed,
     * inclusive of the start, exclusive of the end.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to be removed from the list.
     * <p/>
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param startKey Start key of the range to remove.
     * @param endKey End key of the range to remove.
     * @param returnResultsOfType Type to return.
     * @return The result of the method is a list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
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
    public Mono<E> get(Policy policy, int index) {
        if (policy == null) {
            policy = new Policy(owningEntry.getReadPolicy());
        }

        Interactor interactor = virtualListInteractors.getIndexInteractor(index);
        return reactiveAeroMapper.getReactorClient()
                .operate(null, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    /**
     * Get the size of the virtual list (number of elements)
     * @param policy - The policy to use for the operate() operation.
     * @return The size of the list.
     */
    public Mono<Long> size(Policy policy) {
        if (policy == null) {
            policy = new Policy(owningEntry.getReadPolicy());
        }
        Interactor interactor = virtualListInteractors.getSizeInteractor();
        return reactiveAeroMapper.getReactorClient()
                .operate(null, key, interactor.getOperation())
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
