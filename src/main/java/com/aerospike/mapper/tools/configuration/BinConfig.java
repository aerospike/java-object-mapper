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
}
