package com.aerospike.mapper.tools;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Operation;
import com.aerospike.client.policy.*;
import com.aerospike.client.query.Filter;
import com.aerospike.client.reactor.IAerospikeReactorClient;
import com.aerospike.mapper.tools.virtuallist.ReactiveVirtualList;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.function.Function;

public interface IReactiveAeroMapper extends IBaseAeroMapper {

    /**
     * Save each object in the database. This method will perform a REPLACE on the existing record so any existing
     * data will be overwritten by the data in the passed object. This is a convenience method for
     * <pre>
     * save(A);
     * save(B);
     * save(C);
     * </pre>
     * Not that no transactionality is implied by this method -- if any of the save methods fail, the exception will be
     * thrown without trying the other objects, nor attempting to roll back previously saved objects
     *
     * @param objects One or two objects to save.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> Flux<T> save(@NotNull T... objects);

    /**
     * Save an object in the database. This method will perform a REPLACE on the existing record so any existing
     * data will be overwritten by the data in the passed object
     *
     * @param object The object to save.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> Mono<T> save(@NotNull T object, String... binNames);

    /**
     * Save an object in the database with the given WritePolicy. This write policy will override any other set writePolicy so
     * is effectively an upsert operation
     *
     * @param writePolicy The write policy for the save operation.
     * @param object      The object to save.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> Mono<T> save(@NotNull WritePolicy writePolicy, @NotNull T object, String... binNames);

    /**
     * Updates the object in the database, merging the record with the existing record. This uses the RecordExistsAction
     * of UPDATE. If bins are specified, only bins with the passed names will be updated (or all of them if null is passed)
     *
     * @param object The object to update.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> Mono<T> update(@NotNull T object, String... binNames);

    /**
     * Read a record from the repository and map it to an instance of the passed class, by providing a digest.
     *
     * @param clazz  - The type of the record.
     * @param digest - The Aerospike digest (Unique server hash value generated from set name and user key).
     * @return The returned mapped record.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> Mono<T> readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest);

    /**
     * This method should not be used: It is used by mappers to correctly resolved dependencies.
     */
    <T> Mono<T> readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest, boolean resolveDependencies);

    /**
     * Read a record from the repository and map it to an instance of the passed class, by providing a digest.
     *
     * @param readPolicy - The read policy for the read operation.
     * @param clazz      - The type of the record.
     * @param digest     - The Aerospike digest (Unique server hash value generated from set name and user key).
     * @return The returned mapped record.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> Mono<T> readFromDigest(Policy readPolicy, @NotNull Class<T> clazz, @NotNull byte[] digest);

    /**
     * This method should not be used: It is used by mappers to correctly resolved dependencies.
     */
    <T> Mono<T> readFromDigest(Policy readPolicy, @NotNull Class<T> clazz, @NotNull byte[] digest, boolean resolveDependencies);

    /**
     * Read a record from the repository and map it to an instance of the passed class.
     *
     * @param clazz   - The type of the record.
     * @param userKey - The key of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return The returned mapped record.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> Mono<T> read(@NotNull Class<T> clazz, @NotNull Object userKey);

    /**
     * This method should not be used: It is used by mappers to correctly resolved dependencies.
     */
    <T> Mono<T> read(@NotNull Class<T> clazz, @NotNull Object userKey, boolean resolveDependencies);

    /**
     * Read a record from the repository and map it to an instance of the passed class.
     *
     * @param readPolicy - The read policy for the read operation.
     * @param clazz      - The type of the record.
     * @param userKey    - The key of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return The returned mapped record.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> Mono<T> read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Object userKey);

    /**
     * This method should not be used: It is used by mappers to correctly resolved dependencies.
     */
    <T> Mono<T> read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Object userKey, boolean resolveDependencies);

    /**
     * Read a batch of records from the repository and map them to an instance of the passed class.
     *
     * @param clazz    - The type of the record.
     * @param userKeys - The keys of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return The returned mapped records.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> Flux<T> read(@NotNull Class<T> clazz, @NotNull Object[] userKeys);

    /**
     * Read a batch of records from the repository and map them to an instance of the passed class.
     *
     * @param batchPolicy A given batch policy.
     * @param clazz       - The type of the record.
     * @param userKeys    - The keys of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return The returned mapped records.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> Flux<T> read(BatchPolicy batchPolicy, @NotNull Class<T> clazz, @NotNull Object[] userKeys);

    /**
     * Read a batch of records from the repository using read operations in one batch call and map them to an instance of the passed class.
     *
     * @param clazz      - The type of the record.
     * @param userKeys   - The keys of the record. The namespace and set will be derived from the values specified on the passed class.
     * @param operations - array of read operations on record.
     * @return The returned mapped records.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> Flux<T> read(@NotNull Class<T> clazz, @NotNull Object[] userKeys, Operation... operations);

    /**
     * Read a batch of records from the repository using read operations in one batch call and map them to an instance of the passed class.
     *
     * @param batchPolicy A given batch policy.
     * @param clazz       - The type of the record.
     * @param userKeys    - The keys of the record. The namespace and set will be derived from the values specified on the passed class.
     * @param operations  - array of read operations on record.
     * @return The returned mapped records.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> Flux<T> read(BatchPolicy batchPolicy, @NotNull Class<T> clazz, @NotNull Object[] userKeys, Operation... operations);

    /**
     * Delete a record by specifying a class and a user key.
     *
     * @param clazz   - The type of the record.
     * @param userKey - The key of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return whether record existed on server before deletion
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> Mono<Boolean> delete(@NotNull Class<T> clazz, @NotNull Object userKey);

    /**
     * Delete a record by specifying a write policy, a class and a user key.
     *
     * @param writePolicy - The write policy for the delete operation.
     * @param clazz       - The type of the record.
     * @param userKey     - The key of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return whether record existed on server before deletion
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> Mono<Boolean> delete(WritePolicy writePolicy, @NotNull Class<T> clazz, @NotNull Object userKey);

    /**
     * Delete a record by specifying an object.
     *
     * @param object The object to delete.
     * @return whether record existed on server before deletion
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    Mono<Boolean> delete(@NotNull Object object);

    /**
     * Delete a record by specifying a write policy and an object.
     *
     * @param writePolicy - The write policy for the delete operation.
     * @param object      The object to delete.
     * @return whether record existed on server before deletion
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    Mono<Boolean> delete(WritePolicy writePolicy, @NotNull Object object);

    /**
     * Find a record by specifying a class and a Boolean function.
     *
     * @param clazz    - The type of the record.
     * @param function a Boolean function.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> Mono<Void> find(@NotNull Class<T> clazz, Function<T, Boolean> function);

    /**
     * Scan every record in the set associated with the passed class. Each record will be converted to the appropriate class.
     *
     * @param clazz - the class used to determine which set to scan and to convert the returned records to.
     */
    <T> Flux<T> scan(@NotNull Class<T> clazz);

    /**
     * Scan every record in the set associated with the passed class. Each record will be converted to the appropriate class.
     *
     * @param policy - the scan policy to use. If this is null, the default scan policy of the passed class will be used.
     * @param clazz  - the class used to determine which set to scan and to convert the returned records to.
     */
    <T> Flux<T> scan(ScanPolicy policy, @NotNull Class<T> clazz);

    /**
     * Scan every record in the set associated with the passed class, limiting the throughput to the specified recordsPerSecond. Each record will be converted
     * to the appropriate class.
     *
     * @param clazz            - the class used to determine which set to scan and to convert the returned records to.
     * @param recordsPerSecond - the maximum number of records to be processed every second.
     */
    <T> Flux<T> scan(@NotNull Class<T> clazz, int recordsPerSecond);

    /**
     * Scan every record in the set associated with the passed class. Each record will be converted to the appropriate class.
     *
     * @param policy           - the scan policy to use. If this is null, the default scan policy of the passed class will be used.
     * @param clazz            - the class used to determine which set to scan and to convert the returned records to.
     * @param recordsPerSecond - the number of records to process per second. Set to 0 for unlimited, &gt; 0 for a finite rate, &lt; 0 for no change
     *                         (use the value from the passed policy)
     */
    <T> Flux<T> scan(ScanPolicy policy, @NotNull Class<T> clazz, int recordsPerSecond);

    /**
     * Perform a secondary index query with the specified query policy. Each record will be converted
     * to the appropriate class then passed to the processor. If the processor returns false the query is aborted
     * whereas if the processor returns true subsequent records (if any) are processed.
     * <p/>
     * The query policy used will be the one associated with the passed classtype.
     *
     * @param clazz  - the class used to determine which set to scan and to convert the returned records to.
     * @param filter - the filter used to determine which secondary index to use. If this filter is null, every record in the set
     *               associated with the passed classtype will be scanned, effectively turning the query into a scan
     */
    <T> Flux<T> query(@NotNull Class<T> clazz, Filter filter);

    /**
     * Perform a secondary index query with the specified query policy. Each record will be converted
     * to the appropriate class then passed to the processor. If the processor returns false the query is aborted
     * whereas if the processor returns true subsequent records (if any) are processed.
     *
     * @param policy - The query policy to use. If this parameter is not passed, the query policy associated with the passed classtype will be used
     * @param clazz  - the class used to determine which set to scan and to convert the returned records to.
     * @param filter - the filter used to determine which secondary index to use. If this filter is null, every record in the set
     *               associated with the passed classtype will be scanned, effectively turning the query into a scan
     */
    <T> Flux<T> query(QueryPolicy policy, @NotNull Class<T> clazz, Filter filter);

    /**
     * Create a reactive virtual list against an attribute on a class. The list does all operations to the database and does not affect the underlying
     * class, and is useful for situation when operations are needed to affect the database without having to return all the elements on the
     * list each time.
     * <p/>
     * For example, consider a set of transactions associated with a credit card. Common operations might be
     * <ul>
     * 	<li>Return the last N transactions </li>
     * 	<li>insert a new transaction into the list</li>
     * </ul>
     * These operation can all be done without having the full set of transactions
     *
     * @param <T>          the type of the elements in the list.
     * @param object       The object that will use as a base for the virtual list.
     * @param binName      The Aerospike bin name.
     * @param elementClazz The class of the elements in the list.
     * @return A reactive virtual list.
     */
    <T> ReactiveVirtualList<T> asBackedList(@NotNull Object object, @NotNull String binName, Class<T> elementClazz);

    /**
     * Create a reactive virtual list against an attribute on a class. The list does all operations to the database and does not affect the underlying
     * class, and is useful for situation when operations are needed to affect the database without having to return all the elements on the
     * list each time.
     * <p/>
     * Note that the object being mapped does not need to actually exist in this case. The owning class is used purely for the definitions
     * of how to map the list elements (are they to be mapped in the database as a list or a map, is each element a list or a map, etc), as
     * well as using the namespace / set definition for the location to map into the database.  The
     * passed key is used to map the object to the database.
     * <p/>
     * For example, consider a set of transactions associated with a credit card. Common operations might be
     * <ul>
     * 	<li>Return the last N transactions </li>
     * 	<li>insert a new transaction into the list</li>
     * </ul>
     * These operation can all be done without having the full set of transactions
     *
     * @param <T>          the type of the elements in the list.
     * @param owningClazz  Used for the definitions of how to map the list elements.
     * @param key          The key to map the object to the database.
     * @param binName      The Aerospike bin name.
     * @param elementClazz The class of the elements in the list.
     * @return A reactive virtual list.
     */
    <T> ReactiveVirtualList<T> asBackedList(@NotNull Class<?> owningClazz, @NotNull Object key, @NotNull String binName, Class<T> elementClazz);

    IAerospikeReactorClient getReactorClient();
}
