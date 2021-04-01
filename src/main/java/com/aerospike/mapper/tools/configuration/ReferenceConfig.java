package com.aerospike.mapper.tools.configuration;

import com.aerospike.mapper.annotations.AerospikeReference.ReferenceType;

public class ReferenceConfig {
	private ReferenceType type;
	private Boolean lazy;
	private Boolean batchLoad;
	
	public ReferenceType getType() {
		return type;
	}
	public Boolean getLazy() {
		return lazy;
	}
	public Boolean getBatchLoad() {
		return lazy;
	}
}
