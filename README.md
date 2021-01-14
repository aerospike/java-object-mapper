# Aerospike Java Object Mapper

[Aerospike](https://www.aerospike.com) is one of, if not the fastest, NoSQL database in the world. It presents a Java API which is comprehensive and powerful, but requires a measure of boilder plate code to map the data from Java POJOs to the database. The aim of this repository is to lower the amount of code required when mapping POJOs to Aerospike and back as well as reducing some of the brittleness of the code.

Consider a simple class:

```java
public class Person {
	private String ssn;
    private String firstName;
    private String lastName;
    private int age;
    private Date dob;
    
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
    
    public Date getDob() {
    return dob;
    }
    public void setDob(Date dob) {
    this.dob = dob;
    }
}	
```
 
To store an instance of this class into Aerospike requires code similar to:

```java
public void save(Person person, IAerospikeClient client) {
	long dobAsLong = (person.dob == null) ? 0 : person.dob.getTime();
	client.put( null, new Key("test", "people", person.ssn,
		new Bin("ssn", Value.get(person.getSsn())),
		new Bin("lstNme", Value.get(person.getLastName())),
		new Bin("frstNme", Value.get(person.getFirstName())),
		new Bin("age", Value.get(person.getAge())),
		new Bin("dob", Value.get(dobAsLong)));
}		
```

Similarly, reading an object requires significant code:

```java
public Person get(String ssn, IAerospikeClient client) {
	Record record = client.get(null, new Key("test", "people", ssn);
	Person person = new Person();
	person.setSsn(ssn);
	person.setFirstName(record.getString("frstNme"));
	person.setLastName(record.getString("lstNme"));
	person.setAge(record.getInt("age");
	long dobAsLong = record.getLong("dob");
	person.setDoB(dobAsLong == 0 ? null : new Date(dobAsLong));
	return person;
}
```

This code is brittle. It has information such as the namespace name, the set name, and the names of the bins in multiple places. These should all be extracted as constants so they're only referenced once, but this adds more boilerplate code. 

Additionally, there is complexity not shown in this simple example. Aerospike does not natively support all of Java types. Mapping a ``java.util.Date`` to the database requires additional code to convert to an Aerospike representation and back for example. Sub-objects which also need to be stored in the database must be handled separately. Changing the representation of the information between the database and the POJO requires additional work, such as storing a String representation of a date in Aerospike instead of a numeric representation.  

This repository aims to overcome these issues and more by relying on annotations on the POJOs to describe how to map the data to Aerospike and back. For example, the same functionality is provided by this code:

```java
@AerospikeRecord(namespace="test", set="people", mapAll = true)
public class Person {
	
    @AerospikeKey
    private String ssn; 
    @AerospikeBin(name="frstNme")
    private String firstName;
    
    @AerospikeBin(name="lstNme")
    private String lastName;
    private int age;
    private Date dob;
    
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
    
    public Date getDob() {
    return dob;
    }
    public void setDob(Date dob) {
    this.dob = dob;
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
mapper.save(p);
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
The mapper has 2 ways of mapping child objects associated with parent objects: by reference, or embedding them. Further, embedded objects can be stored either as lists or maps. All of this is controlled by annotations on the owning (parent) class.

Let's see this with and example. Let's define 2 classes, `Parent` and `Child`:

```java
@AerospikeRecord(namespace = "test", set = "parent", mapAll = true)
public static class Parent {
	@AerospikeKey
	int id;
	String name;
	
	@AerospikeEmbed(type = EmbedType.MAP)
	public Child mapEmbedChild;
	
	@AerospikeEmbed(type = EmbedType.LIST)
	public Child listEmbedChild;

	@AerospikeReference
	public Child refChild;

	public Parent(int id, String name, Child child) {
		super();
		this.id = id;
		this.name = name;
		this.mapEmbedChild = child;
		this.listEmbedChild = child;
		this.refChild = child;
	}
}

@AerospikeRecord(namespace = "test", set = "child", mapAll = true)
public static class Child {
	@AerospikeKey
	int id;
	String name;
	Date date;

	public Child(int id, String name, Date date) {
		super();
		this.id = id;
		this.name = name;
		this.date = date;
	}
}
```

This is obviously a contrived example -- we're storing 3 copies of the same `Child` in 3 different ways. The only difference in the way the child is referenced is the annotation: `@AerospikeEmbed(type = EmbedType.MAP)` will store the child in a map as part of the parent, `@AerospikeEmbed(type = EmbedType.LIST)` will store this child as a list, and `@AerospikeReference` will not store the child at all, but rather the key of the child so it can be loaded when needed.

To make use of these definitions, we create a parent and a child:

```java
Child child = new Child(123, "child", new Date());
Parent parent = new Parent(10, "parent", child);
mapper.save(parent);

// Since the child is referenced, it needs to be saved explicitly in the database
// If it were only embedded, it would not be necessary to save explicitly.
mapper.save(child);
```

The object is now saved in Aerospike. Looking at the objects in the database, we see:

```
aql> select * from test.child
*************************** 1. row ***************************
date: 1610640142173
id: 1
name: "child"

1 row in set (0.791 secs)

OK

aql> select * from test.parent
*************************** 1. row ***************************
id: 10
listEmbedChild: LIST('[1610640142173, 1, "child"]')
mapEmbedChild: MAP('{"name":"child", "date":1610640142173, "id":1}')
name: "parent"
refChild: 123

1 row in set (0.785 secs)

OK
```

Let's dig into these further.

### Associating by Reference
A reference is used when the referenced object needs to exist as a separate entity to the referencing entity. For example, a person might have accounts, and the accounts are to be stored in their own set. They are not to be encapsulated into the person (as business logic might dictate actions are to occur on accounts irrespective of their owners).

To indicate that the second object is to be referenced, use the @AerospikeReference annotation:

```java
@AerospikeRecord(namespace = "test", set = "account", mapAll = true)
public class Account {
	@AerospikeKey
	public long id;
	public String title;
	public int balance;
}

@AerospikeRecord(namespace="test", set="people")
public class Person {
	
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

and an object graph of

```
loadedPerson : Person
-  firstName : "John"
-  LastName : "Doe"
-  PrimaryAccount : Account
  - balance : 137
  - id : 103
  - title : "Primary Savings Account" 
```

All dependent objects which are @AerospikeRecord will be loaded, in an arbitrarily deep nested graph.

If it is desired for the objects NOT to load dependent data, the reference can be marked with ```lazy = true```

```java
@AerospikeBin(name = "primAcc")
@AerospikeReference(lazy = true)
public Account primaryAccount;
```

in this case, when the person is loaded a the child data will NOT be loaded from the database. However, a child object (Account in this case) will be created with the id set to the value which would have been loaded. (ie ```loadedPerson.primaryAccount.id``` will be populate, but not other fields will be). So the Person and Account objects in Java would look like

```
loadedPerson : Person
-  firstName : "John"
-  LastName : "Doe"
-  PrimaryAccount : Account
  - balance : 0
  - id : 103
  - title : null 
```

Note that if a reference to an AerospikeRecord annotated object exists, but the reference has neither @AerospikeReference nor @AerospikeEmbed (see below), then it is assumed it will be @AerospikeReference(lazy = false).

There are times when it makes sense to store the digest of the child record as the reference rather than it's primary key. For example, if the native primary key is of significant length then storing a fixed 20-byte digest makes sense. This can be accomplished by adding `type = ReferenceType.DIGEST` to the @AeropikeReference. For example:

```java
@AerospikeRecord(namespace = "test", set = "people", mapAll = true)
public static class Person {
    @AerospikeKey
    public String ssn;
    public String firstName;
    public String lastName;
    public int age;

    @AerospikeBin(name = "primAcc")
    @AerospikeReference(type = ReferenceType.DIGEST)
    public Account primaryAccount;
} 
```

This is will store the digest of the primary account in the database instead of the id:

```
*************************** 1. row ***************************
accts: LIST('[101, 102]')
age: 43
firstName: "John"
lastName: "Doe"
primAcc: 03 A7 08 92 E3 77 BC 2A 12 68 0F A8 55 7D 41 BA 42 6C 04 69
ssn: "123-456-7890"
```

Note that storing the digest as the referencing key is not compatible with lazy loading of records as the object mapper has nowhere in the object model to store the referenced id in the lazy-loaded object. Hence

```java
@AerospikeReference(type = ReferenceType.DIGEST, lazy = true)
```

will throw an exception at runtime.
### Aggregating by Embedding
The other way object relationships can be modeled is by embedding the child object(s) inside the parent object. For example, in some banking systems, Accounts are based off Products. The Products are typically versioned but can have changes made to them by banking officers. Hence the product is effectively specific to a particular account, even though it is derived from a global product. In this case, it makes sense to encapsulate the product into the account object.

Since Aerospike records can have bins (columns) which are lists and maps, we can choose to represent the underlying product in one of two ways, using a list or a map. There are pros and cons of each.

Consider a simple account and product:

```java 
@AerospikeRecord(namespace = "test", set = "product", mapAll = true) 
public static class Product {
	public String productId;
	public int version;
	public String name;
	public Date createdDate;
}

@AerospikeRecord(namespace = "test", set = "account", mapAll = true)
public static class Account {
	@AerospikeKey
	public long id;
	public String title;
	public int balance;
	@AerospikeEmbed
	public Product product;
}

Product product = new Product();
product.createdDate = new Date();
product.name = "Sample Product";
product.productId = "SP-1";
product.version = 1;

Account account = new Account();
account.id = 123;
account.title = "Test Account";
account.balance = 111;
account.product = product;

mapper.save(account);
```

This creates the following record in Aerospike:

```
aql> select * from test.account
*************************** 1. row ***************************
balance: 111
id: 123
product: MAP('{"productId":"SP-1", "name":"Sample Product", "createdDate":1609894948221, "version":1}')
title: "Test Account"
```

Note that the product definition is fully encapsulated inside the account with all the fields stored in a map. Since the product does not need to be selected in it's own right (it can only be accessed by reading it from the account) there is no need for the product to have an @AerospikeKey, nor was there any need to save the product in it's own right. Hence this product definition as it stands would _not_ be suitable to be a reference, it must be embedded. To increase flexibility, it is recommended that all objects are given an @AerospikeKey, even if they are to be embedded.

By default, embedding the child information is placed into a map, with the product bin names as the keys to the map and the values as the data in the product. This results in a very readable sub-record, but it's wasteful on space. If we have 1,000,000 accounts in our database each of which has this product, the strings "productId", "name", "createdDate" and "version" will be repeated 1,000,000 times.

So the other way embedded data can be stored in Aerospike is using a list. Simply change the @AerospikeEmbed annotation to be:

```java
public class Account {
	@AerospikeKey
	public long id;
	public String title;
	public int balance;
	@AerospikeEmbed(type = EmbedType.LIST)
	public Product product;
}
```

In this case, the embedded object will be stored as a list of values, sorted alphabetically:

```
aql> select * from test.account
*************************** 1. row ***************************
balance: 111
id: 123
product: LIST('[1609895951160, "Sample Product", "SP-1", 1]')
title: "Test Account"
```

The elements in the list are (in order): createdDate, name, productId, version.

This is far more compact and wastes less space, but has an issue: How do you add new items to the product? The answer is to use versioning.

#### Versioning Links

Maps and Aerospike records are self-describing -- each value has a name, so it is obvious how to map the data to the database and back. For example, if we have a class

```java
@AerospikeRecord(namespace = "test", set = "testSet", mapAll = true)
public class IntContainer {
	public int a;
	public int b;
	public int c;
}
```

this will be stored in a map as:

```
MAP('{"a":1, "b":2, "c":3}')
```

If we later change the `IntContainer` class to remove the field `a` and add in `d` we get the class:

```java
@AerospikeRecord(namespace = "test", set = "testSet", mapAll = true)
public class IntContainer {
	public int b;
	public int c;
	public int d;
}
```

which is stored as

```
MAP('{"b":2, "c":3, "d":4}')
```

If we had records in the database which were written with the original version of the class and read with the new version of the class, the value of field `a` in the database will be ignored and the value `d` which is not present in the database will be set to 0. So we end up with:

```
b = 2
c = 3
d = 0
```

However, if we store the sub-object as a list, the record in the database will be stored as:

```
LIST('[1, 2, 3]')
```

There is no information in the record to describe which field in the list maps to the values in the Aerospike POJO. So when we upgrade the object to the second version and try to read the record, we end up with

```
b = 1
c = 2
d = 3
```
 
This is obviously sub-optimal. Changing the underlying object has effectively invalidated the existing data in the database. Given that application maintenance in normal development lifecycles will result in changes to the object model, there has a better way to store the data.

The first thing that is needed is to tell the AerospikeMapper that the data has been versioned. This can be done with the `version` attribute on the @AerospikeNamespace. If this is not specified it will default to 1. When it is changed, it should be incremented by one, and never reduced.

For example, version 1 (implicitly) is:

```java 
@AerospikeRecord(namespace = "test", set = "testSet", mapAll = true)
public static class IntContainer {
	public int a;
	public int b;
	public int c;
}
```

and version 2 is:

```java 
@AerospikeRecord(namespace = "test", set = "testSet", mapAll = true, version = 2)
public static class IntContainer {
	public int b;
	public int c;
	public int d;
}
```

This still doesn't give us useful information to be able to map prior versions of the record. Hence, there needs to be further information which defines which fields exist in which versions of the object:

```java 
@AerospikeRecord(namespace = "test", set = "testSet", mapAll = true, version = 2)
public static class IntContainer {
    	@AerospikeVersion(max = 1)
    	public int a;
    	public int b;
    	public int c;
    	@AerospikeVersion(min = 2)
    	public int d;
}
```

Now this object can be stored in the database. As the version is 2, any information stored in field `a` with a maximum version of 1 will not be saved. The record saved in the database will look like:

```
LIST('[2, 3, 4, "@V2"]')
```

Note that a new element has been written which describes the version of the record which was written. When the record is read, this version will tell us which data maps to which fields. Let's say there are 2 records in the database, one written with version 1 and one written with version 2:

```
*************************** 1. row ***************************
container: LIST('[1, 2, 3]')
id: 1
*************************** 2. row ***************************
container: LIST('[2, 3, 4, "@V2"]')
id: 2
```

When reading these records, the results would look like:

```
1: 
   a = 0
   b = 2
   c = 3
   d = 0
   
2:
   a = 0
   b = 2
   c = 3
   d = 4
```

The first object (with key `1`) has `d` = 0 since `d` was not written to the database. `a` is also 0 even though it was written to the database in the original record because version 2 of the object should not have field `a`. (The current version of the object is 2 and `a` has a maximum version of 1). The second object (with key `2`) again has `a` being 0 as it was not written to the database as well as not being valid for this version of the object.

Note: This versioning assumes that the application version of the object will never regress. So, for example, it is not possible to read a version 2 database record with a version 1 application object.
  

#### List Ordinals

The order of the elements in a list can be controlled. By default, all the elements in the list are ordered by the name of the fields, but -- unlike maps and bins -- sometimes there is value in changing the order of values in a list. Consider for example a financial services company who stores credit card transactions, with the transactions embedded in the account that owns them. They may be embedded in a map with the transaction id as a key, and the transaction details as a list. For example:

```java
public static enum AccountType {
	SAVINGS, CHEQUING
}
@AerospikeRecord(namespace = "test", set = "accounts", mapAll = true) 
public static class Accounts {
	@AerospikeKey
	public int id;
	public String name;
	public AccountType type;
	@AerospikeEmbed(elementType = EmbedType.LIST)
	public Map<String, Transactions> transactions;
	
	public Accounts() {
		this.transactions = new HashMap<>();
	}
	public Accounts(int id, String name, AccountType type) {
		this();
		this.id = id;
		this.name = name;
		this.type = type;
	}
}

@AerospikeRecord(namespace = "test", set = "txns", mapAll = true)
public static class Transactions {
	public String txnId;
	public Instant date;
	public double amt;
	public String merchant;
	public Transactions() {}
	public Transactions(String txnId, Instant date, double amt, String merchant) {
		super();
		this.txnId = txnId;
		this.date = date;
		this.amt = amt;
		this.merchant = merchant;
	}
}

@Test
public void testAccounts() {
	Accounts account = new Accounts(1, "Savings Account", AccountType.SAVINGS);
	Transactions txn1 = new Transactions("Txn1", Instant.now(), 100.0, "Bob's store");
	Transactions txn2 = new Transactions("Txn2", Instant.now().minus(Duration.ofHours(8)), 134.99, "Kim's store");
	Transactions txn3 = new Transactions("Txn3", Instant.now().minus(Duration.ofHours(20)), 75.43, "Sue's store");
	
	account.transactions.put(txn1.txnId, txn1);
	account.transactions.put(txn2.txnId, txn2);
	account.transactions.put(txn3.txnId, txn3);
	
	mapper.save(account);
}
```

This gets saved in the database as:

```
id: 1
name: "Savings Account"
transactions: MAP('{"Txn1":[100, 1610478132904000000, "Bob's store", "Txn1"], "Txn2":[134.99, 1610449332904000000, "Kim's store", "Txn2"], "Txn3":[75.43000000000001, 1610406132907000000, "Sue's store", "Txn3"]}')
type: "SAVINGS"
```

Here the transaction time is the second attribute in each list, and the amount is the first attribute. However, a common request is to be able to extract transaction by time. For example, in fraud detection systems, there may be a need to load the N most recent transactions. If the transactions were to be stored with the transaction time as the first element in the list, efficient CDT perations in Aerospike such as `getByValueRange(...)` can be used.

This ordering can be controlled by the @AerospikeOrdinal annotation:

```java
@AerospikeRecord(namespace = "test", set = "txns", mapAll = true)
public static class Transactions {
	public String txnId;
	@AerospikeOrdinal(value = 1)
	public Instant date;
	public double amt;
	public String merchant;
	public Transactions() {}
	public Transactions(String txnId, Instant date, double amt, String merchant) {
		super();
		this.txnId = txnId;
		this.date = date;
		this.amt = amt;
		this.merchant = merchant;
	}
}
```

Now the data will be saved in a different format with the transaction time the first element in the list:

``` 
id: 1
name: "Savings Account"
transactions: MAP('{"Txn1":[1610478716965000000, 100, "Bob's store", "Txn1"], "Txn2":[1610449916965000000, 134.99, "Kim's store", "Txn2"], "Txn3":[1610406716967000000, 75.43000000000001, "Sue's store", "Txn3"]}')
type: "SAVINGS"
```

Multiple ordinals can be specified for a single class, but these must be sequential. So if it is desired to have the first 3 fields in a list specified, they must have @AerospikeOrdinal values of 1,2 and 3.

**Note**: Ordinal fields cannot be versioned.
  
## Advanced Features
### Placeholder replacement
Sometimes it is desirable to have the parameters to the annotations not being hard coded. For example, it might be desirable to have different namespaces for dev, test, staging and production. Annotations in Java must have constant parameters, so they cannot be pulled from environment variables, system properties, etc.

To work around this, the parameters to annotations which are strings can be driven by environment variables or system properties using a special syntax. This is particularly prevalent for namespace names, set names and bin names.

For an environment variable the syntax is: ``"#{ENV_VAR_NAME}"`` or ``#{ENV_VAR_NAME:default_value}``. For system properties, the syntax is ``${system.property.name}"`` or ``${system.property.name:default_value}"``.

For example:

```java
@AerospikeRecord(namespace="test", set="${people.set.name:people}")
public class Person {
```

In this case, if the ``people.set.name`` system parameter is set, that value will be used for the set name. If it is not set, ``people`` will be used as the set name. The system property can be set on the command line in this case using syntax similar to:

```
-Dpeople.set.name=person
```

An example using an environment variable:

```java
@AerospikeBin(name="#{ACCOUNT_TITLE_BIN_NAME}")
private String title;
```

In this case, if the environment variable ``ACCOUNT_TITLE_BIN_NAME`` is set, that will be the name of the bin which is used. If it is not set, it will be like the annotation does not specify the ``name`` paramteter at all, which means that the field name (``title``) will be used for the bin name.

### Custom Mappers
Sometimes, the representation of the data in Aerospike and the representation in Java should be very different. Consider a class which represents a playing card and another class which represents a poker hand:

```java
public enum Suit {
    CLUBS, DIAMONDS, HEARTS, SPADES;
}

@AerospikeRecord(namespace = NAMESPACE, set = "card", mapAll = true)
public class Card {
    public char rank;
    public Suit suit;

    public Card() {}
    public Card(char rank, Suit suit) {
        super();
        this.rank = rank;
        this.suit = suit;
    }
}

@AerospikeRecord(namespace = NAMESPACE, set = "poker", mapAll = true)
public class PokerHand {
    	@AerospikeEmbed
    public Card playerCard1;
    	@AerospikeEmbed
    public Card playerCard2;
    	@AerospikeEmbed
    public List<Card> tableCards;
    @AerospikeKey
    public String id;

    public PokerHand(String id, Card playerCard1, Card playerCard2, List<Card> tableCards) {
		super();
		this.playerCard1 = playerCard1;
		this.playerCard2 = playerCard2;
		this.tableCards = tableCards;
		this.id = id;
	}
    
    public PokerHand() {}
}
```

The program to create and save a poker hand might look like:

```java 
PokerHand blackjackHand = new PokerHand(
        "1",
        new Card('6', Suit.SPADES),
        new Card('9', Suit.HEARTS),
        Arrays.asList(new Card('4', Suit.CLUBS), new Card('A', Suit.HEARTS)));

AeroMapper mapper = new AeroMapper.Builder(client)
        .build();

mapper.save(blackjackHand);
```

This works, but creates a fairly verbose representation of the card in the database:

```
id: "1"
playerCard1: MAP('{"rank":54, "suit":"SPADES"}')
playerCard2: MAP('{"rank":57, "suit":"HEARTS"}')
tableCards: LIST('[{"rank":52, "suit":"CLUBS"}, {"rank":65, "suit":"HEARTS"}]')
```

Why not store the whole class as a simple 2 character string, one character which is the rank, and the second is the suit?

In this case, we have to create a custom mapper:

```java
public static class CardConverter {
    @ToAerospike
    public String toAerospike(Card card) {
        return card.rank + card.suit.name().substring(0, 1);
    }

    @FromAerospike
    public Card fromAerospike(String card) {
        if (card.length() != 2) throw new AerospikeException("Unknown card: " + card);

        char rank = card.charAt(0);
        switch (card.charAt(1)) {
            case 'C': return new Card(rank, Suit.CLUBS);
            case 'D': return new Card(rank, Suit.DIAMONDS);
            case 'H': return new Card(rank, Suit.HEARTS);
            case 'S': return new Card(rank, Suit.SPADES);
            default:
                throw new AerospikeException("unknown suit: " + card);
        }
    }
}
```

The custom converter must have a method annotated with @ToAerospike and another with @FromAerospike. The @ToAerospike method takes 1 parameter which is the representation of the card in POJO format (the `Card` type in the case) and returns the representation used to store the data in Aerospike (`String` in this case). The @FromAerospike similarly takes the Aerospike representation and returns the POJO representation. The return type of the @FromAerospike method must match the parameter type of the @ToAerospike method and vice versa. When determining how to convert a type, the AeroMapper will see if it matches the parameter to the @ToAerospike method and invoke this method.

Note that custom converters take priority over in-built converters. So if it is preferred to store a java.util.Date in the database as a String instead of a number for example, this can be done using a custom type converter.

Before the AeroMapper can use the custom converter, it must be told about it. This is done in the the builder:

```java
mapper = new AeroMapper.Builder(client)
        .addConverter(new CardConverter())
        .build();
```

Now when the object is stored in Aerospike, it is stored in a far more concise format:

```
*************************** 1. row ***************************
id: "1"
playerCard1: "6S"
playerCard2: "9H"
tableCards: LIST('["4C", "AH"]')
```

It should be noted that since the inbuilt converter system in the AeroMapper no longer needs to know about the structure of the card, the card object itself can be simplified. Instead of:

```java
@AerospikeRecord(namespace = NAMESPACE, set = "card", mapAll = true)
public static class Card {
    public char rank;
    public Suit suit;

    public Card() {}
    public Card(char rank, Suit suit) {
        super();
        this.rank = rank;
        this.suit = suit;
    }
}
```

it can now become:

```java
public static class Card {
    public char rank;
    public Suit suit;

    public Card(char rank, Suit suit) {
        super();
        this.rank = rank;
        this.suit = suit;
    }
}
```

Notice the removal of the annotation and the no-argument constructor. The referencing type can now become simpler too, as the Card class is seen as a primitive type, not an associated object. Instead of

```java
@AerospikeRecord(namespace = NAMESPACE, set = "poker", mapAll = true)
public static class PokerHand {
	@AerospikeEmbed
    public Card playerCard1;
	@AerospikeEmbed
    public Card playerCard2;
	@AerospikeEmbed
    public List<Card> tableCards;
    @AerospikeKey
    public String id;

    public PokerHand(String id, Card playerCard1, Card playerCard2, List<Card> tableCards) {
		super();
		this.playerCard1 = playerCard1;
		this.playerCard2 = playerCard2;
		this.tableCards = tableCards;
		this.id = id;
	}
    
    public PokerHand() {}
}
```

It can simply become:

```java
@AerospikeRecord(namespace = NAMESPACE, set = "poker", mapAll = true)
public static class PokerHand {
    public Card playerCard1;
    public Card playerCard2;
    public List<Card> tableCards;
    @AerospikeKey
    public String id;

    public PokerHand(String id, Card playerCard1, Card playerCard2, List<Card> tableCards) {
		super();
		this.playerCard1 = playerCard1;
		this.playerCard2 = playerCard2;
		this.tableCards = tableCards;
		this.id = id;
	}
    
    public PokerHand() {}
}
```

## To finish
- lists of embedded objects
- maps of embedded objects
- lists of referenced objects
- maps of referenced objects
- Check arrays of scalars map to the database correctly and back
- If a class is only used for embedding, it does not need a set attribute or namespace attribute
- Add in a method to add an entry to a collection, get a range from a collection, delete from a collection
- Add a "Save(instance, String ...)" which will perform an update on the desired fields rather than a full replace
- Add in a Update(instance, String ...) method to retrieve on selected properties. The update should be applied to the passed instance and the instance returned.
- Validate some of the limits, eg bin name length, set name length, etc.
- Make all maps (esp Embedded ones) K_ORDERED
- Add policies. Maybe drive via annotations? Certainly need a "sendKey" annotation property.
- Add interface to adaptiveMap, including changing EmbedType
- Lists of references do not load children references
- Make lists of references load the data via batch loads.
- Make mapAll default to TRUE and not FALSE and update documentation
- Document all parameters to annotations and examples of types
- Document enums, dates, instants.
- Validate that all AerospikeRecord objects have a no-arg constructor

