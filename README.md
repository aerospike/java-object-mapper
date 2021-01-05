# aerospike-tools
Tools for interacting with the Aerospike database

# Aerospike Annotations

Annotations simplify reading and writing objects to the Aerospike database. For example, consider the code below that saves a Person object to Aerospike.

```java
@AerospikeRecord(namespace="test", set="people")
public class Person {
	
    @AerospikeKey
    @AerospikeBin(name="ssn")
    private String ssn; 

    @AerospikeBin(name="firstName")
    private String firstName;
    
    @AerospikeBin(name="lastName")
    private String lastName;
    
    @AerospikeBin(name="age")
    private long age;
    
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

    public long getAge() {
	return age;
    }

    public void setAge(int age) {
	this.age = age;
    }	
}
```

To write person to Aerospike, simple use:

```java
Person p = new Person();
p.setFirstName("John");
p.setLastName("Doe");
p.setSsn("123456789");
p.setAge(17);

AerospikeClient client = new AerospikeClient("aerospike hostname",3000);
AeroMapper mapper = new AeroMapper(client);
mapper.save("test",p);
```
 
To read:
 
```java
Person person = mapper.read(Person.class, "123456789");
```
 
To delete:
 
```java
 mapper.delete(person);
```
 
To find:
 
```java
Function<Person,Boolean> function = person -> {
    System.out.println(person.getSsn());
    return true;
};
        	
mapper.find(Person.class, function);
```

## Keys
The key to an Aerospike record can be specified either as a field or a property. Remember that Aerospike keys can be Strings, integer types and binary types only.

To use a field as the key, simply mark the field with the AerospikeKey annotation:

```java
@AerospikeKey
private int personId;
```

If a function is to be used as a key, the function must be declared as to have no parameters and return non-void. The visibility of the method does not matter.

```java
@AerospikeKey
public String getKey() {
	return this.keyPart1 + ":" + this.keyPart2;
}
```

Note that it is not required to have a key on an object annotated with @AerospikeRecord. This is because an object can be embedded in another object (as a map or list) and hence not require a key to identify it to the database.

Also, the existence of @AerospikeKey on a field does not imply that the field will get stored in the database explicitly. Use @AerospikeBin or mapAll attribute to ensure that the key gets mapped to the database too.

## Fields
Fields in Java can be mapped to the database irrespective of the visibility of the field. To do so, simply specify the bin to map to with the @AerospikeBin annotation:

```java
@AerospikeBin(name = "vrsn")
private int version;
```

This will map the contents of the version field to a `vrsn` bin in Aerospike. 

If the name of the bin matches the name of the field in Java, the name can be omitted:

```java
@AerospikeBin
private int age;
```

This will appear in Aerospike as the `age` bin.

If all fields should be mapped to Aerospike with the default name, the `mapAll = true` property can be set on the class @AerospikeRecord annotation and the other annotations omitted:

```java
@AerospikeRecord(namespace = NAMESPACE, set = "testSet", mapAll = true)
public static class Test {
	public int a;
	public int b;
	public int c;
	public int d;
}
```

This saves the record with 4 bins, a,b,c,d.

You can force the name of a particular bin or bins by specifying them in an AerospikeBin:

```java
@AerospikeRecord(namespace = NAMESPACE, set = "testSet", mapAll = true)
public static class Test {
	public int a;
	public int b;
	@AerospikeBin(name = "longCname")
	public int c;
	public int d;
}
```
This will save 4 fields in the database, a, b, longCname, d.

Fields can also be omitted with the @AerospikeExclude annotation:

```java
@AerospikeRecord(namespace = NAMESPACE, set = "testSet", mapAll = true)
public static class Test {
	public int a;
	@AerospikeExclude
	public int b;
	public int c;
	public int d;
}
```

This saves the record with 3 bins, a,c,d. If a field is marked with both @AerospikeExclude and @AerospikeBin, the bin will _not_ be mapped to the database.

## Properties
A pair of methods comprising a getter and setter can also be mapped to a field in the database. These should be annotated with @AerospikeGetter and @AerospikeSetter respectively and the name attribute of these annotations must be provided. The getter must take no arguments and return something, and the setter must return void and take 1 parameter of the same type as the getter return value. Both a setter and a getter must be provided, an exception will be thrown otherwise.

Let's look at an example:

