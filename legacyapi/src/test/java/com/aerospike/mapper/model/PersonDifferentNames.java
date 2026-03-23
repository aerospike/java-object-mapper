package com.aerospike.mapper.model;

import java.util.Base64;
import java.util.Date;
import java.util.List;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AerospikeRecord(namespace = "test", set = "people")
public class PersonDifferentNames {

    @AerospikeKey
    @AerospikeBin(name = "s")
    private String ssn;

    @AerospikeBin(name = "f")
    private String firstName;

    @AerospikeBin(name = "l")
    private String lastName;

    @AerospikeBin(name = "a")
    private int age;

    private Date dateOfBirth;
    private boolean isValid;
    private float balance;
    private double height;
    private byte[] photo;
    private List<String> list;

    public PersonDifferentNames() {
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
