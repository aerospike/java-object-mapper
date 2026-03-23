package com.aerospike.mapper.tools.configuration;

import com.aerospike.mapper.annotations.AerospikeReference.ReferenceType;
import lombok.Getter;

@Getter
public class ReferenceConfig {
    private ReferenceType type;
    private Boolean lazy;
    private Boolean batchLoad;

    public ReferenceConfig() {
    }

    public ReferenceConfig(ReferenceType type, boolean lazy, boolean batchLoad) {
        this.type = type;
        this.lazy = lazy;
        this.batchLoad = batchLoad;
    }
}
