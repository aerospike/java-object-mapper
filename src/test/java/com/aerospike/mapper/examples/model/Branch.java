package com.aerospike.mapper.examples.model;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;

@AerospikeRecord(namespace = "test", set = "branch")
public class Branch {
	@AerospikeKey
	private final String id;
	@AerospikeBin(name="addr")
	@AerospikeEmbed
	private final Address address;
	private final String name;
	public Branch(@ParamFrom("id") String id, @ParamFrom("addr") Address address, @ParamFrom("name") String name) {
		super();
		this.id = id;
		this.address = address;
		this.name = name;
	}
	public String getId() {
		return id;
	}
	public Address getAddress() {
		return address;
	}
	public String getName() {
		return name;
	}
}
