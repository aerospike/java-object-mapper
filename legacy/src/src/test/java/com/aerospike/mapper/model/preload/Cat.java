package com.aerospike.mapper.model.preload;

import com.aerospike.mapper.annotations.AerospikeConstructor;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@AerospikeRecord(namespace = "test", set = "zoo")
public class Cat extends Animal {

    private final String breed;
    private final String lifeSpan;

    @AerospikeConstructor
    public Cat(@ParamFrom("animalId") String catId, @ParamFrom("breed") String breed,
            @ParamFrom("lifeSpan") String lifeSpan) {
        super(catId);
        this.breed = breed;
        this.lifeSpan = lifeSpan;
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
