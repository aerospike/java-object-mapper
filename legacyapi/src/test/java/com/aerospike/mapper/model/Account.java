package com.aerospike.mapper.model;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeExclude;
import com.aerospike.mapper.annotations.AerospikeGetter;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeReference;
import com.aerospike.mapper.annotations.AerospikeSetter;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AerospikeRecord(namespace = "test", set = "account", version = 2)
public class Account {
    @AerospikeKey
    private long id;
    // Allow the "title" bin to be called something else by setting this environment variable. If not set, it will be "title"
    @AerospikeBin(name = "#{ACCOUNT_TITLE_BIN_NAME}")
    private String title;
    private int balance;

    @AerospikeReference
    private Product product;

    @AerospikeExclude
    private int unmapped;

    @AerospikeSetter(name = "bob")
    public void setCraziness(int value) {
        unmapped = value / 3;
    }

    @AerospikeGetter(name = "bob")
    public int getCraziness() {
        return unmapped * 3;
    }

    @Override
    public String toString() {
        return String.format("id: %d, title: %s, balance: %d, unmapped: %d", id, title, balance, unmapped);
    }
}
