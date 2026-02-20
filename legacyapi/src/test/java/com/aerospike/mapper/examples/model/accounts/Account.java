package com.aerospike.mapper.examples.model.accounts;

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
import com.aerospike.mapper.examples.model.Address;
import com.aerospike.mapper.examples.model.Checkbook;
import lombok.Getter;
import lombok.Setter;

@AerospikeRecord(namespace = "test", set = "account")
public class Account {
    @AerospikeKey
    @AerospikeBin(name = "id")
    private final String accountId;
    private final String title;
    private final AccountType type;

    @Setter
    @Getter
    @AerospikeEmbed
    @AerospikeBin(name = "bill")
    private Address billingAddress;

    @Setter
    @AerospikeEmbed
    @AerospikeBin(name = "mail")
    private Address mailingAddress;

    @Setter
    @AerospikeBin(name = "alt")
    @AerospikeEmbed
    private List<Address> alternateAddresses;

    @Setter
    private long balance;
    @Setter
    private String routing;
    @Setter
    @AerospikeBin(name = "odProt")
    private boolean overdraftProtection;
    @Setter
    private boolean card;

    @Setter
    private boolean paperless;
    @Setter
    @AerospikeBin(name = "chkBk")
    private Map<Integer, Checkbook> checkbooks;
    @Setter
    @AerospikeBin(name = "usr")
    private String onlineUserName;
    @Setter
    @AerospikeBin(name = "lstLgn")
    private Date lastLogin;

    public Account(@ParamFrom("id") String accountId, @ParamFrom("custId") String customerId, @ParamFrom("title") String title, @ParamFrom("type") AccountType type) {
        super();
        this.accountId = accountId;
        this.title = title;
        this.type = type;

        alternateAddresses = new ArrayList<>();
        checkbooks = new HashMap<>();
    }

    public List<Address> getAlternateAddresses() {
        return alternateAddresses;
    }

    public long getBalance() {
        return balance;
    }

    public String getRouting() {
        return routing;
    }

    public boolean isOverdraftProtection() {
        return overdraftProtection;
    }

    public boolean isCard() {
        return card;
    }

    public boolean isPaperless() {
        return paperless;
    }

    public Map<Integer, Checkbook> getCheckbooks() {
        return checkbooks;
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

    public Date getLastLogin() {
        return lastLogin;
    }

    public Address getMailingAddress() {
        return mailingAddress;
    }

    public AccountType getType() {
        return type;
    }
}
