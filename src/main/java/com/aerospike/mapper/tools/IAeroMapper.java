package com.aerospike.mapper.tools;

import java.util.List;
import java.util.function.Function;

import javax.validation.constraints.NotNull;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.Filter;
import com.aerospike.mapper.tools.virtuallist.VirtualList;

public interface IAeroMapper extends IBaseAeroMapper {

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
    void save(@NotNull Object... objects);

    /**
     * Save an object in the database. This method will perform a REPLACE on the existing record so any existing
     * data will be overwritten by the data in the passed object
     *
     * @param object The object to save.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    void save(@NotNull Object object, String... binNames);

    /**
     * Save an object in the database with the given WritePolicy. This write policy will override any other set writePolicy so
     * is effectively an upsert operation
     *
     * @param writePolicy The write policy for the save operation.
     * @param object      The object to save.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    void save(@NotNull WritePolicy writePolicy, @NotNull Object object, String... binNames);

    /**
     * Updates the object in the database, merging the record with the existing record. This uses the RecordExistsAction
     * of UPDATE. If bins are specified, only bins with the passed names will be updated (or all of them if null is passed)
     *
     * @param object The object to update.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    void update(@NotNull Object object, String... binNames);

    /**
     * Read a record from the repository and map it to an instance of the passed class, by providing a digest.
     *
     * @param clazz  - The type of the record.
     * @param digest - The Aerospike digest (Unique server hash value generated from set name and user key).
     * @return The returned mapped record.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> T readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest);

    /**
     * This method should not be used: It is used by mappers to correctly resolved dependencies.
     */
    <T> T readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest, boolean resolveDependencies);

    /**
     * Read a record from the repository and map it to an instance of the passed class, by providing a digest.
     *
     * @param readPolicy - The read policy for the read operation.
     * @param clazz      - The type of the record.
     * @param digest     - The Aerospike digest (Unique server hash value generated from set name and user key).
     * @return The returned mapped record.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> T readFromDigest(Policy readPolicy, @NotNull Class<T> clazz, @NotNull byte[] digest);

    /**
     * This method should not be used: It is used by mappers to correctly resolved dependencies.
     */
    <T> T readFromDigest(Policy readPolicy, @NotNull Class<T> clazz, @NotNull byte[] digest, boolean resolveDependencies);

    /**
     * Read a record from the repository and map it to an instance of the passed class.
     *
     * @param clazz   - The type of be returned.
     * @param userKey - The key of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return The returned mapped record.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> T read(@NotNull Class<T> clazz, @NotNull Object userKey);

    /**
     * This method should not be used: It is used by mappers to correctly resolved dependencies.
     */
    <T> T read(@NotNull Class<T> clazz, @NotNull Object userKey, boolean resolveDependencies);

    /**
     * Read a record from the repository and map it to an instance of the passed class.
     *
     * @param readPolicy - The read policy for the read operation.
     * @param clazz      - The type of be returned.
     * @param userKey    - The key of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return The returned mapped record.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> T read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Object userKey);

    /**
     * This method should not be used: It is used by mappers to correctly resolved dependencies.
     */
    <T> T read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Object userKey, boolean resolveDependencies);

    /**
     * Read a batch of records from the repository and map them to an instance of the passed class.
     *
     * @param clazz    - The type of be returned.
     * @param userKeys - The keys of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return The returned mapped records.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> T[] read(@NotNull Class<T> clazz, @NotNull Object... userKeys);

    /**
     * Read a batch of records from the repository and map them to an instance of the passed class.
     *
     * @param batchPolicy A given batch policy.
     * @param clazz       - The type of be returned.
     * @param userKeys    - The keys of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return The returned mapped records.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> T[] read(BatchPolicy batchPolicy, @NotNull Class<T> clazz, @NotNull Object... userKeys);

    /**
     * Delete a record by specifying a class and a user key.
     *
     * @param clazz   - The type of the record.
     * @param userKey - The key of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return whether record existed on server before deletion
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> boolean delete(@NotNull Class<T> clazz, @NotNull Object userKey);

    /**
     * Delete a record by specifying a write policy, a class and a user key.
     *
     * @param writePolicy - The write policy for the delete operation.
     * @param clazz       - The type of the record.
     * @param userKey     - The key of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return whether record existed on server before deletion
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> boolean delete(WritePolicy writePolicy, @NotNull Class<T> clazz, @NotNull Object userKey);

    /**
     * Delete a record by specifying an object.
     *
     * @param object The object to delete.
     * @return whether record existed on server before deletion
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    boolean delete(@NotNull Object object);

    /**
     * Delete a record by specifying a write policy and an object.
     *
     * @param writePolicy - The write policy for the delete operation.
     * @param object      The object to delete.
     * @return whether record existed on server before deletion
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    boolean delete(WritePolicy writePolicy, @NotNull Object object);

    /**
     * Find a record by specifying a class and a Boolean function.
     *
     * @param clazz    - The type of the record.
     * @param function a Boolean function.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    <T> void find(@NotNull Class<T> clazz, Function<T, Boolean> function);

    /**
     * Scan every record in the set associated with the passed class. Each record will be converted to the appropriate class then passed to the
     * processor. If the processor returns true, more records will be processed and if the processor returns false, the scan is aborted.
     * <p/>
     * Depending on the ScanPolicy set up for this class, it is possible for the processor to be called by multiple different
     * threads concurrently, so the processor should be thread-safe
     *
     * @param clazz     - the class used to determine which set to scan and to convert the returned records to.
     * @param processor - the Processor used to process each record
     */
    <T> void scan(@NotNull Class<T> clazz, @NotNull Processor<T> processor);

    /**
     * Scan every record in the set associated with the passed class. Each record will be converted to the appropriate class then passed to the
     * processor. If the processor returns true, more records will be processed and if the processor returns false, the scan is aborted.
     * <p/>
     * Depending on the policy passed or set as the ScanPolicy for this class, it is possible for the processor to be called by multiple different
     * threads concurrently, so the processor should be thread-safe. Note that as a consequence of this, if the processor returns false to abort the
     * scan there is a chance that records are being concurrently processed in other threads and this processing will not be interrupted.
     * <p/>
     *
     * @param policy    - the scan policy to use. If this is null, the default scan policy of the passed class will be used.
     * @param clazz     - the class used to determine which set to scan and to convert the returned records to.
     * @param processor - the Processor used to process each record
     */
    <T> void scan(ScanPolicy policy, @NotNull Class<T> clazz, @NotNull Processor<T> processor);

    /**
     * Scan every record in the set associated with the passed class, limiting the throughput to the specified recordsPerSecond. Each record will be converted
     * to the appropriate class then passed to the
     * processor. If the processor returns true, more records will be processed and if the processor returns false, the scan is aborted.
     * <p/>
     * Depending on the ScanPolicy set up for this class, it is possible for the processor to be called by multiple different
     * threads concurrently, so the processor should be thread-safe
     *
     * @param clazz            - the class used to determine which set to scan and to convert the returned records to.
     * @param processor        - the Processor used to process each record
     * @param recordsPerSecond - the maximum number of records to be processed every second.
     */
    <T> void scan(@NotNull Class<T> clazz, @NotNull Processor<T> processor, int recordsPerSecond);

    /**
     * Scan every record in the set associated with the passed class. Each record will be converted to the appropriate class then passed to the
     * processor. If the processor returns true, more records will be processed and if the processor returns false, the scan is aborted.
     * <p/>
     * Depending on the policy passed or set as the ScanPolicy for this class, it is possible for the processor to be called by multiple different
     * threads concurrently, so the processor should be thread-safe. Note that as a consequence of this, if the processor returns false to abort the
     * scan there is a chance that records are being concurrently processed in other threads and this processing will not be interrupted.
     * <p/>
     *
     * @param policy           - the scan policy to use. If this is null, the default scan policy of the passed class will be used.
     * @param clazz            - the class used to determine which set to scan and to convert the returned records to.
     * @param processor        - the Processor used to process each record
     * @param recordsPerSecond - the number of records to process per second. Set to 0 for unlimited, &gt; 0 for a finite rate, &lt; 0 for no change
     *                         (use the value from the passed policy)
     */
    <T> void scan(ScanPolicy policy, @NotNull Class<T> clazz, @NotNull Processor<T> processor, int recordsPerSecond);

    /**
     * Scan every record in the set associated with the passed class
     * and returns the list of records converted to the appropriate class.
     *
     * @param clazz - the class used to determine which set to scan and to convert the returned records to.
     */
    <T> List<T> scan(@NotNull Class<T> clazz);

    /**
     * Scan every record in the set associated with the passed class using a provided ScanPolicy
     * and returns the list of records converted to the appropriate class.
     *
     * @param policy - the scan policy to use. If this is null, the default scan policy of the passed class will be used.
     * @param clazz  - the class used to determine which set to scan and to convert the returned records to.
     */
    <T> List<T> scan(ScanPolicy policy, @NotNull Class<T> clazz);

    /**
     * Perform a secondary index query with the specified query policy. Each record will be converted
     * to the appropriate class then passed to the processor. If the processor returns false the query is aborted
     * whereas if the processor returns true subsequent records (if any) are processed.
     * <p/>
     * The query policy used will be the one associated with the passed classtype.
     *
     * @param clazz     - the class used to determine which set to scan and to convert the returned records to.
     * @param processor - the Processor used to process each record
     * @param filter    - the filter used to determine which secondary index to use. If this filter is null, every record in the set
     *                  associated with the passed classtype will be scanned, effectively turning the query into a scan
     */
    <T> void query(@NotNull Class<T> clazz, @NotNull Processor<T> processor, Filter filter);

    /**
     * Perform a secondary index query with the specified query policy. Each record will be converted
     * to the appropriate class then passed to the processor. If the processor returns false the query is aborted
     * whereas if the processor returns true subsequent records (if any) are processed.
     *
     * @param policy    - The query policy to use. If this parameter is not passed, the query policy associated with the passed classtype will be used
     * @param clazz     - the class used to determine which set to scan and to convert the returned records to.
     * @param processor - the Processor used to process each record
     * @param filter    - the filter used to determine which secondary index to use. If this filter is null, every record in the set
     *                  associated with the passed classtype will be scanned, effectively turning the query into a scan
     */
    <T> void query(QueryPolicy policy, @NotNull Class<T> clazz, @NotNull Processor<T> processor, Filter filter);

    /**
     * Perform a secondary index query with the specified query policy
     * and returns the list of records converted to the appropriate class.
     * <p/>
     * The query policy used will be the one associated with the passed classtype.
     *
     * @param clazz     - the class used to determine which set to scan and to convert the returned records to.
     * @param filter    - the filter used to determine which secondary index to use. If this filter is null, every record in the set
     *                  associated with the passed classtype will be scanned, effectively turning the query into a scan
     * @return List of records converted to the appropriate class
     */
    <T> List<T> query(@NotNull Class<T> clazz, Filter filter);

    /**
     * Perform a secondary index query with the specified query policy
     * and returns the list of records converted to the appropriate class.
     *
     * @param policy    - The query policy to use. If this parameter is not passed, the query policy associated with the passed classtype will be used
     * @param clazz     - the class used to determine which set to scan and to convert the returned records to.
     * @param filter    - the filter used to determine which secondary index to use. If this filter is null, every record in the set
     *                  associated with the passed classtype will be scanned, effectively turning the query into a scan
     * @return List of records converted to the appropriate class
     */
    <T> List<T> query(QueryPolicy policy, @NotNull Class<T> clazz, Filter filter);

    /**
     * Create a virtual list against an attribute on a class. The list does all operations to the database and does not affect the underlying
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
     * @return A virtual list.
     */
    <T> VirtualList<T> asBackedList(@NotNull Object object, @NotNull String binName, Class<T> elementClazz);

    /**
     * Create a virtual list against an attribute on a class. The list does all operations to the database and does not affect the underlying
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
     * @return A virtual list.
     */
    <T> VirtualList<T> asBackedList(@NotNull Class<?> owningClazz, @NotNull Object key, @NotNull String binName, Class<T> elementClazz);

    IAerospikeClient getClient();
}
