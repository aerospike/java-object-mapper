package com.aerospike.mapper.tools;

import com.aerospike.client.Operation;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.*;
import com.aerospike.mapper.annotations.AerospikeEmbed;

import java.util.Map;
import java.util.function.Function;

public class VirtualListInteractors {

    private final String binName;
    private final AerospikeEmbed.EmbedType listType;
    private final ClassCacheEntry<?> elementEntry;
    private final Function<Object, Object> instanceMapper;
    private final IBaseAeroMapper mapper;

    public VirtualListInteractors(String binName, AerospikeEmbed.EmbedType listType, ClassCacheEntry<?> elementEntry,
                                  Function<Object, Object> instanceMapper, IBaseAeroMapper mapper) {
        this.binName = binName;
        this.listType = listType;
        this.elementEntry = elementEntry;
        this.instanceMapper = instanceMapper;
        this.mapper = mapper;
    }

    public Interactor getGetByValueRangeInteractor(Object startValue, Object endValue) {
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
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                }
                else {
                    return MapOperation.getByValueRange(binName, getValue(startValue, false), getValue(endValue, false),
                            TypeUtils.returnTypeToMapReturnType(operationParams.getNeedsResultOfType()));
                }
            }

            @Override
            public boolean isGetOperation() {
                return true;
            }
        };
        return new Interactor(deferred);
    }

    public Value getValue(Object javaObject, boolean isKey) {
        Object aerospikeObject;
        if (isKey) {
            aerospikeObject = elementEntry.translateKeyToAerospikeKey(javaObject);
        }
        else {
            aerospikeObject = this.mapper.getMappingConverter().translateToAerospike(javaObject);
        }
        if (aerospikeObject == null) {
            return null;
        }
        else {
            return Value.get(aerospikeObject);
        }
    }

    public Interactor getGetByKeyRangeInteractor(Object startKey, Object endKey) {
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
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                }
                else {
                    return MapOperation.getByKeyRange(binName, getValue(startKey, true), getValue(endKey, true),
                            TypeUtils.returnTypeToMapReturnType(operationParams.getNeedsResultOfType()));
                }
            }

            @Override
            public boolean isGetOperation() {
                return true;
            }
        };
        return new Interactor(deferred);
    }

    public Interactor getRemoveKeyRangeInteractor(Object startKey, Object endKey) {
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
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                }
                else {
                    return MapOperation.removeByKeyRange(binName, getValue(startKey, true), getValue(endKey, true),
                            TypeUtils.returnTypeToMapReturnType(operationParams.getNeedsResultOfType()));
                }
            }

            @Override
            public boolean isGetOperation() {
                return false;
            }
        };
        return new Interactor(deferred);
    }

    public Interactor getRemoveKeyInteractor(Object key) {
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
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                }
                else {
                    return MapOperation.removeByKey(binName, getValue(key, true),
                            TypeUtils.returnTypeToMapReturnType(operationParams.getNeedsResultOfType()));
                }
            }

            @Override
            public boolean isGetOperation() {
                return false;
            }
        };
        return new Interactor(deferred);
    }

    public Interactor getRemoveValueRangeInteractor(Object startValue, Object endValue) {
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
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                }
                else {
                    return MapOperation.removeByValueRange(binName, getValue(startValue, false), getValue(endValue, false),
                            TypeUtils.returnTypeToMapReturnType(operationParams.getNeedsResultOfType()));
                }
            }

            @Override
            public boolean isGetOperation() {
                return false;
            }
        };
        return new Interactor(deferred);
    }

    public Operation getAppendOperation(Object aerospikeObject) {
        if (aerospikeObject instanceof Map.Entry) {
            Map.Entry<Object, Object> entry = (Map.Entry) aerospikeObject;
            return MapOperation.put(new MapPolicy(MapOrder.KEY_ORDERED, 0), binName, Value.get(entry.getKey()), Value.get(entry.getValue()));
        }
        else {
            return ListOperation.append(binName, Value.get(aerospikeObject));
        }
    }

    public Interactor getIndexInteractor(int index) {
        if (listType == AerospikeEmbed.EmbedType.LIST) {
            return new Interactor(ListOperation.getByIndex(binName, index, ListReturnType.VALUE), new ResultsUnpacker.ElementUnpacker(instanceMapper));
        }
        else {
            return new Interactor(MapOperation.getByIndex(binName, index, MapReturnType.KEY_VALUE), ResultsUnpacker.ListUnpacker.instance, new ResultsUnpacker.ElementUnpacker(instanceMapper));
        }
    }

    public Interactor getSizeInteractor() {
        if (listType == AerospikeEmbed.EmbedType.LIST) {
            return new Interactor(ListOperation.size(binName));
        } else {
            return new Interactor(MapOperation.size(binName));
        }
    }

    public Interactor getClearInteractor() {
        if (listType == AerospikeEmbed.EmbedType.LIST) {
            return new Interactor(ListOperation.clear(binName));
        } else {
            return new Interactor(MapOperation.clear(binName));
        }
    }
}
