package com.aerospike.mapper.example.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.example.model.accounts.Account;

@AerospikeRecord(namespace = "test", set = "customer")
public class Customer {
	@AerospikeKey
	@AerospikeBin(name = "id")
	private final String customerId;
	
	private String firstName;
	private String lastName;
	
	@AerospikeEmbed
	@AerospikeBin(name = "mail")
	private Address mailingAddress;
	
	private List<Account> accounts;
	
	@AerospikeBin(name = "dob")
	private Date dateOfBirth;
	private String phone;
	private Date joinedBank;
	private boolean vip;
	@AerospikeBin(name = "greet")
	private String preferredSalutation;

	public Customer(@ParamFrom("id") String customerId, @ParamFrom("firstName") String firstName, @ParamFrom("lastName") String lastName) {
		super();
		this.customerId = customerId;
		this.firstName = firstName;
		this.lastName = lastName;
		this.accounts = new ArrayList<>();
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Address getMailingAddress() {
		return mailingAddress;
	}

	public void setMailingAddress(Address mailingAddress) {
		this.mailingAddress = mailingAddress;
	}

	public List<Account> getAccounts() {
		return accounts;
	}

	public void setAccounts(List<Account> accounts) {
		this.accounts = accounts;
	}

	public String getCustomerId() {
		return customerId;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public Date getJoinedBank() {
		return joinedBank;
	}

	public void setJoinedBank(Date joinedBank) {
		this.joinedBank = joinedBank;
	}

	public boolean isVip() {
		return vip;
	}

	public void setVip(boolean vip) {
		this.vip = vip;
	}

	public String getPreferredSalutation() {
		return preferredSalutation;
	}

	public void setPreferredSalutation(String preferredSalutation) {
		this.preferredSalutation = preferredSalutation;
	}
}
