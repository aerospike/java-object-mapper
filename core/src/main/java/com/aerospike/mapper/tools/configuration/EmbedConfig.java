package com.aerospike.mapper.tools.configuration;

import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmbedConfig {
    private EmbedType type;
    private EmbedType elementType;
    private Boolean saveKey;
}
