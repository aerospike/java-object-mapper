package com.aerospike.mapper.model.preload;

import com.aerospike.mapper.annotations.AerospikeConstructor;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AerospikeRecord
public abstract class Animal {
    @AerospikeKey
    private final String animalId;

    @AerospikeConstructor
    public Animal(@ParamFrom("animalId") String animalId) {
        super();
        this.animalId = animalId;
    }

    public String getAnimalId() {
        return animalId;
    }
}
