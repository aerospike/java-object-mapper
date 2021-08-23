package com.aerospike.mapper.tools;

import java.util.function.Function;

import javax.validation.constraints.NotNull;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.Filter;
import com.aerospike.mapper.tools.virtuallist.VirtualList;

public interface IAeroMapper extends IBaseAeroMapper {

    void save(@NotNull Object... objects);

    void save(@NotNull Object object, String... binNames);

    void save(@NotNull WritePolicy writePolicy, @NotNull Object object, String... binNames);

    void update(@NotNull Object object, String... binNames);

    <T> T readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest);

    <T> T readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest, boolean resolveDependencies);

    <T> T readFromDigest(Policy readPolicy, @NotNull Class<T> clazz, @NotNull byte[] digest);

    <T> T readFromDigest(Policy readPolicy, @NotNull Class<T> clazz, @NotNull byte[] digest, boolean resolveDependencies);

    <T> T read(@NotNull Class<T> clazz, @NotNull Object userKey);

    <T> T read(@NotNull Class<T> clazz, @NotNull Object userKey, boolean resolveDependencies);

    <T> T read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Object userKey);

    <T> T read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Object userKey, boolean resolveDependencies);

    <T> T[] read(@NotNull Class<T> clazz, @NotNull Object... userKeys);

    <T> T[] read(BatchPolicy batchPolicy, @NotNull Class<T> clazz, @NotNull Object... userKeys);

    <T> boolean delete(@NotNull Class<T> clazz, @NotNull Object userKey);

    <T> boolean delete(WritePolicy writePolicy, @NotNull Class<T> clazz, @NotNull Object userKey);

    boolean delete(@NotNull Object object);

    boolean delete(WritePolicy writePolicy, @NotNull Object object);

    <T> VirtualList<T> asBackedList(@NotNull Object object, @NotNull String binName, Class<T> elementClazz);

    <T> VirtualList<T> asBackedList(@NotNull Class<?> owningClazz, @NotNull Object key, @NotNull String binName, Class<T> elementClazz);

    <T> void find(@NotNull Class<T> clazz, Function<T, Boolean> function);

    IAerospikeClient getClient();
    
    /**
     * Perform a secondary index query with the specified query policy. Each record will be converted 
	 * to the appropriate class then passed to the processor. If the processor returns false the query is aborted
	 * whereas if the processor returns true subsequent records (if any) are processed.
	 * <p/>
	 * The query policy used will be the one associated with the passed classtype. 
	 * @param clazz     - the class used to determine which set to scan and to convert the returned records to.
	 * @param processor - the Processor used to process each record
	 * @param filter	- the filter used to determine which secondary index to use. If this filter is null, every record in the set
	 * 		associated with the passed classtype will be scanned, effectively turning the query into a scan
     */
    <T> void query(@NotNull Class<T> clazz, @NotNull Processor<T> processor, Filter filter);

    /**
     * Perform a secondary index query with the specified query policy. Each record will be converted 
	 * to the appropriate class then passed to the processor. If the processor returns false the query is aborted
	 * whereas if the processor returns true subsequent records (if any) are processed. 
	 * @param policy	- The query policy to use. If this parameter is not passed, the query policy associated with the passed classtype will be used 
	 * @param clazz     - the class used to determine which set to scan and to convert the returned records to.
	 * @param processor - the Processor used to process each record
	 * @param filter	- the filter used to determine which secondary index to use. If this filter is null, every record in the set
	 * 		associated with the passed classtype will be scanned, effectively turning the query into a scan
     */
	<T> void query(QueryPolicy policy, @NotNull Class<T> clazz, @NotNull Processor<T> processor, Filter filter);

	/**
	 * Scan every record in the set associated with the passed class. Each record will be converted to the appropriate class then passed to the
	 * processor. If the processor returns true, more records will be processed and if the processor returns false, the scan is aborted.
	 * <p/>
	 * Depending on the ScanPolicy set up for this class, it is possible for the processor to be called by multiple different
	 * threads concurrently, so the processor should be thread-safe
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
	 * @param clazz     - the class used to determine which set to scan and to convert the returned records to.
	 * @param processor - the Processor used to process each record
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
	 * @param policy    - the scan policy to use. If this is null, the default scan policy of the passed class will be used.
	 * @param clazz     - the class used to determine which set to scan and to convert the returned records to.
	 * @param processor - the Processor used to process each record
	 * @param recordsPerSecond - the number of records to process per second. Set to 0 for unlimited, &gt; 0 for a finite rate, &lt; 0 for no change
	 * 		(use the value from the passed policy) 
	 */
	<T> void scan(ScanPolicy policy, @NotNull Class<T> clazz, @NotNull Processor<T> processor, int recordsPerSecond);
}
