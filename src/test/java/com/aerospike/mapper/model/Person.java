package com.aerospike.mapper.model;

import com.aerospike.mapper.annotations.*;

import java.util.*;

// The set name will read a system definition of people.set.name for the set name. If not set, it will use "people". For example:
// -Dpeople.set.name=persons
@AerospikeRecord(namespace = "test", set = "${people.set.name:people}")
public class Person {

    @AerospikeKey
    @AerospikeBin(name = "ssn")
    private String ssn;

    @AerospikeBin
    private String firstName;

    @AerospikeBin(name = "lastName")
    private String lastName;

    @AerospikeBin(name = "age")
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
    private long[] longData;

    @AerospikeBin
    @AerospikeEmbed(elementType = AerospikeEmbed.EmbedType.MAP)
    private Account[] accountArray;

    @AerospikeBin
    private List<String> stringList;

    @AerospikeBin
    private String[] stringArray;

    @AerospikeBin
    @AerospikeEmbed(elementType = AerospikeEmbed.EmbedType.LIST)
    private List<Account> accounts;

    @AerospikeBin
    @AerospikeEmbed(elementType = AerospikeEmbed.EmbedType.LIST)
    private Map<String, Product> productMap;

    @AerospikeBin
    private Map<Integer, String> testMap;

    @AerospikeBin
    @AerospikeEmbed(type = AerospikeEmbed.EmbedType.LIST)
    private Account primaryAccount;

    @AerospikeBin(name = "2ndAcc")
    @AerospikeEmbed(type = AerospikeEmbed.EmbedType.MAP)
    private Account secondaryAccount;

    @AerospikeBin(name = "3rdAcc")
    @AerospikeReference()
    private Account tertiaryAccount;

    public Person() {
        accounts = new ArrayList<Account>();
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

    public List<String> getStringList() {
        return stringList;
    }

    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
    }

    public Map<String, Product> getProductMap() {
        return productMap;
    }

    public void setProductMap(Map<String, Product> productMap) {
        this.productMap = productMap;
    }

    public Map<Integer, String> getTestMap() {
        return testMap;
    }

    public void setTestMap(Map<Integer, String> testMap) {
        this.testMap = testMap;
    }

    public String[] getStringArray() {
        return stringArray;
    }

    public void setStringArray(String[] stringArray) {
        this.stringArray = stringArray;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    public Account getPrimaryAccount() {
        return primaryAccount;
    }

    public void setPrimaryAccount(Account primaryAccount) {
        this.primaryAccount = primaryAccount;
    }

    public Account getSecondaryAccount() {
        return secondaryAccount;
    }

    public void setSecondaryAccount(Account secondaryAccount) {
        this.secondaryAccount = secondaryAccount;
    }

    public Account getTertiaryAccount() {
        return tertiaryAccount;
    }

    public void setTertiaryAccount(Account tertiaryAccount) {
        this.tertiaryAccount = tertiaryAccount;
    }

    public long[] getLongData() {
        return longData;
    }

    public void setLongData(long[] longData) {
        this.longData = longData;
    }

    public Account[] getAccountArray() {
        return accountArray;
    }

    public void setAccountArray(Account[] accountArray) {
        this.accountArray = accountArray;
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
