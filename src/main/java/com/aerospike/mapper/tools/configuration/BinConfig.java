package com.aerospike.mapper.tools.configuration;

import org.apache.commons.lang3.StringUtils;

import com.aerospike.client.AerospikeException;

public class BinConfig {
    private String name;
    private String field;
    private Boolean useAccessors;
    private String getter;
    private String setter;
    private Boolean exclude;
    private Integer ordinal;
    private EmbedConfig embed;
    private ReferenceConfig reference;
    private Boolean generation;

    public String getName() {
        return name;
    }

    public String getField() {
        return field;
    }

    public Boolean getUseAccessors() {
        return useAccessors;
    }

    public String getGetter() {
        return getter;
    }

    public String getSetter() {
        return setter;
    }

    public Boolean isExclude() {
        return exclude;
    }

    public Integer getOrdinal() {
        return ordinal;
    }

    public EmbedConfig getEmbed() {
        return embed;
    }

    public ReferenceConfig getReference() {
        return reference;
    }

    public Boolean isGeneration() {
        return generation;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setField(String field) {
        this.field = field;
    }

    public void setUseAccessors(Boolean useAccessors) {
        this.useAccessors = useAccessors;
    }

    public void setGetter(String getter) {
        this.getter = getter;
    }

    public void setSetter(String setter) {
        this.setter = setter;
    }

    public void setExclude(Boolean exclude) {
        this.exclude = exclude;
    }

    public void setOrdinal(Integer ordinal) {
        this.ordinal = ordinal;
    }

    public void setEmbed(EmbedConfig embed) {
        this.embed = embed;
    }

    public void setReference(ReferenceConfig reference) {
        this.reference = reference;
    }

    public void setGeneration(Boolean generation) {
        this.generation = generation;
    }

    public void validate(String className) {
        if (StringUtils.isBlank(this.name) && StringUtils.isBlank(this.field)) {
            throw new AerospikeException("Configuration for class " + className + " defines a bin which contains neither a name nor a field");
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
