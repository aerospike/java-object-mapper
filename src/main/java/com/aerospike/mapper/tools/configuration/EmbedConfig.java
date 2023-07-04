package com.aerospike.mapper.tools.configuration;

import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;

public class EmbedConfig {
    private EmbedType type;
    private EmbedType elementType;
    private Boolean saveKey;

    public EmbedType getType() {
        return type;
    }

    public EmbedType getElementType() {
        return elementType;
    }

    public Boolean getSaveKey() {
        return saveKey;
    }

    public void setType(EmbedType type) {
        this.type = type;
    }

    public void setElementType(EmbedType elementType) {
        this.elementType = elementType;
    }

    public void setSaveKey(Boolean saveKey) {
        this.saveKey = saveKey;
    }
    
    
}
