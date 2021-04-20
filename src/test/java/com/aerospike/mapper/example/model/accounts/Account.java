package com.aerospike.mapper.example.model.accounts;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.example.model.Address;
import com.aerospike.mapper.example.model.Checkbook;

@AerospikeRecord(namespace = "test", set = "account")
public class Account {
	@AerospikeKey
	@AerospikeBin(name = "id")
	private final String accountId;
	private final String title;
	private final AccountType type;

	@AerospikeBin(name = "custId")
	private final String customerId;

	@AerospikeEmbed
	@AerospikeBin(name = "bill")
	private Address billingAddress;
	
	@AerospikeEmbed
	@AerospikeBin(name = "mail")
	private Address mailingAddress;
	
	@AerospikeBin(name = "alt")
	@AerospikeEmbed
	private List<Address> alternateAddresses;
	
	private long balance;
	private String routing;
	@AerospikeBin(name = "odProt")
	private boolean overdraftProtection;
	private boolean card;
	
	private boolean paperless;
	@AerospikeBin(name = "chkBk")
	private Map<Integer, Checkbook> checkbooks;
	@AerospikeBin(name = "usr")
	private String onlineUserName;
	@AerospikeBin(name = "lstLgn")
	private Date lastLogin;
	
	public Account(@ParamFrom("id") String accountId, @ParamFrom("custId") String customerId, @ParamFrom("title") String title, @ParamFrom("type") AccountType type) {
		super();
		this.accountId = accountId;
		this.title = title;
		this.customerId = customerId;
		this.type = type;
		
		alternateAddresses = new ArrayList<>();
		checkbooks = new HashMap<>();
	}
	
	public Address getBillingAddress() {
		return billingAddress;
	}
	public void setBillingAddress(Address billingAddress) {
		this.billingAddress = billingAddress;
	}
	public List<Address> getAlternateAddresses() {
		return alternateAddresses;
	}
	public void setAlternateAddresses(List<Address> alternateAddresses) {
		this.alternateAddresses = alternateAddresses;
	}
	public long getBalance() {
		return balance;
	}
	public void setBalance(long balance) {
		this.balance = balance;
	}
	public String getRouting() {
		return routing;
	}
	public void setRouting(String routing) {
		this.routing = routing;
	}
	public boolean isOverdraftProtection() {
		return overdraftProtection;
	}
	public void setOverdraftProtection(boolean overdraftProtection) {
		this.overdraftProtection = overdraftProtection;
	}
	public boolean isCard() {
		return card;
	}
	public void setCard(boolean card) {
		this.card = card;
	}
	public boolean isPaperless() {
		return paperless;
	}
	public void setPaperless(boolean paperless) {
		this.paperless = paperless;
	}
	public Map<Integer, Checkbook> getCheckbooks() {
		return checkbooks;
	}
	public void setCheckbooks(Map<Integer, Checkbook> checkbooks) {
		this.checkbooks = checkbooks;
	}
	public String getAccountId() {
		return accountId;
	}
	public String getTitle() {
		return title;
	}

	public String getOnlineUserName() {
		return onlineUserName;
	}

	public void setOnlineUserName(String onlineUserName) {
		this.onlineUserName = onlineUserName;
	}

	public Date getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(Date lastLogin) {
		this.lastLogin = lastLogin;
	}

	public Address getMailingAddress() {
		return mailingAddress;
	}

	public void setMailingAddress(Address mailingAddress) {
		this.mailingAddress = mailingAddress;
	}
	
	public AccountType getType() {
		return type;
	}
}
