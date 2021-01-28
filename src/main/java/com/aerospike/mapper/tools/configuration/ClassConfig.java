package com.aerospike.mapper.tools.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClassConfig {
	@JsonProperty(value = "class")
	private String className;
	private String namespace;
	private String set;
	private Integer ttl;
	private Integer version;
	private Boolean sendKey;
	private Boolean mapAll;
	private Boolean durableDelete;
	
	public ClassConfig() {
	}

	public String getClassName() {
		return className;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getSet() {
		return set;
	}

	public Integer getTtl() {
		return ttl;
	}

	public Integer getVersion() {
		return version;
	}

	public Boolean getSendKey() {
		return sendKey;
	}

	public Boolean getMapAll() {
		return mapAll;
	}
	
	public Boolean getDurableDelete() {
		return durableDelete;
	}
}