package com.aerospike.mapper.tools.configuration;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

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
	private KeyConfig key;
	private String shortName;
	private List<BinConfig> bins;
	
	public ClassConfig() {
		bins = new ArrayList<>();
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
	
	public String getShortName() {
		return shortName;
	}
	
	public KeyConfig getKey() {
		return key;
	}
	public List<BinConfig> getBins() {
		return bins;
	}
	
	public BinConfig getBinByName(@NotNull String name) {
		if (bins == null) {
			return null;
		}
		for (BinConfig thisBin : bins) {
			if (name.equals(thisBin.getName())) {
				return thisBin;
			}
		}
		return null;
	}
	
	public BinConfig getBinByGetterName(@NotNull String getterName) {
		if (bins == null) {
			return null;
		}
		for (BinConfig thisBin : bins) {
			if (getterName.equals(thisBin.getGetter())) {
				return thisBin;
			}
		}
		return null;
	}
	
	public BinConfig getBinByFieldName(@NotNull String fieldName) {
		if (bins == null) {
			return null;
		}
		for (BinConfig thisBin : bins) {
			if (fieldName.equals(thisBin.getField())) {
				return thisBin;
			}
		}
		return null;
	}
	public void validate() {
		if (this.bins != null) {
			for (BinConfig thisBin : bins) {
				thisBin.validate(this.className);
			}
		}
	}
}