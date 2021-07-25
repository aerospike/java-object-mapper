package com.aerospike.mapper.examples.model.accounts;

import java.util.Date;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.examples.model.InterestType;
import com.aerospike.mapper.examples.model.Property;

// Loan account rolls up under the Account
@AerospikeRecord
public class LoanAccount extends Account {
	
	@AerospikeBin(name = "prop")
	private Property securityProperty;
	@AerospikeBin(name = "intType")
	private InterestType interestType;
	@AerospikeBin(name="orig")
	private final Date originationDate;
	@AerospikeBin(name="exp")
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

	public Property getSecurityProperty() {
		return securityProperty;
	}

	public void setSecurityProperty(Property securityProperty) {
		this.securityProperty = securityProperty;
	}

	public InterestType getInterestType() {
		return interestType;
	}

	public void setInterestType(InterestType interestType) {
		this.interestType = interestType;
	}

	public Date getOriginationDate() {
		return originationDate;
	}

	public Date getExpirationDate() {
		return expirationDate;
	}

	public float getRate() {
		return rate;
	}
}