```java
@AerospikeSetter(name="bob")
public void setCrazyness(int value) {
	unmapped = value/3;
}
@AerospikeGetter(name="bob")
public int getCrazyness() {
	return unmapped*3;
}
```

This will create a bin in the database with the name "bob".

## References to other objects
The mapper has 2 ways of mapping child objects associated with parent objects: by reference, or embedding them.

### Associating by Reference
A reference is used when the referenced object needs to exist as a separate entity to the referencing entity. For example, a person might have accounts, and the accounts are to be stored in their own set. They are not to be encapsulated into the person (as business logic might dictate actions are to occur on accounts irrespective of their owners).

To indicate that the second object is to be referenced, use the @AerospikeReference annotation:

```java
	@AerospikeRecord(namespace = "test", set = "account", mapAll = true)
	public static class Account {
		@AerospikeKey
		public long id;
		public String title;
		public int balance;
	}

	@AerospikeRecord(namespace="test", set="people")
	public static class Person {
		
	    @AerospikeKey
	    @AerospikeBin(name="ssn")
	    public String ssn; 
	    @AerospikeBin
	    public String firstName;
	    
	    @AerospikeBin(name="lastName")
	    public String lastName;
	    
	    @AerospikeBin(name="age")
	    public int age;

		@AerospikeBin(name = "primAcc")
		@AerospikeReference
		public Account primaryAccount;
	}
	
	Account account = new Account();
	account.id = 103;
	account.title = "Primary Savings Account";
	account.balance = 137;
	
	Person person = new Person();
	person.ssn = "123-456-7890";
	person.firstName = "John";
	person.lastName = "Doe";
	person.age = 43;
	person.primaryAccount = account;
	
	mapper.save(account);
	mapper.save(person);
```

This code results in the following data in Aerospike:

```
aql> select * from test.account
*************************** 1. row ***************************
balance: 137
id: 103
title: "Primary Savings Account"

aql> select * from test.people
*************************** 1. row ***************************
age: 43
firstName: "John"
lastName: "Doe"
primAcc: 103
ssn: "123-456-7890"
```

Note: the fields in this example are public for the sake of brevity. In reality, the class would have the fields private and appropriate accessors and mutators defined. But the annotations could still stay on the fields.

Since the account is being saved externally to the person, it must be saved as a separate call to mapper.save(...).

However, to load the data, only one call is necessary:

```java
	Person loadedPerson = mapper.read(Person.class, "123-456-7890");
	System.out.printf("ssn = %s, name = %s %s, balance = %d",
			loadedPerson.ssn, loadedPerson.firstName, loadedPerson.lastName,
			loadedPerson.primaryAccount.balance);
```

which results in:

```
ssn = 123-456-7890, name = John Doe, balance = 137
```

All dependent objects which are @AerospikeRecord will be loaded, in an arbitrarily deep nested graph.

If it is desired for the objects NOT to load dependent data, the reference can be marked with ```lazy = true```

```java
	@AerospikeBin(name="accts")
	@AerospikeReference(lazy = true)
	public List<Account> accounts;
```

in this case, when the person is loaded a the child data will NOT be loaded from the database. However, a child object (Account in this case) will be created with the id set to the value which would have been loaded. (ie ```loadedPerson.primaryAccount.id``` will be populate, but not other fields will be).

Note that if a reference to an AerospikeRecord annotated object exists, but the reference has neither @AerospikeReference nor @AerospikeEmbed (see below), then it is assumed it will be @AerospikeReference(lazy = false).


### Aggregating by Embedding

### Versioning Links
## To finish
- lists of embedded objects
- maps of embedded objects
- lists of referenced objects
- maps of referenced objects
- Check arrays of scalars map to the database correctly
- If a class is only used for embedding, it does not need a set attribute or namespace attribute
- Add in a method to add an entry to a collection, get a range from a collection, delete from a collection
- Add a "Save(instance, String ...)" which will perform an update on the desired fields rather than a full replace
- Add in a Update(instance, String ...) method to retrieve on selected properties. The update should be applied to the passed instance and the instance returned.
- Validate some of the limits, eg bin name length, set name length, etc.
- Custom type mappers -- called first before standard ones, no annotations needed
- Make all maps (esp Embedded ones) K_ORDERED
- Add policies
- Add interface to adaptiveMap, including changing EmbedType

- Lists of references do not load children references
- Make lists of references load the data via batch loads.


