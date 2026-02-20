package com.aerospike.mapper.model;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeReference;
import lombok.Getter;
import lombok.Setter;

// The set name will read a system definition of people.set.name for the set name. If not set, it will use "people". For example:
// -Dpeople.set.name=persons
@Setter
@Getter
@AerospikeRecord(namespace = "test", set = "${people.set.name:people}")
public class Person {

    @AerospikeKey
    private String ssn;

    private String firstName;
    private String lastName;
    private int age;
    private Date dateOfBirth;
    private boolean isValid;
    private float balance;
    private double height;
    private byte[] photo;
    private long[] longData;

    @AerospikeEmbed(elementType = AerospikeEmbed.EmbedType.MAP)
    private Account[] accountArray;

    private List<String> stringList;
    private String[] stringArray;

    @AerospikeEmbed(elementType = AerospikeEmbed.EmbedType.LIST)
    private List<Account> accounts;

    @AerospikeEmbed(elementType = AerospikeEmbed.EmbedType.LIST)
    private Map<String, Product> productMap;

    private Map<Integer, String> testMap;

    @AerospikeEmbed(type = AerospikeEmbed.EmbedType.LIST)
    private Account primaryAccount;

    @AerospikeBin(name = "2ndAcc")
    @AerospikeEmbed(type = AerospikeEmbed.EmbedType.MAP)
    private Account secondaryAccount;

    @AerospikeBin(name = "3rdAcc")
    @AerospikeReference()
    private Account tertiaryAccount;

    public Person() {
        accounts = new ArrayList<>();
    }


    @Override
    public String toString() {
        byte[] bytes = getPhoto();
        String byteStr = bytes == null ? "null" : Base64.getEncoder().encodeToString(bytes);
        return String.format("{ssn=%s, firstName=%s, lastName=%s, age=%d, dob=%s, valid=%b, balance=%f, height=%f, photo=%s}",
                this.getSsn(), this.getFirstName(), this.getLastName(), this.getAge(),
                this.dateOfBirth == null ? null : this.getDateOfBirth().toString(), this.isValid(),
                this.getBalance(), getHeight(), byteStr);
    }
}
