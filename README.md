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
@AerospikeRecord(namespace="test", set="people")
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

----

## Getting Started
The first thing that needs to be done is to create an instance of the AeroMapper class. This is achieved through the Builder class which allows you to specify
various options. Once the options have been specified, `build()` is called to get an instance of the AeroMapper. Thus, the simplest usage is:

``` java
AeroMapper mapper = new AeroMapper.Builder(client).build();
```

The Builder constructor simply takes an IAerospikeClient which it uses for access to the database. Other options can be added to the mapper between the constructor for the Builder and the invocation of the build() method. These options include:

`.addConverter(Object converter)`: Registers a class as a custom converter, which allows programmatic control over how data types are mapped to and from Aerospike. This custom converter must have @ToAerospike and @FromAerospike annotated methods. For more information, see [Custom Object Converters](#custom-object-converters) below.

`.preLoadClass(Class<?>)`: Used to load a class before it is needed. The process of loading a class for the first time can be moderately expensive -- there is lots of introspection which goes on to determine how to map the classes to and from the database with the help of the annotations or configuration file. The results of this process are cached so it only has to happen once, and as few introspection calls as possible are called during the actual transformation. If a class is not preloaded, this computation will happen the first time an instance of that class is encountered, resulting in slowdown on the first call.

`withConfigurationFile`: Whilst mapping information from POJOs via annotations is efficient and has the mapping code inline with the POJO code, there are times when this is not available. For example, if an external library with POJOs is being used and it is desired to map those POJOs to the database, there is no easy way of annotating the source code. Another case this applies is if different mapping parameters are needed between different environments. For example, embedded objects might be stored in a map in development for ease of debugging, but stored in a list in production for compaction of stored data. In these cases an external configuration YAML file can be used to specify how to map the data to the database. See [External Configuration File](#external-configuration-file) for more details. There is an overload of this method which takes an additional boolean parameter -- if this is `true` and the configuration file is not valid, errors will be logged to `stderr` and the process continue. It is normally not recommended to set this parameter to true.

`withConfiguration`: Similar to the `withConfigurationFile` above, this allows configuration to be externally specified. In this case, the configuration is passed as a YAML string.

`withReadPolicy`, `withWritePolicy`, `withBatchPolicy`, `withScanPolicy`, `withQueryPolicy`: This allows setting of the appropriate policy type. The following discussion uses read policies, but applies equally to all the other policies.

After the specified policy, there are 3 possible options: 

- `forAll()`: The passed policy is used for all classes. This is similar to setting the defaultReadPolicy on the IAerospikeClient but allows it to be set after the client is created. 
- `forChildrenOf(Class<?> class)`: The passed policy is used for the passed class and all subclasses of the passed class.
- `forClasses(Class<?> ... classes)`: The passed policy is used for the passed class(es), but no subclasses.

It is entirely possible that a class falls into more than one category, in which case the most specific policy is used. If no policy is specified, the defaultReadPolicy passed to the IAerospikeClient is used. For example, if there are classes A, B, C with C being a subclass of B, a definition could be for example:

```java
Policy readPolicy1, readPolicy2, readPolicy3;
// ... code to set up the policies goes here...
AeroMapper.Builder(client)
          .withReadPolicy(readPolicy1).forAll()
          .withReadPolicy(readPolicy2).forChildrenOf(B.class)
          .withReadPolicy(readPolicy3).forClasses(C.class)
          .build();
```

In this case the `forAll()` would apply to A,B,C, the `forChildrenOf` would apply to B,C and `forClasses` would apply to C. So the policies used for each class would be:

- A: `readPolicy1`
- B: `readPolicy2`
- C: `readPolicy3`
           
Note that each operation can also optionally take a policy if it is desired to change any of the policy settings on the fly. The explicitly provided policy will override any other settings, such as `durableDelete` on the `@AerospikeRecord`
 

---

## Constructors
Given that the AeroMapper is designed to read and write information to an Aerospike database, it must be able to create objects when the data has been read from the database. To construct an object, it will typically use the default (no argument) constructor. 

However, there are times when this is not desirable, for example when the class declares final fields which must be mapped to the constructor. For example, consider the following class:

```java
@AerospikeRecord(namespace = "test", set = "testSet")
public class ConstructoredClass {
	@AerospikeKey
	public final int id;
	public final int age;
	public final String name;
	public final Date date;
	
	public ConstructoredClass(int id, int age, String name, Date date) {
		super();
		this.id = id;
		this.age = age;
		this.name = name;
		this.date = date;
	}
}
```

As it stands, this class cannot be used with the AeroMapper because there is no valid constructor to invoke when an object needs to be created. There is a constructor but it does not contain enough information to map the reocrd on the database to the parameters of the constructor. (Remember that at runtime method and argument names are typically lost and become "arg1", "arg2" and so on). We can use this constructor, but we need to provide this missing information with annotations:

```java
@AerospikeRecord(namespace = "test", set = "testSet")
public class ConstructoredClass {
	@AerospikeKey
	public final int id;
	public final int age;
	public final String name;
	public final Date date;
	
	public ConstructoredClass(@ParamFrom("id") int id, @ParamFrom("age") int age, @ParamFrom("name") String name, @ParamFrom("date")Date date) {
		super();
		this.id = id;
		this.age = age;
		this.name = name;
		this.date = date;
	}
}
```

Now there is enough information to be able to construct an instance of this class from a database record. Note that the names of the @ParamFrom annotation are the bin names, not the underlying field names. So if you have a field declared as

```java
@AerospikeBin(name = "shrtNm")
private String fieldWithAVeryLongName;
```

then the constructor might look line:

```java
public FieldNameTest(@ParamFrom("shrtNm") String fieldWithAVeryLongName) {
	this.fieldWithAVeryLongName = fieldWithAVeryLongName;
}
```

Note that not all the fields in the class need to be specified in the constructor (unless needed to satisfy the Java compiler, eg setting any final fields). Any values not passed in the constructor will be explicitly set. For example:

```java
@AerospikeRecord(namespace = "test", set = "testSet") 
public class ConstructoredClass2 {
	@AerospikeKey
	public final int id;
	public final int a;
	public int b;
	public int c;
	
	public ConstructoredClass2(@ParamFrom("id") int id, @ParamFrom("a") int a) {
		this.id = id;
		this.a = a;
	}
}
```

Whilst these examples show only final fields being set, this is not a requirement.

If there are multiple constructors on the class, the one to be used by the AeroMapper should be annotated with @AerospikeConstructor:

```java
@AerospikeRecord(namespace = "test", set = "testSet") 
public class ConstructoredClass2 {
	@AerospikeKey
	public final int id;
	public final int a;
	public int b;
	public int c;
	
	public ConstructoredClass2(@ParamFrom("id") int id, @ParamFrom("a") int a) {
		this.id = id;
		this.a = a;
	}
	@AerospikeConstructor
	public ConstructoredClass2(@ParamFrom("id") int id, @ParamFrom("a") int a, @ParamFrom("b") int b) {
		this.id = id;
		this.a = a;
		this.b = b;
	}
}
```

In this case, the 3 argument constructor will be used. Note that you must annotate the desired constructor with @AerospikeConstructor on any class with multiple constructors, irrespective of how many of those constructors have the @ParamFrom annotations on their arguments. It is only allowed to have one constructor so annotated.

If no constructor is annotated with @AerospikeConstructor, the default no-argument constructor will be used. If there is no no-argument constructor and no @AerospikeConstructor annotated constructor has been declared, an exception will be thrown when the class is first used.

---
 
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

----

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

By default, all fields will be mapped to the database. Fields can be excluded with the @AerospikeExclude annotation, and renamed with the @AerospikeBin annotation. If it is desired to save only bins annotated with @AerospikeBin, use `mapAll = false` on the @AerospikeRecord. For example:

```java
@AerospikeRecord(namespace = NAMESPACE, set = "testSet")
public static class Test {
	public int a;
	public int b;
	public int c;
	public int d;
}
```

This saves the record with 4 bins, a,b,c,d. To save only fields a,b,c you can do either:

```java
@AerospikeRecord(namespace = NAMESPACE, set = "testSet")
public static class Test {
	public int a;
	public int b;
	public int c;
	@AerospikeExclude
	public int d;
}
```

or

```java
@AerospikeRecord(namespace = NAMESPACE, set = "testSet", mapAll = false)
public static class Test {
	@AerospikeBin
	public int a;
	@AerospikeBin
	public int b;
	@AerospikeBin
	public int c;
	public int d;
}
```

If a field is marked with both @AerospikeExclude and @AerospikeBin, the bin will _not_ be mapped to the database.

You can force the name of a particular bin or bins by specifying them in an AerospikeBin:

```java
@AerospikeRecord(namespace = NAMESPACE, set = "testSet")
public static class Test {
	public int a;
	public int b;
	@AerospikeBin(name = "longCname")
	public int c;
	public int d;
}
```
This will save 4 fields in the database, a, b, longCname, d.

----

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

----

## References to other objects
The mapper has 2 ways of mapping child objects associated with parent objects: by reference, or embedding them. Further, embedded objects can be stored either as lists or maps. All of this is controlled by annotations on the owning (parent) class.

Let's see this with and example. Let's define 2 classes, `Parent` and `Child`:

```java
@AerospikeRecord(namespace = "test", set = "parent")
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

@AerospikeRecord(namespace = "test", set = "child")
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
@AerospikeRecord(namespace = "test", set = "account")
public class Account {
	@AerospikeKey
	public long id;
	public String title;
	public int balance;
}

@AerospikeRecord(namespace="test", set="people", mapAll = false)
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
@AerospikeRecord(namespace = "test", set = "people")
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
@AerospikeRecord(namespace = "test", set = "product") 
public static class Product {
	public String productId;
	public int version;
	public String name;
	public Date createdDate;
}

@AerospikeRecord(namespace = "test", set = "account")
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

Similarly, the Product does not need to specify either a set or a namespace in the @AerospikeRecord annotation as they are not being stored in Aerospike in their own right.

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
@AerospikeRecord(namespace = "test", set = "testSet")
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
@AerospikeRecord(namespace = "test", set = "testSet")
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
@AerospikeRecord(namespace = "test", set = "testSet")
public static class IntContainer {
	public int a;
	public int b;
	public int c;
}
```

and version 2 is:

```java 
@AerospikeRecord(namespace = "test", set = "testSet", version = 2)
public static class IntContainer {
	public int b;
	public int c;
	public int d;
}
```

This still doesn't give us useful information to be able to map prior versions of the record. Hence, there needs to be further information which defines which fields exist in which versions of the object:

```java 
@AerospikeRecord(namespace = "test", set = "testSet", version = 2)
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

@AerospikeRecord(namespace = "test", set = "accounts") 
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

@AerospikeRecord(namespace = "test", set = "txns")
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
@AerospikeRecord(namespace = "test", set = "txns")
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
  
----

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

----

### Subclasses
The AeroMapper also supports mapping object hierarchies. To see this, consider the following class hierarchy:

![Hierarchy](/images/classHierarchy.png)

There are 2 abstract classes here: BaseClass which every business class in the hierarchy will inherit from, and Account which is an abstract superclass of all the different sort of Accounts (at the moment just Savings and Checking). In terms of mapping data, the Customer class will be mapped to it's own set in Aerospike. However, when considering the Checking and Savings accounts there are 2 different strategies which can be used for mapping the data:

1. Both Account types are mapped to the same set (eg Account) and co-mingled with one another. 
2. Checking and Savings are written to independent sets, holding only records of that type.

The AeroMapper supports both strategies for resolving subclasses, as well as being able to inherit just data fields from superclasses.


#### Data Inheritance

Consider the Customer class which inherits from the BaseClass:

```java
@AerospikeRecord
public static class BaseClass {
	private Date lastReadTime;
	private final Date creationTime;
	
	public BaseClass() {
		this.creationTime = new Date();
	}
}

@AerospikeRecord(set = "customer", namespace = "test")
public static class Customer extends BaseClass {
	@AerospikeKey
	@AerospikeBin(name = "id")
	private final String customerId;
	private final String name;
	@AerospikeBin(name = "date")
	private Date dateJoined;
	@AerospikeReference
	private List<Account> accounts;

	public Customer(String customerId, String name) {
		this(customerId, name, new Date());
	}
	
	@AerospikeConstructor
	public Customer(@ParamFrom("id") String customerId, @ParamFrom("name") String name, @ParamFrom("date") Date dateJoined) {
		this.customerId = customerId;
		this.name = name;
		this.dateJoined = dateJoined;
		this.accounts = new ArrayList<>();
		this.accountMap = new HashMap<>();
	}
	public Date getDateJoined() {
		return dateJoined;
	}
	public void setDateJoined(Date dateJoined) {
		this.dateJoined = dateJoined;
	}
	public List<Account> getAccounts() {
		return accounts;
	}
	public void setAccounts(List<Account> accounts) {
		this.accounts = accounts;
	}
	public String getCustomerId() {
		return customerId;
	}
	public String getName() {
		return name;
	}
}
```

In this case, the data from both classes (BaseClass and Customer) will be aggregated into the Customer record resulting in a record like:

```
aql> select * from test.customer
*************************** 1. row ***************************
accounts: LIST('[["SVNG1", "SVG"], ["CHK2", "CHK"]]')
date: 1614025827020
id: "cust1"
name: "Tim"
creationTime: 1614025827020
```

Note that the data here contains both the data for the child class (Customer) and the superclass (BaseClass)
 
#### Subclass Inheritance

As the first example, let's roll the Checking and Savings account up to the Account set.

```
@AerospikeRecord(namespace = "test", set = "subaccs", ttl=3600, durableDelete = true, sendKey = true)
public static class Account extends BaseClass {
	@AerospikeKey
	protected String id;
	protected long balance;
}

@AerospikeRecord(shortName = "SVG")
public static class Savings extends Account {
	private long numDeposits;
	private float interestRate;
}

@AerospikeRecord(shortName = "CHK")
public static class Checking extends Account {
	private int checksWritten;
}
```

In this case the 2 subclasses (Checking and Savings) do not define their own set so they will inherit the namespace and set from the closest superclass which has a namespace and set (in this case Account). Since this set will now contain both Savings and Checking accounts, we need some way of differentiating them. By default the name of the class will be added: `Savings` and `Checking` respectively. However, to keep these names short, we can specify `shortName`s for these classes, `SVG` and `CHK` respectively.

If a class defines the same set and namespace as it's closest parent with a set and namespace, the effect will be as if the child class did not define the set and namespace. That is, the following 2 sections of code will have exactly the same effect:

```java
@AerospikeRecord(namespace = "test", set = "subaccs", ttl=3600, durableDelete = true, sendKey = true)
public static class Account extends BaseClass {
	@AerospikeKey
	protected String id;
	protected long balance;
}

@AerospikeRecord(shortName = "SVG")
public static class Savings extends Account {
	private long numDeposits;
	private float interestRate;
}
```

and

```java
@AerospikeRecord(namespace = "test", set = "subaccs", ttl=3600, durableDelete = true, sendKey = true)
public static class Account extends BaseClass {
	@AerospikeKey
	protected String id;
	protected long balance;
}

@AerospikeRecord(namespace = "test", set = "subaccs", shortName = "SVG")
public static class Savings extends Account {
	private long numDeposits;
	private float interestRate;
}
```
It should be noted that the names used to refer to the class (whether it is a `shortName` or the normal class name) must be globally unique in the system.

Let's look at a rather contrived example, which shows how these are used in practice. We will define 2 Savings and 2 Checking accounts and save them to the database, as well as one Account. In a real application the Account class would likely be `abstract` and hence would not be saved to the database in it's own right, but it's a useful exercise to understand how things are mapped.

```java
Savings savingsAccount1 = new Savings();
savingsAccount1.interestRate = 0.03f;
savingsAccount1.numDeposits = 17;
savingsAccount1.id = "SVNG1";
savingsAccount1.balance = 200;
mapper.save(savingsAccount1);

Savings savingsAccount2 = new Savings();
savingsAccount2.interestRate = 0.045f;
savingsAccount2.numDeposits = 11;
savingsAccount2.id = "SVNG2";
savingsAccount2.balance = 99;
mapper.save(savingsAccount2);

Checking checkingAccount1 = new Checking();
checkingAccount1.checksWritten = 4;
checkingAccount1.id = "CHK1";
checkingAccount1.balance = 600;
mapper.save(checkingAccount1);

Checking checkingAccount2 = new Checking();
checkingAccount2.checksWritten = 23;
checkingAccount2.id = "CHK2";
checkingAccount2.balance = 10902;
mapper.save(checkingAccount2);
		
Account account = new Account();
account.balance = 927;
account.id = "Account1";
mapper.save(account);
```

Running this and querying the data gives:

```
aql> set output raw
OUTPUT = RAW
aql> select * from test.subaccs
*************************** 1. row ***************************
interestRate: 0.02999999932944775
numDeposits: 17
balance: 200
id: "SVNG1"
creationTime: 1614231134837
*************************** 2. row ***************************
checksWritten: 23
balance: 10902
id: "CHK2"
creationTime: 1614231134852
*************************** 3. row ***************************
interestRate: 0.04500000178813934
numDeposits: 11
balance: 99
id: "SVNG2"
creationTime: 1614231134851
*************************** 4. row ***************************
checksWritten: 4
balance: 600
id: "CHK1"
creationTime: 1614231134852
PK: "Account1"
*************************** 5. row ***************************
balance: 927
id: "Account1"
creationTime: 1614231134853
```

As you can see, the savings and checking accounts are intermingled in the same set, each with the appropriate fields.

In order to show how these items are referenced, we create a contrived Container class:

```java
@AerospikeRecord(namespace = "test", set = "container")
private class Container {
	@AerospikeKey
	private long id;
	private Account account;
	private Savings savings;
	private Checking checking;
	private List<Account> accountList = new ArrayList<>();
	private Account primaryAccount;
}
```

And then populate and save it:

```java
Container container = new Container();
container.account = account;
container.checking = checkingAccount1;
container.savings = savingsAccount1;
container.primaryAccount = savingsAccount1;
container.accountList.add(account);
container.accountList.add(savingsAccount1);
container.accountList.add(checkingAccount1);
mapper.save(container);
```

then looking at the database we see:

```
aql> select * from test.container
*************************** 1. row ***************************
account: "Account1"
accountList: LIST('["Account1", ["SVNG1", "SVG"], ["CHK1", "CHK"]]')
checking: "CHK1"
id: 0
primaryAccount: LIST('["SVNG1", "SVG"]')
savings: "SVNG1"
```

Note that if an object is mapped to the actual type (eg Account to Account) then the reference simply contains the id. However, if a subclass is mapped to a variable declared as the supertype (eg Savings to Account) then the reference must contain the type of the subclass as well as the key, and hence is contained within a list. If the AeroMapper didn't do this, when it went to load the record from the database it would not know which class to instantiate and hence could not determine the how to map the data to the record.

For this reason, it is strongly recommended that all attributes use a parameterized type, eg `List<Account>` rather than `List`

It should be noted that the use of subclasses 



----

### Custom Object Converters
Sometimes, the representation of the data in Aerospike and the representation in Java should be very different. Consider a class which represents a playing card and another class which represents a poker hand:

```java
public enum Suit {
    CLUBS, DIAMONDS, HEARTS, SPADES;
}

@AerospikeRecord(namespace = NAMESPACE, set = "card")
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

@AerospikeRecord(namespace = NAMESPACE, set = "poker")
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

In this case, we have to create a custom object converter:

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
@AerospikeRecord(namespace = NAMESPACE, set = "card")
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
@AerospikeRecord(namespace = NAMESPACE, set = "poker")
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
@AerospikeRecord(namespace = NAMESPACE, set = "poker")
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

----

## External Configuration File
An configuration file in YAML format can be created and passed to the builder either as a File object containing the YAML file or as a string containing the YAML. Note that passing a string representing a filename does not work -- it should be explicitly turned into a file using `new File(fileName)` for example. 

All of the properties which can be specified via annotations can also be specified via the configuration file. If the same property exists in both a configuration file and an annotation, the value in the configuration file is used in preference to the value in the annotation. This allows for changing the way data is mapped in different environments by specifying a different configuration file. For example, in a development environment it might be desirable for an embedded object to be stored as a map for ease of debugging, but then in test, staging and prod environments it might be useful to store the same object as a list to prevent bloating of the data.

The syntax of the builder allows for multiple configuration files to be specified. If the same class definition appears in 2 different configuration files, the first one encountered for that class will be taken and subsequent ones ignored, even if those subsequent ones contain additional information not specified in the first one.

An example configuration file might contain:

```yaml
---
classes:
  - class: com.aerospike.mapper.AeroMapperConfigurationYamlTest$DataClass
    namespace: test
    set: dataClass
    key:
      field: id
    bins:
      - field: date
        name: d1
  - class: com.aerospike.mapper.AeroMapperConfigurationYamlTest$ContainerClass
    namespace: test
    set: containers
    key:
      field: id
    bins:
      - field: dataClasses
        embed:
          type: MAP
          elementType: LIST
        name: data
```
 
 The structure of the file is: 
 
 Top level is an array of classes. Each class has:
 - **class**: the name of the class. This must match the full class name to which you want to apply the configuration
 - **namespace**: The namespace to map this class to. Can be unspecified if the class is only ever used for embedding in another object
 - **set**: The set to map this class to. Can be unspecified if the class is only ever used for embedding in another object
 - **durableDelete** (boolean): If set to `true`, any deletes on this class will use [durable deletes](https://www.aerospike.com/docs/guide/durable_deletes.html). If not set, it will use the flag from the policy for this class
 - **mapAll** (boolean, default `true`): If true, all fields of this class will automatically be mapped to the database. Fields can be excluded using `exclude` on the bin config. If this is set to false, only the fields specified with an explicit bin configuration will be stored.
 - **sendKey** (boolean): If true, the key of the record will be stored in Aerospike. See [send key](https://www.aerospike.com/docs/guide/policies.html#send-key) for more details. If this is false, the key will not be stored in Aerospike. If not set, the `sendKey` field from the policy will be used.
 - **ttl**: the time to live for the record, mapped to the expiration time on the policy. If not set, the expiration from the policy will be used.
 - **shortName**: When this class name must be stored in the database, this is the name to store instead of the full class names. This is used particularly for sub-classes. For example, if an Account class has a Checking class and Savings class as subclasses, an object might store a reference to an Account (compiled type of Account), but this really is a Checking account (runtime type of Checking). If the reference to the account is persisted, a list storing the key and the type will be saved, and this name will be used as the type.
 - **key**: a key structure, specified below
 - **bins**: a list of bin structure, specified below
 
 The key structure contains:
 - **field**: the field for the key. Can be unspecified if methods are being used for the key
 - **getter**: the name of the method to be used as the getter for the key. 
 - **setter**: the name of the method to be used as the setter for the key. This is optional -- if lazy loading of referenced objects is used, a setter must be specified for the child class if a getter is
 Note that either a field should be specified, or a getter (potentially with a setter). Using both a field and a getter will throw an error. Also note that the method is specified by names only, not parameters so it is a good idea to us a unique method. 
 
 The bin structure contains:
 - **embed**:
 - **exclude**:
 - **field**:
 - **getter**:
 - **name**:
 - **ordinal**:
 - **reference**:
 - **setter**:
 
  
## Virtual Lists

When mapping a Java object to Aerospike the most common operations to do are to save the whole object and load the whole object. The AeroMapper is set up primarily for these use cases. However, there are cases where it makes sense to manipulate objects directly in the database, particularly when it comes to manipulating lists and maps. This functionality is provided via virtual lists.  

----

## To finish
- lists of embedded objects
- maps of embedded objects
- lists of referenced objects
- maps of referenced objects
- Document virtual lists
- Validate some of the limits, eg bin name length, set name length, etc.
- Make all maps (esp Embedded ones) K_ORDERED
- Add interface to adaptiveMap, including changing EmbedType
- Document all parameters to annotations and examples of types
- Document enums, dates, instants.
- Document configuration file. 
- Document creation of builder -- multiple configuration files are allowed, if the same class is declared in both the first one encountered wins. 
- Document methods with 2 parameters for keys and setters, the second one either a Key or a Value
- Document subclasses and the mapping to tables + references stored as lists
- Batch load of child items on Maps and References. Ensure testing of non-parameterized classes too. Also of methods on Virtual LIsts
- Document batch loading
- Ensure batch loading option exists in AerospikeReference Configuration
- handle object graph circularities (A->B->C). Be careful of: A->B(Lazy), A->C->B: B should end up fully hydrated in both instances, not lazy in both instances
- Consider the items on virtual list which return a list to be able to return a map as well (ELEMENT_LIST, ELEMENT_MAP) 
- Test if map supports lazy loading of referenced objects.
