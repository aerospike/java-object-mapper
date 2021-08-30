package com.aerospike.mapper.examples.model;

import java.util.Date;

import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.annotations.AerospikeRecord;

@AerospikeRecord
public class Valuation {
    private Date date;
    private long amount;
    private long amountMargin;
    private String valuer;
    private long valuerCompanyId;
    @AerospikeEmbed(type = EmbedType.LIST)
    private Address valuerAddress;

    public Valuation() {
    }

    public Valuation(Date date, long amount, long amountMargin, String valuer, long valuerCompanyId, Address valuerAddress) {
        super();
        this.date = date;
        this.amount = amount;
        this.amountMargin = amountMargin;
        this.valuer = valuer;
        this.valuerCompanyId = valuerCompanyId;
        this.valuerAddress = valuerAddress;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public long getAmountMargin() {
        return amountMargin;
    }

    public void setAmountMargin(long amountMargin) {
        this.amountMargin = amountMargin;
    }

    public String getValuer() {
        return valuer;
    }

    public void setValuer(String valuer) {
        this.valuer = valuer;
    }

    public long getValuerCompanyId() {
        return valuerCompanyId;
    }

    public void setValuerCompanyId(long valuerCompanyId) {
        this.valuerCompanyId = valuerCompanyId;
    }
}
