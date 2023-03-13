package com.aerospike.mapper.model.preload;

import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;

import lombok.Data;

@Data
@AerospikeRecord(namespace = "test", set = "zoo")
public class Cat extends Animal {
	
	private String breed;
	private String lifeSpan;

	public Cat(@ParamFrom("animalId") String catId, @ParamFrom("breed") String breed, @ParamFrom("lifeSpan") String lifeSpan) {
		super(catId);
		this.breed = breed;
		this.lifeSpan = lifeSpan;
		
		catId = "" + ((int) (Math.random() * 1000));
	}

	public String getBreed() {
		return breed;
	}
	
	public String getCatId() {
		return super.getAnimalId();
	}
	
	public String getLifeSpan() {
		return lifeSpan;
	}
}
