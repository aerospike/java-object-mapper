package com.aerospike.mapper.tools.virtuallist;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.tools.ClassCache;
import com.aerospike.mapper.tools.IAeroMapper;

import javax.validation.constraints.NotNull;
import java.util.List;

public class VirtualList<E> extends BaseVirtualList<E> implements IVirtualList<E> {

	private final IAeroMapper mapper;

	public VirtualList(@NotNull IAeroMapper mapper, @NotNull Class<?> owningClazz, @NotNull Object key,
					   @NotNull String binName, @NotNull Class<E> clazz) {
		super(mapper, null, owningClazz, key, binName, clazz);
		this.mapper = mapper;
	}

	public VirtualList(@NotNull IAeroMapper mapper, @NotNull Object object, @NotNull String binName,
					   @NotNull Class<E> clazz) {
		super(mapper, object, null, null, binName, clazz);
		this.mapper = mapper;
	}
	
	public VirtualList<E> changeKey(Object newKey) {
        String set = alignedSet();
        this.key = new Key(owningEntry.getNamespace(), set, Value.get(owningEntry.translateKeyToAerospikeKey(key)));
        return this;
	}

	public MultiOperation<E> beginMultiOperation() {
		return this.beginMulti(null);
	}
	
	public MultiOperation<E> beginMulti(WritePolicy writePolicy) {
    	if (writePolicy == null) {
        	writePolicy = new WritePolicy(owningEntry.getWritePolicy());
    		writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
    	}
		return new MultiOperation<>(writePolicy, binName, listMapper, key, virtualListInteractors, mapper);
	}

	@Override
	public List<E> getByValue(Object value, ReturnType returnResultsOfType) {
		return this.getByValue(null, value, returnResultsOfType);
	}

