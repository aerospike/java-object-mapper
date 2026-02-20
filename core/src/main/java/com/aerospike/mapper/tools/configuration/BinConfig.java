package com.aerospike.mapper.tools.configuration;

import com.aerospike.mapper.exceptions.AerospikeMapperException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
public class BinConfig {
    private String name;
    private String field;
    private Boolean useAccessors;
    private String getter;
    private String setter;
    @Getter(AccessLevel.NONE)
    private Boolean exclude;
    private Integer ordinal;
    private EmbedConfig embed;
    private ReferenceConfig reference;
    @Getter(AccessLevel.NONE)
    private Boolean generation;

    public Boolean isExclude() {
        return exclude;
    }

    public Boolean isGeneration() {
        return generation;
    }

    public void validate(String className) {
        if (StringUtils.isBlank(this.name) && StringUtils.isBlank(this.field)) {
            throw new AerospikeMapperException("Configuration for class " + className + " defines a bin which contains neither a name nor a field");
        }
    }

    public String getDerivedName() {
        if (!StringUtils.isBlank(this.name)) {
            return this.name;
        }
        return this.field;
    }
    
    public BinConfig merge(BinConfig other) {
        if (this.name == null && other.name != null) {
            this.name = other.name;
        }
        if (this.field == null && other.field != null) {
            this.field = other.field;
        }
        if (this.useAccessors == null && other.useAccessors != null) {
            this.useAccessors = other.useAccessors;
        }
        if (this.getter == null && other.getter != null) {
            this.getter = other.getter;
        }
        if (this.setter == null && other.setter != null) {
            this.setter = other.setter;
        }
        if (this.exclude == null && other.exclude != null) {
            this.exclude = other.exclude;
        }
        if (this.ordinal == null && other.ordinal != null) {
            this.ordinal = other.ordinal;
        }
        if (this.embed == null && other.embed != null) {
            this.embed = other.embed;
        }
        if (this.reference == null && other.reference != null) {
            this.reference = other.reference;
        }
        if (this.generation == null && other.generation != null) {
            this.generation = other.generation;
        }
        return this;
    }
}
