package com.aerospike.mapper.examples.model;

import java.time.Instant;

import com.aerospike.mapper.annotations.AerospikeRecord;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AerospikeRecord
public class Transaction {
    private long amount;
    private String description;
    private Instant time;
    private boolean isFraud;

}
