package com.aerospike.mapper.tools;

/**
 * Client-agnostic identifier for an Aerospike record.
 * Used by RecordLoader for batch operations.
 */
public class RecordKey {
    public final String namespace;
    public final String setName;
    /** User-key value; null when the record is identified by digest only. */
    public final Object keyValue;
    /** Pre-computed digest; null when the record is identified by keyValue. */
    public final byte[] digest;

    public RecordKey(String namespace, String setName, Object keyValue) {
        this.namespace = namespace;
        this.setName = setName;
        this.keyValue = keyValue;
        this.digest = null;
    }

    public RecordKey(String namespace, String setName, byte[] digest) {
        this.namespace = namespace;
        this.setName = setName;
        this.keyValue = null;
        this.digest = digest;
    }
}
