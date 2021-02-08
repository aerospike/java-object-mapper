package com.aerospike.mapper.model;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeExclude;
import com.aerospike.mapper.annotations.AerospikeGetter;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeReference;
import com.aerospike.mapper.annotations.AerospikeSetter;

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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public int getUnmapped() {
        return unmapped;
    }

    public void setUnmapped(int unmapped) {
        this.unmapped = unmapped;
    }

    @AerospikeSetter(name = "bob")
    public void setCrazyness(int value) {
        unmapped = value / 3;
    }

    @AerospikeGetter(name = "bob")
    public int getCrazyness() {
        return unmapped * 3;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    @Override
    public String toString() {
        return String.format("id: %d, title: %s, balance: %d, unmapped: %d", id, title, balance, unmapped);
    }
}
