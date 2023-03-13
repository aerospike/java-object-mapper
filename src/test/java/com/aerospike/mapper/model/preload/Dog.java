package com.aerospike.mapper.model.preload;

import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;

import lombok.Data;

@Data
@AerospikeRecord(namespace = "test", set = "zoo")
public class Dog extends Animal{
		
    private String breed;
    
    public Dog(@ParamFrom("animalId") String dogId, @ParamFrom("breed") String breed) {
    	super(dogId);
    	this.breed = breed;
    }
    
    public String getBreed() {
		return breed;
	}
    
    public String getDogId() {
		return super.getAnimalId();
	}
}
