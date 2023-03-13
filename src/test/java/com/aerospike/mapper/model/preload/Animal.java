package com.aerospike.mapper.model.preload;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;

@AerospikeRecord
public abstract class Animal {
	@AerospikeKey
	private String animalId;

	public Animal(@ParamFrom("animalId") String animalId) {
		super();
		this.animalId = animalId;
	}
	
	public String getAnimalId() {
		return animalId;
	}
}
