package com.aerospike.mapper.examples.model;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import lombok.Getter;
import lombok.Setter;

// Addresses are only ever embedded in parent records, so do not need information to map to Aerospike.
@Setter
@Getter
@AerospikeRecord
public class Address {
    @AerospikeKey
    private String line1;
    private String line2;
    private String city;
    private String state;
    @AerospikeBin(name = "zip")
    private String zipCode;

    public Address(
            @ParamFrom("line1") String line1,
            @ParamFrom("line2") String line2,
            @ParamFrom("city") String city,
            @ParamFrom("state") String state,
            @ParamFrom("zip") String zipCode) {
        super();
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
    }

}
