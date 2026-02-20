package com.aerospike.mapper.tools;

import java.util.Arrays;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecordKey)) return false;
        RecordKey that = (RecordKey) o;
        return Objects.equals(namespace, that.namespace)
                && Objects.equals(setName, that.setName)
                && Objects.equals(keyValue, that.keyValue)
                && Arrays.equals(digest, that.digest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, setName, keyValue, Arrays.hashCode(digest));
    }
}
