package com.aerospike.mapper.tools;

import lombok.Getter;
import lombok.Setter;

/**
 * Holds annotation-derived per-class Aerospike record settings.
 * Decouples ClassCacheEntry from legacy client policy types.
 */
@Getter
@Setter
public class RecordMetaConfig {
    private int ttl = -1;
    private Boolean sendKey = null;
    private Boolean durableDelete = null;
    private Boolean mapAll = null;
}
