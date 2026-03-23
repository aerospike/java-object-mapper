package com.aerospike.mapper.examples.model.accounts;

import java.util.Date;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.examples.model.InterestType;
import com.aerospike.mapper.examples.model.Property;
import lombok.Getter;
import lombok.Setter;

// Loan account rolls up under the Account
@Getter
@AerospikeRecord
public class LoanAccount extends Account {

    @Setter
    @AerospikeBin(name = "prop")
    private Property securityProperty;
    @Setter
    @AerospikeBin(name = "intType")
    private InterestType interestType;
    @AerospikeBin(name = "orig")
    private final Date originationDate;
    @AerospikeBin(name = "exp")
    private final Date expirationDate;
    private final float rate;

    public LoanAccount(
            @ParamFrom("id") String accountId,
            @ParamFrom("custId") String customerId,
            @ParamFrom("title") String title,
            @ParamFrom("type") AccountType type,
            @ParamFrom("orig") Date originationDate,
            @ParamFrom("exp") Date expirationDate,
            @ParamFrom("rate") float rate) {
        super(accountId, customerId, title, type);
        this.rate = rate;
        this.expirationDate = expirationDate;
        this.originationDate = originationDate;
    }

}