	@Override
	public List<E> getByValue(WritePolicy writePolicy, Object value, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getGetByValueInteractor(value);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> getByValueRange(Object startValue, Object endValue, ReturnType returnResultsOfType) {
		return this.getByValueRange(null, startValue, endValue, returnResultsOfType);
	}

	@Override
	public List<E> getByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType) {
    	if (writePolicy == null) {
        	writePolicy = new WritePolicy(owningEntry.getWritePolicy());
    		writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
    	}
		Interactor interactor = virtualListInteractors.getGetByValueRangeInteractor(startValue, endValue);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> getByValueList(List<Object> values, ReturnType returnResultsOfType) {
		return this.getByValueList(null, values, returnResultsOfType);
	}

	@Override
	public List<E> getByValueList(WritePolicy writePolicy, List<Object> values, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getGetByValueListInteractor(values);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> getByValueRelativeRankRange(Object value, int rank, ReturnType returnResultsOfType) {
		return this.getByValueRelativeRankRange(null, value, rank, returnResultsOfType);
	}

	@Override
	public List<E> getByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getGetByValueRelativeRankRangeInteractor(value, rank);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> getByValueRelativeRankRange(Object value, int rank, int count, ReturnType returnResultsOfType) {
		return this.getByValueRelativeRankRange(null, value, rank, count, returnResultsOfType);
	}

	@Override
	public List<E> getByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, int count, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getGetByValueRelativeRankRangeInteractor(value, rank, count);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> getByIndexRange(int index, ReturnType returnResultsOfType) {
		return this.getByIndexRange(null, index, returnResultsOfType);
	}

	@Override
	public List<E> getByIndexRange(WritePolicy writePolicy, int index, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getGetByIndexRangeInteractor(index);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> getByIndexRange(int index, int count, ReturnType returnResultsOfType) {
		return this.getByIndexRange(null, index, count, returnResultsOfType);
	}

	@Override
	public List<E> getByIndexRange(WritePolicy writePolicy, int index, int count, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getGetByIndexRangeInteractor(index, count);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> getByRank(int rank, ReturnType returnResultsOfType) {
		return this.getByRank(null, rank, returnResultsOfType);
	}

	@Override
	public List<E> getByRank(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getGetByRankInteractor(rank);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> getByRankRange(int rank, ReturnType returnResultsOfType) {
		return this.getByRankRange(null, rank, returnResultsOfType);
	}

	@Override
	public List<E> getByRankRange(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getGetByRankRangeInteractor(rank);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> getByRankRange(int rank, int count, ReturnType returnResultsOfType) {
		return this.getByRankRange(null, rank, count, returnResultsOfType);
	}

	@Override
	public List<E> getByRankRange(WritePolicy writePolicy, int rank, int count, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getGetByRankRangeInteractor(rank, count);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> getByKey(Object key, ReturnType returnResultsOfType) {
		return getByKey(null, key, returnResultsOfType);
	}

	@Override
	public List<E> getByKey(WritePolicy writePolicy, Object key, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getGetByKeyInteractor(key);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, this.key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> getByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType) {
		return getByKeyRange(null, startKey, endKey, returnResultsOfType);
	}

	@Override
	public List<E> getByKeyRange(WritePolicy writePolicy, Object startKey, Object endKey, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getGetByKeyRangeInteractor(startKey, endKey);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, this.key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> removeByKey(Object key, ReturnType returnResultsOfType) {
		return removeByKey(null, key, returnResultsOfType);
	}

	@Override
	public List<E> removeByKey(WritePolicy writePolicy, Object key, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getRemoveKeyInteractor(key);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, this.key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> removeByValue(Object value, ReturnType returnResultsOfType) {
		return this.removeByValue(null, value, returnResultsOfType);
	}

	@Override
	public List<E> removeByValue(WritePolicy writePolicy, Object value, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getRemoveByValueInteractor(value);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> removeByValueList(List<Object> values, ReturnType returnResultsOfType) {
		return this.removeByValueList(null, values, returnResultsOfType);
	}

	@Override
	public List<E> removeByValueList(WritePolicy writePolicy, List<Object> values, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getRemoveByValueListInteractor(values);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> removeByValueRange(Object startValue, Object endValue, ReturnType returnResultsOfType) {
		return this.removeByValueRange(null, startValue, endValue, returnResultsOfType);
	}

	@Override
	public List<E> removeByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType) {
    	if (writePolicy == null) {
        	writePolicy = new WritePolicy(owningEntry.getWritePolicy());
    		writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
    	}
		Interactor interactor = virtualListInteractors.getRemoveByValueRangeInteractor(startValue, endValue);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> removeByValueRelativeRankRange(Object value, int rank, ReturnType returnResultsOfType) {
		return this.removeByValueRelativeRankRange(null, value, rank, returnResultsOfType);
	}

	@Override
	public List<E> removeByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getRemoveByValueRelativeRankRangeInteractor(value, rank);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> removeByValueRelativeRankRange(Object value, int rank, int count, ReturnType returnResultsOfType) {
		return this.removeByValueRelativeRankRange(null, value, rank, count, returnResultsOfType);
	}

	@Override
	public List<E> removeByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, int count, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getRemoveByValueRelativeRankRangeInteractor(value, rank, count);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> removeByIndex(int index, ReturnType returnResultsOfType) {
		return this.removeByIndex(null, index, returnResultsOfType);
	}

	@Override
	public List<E> removeByIndex(WritePolicy writePolicy, int index, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getRemoveByIndexInteractor(index);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> removeByIndexRange(int index, ReturnType returnResultsOfType) {
		return this.removeByIndexRange(null, index, returnResultsOfType);
	}

	@Override
	public List<E> removeByIndexRange(WritePolicy writePolicy, int index, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getRemoveByIndexRangeInteractor(index);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> removeByIndexRange(int index, int count, ReturnType returnResultsOfType) {
		return this.removeByIndexRange(null, index, count, returnResultsOfType);
	}

	@Override
	public List<E> removeByIndexRange(WritePolicy writePolicy, int index, int count, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getRemoveByIndexRangeInteractor(index, count);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> removeByRank(int rank, ReturnType returnResultsOfType) {
		return this.removeByRank(null, rank, returnResultsOfType);
	}

	@Override
	public List<E> removeByRank(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getRemoveByRankInteractor(rank);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> removeByRankRange(int rank, ReturnType returnResultsOfType) {
		return this.removeByRankRange(null, rank, returnResultsOfType);
	}

	@Override
	public List<E> removeByRankRange(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getRemoveByRankRangeInteractor(rank);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public List<E> removeByRankRange(int rank, int count, ReturnType returnResultsOfType) {
		return this.removeByRankRange(null, rank, count, returnResultsOfType);
	}

	@Override
	public List<E> removeByRankRange(WritePolicy writePolicy, int rank, int count, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getRemoveByRankRangeInteractor(rank, count);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}
	
	@Override
	public List<E> removeByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType) {
		return this.removeByKeyRange(null, startKey, endKey, returnResultsOfType);
	}

	@Override
	public List<E> removeByKeyRange(WritePolicy writePolicy, Object startKey, Object endKey, ReturnType returnResultsOfType) {
    	if (writePolicy == null) {
        	writePolicy = new WritePolicy(owningEntry.getWritePolicy());
    		writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
    	}
		Interactor interactor = virtualListInteractors.getRemoveKeyRangeInteractor(startKey, endKey);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());
		return getResultsAsListWithDependencies(record, interactor);
	}

	@Override
	public long append(E element) {
		return this.append(null, element);
	}

	@Override
	public long append(WritePolicy writePolicy, E element) {
    	Object result = listMapper.toAerospikeInstanceFormat(element);
    	if (writePolicy == null) {
        	writePolicy = new WritePolicy(owningEntry.getWritePolicy());
    		writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
    	}
		Record record = this.mapper.getClient().operate(writePolicy, key, virtualListInteractors.getAppendOperation(result));
    	return record == null ? -1L : record.getLong(binName);
	}

	@Override
	public E get(int index) {
		return get(null, index);
	}

	@Override
	public E get(Policy policy, int index) {
    	Interactor interactor = virtualListInteractors.getByIndexInteractor(index);
		Record record = this.mapper.getClient().operate(getWritePolicy(policy), key, interactor.getOperation());
		return getResultsWithDependencies(record, interactor);
	}

	@Override
	public long size(Policy policy) {
    	Interactor interactor = virtualListInteractors.getSizeInteractor();
		Record record = this.mapper.getClient().operate(getWritePolicy(policy), key, interactor.getOperation());
		return record == null ? -1L : record.getLong(binName);
	}

	@Override
	public void clear() {
		Interactor interactor = virtualListInteractors.getClearInteractor();
		this.mapper.getClient().operate(null, key, interactor.getOperation());
	}

	@SuppressWarnings("unchecked")
	private E getResultsWithDependencies(Record record, Interactor interactor) {
		E result = record == null ? null : (E)interactor.getResult(record.getList(binName));
		if (result != null) {
			mapper.getMappingConverter().resolveDependencies(ClassCache.getInstance().loadClass(result.getClass(), mapper));
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<E> getResultsAsListWithDependencies(Record record, Interactor interactor) {
		List<E> result = record == null ? null : (List<E>)interactor.getResult(record.getList(binName));
		if (result != null) {
			mapper.getMappingConverter().resolveDependencies(ClassCache.getInstance().loadClass(result.getClass(), mapper));
		}
		return result;
	}
}
