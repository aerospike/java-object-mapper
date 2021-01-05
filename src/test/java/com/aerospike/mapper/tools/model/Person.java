package com.aerospike.mapper.tools.model;

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

@AerospikeRecord(namespace="test", set="people")
public class Person {
	
    @AerospikeKey
    @AerospikeBin(name="ssn")
    private String ssn; 

    @AerospikeBin
    private String firstName;
    
    @AerospikeBin(name="lastName")
    private String lastName;
    
    @AerospikeBin(name="age")
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
	
	@AerospikeBin
	private Map<String, Person> map;
	
	@AerospikeBin
	private String[] stringArray;

	@AerospikeBin
	@AerospikeEmbed
	private List<Account> accounts;
	
	@AerospikeBin
//	@AerospikeReference()
	@AerospikeEmbed
	private Account primaryAccount;
	
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

	public List<String> getList() {
		return list;
	}

	public void setList(List<String> list) {
		this.list = list;
	}

	public Map<String, Person> getMap() {
		return map;
	}

	public void setMap(Map<String, Person> map) {
		this.map = map;
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
	
	@Override
    public String toString() {
		byte[] bytes = getPhoto();
		String byteStr = bytes == null ? "null" : Base64.getEncoder().encodeToString(bytes);
    	return String.format("{ssn=%s, firstName=%s, lastName=%s, age=%d, dob=%s, valid=%b, balance=%f, height=%f, photo=%s}", this.getSsn(),
    			this.getFirstName(), this.getLastName(), this.getAge(), this.dateOfBirth == null ? null : this.getDateOfBirth().toString(), this.isValid(), this.getBalance(), getHeight(), byteStr);
    }
}