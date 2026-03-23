package com.aerospike.mapper.model;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AerospikeRecord(namespace = "test", set = "product")
public class Product {

    private long id;
    private String name;
    private ProductType type;
    private int version;

    public Product() {
    }

    public Product(long id, int version, String name, ProductType type) {
        super();
        this.id = id;
        this.name = name;
        this.type = type;
        this.version = version;
    }

    @AerospikeKey
    public String getKey() {
        return id + ":" + version;
    }

}
