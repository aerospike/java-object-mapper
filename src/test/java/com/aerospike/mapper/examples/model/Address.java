package com.aerospike.mapper.examples.model;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;

// Addresses are only ever embedded in parent records, so do not need information to map to Aerospike.
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

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
}
