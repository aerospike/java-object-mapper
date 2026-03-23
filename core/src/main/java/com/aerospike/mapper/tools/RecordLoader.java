package com.aerospike.mapper.tools;

import java.util.List;
import java.util.Map;

/**
 * Abstraction over the Aerospike client's record-fetch operations.
 * Implemented by the legacy module (via IAerospikeClient) and the fluent module (via Session).
 * Used by MappingConverter to resolve @AerospikeReference objects without a direct client dependency.
 */
public interface RecordLoader {
    /**
     * Fetch a single record by its user-key value. Returns null if not found.
     */
    Map<String, Object> getRecord(String namespace, String setName, Object keyValue);

    /**
     * Fetch a single record by its pre-computed digest. Returns null if not found.
     */
    Map<String, Object> getRecordByDigest(String namespace, String setName, byte[] digest);

    /**
     * Batch-fetch records. Entries with no matching record are returned as null in the list.
     */
    List<Map<String, Object>> getBatchRecords(List<RecordKey> keys);

    /**
     * Compute the Aerospike record digest for the given set and user key.
     * Used for lazy-loaded @AerospikeReference objects stored by digest.
     */
    byte[] computeDigest(String setName, Object userKey);
}
