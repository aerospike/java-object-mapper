package com.aerospike.mapper.tools.virtuallist;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
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
	public List<E> getByValueRange(Object startValue, Object endValue, ReturnType returnResultsOfType) {
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
	public List<E> getByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType) {
    	if (writePolicy == null) {
        	writePolicy = new WritePolicy(owningEntry.getWritePolicy());
    		writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
    	}
		Interactor interactor = virtualListInteractors.getGetByValueRangeInteractor(startValue, endValue);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());

		return (List<E>)interactor.getResult(record.getList(binName));
	}

	/**
	 * Get items from the list matching the specified key range. If the list is mapped to a MAP in Aerospike, the start key and end key will dictate the range of keys to get,
	 * inclusive of the start, exclusive of the end.
	 * <p/>
	 * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to get from the list.
	 * <p/>
	 * @param startKey Start key of the range to get.
	 * @param endKey End key of the range to get.
	 * @param returnResultsOfType Type to return.
	 * @return A list of the records which match the given key range.
	 */
	public List<E> getByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType) {
		return getByKeyRange(null, startKey, endKey, returnResultsOfType);
	}

	/**
	 * Get items from the list matching the specified key range. If the list is mapped to a MAP in Aerospike, the start key and end key will dictate the range of keys to get,
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
	public List<E> getByKeyRange(WritePolicy writePolicy, Object startKey, Object endKey, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getGetByKeyRangeInteractor(startKey, endKey);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, this.key, interactor.getOperation());

		return (List<E>)interactor.getResult(record.getList(binName));
	}

	/**
	 * Remove items from the list matching the specified key. If the list is mapped to a MAP in Aerospike, the key will dictate the map key to be removed.
	 * <p/>
	 * If the list is mapped to a LIST in Aerospike however, the given key will use as the value to remove from the list.
	 * <p/>
	 * @param key Key to remove.
	 * @param returnResultsOfType Type to return.
	 * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
	 */
	public List<E> removeByKey(Object key, ReturnType returnResultsOfType) {
		return removeByKey(null, key, returnResultsOfType);
	}

	/**
	 * Remove items from the list matching the specified key. If the list is mapped to a MAP in Aerospike, the key will dictate the map key to be removed.
	 * <p/>
	 * If the list is mapped to a LIST in Aerospike however, the given key will use as the value to remove from the list.
	 * <p/>
	 * @param writePolicy An Aerospike write policy to use for the operate() operation.
	 * @param key Key to remove.
	 * @param returnResultsOfType Type to return.
	 * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
	 */
	public List<E> removeByKey(WritePolicy writePolicy, Object key, ReturnType returnResultsOfType) {
		if (writePolicy == null) {
			writePolicy = new WritePolicy(owningEntry.getWritePolicy());
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
		}
		Interactor interactor = virtualListInteractors.getRemoveKeyInteractor(key);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, this.key, interactor.getOperation());

		return (List<E>)interactor.getResult(record.getList(binName));
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
	public List<E> removeByValueRange(Object startValue, Object endValue, ReturnType returnResultsOfType) {
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
	public List<E> removeByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType) {
    	if (writePolicy == null) {
        	writePolicy = new WritePolicy(owningEntry.getWritePolicy());
    		writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
    	}
		Interactor interactor = virtualListInteractors.getRemoveValueRangeInteractor(startValue, endValue);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());

		return (List<E>)interactor.getResult(record.getList(binName));
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
	public List<E> removeByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType) {
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
	public List<E> removeByKeyRange(WritePolicy writePolicy, Object startKey, Object endKey, ReturnType returnResultsOfType) {
    	if (writePolicy == null) {
        	writePolicy = new WritePolicy(owningEntry.getWritePolicy());
    		writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
    	}
		Interactor interactor = virtualListInteractors.getRemoveKeyRangeInteractor(startKey, endKey);
		interactor.setNeedsResultOfType(returnResultsOfType);
		Record record = this.mapper.getClient().operate(writePolicy, key, interactor.getOperation());

		return (List<E>)interactor.getResult(record.getList(binName));
	}

	/**
	 * Append a new element at the end of the virtual list.
	 * @param element The given element to append.
	 * @return The list size.
	 */
	public long append(E element) {
		return this.append(null, element);
	}

	/**
	 * Append a new element at the end of the virtual list.
	 * @param writePolicy An Aerospike write policy to use for the operate() operation.
	 * @param element The given element to append.
	 * @return The size of the list.
	 */
	public long append(WritePolicy writePolicy, E element) {
    	Object result = listMapper.toAerospikeInstanceFormat(element);
    	if (writePolicy == null) {
        	writePolicy = new WritePolicy(owningEntry.getWritePolicy());
    		writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
    	}
		Record record = this.mapper.getClient().operate(writePolicy, key, virtualListInteractors.getAppendOperation(result));
    	return record.getLong(binName);
	}

	/**
	 * Get an element from the virtual list at a specific index.
	 * @param index The index to get the item from.
	 * @return The element to get from the virtual list.
	 */
	public E get(int index) {
		return get(null, index);
	}

	/**
	 * Get an element from the virtual list at a specific index.
	 * @param policy - The policy to use for the operate() operation.
	 * @param index The index to get the item from.
	 * @return The element to get from the virtual list.
	 */
	public E get(Policy policy, int index) {
    	if (policy == null) {
    		policy = new Policy(owningEntry.getReadPolicy());
    	}

    	Interactor interactor = virtualListInteractors.getIndexInteractor(index);
		Record record = this.mapper.getClient().operate(null, key, interactor.getOperation());
		return (E)interactor.getResult(record.getList(binName));
	}

	/**
	 * Get the size of the virtual list (number of elements)
	 * @param policy - The policy to use for the operate() operation.
	 * @return The size of the list.
	 */
	public long size(Policy policy) {
    	if (policy == null) {
    		policy = new Policy(owningEntry.getReadPolicy());
    	}
    	Interactor interactor = virtualListInteractors.getSizeInteractor();
		Record record = this.mapper.getClient().operate(null, key, interactor.getOperation());
		return record.getLong(binName);
	}

	/**
	 * Remove all the items in the virtual list.
	 */
	public void clear() {
		Interactor interactor = virtualListInteractors.getClearInteractor();
		this.mapper.getClient().operate(null, key, interactor.getOperation());
	}
}