package com.aerospike.mapper.tools;

import com.aerospike.client.*;
import com.aerospike.client.cdt.*;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.KeyRecord;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.tools.mappers.ListMapper;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ReactiveVirtualList<E> {
    private final IReactiveAeroMapper reactiveAeroMapper;
    private final ValueType value;
    private final ClassCacheEntry<?> owningEntry;
    private final ClassCacheEntry<?> elementEntry;
    private final String binName;
    private final ListMapper listMapper;
    private Key key;
    private final AerospikeEmbed.EmbedType listType;
    private final AerospikeEmbed.EmbedType elementType;
    private final Function<Object, Object> instanceMapper;

    // package level visibility
    ReactiveVirtualList(@NotNull IReactiveAeroMapper reactiveAeroMapper, @NotNull Class<?> owningClazz, @NotNull Object key, @NotNull String binName, @NotNull Class<E> clazz) {
        this(reactiveAeroMapper, null, owningClazz, key, binName, clazz);
    }

    // package level visibility
    ReactiveVirtualList(@NotNull IReactiveAeroMapper reactiveAeroMapper, @NotNull Object object, @NotNull String binName, @NotNull Class<E> clazz) {
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
        this.elementEntry = ClassCache.getInstance().loadClass(clazz, reactiveAeroMapper);
        this.reactiveAeroMapper = reactiveAeroMapper;
        this.binName = binName;
        this.value = owningEntry.getValueFromBinName(binName);
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
        listType = embed.type() == AerospikeEmbed.EmbedType.DEFAULT ? AerospikeEmbed.EmbedType.LIST : embed.type();
        elementType = embed.elementType() == AerospikeEmbed.EmbedType.DEFAULT ? AerospikeEmbed.EmbedType.MAP : embed.elementType();
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
        this.instanceMapper = listMapper::fromAerospikeInstanceFormat;
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

    public class MultiOperation<E> {
        private final ReactiveVirtualList<E> reactiveVirtualList;
        private final List<Interactor> interactions;
        private int indexToReturn = -1;
        private final WritePolicy writePolicy;

        private MultiOperation(@NotNull ReactiveVirtualList<E> virtualList, @NotNull WritePolicy writePolicy) {
            this.reactiveVirtualList = virtualList;
            this.interactions = new ArrayList<>();
            this.writePolicy = writePolicy;
        }

        public MultiOperation<E> append(E item) {
            Object aerospikeItem = listMapper.toAerospikeInstanceFormat(item);
            this.interactions.add(new Interactor(reactiveVirtualList.getAppendOperation(aerospikeItem)));
            return this;
        }

        public MultiOperation<E> removeByKey(Object key) {
            this.interactions.add(getRemoveKeyInteractor(key));
            return this;
        }

        public MultiOperation<E> removeByKeyRange(Object startKey, Object endKey) {
            this.interactions.add(getRemoveKeyRangeInteractor(startKey, endKey));
            return this;
        }

        public MultiOperation<E> removeByValueRange(Object startKey, Object endKey) {
            this.interactions.add(getRemoveValueRangeInteractor(startKey, endKey));
            return this;
        }

        public MultiOperation<E> getByValueRange(Object startKey, Object endKey) {
            this.interactions.add(getGetByValueRangeInteractor(startKey, endKey));
            return this;
        }

        public MultiOperation<E> getByKeyRange(Object startKey, Object endKey) {
            this.interactions.add(getGetByKeyRangeInteractor(startKey, endKey));
            return this;
        }

        public MultiOperation<E> get(int index) {
            this.interactions.add(getIndexInteractor(index));
            return this;
        }

        public MultiOperation<E> size() {
            this.interactions.add(getSizeInteractor());
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
        public <T> T end() {
            return end(null);
        }

        /**
         * Finish the multi operation and process it.
         * @param resultType The return type for the result.
         * @return The multi operation result with the given result type.
         */
        public <T> T end(Class<T> resultType) {
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

            Mono<KeyRecord> keyRecordMono = reactiveVirtualList.reactiveAeroMapper.getReactorClient().operate(writePolicy, key, operations);

            T result;
            if (count == 1) {
                result = (T)this.interactions.get(0).getResult(keyRecordMono.map(keyRecord -> keyRecord.record.getValue(binName)).subscribeOn(Schedulers.parallel()).block());
            }
            else {
                List<?> resultList = keyRecordMono.map(keyRecord -> keyRecord.record.getList(binName)).subscribeOn(Schedulers.parallel()).block();
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
                    Collection<T> collection = (Collection<T>)result;
                    object = collection.isEmpty() ? null : collection.iterator().next();
                }
                reactiveAeroMapper.getMappingConverter().resolveDependencies(ClassCache.getInstance().loadClass(object.getClass(), reactiveAeroMapper));
            }
            return result;
        }
    }

    public MultiOperation<E> beginMultiOperation() {
        return this.beginMulti(null);
    }

    public MultiOperation<E> beginMulti(WritePolicy writePolicy) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        return new MultiOperation<>(this, writePolicy);
    }

    public Mono<E> getByValueRange(Object startKey, Object endKey, ReturnType returnResultsOfType) {
        return this.getByValueRange(null, startKey, endKey, returnResultsOfType);
    }

    public Mono<E> getByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = getGetByValueRangeInteractor(startValue, endValue);
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
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */

    public Mono<E> removeByValueRange(Object startKey, Object endKey, ReturnType returnResultsOfType) {
        return this.removeByValueRange(null, startKey, endKey, returnResultsOfType);
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
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    public Mono<E> removeByValueRange(WritePolicy writePolicy, Object startKey, Object endKey, ReturnType returnResultsOfType) {
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        Interactor interactor = getRemoveValueRangeInteractor(startKey, endKey);
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
        Interactor interactor = getRemoveKeyRangeInteractor(startKey, endKey);
        interactor.setNeedsResultOfType(returnResultsOfType);

        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    private int returnTypeToListReturnType(ReturnType returnType) {
        switch (returnType) {
            case DEFAULT:
            case ELEMENTS:
                return ListReturnType.VALUE;
            case COUNT:
                return ListReturnType.COUNT;
            case INDEX:
                return ListReturnType.INDEX;
            case NONE:
            default:
                return ListReturnType.NONE;
        }
    }

    private int returnTypeToMapReturnType(ReturnType returnType) {
        switch (returnType) {
            case DEFAULT:
            case ELEMENTS:
                return MapReturnType.KEY_VALUE;
            case COUNT:
                return MapReturnType.COUNT;
            case INDEX:
                return MapReturnType.INDEX;
            case NONE:
            default:
                return MapReturnType.NONE;
        }
    }

    private Interactor getGetByValueRangeInteractor(Object startValue, Object endValue) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[] { new ResultsUnpacker.ArrayUnpacker(instanceMapper) };
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.getByValueRange(binName, getValue(startValue, false), getValue(endValue, false),
                            returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                }
                else {
                    return MapOperation.getByValueRange(binName, getValue(startValue, false), getValue(endValue, false),
                            returnTypeToMapReturnType(operationParams.getNeedsResultOfType()));
                }
            }

            @Override
            public boolean isGetOperation() {
                return true;
            }
        };
        return new Interactor(deferred);
    }

    private Value getValue(Object javaObject, boolean isKey) {
        Object aerospikeObject;
        if (isKey) {
            aerospikeObject = elementEntry.translateKeyToAerospikeKey(javaObject);
        }
        else {
            aerospikeObject = reactiveAeroMapper.getMappingConverter().translateToAerospike(javaObject);
        }
        if (aerospikeObject == null) {
            return null;
        }
        else {
            return Value.get(aerospikeObject);
        }
    }

    private Interactor getGetByKeyRangeInteractor(Object startKey, Object endKey) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[] { new ResultsUnpacker.ArrayUnpacker(instanceMapper) };
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.getByValueRange(binName, getValue(startKey, true), getValue(endKey, true),
                            returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                }
                else {
                    return MapOperation.getByKeyRange(binName, getValue(startKey, true), getValue(endKey, true),
                            returnTypeToMapReturnType(operationParams.getNeedsResultOfType()));
                }
            }

            @Override
            public boolean isGetOperation() {
                return true;
            }
        };
        return new Interactor(deferred);
    }

    private Interactor getRemoveKeyRangeInteractor(Object startKey, Object endKey) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[] { new ResultsUnpacker.ArrayUnpacker(instanceMapper) };
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.removeByValueRange(binName, getValue(startKey, true), getValue(endKey, true),
                            returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                }
                else {
                    return MapOperation.removeByKeyRange(binName, getValue(startKey, true), getValue(endKey, true),
                            returnTypeToMapReturnType(operationParams.getNeedsResultOfType()));
                }
            }

            @Override
            public boolean isGetOperation() {
                return false;
            }
        };
        return new Interactor(deferred);
    }

    private Interactor getRemoveKeyInteractor(Object key) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[] { new ResultsUnpacker.ArrayUnpacker(instanceMapper) };
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.removeByValue(binName, getValue(key, true),
                            returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                }
                else {
                    return MapOperation.removeByKey(binName, getValue(key, true),
                            returnTypeToMapReturnType(operationParams.getNeedsResultOfType()));
                }
            }

            @Override
            public boolean isGetOperation() {
                return false;
            }
        };
        return new Interactor(deferred);
    }

    private Interactor getRemoveValueRangeInteractor(Object startValue, Object endValue) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[] { new ResultsUnpacker.ArrayUnpacker(instanceMapper) };
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.removeByValueRange(binName, getValue(startValue, false), getValue(endValue, false),
                            returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                }
                else {
                    return MapOperation.removeByValueRange(binName, getValue(startValue, false), getValue(endValue, false),
                            returnTypeToMapReturnType(operationParams.getNeedsResultOfType()));
                }
            }

            @Override
            public boolean isGetOperation() {
                return false;
            }
        };
        return new Interactor(deferred);
    }

    private Operation getAppendOperation(Object aerospikeObject) {
        if (aerospikeObject instanceof Map.Entry) {
            Map.Entry<Object, Object> entry = (Map.Entry) aerospikeObject;
            return MapOperation.put(new MapPolicy(MapOrder.KEY_ORDERED, 0), binName, Value.get(entry.getKey()), Value.get(entry.getValue()));
        }
        else {
            return ListOperation.append(binName, Value.get(aerospikeObject));
        }
    }
    public Mono<Long> append(E element) {
        return this.append(null, element);
    }

    public Mono<Long> append(WritePolicy writePolicy, E element) {
        Object result = listMapper.toAerospikeInstanceFormat(element);
        if (writePolicy == null) {
            writePolicy = new WritePolicy(owningEntry.getWritePolicy());
            writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        }
        return reactiveAeroMapper.getReactorClient()
                .operate(writePolicy, key, getAppendOperation(result))
                .map(keyRecord -> keyRecord.record.getLong(binName));
    }

    public Mono<E> get(int index) {
        return get(null, index);
    }

    private Interactor getIndexInteractor(int index) {
        if (listType == AerospikeEmbed.EmbedType.LIST) {
            return new Interactor(ListOperation.getByIndex(binName, index, ListReturnType.VALUE), new ResultsUnpacker.ElementUnpacker(instanceMapper));
        }
        else {
            return new Interactor(MapOperation.getByIndex(binName, index, MapReturnType.KEY_VALUE), ResultsUnpacker.ListUnpacker.instance, new ResultsUnpacker.ElementUnpacker(instanceMapper));
        }
    }

    public Mono<E> get(Policy policy, int index) {
        if (policy == null) {
            policy = new Policy(owningEntry.getReadPolicy());
        }

        Interactor interactor = getIndexInteractor(index);
        return reactiveAeroMapper.getReactorClient()
                .operate(null, key, interactor.getOperation())
                .map(keyRecord -> (E)interactor.getResult(keyRecord.record.getList(binName)));
    }

    private Interactor getSizeInteractor() {
        if (listType == AerospikeEmbed.EmbedType.LIST) {
            return new Interactor(ListOperation.size(binName));
        }
        else {
            return new Interactor(MapOperation.size(binName));
        }
    }

    public Mono<Long> size(Policy policy) {
        if (policy == null) {
            policy = new Policy(owningEntry.getReadPolicy());
        }
        Interactor interactor = getSizeInteractor();
        return reactiveAeroMapper.getReactorClient().
                operate(null, key, interactor.getOperation())
                .map(keyRecord -> keyRecord.record.getLong(binName));
    }
}
