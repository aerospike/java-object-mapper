package com.aerospike.mapper.examples.model;

import java.util.Date;

import com.aerospike.mapper.annotations.AerospikeRecord;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AerospikeRecord
public class Valuation {
    private Date date;
    private long amount;
    private long amountMargin;
    private String valuer;
    private long valuerCompanyId;

    public Valuation() {
    }

    public Valuation(Date date, long amount, long amountMargin, String valuer, long valuerCompanyId, Address valuerAddress) {
        super();
        this.date = date;
        this.amount = amount;
        this.amountMargin = amountMargin;
        this.valuer = valuer;
        this.valuerCompanyId = valuerCompanyId;
    }

}
