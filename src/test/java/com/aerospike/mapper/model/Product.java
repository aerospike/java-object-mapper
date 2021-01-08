package com.aerospike.mapper.model;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;

@AerospikeRecord(namespace = "test", set = "product", ttl = 0, mapAll = true)
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProductType getType() {
        return type;
    }

    public void setType(ProductType type) {
        this.type = type;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
