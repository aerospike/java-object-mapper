package com.aerospike.mapper.example.model;

import java.util.Date;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;

@AerospikeRecord(namespace = "test", set = "chkbook")
public class Checkbook {
	private String acctId;
	private long first;
	private long last;
	private final Date issued;
	private boolean recalled;
	private Branch issuer;

	public Checkbook(@ParamFrom("acctId") String acctId, @ParamFrom("first") long first, @ParamFrom("last") long last, @ParamFrom("issued") Date issued) {
		super();
		this.acctId = acctId;
		this.first = first;
		this.last = last;
		this.issued = issued;
	}
	
	public long getFirst() {
		return first;
	}

	public long getLast() {
		return last;
	}

	public Date getIssued() {
		return issued;
	}

	public boolean isRecalled() {
		return recalled;
	}

	public void setRecalled(boolean recalled) {
		this.recalled = recalled;
	}

	public Branch getIssuer() {
		return issuer;
	}

	public void setIssuer(Branch issuer) {
		this.issuer = issuer;
	}
	
	/* The checkbook class does not have a key in it's own right, it uses a composite key */
	@AerospikeKey
	public String getKey() {
		return String.format("%s-%d-%d", acctId, first, last);
	}
	
	@AerospikeKey(setter = true) 
	public void setKey(String key) {
		int index = key.lastIndexOf('-');
		this.last = Long.parseLong(key.substring(index+1));
		int firstIndex = key.lastIndexOf('-', index-1);
		this.first = Long.parseLong(key.substring(firstIndex+1, index));
		this.acctId = key.substring(0, firstIndex);
	}
}
