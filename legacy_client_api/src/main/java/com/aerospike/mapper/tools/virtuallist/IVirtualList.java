package com.aerospike.mapper.tools.virtuallist;

import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;

import java.util.List;

public interface IVirtualList<E> {

    /**
     * Get items from the list matching the specified value.
     *
     * @param value               The value to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given value.
     */
    List<E> getByValue(Object value, ReturnType returnResultsOfType);

    /**
     * Get items from the list matching the specified value.
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param value               The value to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given value.
     */
    List<E> getByValue(WritePolicy writePolicy, Object value, ReturnType returnResultsOfType);

    /**
     * Get items from the list matching the specified value. If the list is mapped to a MAP in Aerospike,
     * the start value and end value will dictate the range of values to get,
     * inclusive of the start, exclusive of the end.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to get from the list.
     * <p/>
     *
     * @param startValue          Start value of the range to get.
     * @param endValue            End value of the range to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given value range.
     */
    List<E> getByValueRange(Object startValue, Object endValue, ReturnType returnResultsOfType);

    /**
     * Get items from the list matching the specified value. If the list is mapped to a MAP in Aerospike,
     * the start value and end value will dictate the range of values to get,
     * inclusive of the start, exclusive of the end.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to get from the list.
     * <p/>
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param startValue          Start value of the range to get.
     * @param endValue            End value of the range to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given value range.
     */
    List<E> getByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType);

    /**
     * Get items from the list matching the specified list of values.
     *
     * @param values              The list of values to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given list of values.
     */
    List<E> getByValueList(List<Object> values, ReturnType returnResultsOfType);

    /**
     * Get items from the list matching the specified list of values.
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param values              The list of values to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given list of values.
     */
    List<E> getByValueList(WritePolicy writePolicy, List<Object> values, ReturnType returnResultsOfType);

    /**
     * Get items nearest to value and greater by relative rank.
     *
     * @param value               The value to base the relative rank range calculation on.
     * @param rank                The relative rank.
     * @param returnResultsOfType Type to return.
     * @return A list of records that matches the given value and rank.
     */
    List<E> getByValueRelativeRankRange(Object value, int rank, ReturnType returnResultsOfType);

    /**
     * Get items nearest to value and greater by relative rank.
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param value               The value to base the relative rank range calculation on.
     * @param rank                The relative rank.
     * @param returnResultsOfType Type to return.
     * @return A list of records that matches the given value and rank.
     */
    List<E> getByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, ReturnType returnResultsOfType);

    /**
     * Get items nearest to value and greater by relative rank with a count limit.
     *
     * @param value               The value to base the relative rank range calculation on.
     * @param rank                The relative rank.
     * @param count               The count limit.
     * @param returnResultsOfType Type to return.
     * @return A list of records that matches the given value, rank and count.
     */
    List<E> getByValueRelativeRankRange(Object value, int rank, int count, ReturnType returnResultsOfType);

    /**
     * Get items nearest to value and greater by relative rank with a count limit.
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param value               The value to base the relative rank range calculation on.
     * @param rank                The relative rank.
     * @param count               The count limit.
     * @param returnResultsOfType Type to return.
     * @return A list of records that matches the given value, rank and count.
     */
    List<E> getByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, int count, ReturnType returnResultsOfType);

    /**
     * Get items starting at specified index to the end of virtual list.
     *
     * @param index               The start index to get items from to the end of the virtual list.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given index.
     */
    List<E> getByIndexRange(int index, ReturnType returnResultsOfType);

    /**
     * Get items starting at specified index to the end of virtual list.
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param index               The start index to get items from to the end of the virtual list.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given index.
     */
    List<E> getByIndexRange(WritePolicy writePolicy, int index, ReturnType returnResultsOfType);

    /**
     * Get "count" items starting at specified index.
     *
     * @param index               The start index to get the "count" items from.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given index and count.
     */
    List<E> getByIndexRange(int index, int count, ReturnType returnResultsOfType);

    /**
     * Get "count" items starting at specified index.
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param index               The start index to get the "count" items from.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given index and count.
     */
    List<E> getByIndexRange(WritePolicy writePolicy, int index, int count, ReturnType returnResultsOfType);

    /**
     * Get items identified by rank.
     *
     * @param rank                The rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given rank.
     */
    List<E> getByRank(int rank, ReturnType returnResultsOfType);

    /**
     * Get items identified by rank.
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param rank                The rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given rank.
     */
    List<E> getByRank(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType);

    /**
     * Get items starting at specified rank to the last ranked item.
     *
     * @param rank                The starting rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given rank.
     */
    List<E> getByRankRange(int rank, ReturnType returnResultsOfType);

    /**
     * Get items starting at specified rank to the last ranked item.
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param rank                The starting rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given rank.
     */
    List<E> getByRankRange(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType);

    /**
     * Get "count" items starting at specified rank to the last ranked item.
     *
     * @param rank                The starting rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given rank and count.
     */
    List<E> getByRankRange(int rank, int count, ReturnType returnResultsOfType);

    /**
     * Get "count" items starting at specified rank to the last ranked item.
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param rank                The starting rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given rank and count.
     */
    List<E> getByRankRange(WritePolicy writePolicy, int rank, int count, ReturnType returnResultsOfType);

    /**
     * Get items from the list matching the specified key. If the list is mapped to a MAP in Aerospike,
     * It will get the matching key.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, it will get the matching value.
     * <p/>
     *
     * @param key                 Key to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given key range.
     */
    List<E> getByKey(Object key, ReturnType returnResultsOfType);

    /**
     * Get items from the list matching the specified key. If the list is mapped to a MAP in Aerospike,
     * It will get the matching key.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, it will get the matching value.
     * <p/>
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param key                 Key to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given key range.
     */
    List<E> getByKey(WritePolicy writePolicy, Object key, ReturnType returnResultsOfType);

    /**
     * Get items from the list matching the specified key range. If the list is mapped to a MAP in Aerospike,
     * the start key and end key will dictate the range of keys to get,
     * inclusive of the start, exclusive of the end.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to get from the list.
     * <p/>
     *
     * @param startKey            Start key of the range to get.
     * @param endKey              End key of the range to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given key range.
     */
    List<E> getByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType);

    /**
     * Get items from the list matching the specified key range. If the list is mapped to a MAP in Aerospike,
     * the start key and end key will dictate the range of keys to get,
     * inclusive of the start, exclusive of the end.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to get from the list.
     * <p/>
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param startKey            Start key of the range to get.
     * @param endKey              End key of the range to get.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which match the given key range.
     */
    List<E> getByKeyRange(WritePolicy writePolicy, Object startKey, Object endKey, ReturnType returnResultsOfType);

    /**
     * Remove items from the list matching the specified key. If the list is mapped to a MAP in Aerospike,
     * the key will dictate the map key to be removed.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the given key will use as the value to remove from the list.
     * <p/>
     *
     * @param key                 Key to remove.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByKey(Object key, ReturnType returnResultsOfType);

    /**
     * Remove items from the list matching the specified key. If the list is mapped to a MAP in Aerospike,
     * the key will dictate the map key to be removed.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the given key will use as the value to remove from the list.
     * <p/>
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param key                 Key to remove.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByKey(WritePolicy writePolicy, Object key, ReturnType returnResultsOfType);

    /**
     * Remove items identified by value and returns the removed data.
     *
     * @param value               The value to base the items to remove on.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByValue(Object value, ReturnType returnResultsOfType);

    /**
     * Remove items identified by value and returns the removed data.
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param value               The value to base the items to remove on.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByValue(WritePolicy writePolicy, Object value, ReturnType returnResultsOfType);

    /**
     * Remove items identified by list of values and returns the removed data.
     *
     * @param values              The list of values to base the items to remove on.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByValueList(List<Object> values, ReturnType returnResultsOfType);

    /**
     * Remove items identified by list of values and returns the removed data.
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param values              The list of values to base the items to remove on.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByValueList(WritePolicy writePolicy, List<Object> values, ReturnType returnResultsOfType);

    /**
     * Remove items from the list matching the specified value. If the list is mapped to a MAP in Aerospike,
     * the start value and end value will dictate the range of values to be removed,
     * inclusive of the start, exclusive of the end.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to removed from the list.
     * <p/>
     *
     * @param startValue          Start value of the range to remove.
     * @param endValue            End value of the range to remove.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByValueRange(Object startValue, Object endValue, ReturnType returnResultsOfType);

    /**
     * Remove items from the list matching the specified value. If the list is mapped to a MAP in Aerospike,
     * the start value and end value will dictate the range of values to be removed,
     * inclusive of the start, exclusive of the end.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to be removed from the list.
     * <p/>
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param startValue          Start value of the range to remove.
     * @param endValue            End value of the range to remove.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByValueRange(WritePolicy writePolicy, Object startValue, Object endValue, ReturnType returnResultsOfType);

    /**
     * Remove items nearest to value and greater by relative rank.
     *
     * @param value               The value to base the items to remove on.
     * @param rank                The rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByValueRelativeRankRange(Object value, int rank, ReturnType returnResultsOfType);

    /**
     * Remove items nearest to value and greater by relative rank.
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param value               The value to base the items to remove on.
     * @param rank                The rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, ReturnType returnResultsOfType);

    /**
     * Remove items nearest to value and greater by relative rank with a count limit.
     *
     * @param value               The value to base the items to remove on.
     * @param rank                The rank.
     * @param count               The count limit.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByValueRelativeRankRange(Object value, int rank, int count, ReturnType returnResultsOfType);

    /**
     * Remove items nearest to value and greater by relative rank with a count limit.
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param value               The value to base the items to remove on.
     * @param rank                The rank.
     * @param count               The count limit.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByValueRelativeRankRange(WritePolicy writePolicy, Object value, int rank, int count, ReturnType returnResultsOfType);

    /**
     * Remove item identified by index and returns removed data.
     *
     * @param index               The index to remove the item from.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByIndex(int index, ReturnType returnResultsOfType);

    /**
     * Remove item identified by index and returns removed data.
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param index               The index to remove the item from.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByIndex(WritePolicy writePolicy, int index, ReturnType returnResultsOfType);

    /**
     * Remove items starting at specified index to the end of list and returns removed data.
     *
     * @param index               The start index to remove the item from.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByIndexRange(int index, ReturnType returnResultsOfType);

    /**
     * Remove items starting at specified index to the end of list and returns removed data.
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param index               The start index to remove the item from.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByIndexRange(WritePolicy writePolicy, int index, ReturnType returnResultsOfType);

    /**
     * Remove "count" items starting at specified index and returns removed data.
     *
     * @param index               The start index to remove the item from.
     * @param count               The count limit.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByIndexRange(int index, int count, ReturnType returnResultsOfType);

    /**
     * Remove "count" items starting at specified index and returns removed data.
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param index               The start index to remove the item from.
     * @param count               The count limit.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByIndexRange(WritePolicy writePolicy, int index, int count, ReturnType returnResultsOfType);

    /**
     * Remove item identified by rank and returns removed data.
     *
     * @param rank                The rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByRank(int rank, ReturnType returnResultsOfType);

    /**
     * Remove item identified by rank and returns removed data.
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param rank                The rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByRank(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType);

    /**
     * Remove items starting at specified rank to the last ranked item and returns removed data.
     *
     * @param rank                The starting rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByRankRange(int rank, ReturnType returnResultsOfType);

    /**
     * Remove items starting at specified rank to the last ranked item and returns removed data.
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param rank                The starting rank.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByRankRange(WritePolicy writePolicy, int rank, ReturnType returnResultsOfType);

    /**
     * Remove "count" items starting at specified rank and returns removed data.
     *
     * @param rank                The starting rank.
     * @param count               The count limit.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByRankRange(int rank, int count, ReturnType returnResultsOfType);

    /**
     * Remove "count" items starting at specified rank and returns removed data.
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param rank                The starting rank.
     * @param count               The count limit.
     * @param returnResultsOfType Type to return.
     * @return A list of the records which have been removed from the database if returnResults is true, null otherwise.
     */
    List<E> removeByRankRange(WritePolicy writePolicy, int rank, int count, ReturnType returnResultsOfType);

    /**
     * Remove items from the list matching the specified key. If the list is mapped to a MAP in Aerospike,
     * the start key and end key will dictate the range of keys to be removed,
     * inclusive of the start, exclusive of the end.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to be removed from the list.
     * <p/>
     *
     * @param startKey            Start key of the range to remove.
     * @param endKey              End key of the range to remove.
     * @param returnResultsOfType Type to return.
     * @return The result of the method is a list of the records which have been removed from the database if
     * returnResults is true, null otherwise.
     */
    List<E> removeByKeyRange(Object startKey, Object endKey, ReturnType returnResultsOfType);

    /**
     * Remove items from the list matching the specified key. If the list is mapped to a MAP in Aerospike,
     * the start key and end key will dictate the range of keys to be removed,
     * inclusive of the start, exclusive of the end.
     * <p/>
     * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to be removed from the list.
     * <p/>
     *
     * @param writePolicy         An Aerospike write policy to use for the operate() operation.
     * @param startKey            Start key of the range to remove.
     * @param endKey              End key of the range to remove.
     * @param returnResultsOfType Type to return.
     * @return The result of the method is a list of the records which have been removed from the database if
     * returnResults is true, null otherwise.
     */
    List<E> removeByKeyRange(WritePolicy writePolicy, Object startKey, Object endKey, ReturnType returnResultsOfType);

    /**
     * Append a new element at the end of the virtual list.
     *
     * @param element The given element to append.
     * @return The list size.
     */
    long append(E element);

    /**
     * Append a new element at the end of the virtual list.
     *
     * @param writePolicy An Aerospike write policy to use for the operate() operation.
     * @param element     The given element to append.
     * @return The size of the list. If the record is not found, this method returns -1.
     */
    long append(WritePolicy writePolicy, E element);

    /**
     * Get an element from the virtual list at a specific index.
     *
     * @param index The index to get the item from.
     * @return The element to get from the virtual list.
     */
    E get(int index);

    /**
     * Get an element from the virtual list at a specific index.
     *
     * @param policy - The policy to use for the operate() operation.
     * @param index  The index to get the item from.
     * @return The element to get from the virtual list.
     */
    E get(Policy policy, int index);

    /**
     * Get the size of the virtual list (number of elements)
     *
     * @param policy - The policy to use for the operate() operation.
     * @return The size of the list. If the record is not found, this method returns -1.
     */
    long size(Policy policy);

    /**
     * Remove all the items in the virtual list.
     */
    void clear();
}
