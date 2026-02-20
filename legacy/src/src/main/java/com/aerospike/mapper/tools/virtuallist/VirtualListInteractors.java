package com.aerospike.mapper.tools.virtuallist;

import com.aerospike.client.Operation;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.*;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.tools.ClassCacheEntry;
import com.aerospike.mapper.tools.IBaseAeroMapper;
import com.aerospike.mapper.tools.utils.TypeUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public Interactor getGetByValueInteractor(Object value) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.getByValue(binName, getValue(value, false),
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.getByValue(binName, getValue(value, false),
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

    public Interactor getGetByValueRangeInteractor(Object startValue, Object endValue) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.getByValueRange(binName, getValue(startValue, false), getValue(endValue, false),
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
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

    public Interactor getGetByValueListInteractor(List<Object> values) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                List<Value> aerospikeValues = values.stream()
                        .map(x -> getValue(x, false))
                        .collect(Collectors.toList());
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.getByValueList(binName, aerospikeValues,
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.getByValueList(binName, aerospikeValues,
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

    public Interactor getGetByValueRelativeRankRangeInteractor(Object value, int rank) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.getByValueRelativeRankRange(binName, getValue(value, false), rank,
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.getByValueRelativeRankRange(binName, getValue(value, false), rank,
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

    public Interactor getGetByValueRelativeRankRangeInteractor(Object value, int rank, int count) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.getByValueRelativeRankRange(binName, getValue(value, false), rank, count,
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.getByValueRelativeRankRange(binName, getValue(value, false), rank, count,
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

    public Interactor getGetByIndexRangeInteractor(int index) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.getByIndexRange(binName, index,
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.getByIndexRange(binName, index,
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

    public Interactor getGetByIndexRangeInteractor(int index, int count) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.getByIndexRange(binName, index, count,
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.getByIndexRange(binName, index, count,
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

    public Interactor getGetByRankInteractor(int index) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.getByRank(binName, index,
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.getByRank(binName, index,
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

    public Interactor getGetByRankRangeInteractor(int index) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.getByRankRange(binName, index,
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.getByRankRange(binName, index,
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

    public Interactor getGetByRankRangeInteractor(int index, int count) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.getByRankRange(binName, index, count,
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.getByRankRange(binName, index, count,
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

    public Interactor getGetByKeyInteractor(Object key) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.getByValue(binName, getValue(key, true),
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.getByKey(binName, getValue(key, true),
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

    public Interactor getGetByKeyRangeInteractor(Object startKey, Object endKey) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.getByValueRange(binName, getValue(startKey, true), getValue(endKey, true),
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
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
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.removeByValueRange(binName, getValue(startKey, true), getValue(endKey, true),
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
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
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.removeByValue(binName, getValue(key, true),
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
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

    public Interactor getRemoveByValueInteractor(Object value) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.removeByValue(binName, getValue(value, false),
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.removeByValue(binName, getValue(value, false),
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

    public Interactor getRemoveByValueListInteractor(List<Object> values) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                List<Value> aerospikeValues = values.stream()
                        .map(x -> getValue(x, false))
                        .collect(Collectors.toList());
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.removeByValueList(binName, aerospikeValues,
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.removeByValueList(binName, aerospikeValues,
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

    public Interactor getRemoveByValueRangeInteractor(Object startValue, Object endValue) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.removeByValueRange(binName, getValue(startValue, false), getValue(endValue, false),
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
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

    public Interactor getRemoveByValueRelativeRankRangeInteractor(Object value, int rank) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.removeByValueRelativeRankRange(binName, getValue(value, false), rank,
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.removeByValueRelativeRankRange(binName, getValue(value, false), rank,
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

    public Interactor getRemoveByValueRelativeRankRangeInteractor(Object value, int rank, int count) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.removeByValueRelativeRankRange(binName, getValue(value, false), rank, count,
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.removeByValueRelativeRankRange(binName, getValue(value, false), rank, count,
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

    public Interactor getRemoveByIndexInteractor(int index) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.removeByIndex(binName, index,
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.removeByIndex(binName, index,
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

    public Interactor getRemoveByIndexRangeInteractor(int index) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.removeByIndexRange(binName, index,
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.removeByIndexRange(binName, index,
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

    public Interactor getRemoveByIndexRangeInteractor(int index, int count) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.removeByIndexRange(binName, index, count,
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.removeByIndexRange(binName, index, count,
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

    public Interactor getRemoveByRankInteractor(int rank) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.removeByRank(binName, rank,
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.removeByRank(binName, rank,
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

    public Interactor getRemoveByRankRangeInteractor(int rank) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.removeByRankRange(binName, rank,
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.removeByRankRange(binName, rank,
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

    public Interactor getRemoveByRankRangeInteractor(int rank, int count) {
        DeferredOperation deferred = new DeferredOperation() {

            @Override
            public ResultsUnpacker[] getUnpackers(OperationParameters operationParams) {
                switch (operationParams.getNeedsResultOfType()) {
                    case DEFAULT:
                    case ELEMENTS:
                        return new ResultsUnpacker[]{new ResultsUnpacker.ArrayUnpacker(instanceMapper)};
                    default:
                        return new ResultsUnpacker[0];
                }
            }

            @Override
            public Operation getOperation(OperationParameters operationParams) {
                if (listType == AerospikeEmbed.EmbedType.LIST) {
                    return ListOperation.removeByRankRange(binName, rank, count,
                            TypeUtils.returnTypeToListReturnType(operationParams.getNeedsResultOfType()));
                } else {
                    return MapOperation.removeByRankRange(binName, rank, count,
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

    public Value getValue(Object javaObject, boolean isKey) {
        Object aerospikeObject;
        if (isKey) {
            aerospikeObject = elementEntry.translateKeyToAerospikeKey(javaObject);
        } else {
            aerospikeObject = this.mapper.getMappingConverter().translateToAerospike(javaObject);
        }
        if (aerospikeObject == null) {
            return null;
        } else {
            return Value.get(aerospikeObject);
        }
    }

    @SuppressWarnings("unchecked")
    public Operation getAppendOperation(Object aerospikeObject) {
        if (aerospikeObject instanceof Map.Entry) {
            Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>) aerospikeObject;
            return MapOperation.put(new MapPolicy(MapOrder.KEY_ORDERED, 0), binName,
                    Value.get(entry.getKey()), Value.get(entry.getValue()));
        } else {
            return ListOperation.append(binName, Value.get(aerospikeObject));
        }
    }

    public Interactor getByIndexInteractor(int index) {
        if (listType == AerospikeEmbed.EmbedType.LIST) {
            return new Interactor(ListOperation.getByIndex(binName, index, ListReturnType.VALUE),
                    new ResultsUnpacker.ElementUnpacker(instanceMapper));
        } else {
            return new Interactor(MapOperation.getByIndex(binName, index, MapReturnType.KEY_VALUE),
                    ResultsUnpacker.ListUnpacker.instance, new ResultsUnpacker.ElementUnpacker(instanceMapper));
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
