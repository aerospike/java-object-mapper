package com.aerospike.mapper.examples.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.examples.model.accounts.Account;
import lombok.Getter;
import lombok.Setter;

@Getter
@AerospikeRecord(namespace = "test", set = "customer")
public class Customer {
    @AerospikeKey
    @AerospikeBin(name = "id")
    private final String customerId;

    @Setter
    private String firstName;
    @Setter
    private String lastName;

    @Setter
    @AerospikeEmbed
    @AerospikeBin(name = "mail")
    private Address mailingAddress;

    @Setter
    private List<Account> accounts;

    @Setter
    @AerospikeBin(name = "dob")
    private Date dateOfBirth;
    @Setter
    private String phone;
    @Setter
    private Date joinedBank;
    @Setter
    private boolean vip;
    @Setter
    @AerospikeBin(name = "greet")
    private String preferredSalutation;

    public Customer(@ParamFrom("id") String customerId, @ParamFrom("firstName") String firstName, @ParamFrom("lastName") String lastName) {
        super();
        this.customerId = customerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.accounts = new ArrayList<>();
    }

}
