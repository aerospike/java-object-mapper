package com.aerospike.mapper.model;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;

import java.util.Base64;
import java.util.Date;
import java.util.List;

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

    @AerospikeBin
    private Date dateOfBirth;

    @AerospikeBin
    private boolean isValid;

    @AerospikeBin
    private float balance;

    @AerospikeBin
    private double height;

    @AerospikeBin
    private byte[] photo;

    @AerospikeBin
    private List<String> list;

    public PersonDifferentNames() {
    }

    public String getSsn() {
        return ssn;
    }

    public void setSsn(String ssn) {
        this.ssn = ssn;
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

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }

    public float getBalance() {
        return balance;
    }

    public void setBalance(float balance) {
        this.balance = balance;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public List<String> getList() {
        return list;
    }

    public void setList(List<String> list) {
        this.list = list;
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
