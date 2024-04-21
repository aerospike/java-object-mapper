# Aerospike Java Object Mapper
[![Build project](https://github.com/aerospike/java-object-mapper/actions/workflows/build.yml/badge.svg)](https://github.com/aerospike/java-object-mapper/actions/workflows/build.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.aerospike/java-object-mapper/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.aerospike/java-object-mapper/)
[![javadoc](https://javadoc.io/badge2/com.aerospike/java-object-mapper/javadoc.svg)](https://javadoc.io/doc/com.aerospike/java-object-mapper)

[Aerospike](https://www.aerospike.com) is one of, if not the fastest, NoSQL database in the world. It presents a Java API which is comprehensive and powerful, but requires a measure of boilerplate code to map the data from Java POJOs to the database. The aim of this repository is to lower the amount of code required when mapping POJOs to Aerospike and back as well as reducing some of the brittleness of the code.

## Documentation

The documentation for this project can be found on [javadoc.io](https://www.javadoc.io/doc/com.aerospike/java-object-mapper).

# Table of contents
1. [Compatibility with Aerospike Clients](#Compatibility-with-Aerospike-Clients)
2. [Motivation and a simple example](#Motivation-and-a-simple-example)
3. [Getting Started](#Getting-Started)
4. [Constructors](#Constructors)
   + 4.1. [Constructor Factories](#Constructor-Factories)
5. [Keys](#Keys)
6. [Fields](#Fields)
7. [Properties](#Properties)
8. [References to other objects](#References-to-other-objects)
    + 8.1. [Associating by Reference](#Associating-by-Reference)
        + 8.1.1. [Batch Loading](#Batch-Loading)
    + 8.2. [Aggregating by Embedding](#Aggregating-by-Embedding)
        + 8.2.1. [Versioning Lists](#Versioning-Lists)
        + 8.2.2. [List Ordinals](#List-Ordinals)
        + 8.2.3. [The importance of Generic Types](#The-importance-of-Generic-Types)
9. [Advanced Features](#Advanced-Features)
    + 9.1. [Placeholder replacement](#Placeholder-replacement)
    + 9.2. [Subclasses](#Subclasses)
        + 9.2.1. [Data Inheritance](#Data-Inheritance)
        + 9.2.2. [Subclass Inheritance](#Subclass-Inheritance)
        + 9.2.3 [Using Interfaces](#Using-Interfaces)        
    + 9.3. [Custom Object Converters](#Custom-Object-Converters)
10. [External Configuration File](#External-Configuration-File)
    + 10.1. [File Structure](#File-Structure)
        + 10.1.1. [Key Structure](#Key-Structure)
        + 10.1.2. [Bin Structure](#Bin-Structure)
        + 10.1.3. [Embed Structure](#Embed-Structure)
        + 10.1.4. [Reference Structure](#Reference-Structure)
11. [Virtual Lists](#Virtual-Lists)
12. [Scans](#Scans)
13. [Queries](#Queries)

# Compatibility with Aerospike Clients

| Java Object Mapper Version | Aerospike Client | Aerospike Reactor Client
|:---------------------------|:-----------------| :-----------
| 2.4.x                      | 8.1.x (jdk8)     | 7.1.x
| 2.1.x, 2.2.x, 2.3.x        | 6.1.x            | 6.1.x
| 2.0.x                      | 5.1.x            | 5.1.x
| 1.2.x, 1.3.x, 1.4.x        | 5.1.x            | 5.0.x
| 1.1.x                      | 5.0.x            | 

# Installing the Mapper
The easiest way to use the mapper is through Maven or Gradle. For Maven, pull it in from Maven Central:
```
<!-- https://mvnrepository.com/artifact/com.aerospike/java-object-mapper -->
<dependency>
    <groupId>com.aerospike</groupId>
    <artifactId>java-object-mapper</artifactId>
    <version>2.4.0</version>
</dependency>
```
For Gradle, you can use
```
// https://mvnrepository.com/artifact/com.aerospike/java-object-mapper
implementation group: 'com.aerospike', name: 'java-object-mapper', version: '2.4.0'
```

# Motivation and a simple example
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
AeroMapper mapper = new AeroMapper.Builder(client).build();
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

Another reason to preload a class is situations where an abstract superclass might be read without the subclasses being seen by the AeroMapper first. For example, a list of `Animal` might be stored in the database, but `Animal` is an abstract class with concrete subclasses like `Dog`, `Cat`, etc. If the first call of the AeroMapper is to read a list of `Animal` from the database, there is not enough information to resolve the concrete sub-classes without preloading them.

`.preLoadClasses(Class<?> ...)`: Use to preload several classes before they are called. This is a convenience mechanism which calls `.preLoadClass` for each of the classes in the list.

`.preLoadClassesFromPackage(String | Class<?>)`: Preload all the classes in the specified package which are annotated with `@AerospikeRecord`. The package can be specified by passing a string of the package name or by passing a class in that package. The latter method is preferred as this is less brittle as code is refactored. Note that if a class is passed this class is used only for the package name and does not necessarily need to be a class annotated with `@AerospikeRecord`. Creating a 'marker' class in the package with no functionality and passing to this method is a good way of preventing breaking the preloading as classes are moved around.

`withConfigurationFile`: Whilst mapping information from POJOs via annotations is efficient and has the mapping code inline with the POJO code, there are times when this is not available. For example, if an external library with POJOs is being used and it is desired to map those POJOs to the database, there is no easy way of annotating the source code. Another case this applies is if different mapping parameters are needed between different environments. For example, embedded objects might be stored in a map in development for ease of debugging, but stored in a list in production for compaction of stored data. In these cases an external configuration YAML file can be used to specify how to map the data to the database. See [External Configuration File](#external-configuration-file) for more details. There is an overload of this method which takes an additional boolean parameter -- if this is `true` and the configuration file is not valid, errors will be logged to `stderr` and the process continue. It is normally not recommended to set this parameter to true.

If multiple configuration files are used and the same class is defined in multiple configuration files, the definitions in the first configuration file for a class will be used. 

`withConfiguration`: Similar to the `withConfigurationFile` above, this allows configuration to be externally specified. In this case, the configuration is passed as a YAML string.

`withReadPolicy`, `withWritePolicy`, `withBatchPolicy`, `withScanPolicy`, `withQueryPolicy`: This allows setting of the appropriate policy type. The following discussion uses read policies, but applies equally to all the other policies.

After the specified policy, there are 3 possible options: 

- `forAll()`: The passed policy is used for all classes. This is similar to setting the defaultReadPolicy on the IAerospikeClient but allows it to be set after the client is created. 
- `forThisOrChildrenOf(Class<?> class)`: The passed policy is used for the passed class and all subclasses of the passed class.
- `forClasses(Class<?>... classes)`: The passed policy is used for the passed class(es), but no subclasses.

It is entirely possible that a class falls into more than one category, in which case the most specific policy is used. If no policy is specified, the defaultReadPolicy passed to the IAerospikeClient is used. For example, if there are classes A, B, C with C being a subclass of B, a definition could be for example:

```java
Policy readPolicy1, readPolicy2, readPolicy3;
// ... code to set up the policies goes here...
AeroMapper.Builder(client)
          .withReadPolicy(readPolicy1).forAll()
          .withReadPolicy(readPolicy2).forThisOrChildrenOf(B.class)
          .withReadPolicy(readPolicy3).forClasses(C.class)
          .build();
```

In this case the `forAll()` would apply to A,B,C, the `forThisOrChildrenOf` would apply to B,C and `forClasses` would apply to C. So the policies used for each class would be:

- A: `readPolicy1`
- B: `readPolicy2`
- C: `readPolicy3`
           
Note that each operation can also optionally take a policy if it is desired to change any of the policy settings on the fly. The explicitly provided policy will override any other settings, such as `durableDelete` on the `@AerospikeRecord`

If it is desired to change one part of a policy but keep the rest as the defaults set up with these policies, the appropriate policy can be read with `getReadPolicy`, `getWritePolicy`, `getBatchPolicy`, `getScanPolicy` and `getQueryPolicy` methods on the AeroMapper. For example, if we need a policy which was previously set up on a Customer class but need to change the `durableDelete` property, we could do

```java
WritePolicy writePolicy = new WritePolicy(mapper.getWritePolicy(Customer.class));
writePolicy.durableDelete = true;
mapper.delete(writePolicy, myCustomer);
```
Note that the `getXxxPolicy` methods return the actual underlying policy rather than a copy of it, so it is best to instantiate a new instance of this object before changing it.

In summary, the policy which will be used for a call are: (lower number is a higher priority)

1. Policy passed as a parameter
2. Policy passed to  `forClasses` method
3. Policy passed to `forThisOrChildrenOf` method
4. Policy passed to `forAll` method
5. AerospikeClient.getXxxxPolicyDefault

---

## Constructors
Given that the AeroMapper is designed to read and write information to an Aerospike database, it must be able to create objects when the data has been read from the database. To construct an object, it will typically use the default (no argument) constructor. 

However, there are times when this is not desirable, for example when the class declares final fields which must be mapped to the constructor. For example, consider the following class:

```java
@AerospikeRecord(namespace = "test", set = "testSet")
public class ConstructedClass {
	@AerospikeKey
	public final int id;
	public final int age;
	public final String name;
	public final Date date;
	
	public ConstructedClass(int id, int age, String name, Date date) {
		super();
		this.id = id;
		this.age = age;
		this.name = name;
		this.date = date;
	}
}
```

As it stands, this class cannot be used with the AeroMapper because there is no valid constructor to invoke when an object needs to be created. There is a constructor but it does not contain enough information to map the record on the database to the parameters of the constructor. (Remember that at runtime method and argument names are typically lost and become "arg1", "arg2" and so on). We can use this constructor in one of two ways:

1. We specify '-parameters' to javac, which will prevent it stripping out the names to the constructor
2. We can to provide this missing information with annotations:

```java
@AerospikeRecord(namespace = "test", set = "testSet")
public class ConstructedClass {
	@AerospikeKey
	public final int id;
	public final int age;
	public final String name;
	public final Date date;
	
	public ConstructedClass(@ParamFrom("id") int id,
                            @ParamFrom("age") int age,
                            @ParamFrom("name") String name,
                            @ParamFrom("date") Date date) {
		super();
		this.id = id;
		this.age = age;
		this.name = name;
		this.date = date;
	}
}
```

Now there is enough information to be able to construct an instance of this class from a database record. Note that the names of the @ParamFrom annotation (or the argument names if using -parameters) are the bin names, not the underlying field names. So if you have a field declared as

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
public class ConstructedClass2 {
	@AerospikeKey
	public final int id;
	public final int a;
	public int b;
	public int c;
	
	public ConstructedClass2(@ParamFrom("id") int id, @ParamFrom("a") int a) {
		this.id = id;
		this.a = a;
	}
}
```

When an instance of the ConstructedClass2 is read from the database, the constructor will be invoked and `a` and `id` set via the constructor, then `b` and `c` will be set by direct field access.

Note that whilst these examples show only final fields being set, this is not a requirement. The constructor can set any or all fields.

If there are multiple constructors on the class, the one to be used by the AeroMapper should be annotated with @AerospikeConstructor:

```java
@AerospikeRecord(namespace = "test", set = "testSet") 
public class ConstructedClass2 {
	@AerospikeKey
	public final int id;
	public final int a;
	public int b;
	public int c;
	
	public ConstructedClass2(@ParamFrom("id") int id, @ParamFrom("a") int a) {
		this.id = id;
		this.a = a;
	}
	@AerospikeConstructor
	public ConstructedClass2(@ParamFrom("id") int id, @ParamFrom("a") int a, @ParamFrom("b") int b) {
		this.id = id;
		this.a = a;
		this.b = b;
	}
}
```

In this case, the 3 argument constructor will be used. Note that you must annotate the desired constructor with @AerospikeConstructor on any class with multiple constructors, irrespective of how many of those constructors have the @ParamFrom annotations on their arguments. If more than 1 constructor is annotated with @AerospikeConstructor on a class an exception will be thrown the first time the mapper sees that class.

If no constructor is annotated with @AerospikeConstructor, the default no-argument constructor will be used. If there is no no-argument constructor but only one constructor on the class, that constructor will be used. If there is no default constructor and multiple other constructors but no @AerospikeConstructor annotated constructor has been declared, an exception will be thrown when the class is first used.

### Constructor Factories

Sometimes it is required to use a method to create an object instead of a constructor. For example, if an object is generated by a protobuf compiler it is created by calling `myClass.Builder.newBuilder()`. In these cases a factory class and a factory method can be used. 

As an example:

```java
public class Factory {
	public static A createA() {
		A newA = new A();
		newA.factory = "factory created";
		return newA;
	}
}

@AerospikeRecord(namespace = "test", set = "A", factoryMethod = "createA", factoryClass = "com.aerospike.mapper.Factory")
public class A {
	public String name;
	public int age;
	@AerospikeKey
	public int id;
	@AerospikeExclude
	public String factory;
	
	A() {}

	public A(String name, int age, int id) {
		super();
		this.name = name;
		this.id = id;
	}
}
```

When the Object Mapper needs to create a new instance of `A`, it will call the `createA` method on `com.aerospike.mapper.Factory` class. This method has a few requirements:

1. The method on the class must be static
2. The method can take zero parameters, one parameter or two parameters. If it takes one parameter, this can be either a `java.lang.Class` or `java.util.Map` and if it takes 2 parameters these must be a `java.lang.Class` followed by a `java.util.Map`. The `Class` parameter represents the type being instantiated, and the `Map` is a map of the attributes the Object Mapper knows will require instantiating. Hence the map is effectively a `Map<String, Object>`.

Note that you cannot specify a `factoryMethod` without a `factoryClass` or vice versa; either both must be specified or neither.

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
public void setCraziness(int value) {
	unmapped = value/3;
}
@AerospikeGetter(name="bob")
public int getCraziness() {
	return unmapped*3;
}
```

This will create a bin in the database with the name "bob".

It is possible for the setter to take an additional parameter too, providing this additional parameter is either a `Key` or `Value` object. This will be the key of the last object being loaded. 

So, for example, if we have an A object which embeds a B, when the setter for B is called the second parameter will represent A's key:

```java
@AerospikeRecord(namespace = "test", set = "A", mapAll = false)
public class A {
	@AerospikeBin
	private String key;
	private String value1;
	private long value2;
	
	@AerospikeGetter(name = "v1")
	public String getValue1() {
		return value1;
	}
	@AerospikeSetter(name = "v1")
	public void setValue1(String value1, Value owningKey) {
		// owningKey.getObject() will be a String of "B-1"
		this.value1 = value1;
	}
	
	@AerospikeGetter(name = "v2")
	public long getValue2() {
		return value2;
	}
	
	@AerospikeSetter(name = "v2")
	public void setValue2(long value2, Key key) {
		// Key will have namespace="test", setName = "B", key.userKey.getObject() = "B-1"
		this.value2 = value2;
	}
}

@AerospikeRecord(namespace = "test", set = "B")
public class B {
	@AerospikeKey 
	private String key;
	@AerospikeEmbed
	private A a;
}

@Test
public void test() {
	A a = new A();
	a.key = "A-1";
	a.value1 = "value1";
	a.value2 = 1000;
	
	B b = new B();
	b.key = "B-1";
	b.a = a;
	
	AeroMapper mapper = new AeroMapper.Builder(client).build();
	mapper.save(b);
	B b2 = mapper.read(B.class, b.key);
	
}
```

This can be useful in situations where the full key does not need to be stored in subordinate parts of the record. Consider a time-series use case where transactions are stored in a transaction container. The transactions for a single day might be grouped into a single transaction container, and the time of the transaction in microseconds may be the primary key of the transaction. If we model this with the transactions in the transaction container, the key for the transaction record could simply be the number of microseconds since the start of the day, as the microseconds representing the start of the day would be contained in the day number used as the transaction container key.

Since this information is redundant, it could be stripped out, shortening the length of the transaction key and hence saving storage space. However, when we wish to rebuild the transaction, we need the key of the transaction container to be able to derive the microseconds of the key to the start of the day to reform the appropriate transaction key.

----

## Default Mappings of Java Data type
Here are how standard Java types are mapped to Aerospike types:
| Java Type | Aerospike Type |
| --- | --- |
| byte | integral numeric |
| char | integral numeric |
| short | integral numeric |
| int | integral numeric |
| long | integral numeric |
| boolean | integral numeric |
| Byte | integral numeric |
| Character | integral numeric |
| Short | integral numeric |
| Integer | integral numeric |
| Long | integral numeric |
| Boolean | integral numeric |
| float | double numeric |
| double | double numeric |
| Float | double numeric |
| Double | double numeric |
| java.util.Date | integral numeric |
| java.time.Instant | integral numeric |
| String | String |
| byte[] | BLOB |
| enums | String |
| Arrays (int[], String[], Customer[], etc) | List |
| List<?> | List or Map |
| Map<?,?> | Map |
| Object Reference (@AerospikeRecord) | List or Map |

These types are built into the converter. However, if you wish to change them, you can use a [Custom Object Converter](#Custom-Object-Converters). For example, if you want Dates stored in the database as a string, you could do:

```java
public static class DateConverter {
    	private static final ThreadLocal<SimpleDateFormat> dateFormatter = ThreadLocal.withInitial(() ->
                new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS zzzZ"));
    @ToAerospike
    public String toAerospike(Date date) {
    	if (date == null) {
    		return null;
    	}
		return dateFormatter.get().format(date);
    }

    @FromAerospike
    public Date fromAerospike(String dateStr) throws ParseException {
    	if (dateStr == null) {
    		return null;
    	}
    	return dateFormatter.get().parse(dateStr);
    }
}

AeroMapper convertingMapper = new AeroMapper.Builder(client).addConverter(new DateConverter()).build();
```

(Note that SimpleDateFormat is not thread-safe, and hence the use of the ThreadLocal variable)

This would affect all dates. If you wanted to affect the format of some dates, create a sub-class Date and have the converter change that to the String format.

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

There are times when it makes sense to store the digest of the child record as the reference rather than it's primary key. For example, if the native primary key is of significant length then storing a fixed 20-byte digest makes sense. This can be accomplished by adding `type = ReferenceType.DIGEST` to the @AerospikeReference. For example:

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

#### Batch Loading

Note that when objects are stored by non-lazy references, all dependent children objects will be loaded by batch loading. For example, assume there is a complex object graph like:

![Object Diagram](/images/complexObjectGraph.png)

Note that some of the objects are embedded and some are references.

If we then instantiate a complex object graph like:

![Object Graph](/images/objectInstantiation.png)

Here you can see the Customer has a lot of dependent objects, where the white objects are being loaded by reference and the grey objects are being embedded into the parent. When the Customer is loaded the entire object graph is loaded. Looking at the calls that are performed to the database, we see:

```
Get: [test:customer:cust1:818d8a436587c36aef4da99d28eaf17e3ce3a0e1] took 0.211ms, record found
Batch: [4/4 keys] took 0.258ms
Batch: [6/6 keys] took 0.262ms
Batch: [2/2 keys] took 0.205ms
```

The first call (the `get`) is for the Customer object, the first batch of 4 is for the Cusomter's 4 accounts (Checking, Savings, Loan, Portfolio), the second batch of 6 items is for the 2 checkbooks and 4 security properties, and the last batch of 2 items is for the 2 branches. The AeroMapper will load all dependent objects it can in one hit, even if they're of different classes. This includes elements within LIsts, Arrays and Maps as well as straight dependent objects. This can make loading complex object graphs very efficient.


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

#### Versioning Lists

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
transactions: MAP('{"Txn1":[100, 1610478132904000000, "Bob's store", "Txn1"], 
				"Txn2":[134.99, 1610449332904000000, "Kim's store", "Txn2"], 
				"Txn3":[75.43000000000001, 1610406132907000000, "Sue's store", "Txn3"]}')
type: "SAVINGS"
```

Here the transaction time is the second attribute in each list, and the amount is the first attribute. However, a common request is to be able to extract transaction by time. For example, in fraud detection systems, there may be a need to load the N most recent transactions. If the transactions were to be stored with the transaction time as the first element in the list, efficient CDT operations in Aerospike such as `getByValueRange(...)` can be used.

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
transactions: MAP('{"Txn1":[1610478716965000000, 100, "Bob's store", "Txn1"], 
				"Txn2":[1610449916965000000, 134.99, "Kim's store", "Txn2"], 
				"Txn3":[1610406716967000000, 75.43000000000001, "Sue's store", "Txn3"]}')
type: "SAVINGS"
```

Multiple ordinals can be specified for a single class, but these must be sequential. So if it is desired to have the first 3 fields in a list specified, they must have @AerospikeOrdinal values of 1,2 and 3.

**Note**: Ordinal fields cannot be versioned.
  
#### The importance of Generic Types

When using the object mapper, it is important to use generics to describe the types as fully as possible. For example instead of `List accounts;` this should be `List<Account> accounts;`. Not only is this best practices for Java, but it gives the AeroMapper hints about what is mapped so it can optimize the type and minimize the amount of reflection needed at runtime and hence minimize the performance cost. 

For example, assume there is a mapped type "B", and another "A" which has a list of B's:

```java
@AerospikeRecord(namespace = "test", set = "A")
public static class A {
	@AerospikeKey
	public int id;
	public List<B> listB;
	
	public A() {
		listB = new ArrayList<>();
	}
}

@AerospikeRecord(namespace = "test", set = "B")
public static class B {
	@AerospikeKey
	public int id;
	public String name;
}
```

In this case, the AeroMapper knows that the `listB` object contains either B's or sub-classes of B's. If they're B's, it knows the type (it assumes they're of the declared type by default) and hence needs no extra information to describe it. If an element is a subclass of B it would include the type name in the object reference. In this case we store a B:

```java
	B b = new B();
	b.id = 2;
	b.name = "test";
	mapper.save(b);
	
	A a = new A();
	a.id = 1;
	a.listB.add(b);
	mapper.save(a);
```


But in this case, the object is of the declared type (B) so this needs no type information. Hence, the object stored in the database is:

```
id: 1
listB: LIST('[2]')
```

However, if the class A was declared as:

```java
public static class A {
	@AerospikeKey
	public int id;
	public List listB;
	
	
	public A() {
		listB = new ArrayList<>();
	}
}
```

(Note the only difference is that the `List<B> listB` has now become `List listB`).

In this case, the AeroMapper no longer has any type information so it needs to store full type information against each element in the list:

```
id: 1
listB: LIST('[[2, "@T:B"]]')
```

Note that the element is annotated with `@T:` and the short name of the type. (The short name of the type must be unique within the system, and can be changed using the `shortName` attribute of the `AerospikeRecord` annotation.

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

In this case, if the environment variable ``ACCOUNT_TITLE_BIN_NAME`` is set, that will be the name of the bin which is used. If it is not set, it will be like the annotation does not specify the ``name`` parameter at all, which means that the field name (``title``) will be used for the bin name.

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

It should be noted that the use of subclasses can have a minor degradation on performance. When the declared type is the same as the instantiated type, the Java Object Mapper has already computed the optimal way of accessing that information. If it encounters a sub-class at runtime (i.e. the instantiated type is not the same as the declared type), it must then work out how to store the passed sub-class. The sub-class information is also typically cached so the performance hit should not be significant, but it is there.

By the same token, it is always better to use Java generics in collection types to give the Java Object Mapper hints about how to store the data in Aerospike so it can optimize its internal processes.

For example, say we need a list of Customers as a field on a class. We could declare this as:

```java
public List<Customer> customers;
```

or

```java
public List customers;
```

The former is considered better style in Java and also provides the Java Object Mapper with information about the elements in the list, so it will optimize its workings to know how to store a list of Customers. The latter gives it no type information so it must derive the type -- and hence how to map it to Aerospike -- for every element in this list. This can have a noticeable performance impact for large lists, as well as consuming more database space (as it must store the runtime type of each element in the list in addition to the data).

### Using Interfaces
Sometimes it is better to have an interface to group common types rather an an abstract superclass. In this case the Object Mapper supports placing the `@AerospikeReocrd` annotation on the interface and it will behave as if the annotation was on a superclass. There are multiple different was of placing the `@AerospikeRecord` annotation on a single class, and the order the Object Mapper looks for them in is:
1. Configuration file
2. Class level definition
3. First parent class with `@AerospikeRecord` annotation (of any ancestor)
4. Interface with `@AerospikeRecord` annotation (first one found)

Once the Object Mapper finds an appropriate annotation it ignores any further annotations and uses the definitions on the first one found.

----

### Custom Object Converters
Sometimes, the representation of the data in Aerospike and the representation in Java should be very different. Consider a class which represents a playing card and another class which represents a poker hand:

```java
public enum Suit {
    CLUBS, DIAMONDS, HEARTS, SPADES
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
(Note: DataClass and ContainerClasss were defined as static inner classes inside AeroMapperConfigurationYamlTest, hence the need for the long class name. In real production applications this isn't likely to be needed)
 
### File Structure
The structure of the file is: 
 
Top level is an array of classes. Each class has:
- **class**: the name of the class. This must match the full class name to which you want to apply the configuration
- **namespace**: The namespace to map this class to. Can be unspecified if the class is only ever used for embedding in another object
- **set**: The set to map this class to. Can be unspecified if the class is only ever used for embedding in another object
- **factoryClass**: The class of the factory to use, if any. If this is specified, so must a factoryMethod.
- **factoryMethod**: The static method of the factory to use, if any. If this is specified, so must a factoryClass. See [Constructor Factories](#constructor-factories) for more information.
- **durableDelete** (boolean): If set to `true`, any deletes on this class will use [durable deletes](https://www.aerospike.com/docs/guide/durable_deletes.html). If not set, it will use the flag from the policy for this class
 - **mapAll** (boolean, default `true`): If true, all fields of this class will automatically be mapped to the database. Fields can be excluded using `exclude` on the bin config. If this is set to false, only the fields specified with an explicit bin configuration will be stored.
 - **sendKey** (boolean): If true, the key of the record will be stored in Aerospike. See [send key](https://www.aerospike.com/docs/guide/policies.html#send-key) for more details. If this is false, the key will not be stored in Aerospike. If not set, the `sendKey` field from the policy will be used.
 - **ttl**: the time to live for the record, mapped to the expiration time on the policy. If not set, the expiration from the policy will be used.
 - **shortName**: When this class name must be stored in the database, this is the name to store instead of the full class names. This is used particularly for sub-classes. For example, if an Account class has a Checking class and Savings class as subclasses, an object might store a reference to an Account (compiled type of Account), but this really is a Checking account (runtime type of Checking). If the reference to the account is persisted, a list storing the key and the type will be saved, and this name will be used as the type.
 - **key**: a [key structure](key-structure), specified below
 - **bins**: a list of [bin structure](bin-structure), specified below
 - **version**: The version of the record. Must be an integer with a positive value. If not specified, will default to 1. See [Versioning Links](#versioning-lists) for more details. 

#### Key Structure
The key structure is used to specify the key to a record. Keys are optional in some situations. For example, if Object A embeds an Object B, B does not need a key as it is not stored in Aerospike in its own right.

The key structure contains:
- **field**: The name of the field which to which this key is mapped. If this is provided, the getter and setter cannot be provided.
- **getter**: The getter method used to populate the key. This must be used in conjunction with a setter method, and excludes the use of the field attribute.
- **setter**: The setter method used to map data back to the Java key. This is used in conjunction with the getter method and precludes the use of the field attribute. Note that the return type of the getter must match the type of the first parameter of the setter, and the setter can have either 1 or 2 parameters, with the second (optional) parameter being either of type [com.aerospike.client.Key](https://www.aerospike.com/apidocs/java/com/aerospike/client/Key.html) or Object.

#### Bin Structure
The bin structure contains:
- **embed**: An [embed structure](#embed-structure) used for specifying that the contents of this bin should be included in the parent record, rather than being a reference to a child record. There can only be one embed structure per field, and if an embed structure is present, a [reference structure](#reference-structure) cannot be. If a field refers to another AerospikeRecord, either in a collection or in it's own right, and neither an embed or reference structure is specified, a reference will be assumed by default.
- **exclude**: A boolean value as to whether this bin should be mapped to the database. Defaults to true.
- **field**: The name of the field which to which this bin is mapped. If this is provided, the getter and setter cannot be provided.
- **getter**: The getter method used to populate the bin. This must be used in conjunction with a setter method, and excludes the use of the field attribute.
- **name**: The name of the bin to map to. If this is not provided and a field is, this will default to the field name. The name must be provided if this bin maps to a getter/setter combination.
- **ordinal**: For items mapped as lists, this ordinal specifies the location of this bin in the list. If this is not provided, the position of the bins in the list will be determined by alphabetical ordering.
- **reference**: A [reference structure](#reference-structure) detailing that a child object referenced by this bin should be stored as the key of the child rather than embedding it in the parent object. The use of a reference precludes the use of the embed attribute, and if neither is specified then reference is assumed as the default.
- **setter**: The setter method used to map data back to the Java POJO. This is used in conjunction with the getter method and precludes the use of the field attribute. Note that the return type of the getter must match the type of the first parameter of the setter, and the setter can have either 1 or 2 parameters, with the second (optional) parameter being either of type [com.aerospike.client.Key](https://www.aerospike.com/apidocs/java/com/aerospike/client/Key.html) or Object.

#### Embed Structure
The embed structure is used when a child object should be fully contained in the parent object without needing to be stored in the database as a separate record. For example, it might be that Customer object contains an Address, but the Address is not stored in a separate table in Aerospike, but rather put into the database as part of the customer record.

The Embed structure contains:
- **type**: The type of the top level reference. If this is just a reference to another object, eg 

```java
public class Customer {
	...
	@AerospikeEmbed
	Address address;
```

then the type refers to how the child will be stored in the parent. There are 2 options: LIST or MAP. Maps are more readable in that each bin in the child object is named, but this name consumes space in the database and hence this is the less efficient storage structure.

If the top level reference is a container class (List or Map), then this type refers to how the list or map is represented in the database. For example, 

```java
public class Customer {
	...
	List<Address> address;
```

If this has a type of LIST, then the addresses in Aerospike will be stored in a list. Lists preserve the ordering in the original list. However, it can also be stored as a MAP, in which case the key of the sub-object (Address in this case) becomes the map key and the elements become the value in the map. In this case the list ordering is NOT preserved upon retrieval -- the map elements are stored in a K_ORDERED map, so the elements will be returned sorted by their key.

- **elementType**:  If the top level reference is a container (List or Map), this type specifies how the children objects are to be stored in Aerospike. For example, if `type = MAP` and `elementType = LIST` for the list of Customers in the above example, the bin in Aerospike will contain a K_ORDERED map, each of which will have an Address as the value, and the elements of the address will be stored in a list.

- **saveKey**: Boolean, defaults to false. This is useful when storing a list of elements as a LIST inside a MAP. Given the map key is the key of the record, it is often redundant to have the key stored separately in the list of values for the underlying object. However, if it is desired to have the key again in the list, set this value to true.

#### Reference Structure
The reference structure is used when the object being referenced is not to be embedded in the owning object, but rather is to be stored in a separate table. 
- **lazy**: Boolean, defaults to false. When the parent object is loaded, references marked as lazy are NOT loaded. Instead a placeholder object is created with only the primary key information populated, so those objects can be loaded later.
- **batchLoad**: Boolean, defaults to true. When the parent object is loaded, all non-lazy children will also be loaded. If there are several children, it is more efficient to load them from the database using a batch load. if this flag is set to false, children will not be loaded via a batch load. Note that if the parent object has 2 or less children to load, it will single thread the batch load as this is typically more performant than doing a very small batch. Otherwise the batchPolicy on the parent class will dictate how many nodes are hit in the batch at once.
- **type**: Either ID or DIGEST, defaults to ID. The ID option stores the primary key of the referred object in the referencer, the DIGEST stores the digest instead. Note that DIGEST is not compatible with `lazy=true` as there is nowhere to store the digest. (For example, if the primary key of the object is a long, the digest is 20 bytes, without dynamically creating proxies or subtypes at runtime there is nowhere to store these 20 bytes. Dynamically creating objects like this is not performant so is not allowed).

### Configuration through code

It is also possible to configure classes through code. This is very useful in situations where external libraries (whose source code is not available) are used and providing all the information in an external configuration file is overkill. This configuration is performed when building the Object Mapper. Let's look at this with an example:

```java
@Data
@AerospikeRecord(namespace = "test")
public class A {
    @AerospikeKey
    private long id;
    @AerospikeEmbed(type = AerospikeEmbed.EmbedType.LIST)
    private List<B> b;
    private String aData;
}
    
@Data
public class B {
    private C c;
    private String bData;
}
    
@Data
public class C {
    private String id;
    private String cData;
}
```

In this example, let's assume that the source code is available for class `A` but not for either `B` or `C`. If we run this as is, the Object Mapper will not know how to handle the child classes. It will determine that B should be mapped as it's referenced directly from A, but has no idea what to do with C. Using a default builder will throw a `NotSerializableException`. 

To solve this, we can introduce some configuration in the builder:
```java
ClassConfig classConfigC = new ClassConfig.Builder(C.class)
        .withKeyField("id")
        .build();
ClassConfig classConfigB = new ClassConfig.Builder(B.class)
        .withFieldNamed("c").beingEmbeddedAs(AerospikeEmbed.EmbedType.MAP)
        .build();
AeroMapper mapper = new AeroMapper.Builder(client)
        .withClassConfigurations(classConfigB, classConfigC)
        .build();
```

In this case we've told the mapper that `B.class` should be treated as an `AerospikeRecord` (`.withConfigurationForClass(B.class)`) and that the 'c' field in that class should be embedded as a MAP. The class `C` is also set to be a mapped class and that the key of that class is to be the field `id`. The class needs to have a key as it's being stored in a map, and objects being stored in a map must be identified by a key.

## Virtual Lists

When mapping a Java object to Aerospike the most common operations to do are to save the whole object and load the whole object. The AeroMapper is set up primarily for these use cases. However, there are cases where it makes sense to manipulate objects directly in the database, particularly when it comes to manipulating lists and maps. This functionality is provided via virtual lists.  

Consider a TODO list, where there are Items which contain the items to be performed and a container for these items:

```java
@AerospikeRecord(namespace = "test", set = "item")
public class Item {
	@AerospikeKey
	private int id;
	private Date due;
	private String desc;
	public Item(int id, Date due, String desc) {
		super();
		this.id = id;
		this.due = due;
		this.desc = desc;
	}
	
	public Item() {
	}
}

@AerospikeRecord(namespace = "test", set = "container")
public class Container {
	@AerospikeKey
	private int id;
	private String name;
	@AerospikeEmbed(type = EmbedType.MAP, elementType = EmbedType.LIST)
	private List<Item> items;
	
	public Container() {
		this.items = new ArrayList<>();
	}
}
````

Note that in this case the items are embedded into the container and not referenced. This is what is needed for virtual lists, they must have a list of items in the database associated with a single record.

These items can be populated using the functionally presented above. For example:

```javs
Container container = new Container();
container.id = 1;
container.name = "container";

container.items.add(new Item(100, new Date(), "Item 1"));
container.items.add(new Item(200, new Date(), "Item 2"));
container.items.add(new Item(300, new Date(), "Item 3"));
container.items.add(new Item(400, new Date(), "Item 4"));

AeroMapper mapper = new AeroMapper.Builder(client).build();
mapper.save(container);
````

This yields a container with 4 items as expected:

```
id: 1
items: KEY_ORDERED_MAP('{
	100:["Item 1", 1618442036607], 
	200:["Item 2", 1618442036607], 
	300:["Item 3", 1618442036607], 
	400:["Item 4", 1618442036607]}')
name: "container"
```

Note that whilst in this case the list is pre-populated with information, this is not a requirement for using virtual list.

A virtual list is created through the mapper:

```java
VirtualList<Item> list = mapper.asBackedList(container, "items", Item.class);
```

The container is passed as the first parameter, and is used for 2 things: The class type (so the annotations and field definitions can be discovered) and the primary key. It is possible to pass these 2 parameters instead of explicitly passing an object.

Once a virtual list has been created, methods to manipulate the list can be executed. For example:

```java
list.append(new Item(500, new Date(), "Item5"));
```

After this, the list in the database looks like:

```
id: 1
items: KEY_ORDERED_MAP('{
	100:["Item 1", 1618442036607], 
	200:["Item 2", 1618442036607], 
	300:["Item 3", 1618442036607], 
	400:["Item 4", 1618442036607], 
	500:["Item5", 1618442991205]}')
name: "container"
```

Note however that the list in the object in memory still contains only 4 items. *Virtual lists affect only the database representation of the data and not the Java POJO.*
Virtual Lists tend to use the [Operate](https://www.aerospike.com/docs/client/java/usage/kvs/multiops.html) command which allows multiple operations to be performed on the same key at the same time. As a consequence, multiple commands can be done on a list with a single Aerospike operation. For example:

```java
List<Item> results = (List<Item>) list.beginMultiOperation()
		.append(new Item(600, new Date(), "Item6"))
		.removeByKey(200)
		.getByKeyRange(100, 450)
	.end();
```

This operation will add a new item (600) into the list, remove key 200 and get any keys between 100 (inclusive) and 450 (exclusive). As a result, the data in the database is:

```
id: 1
items: KEY_ORDERED_MAP('{
	100:["Item 1", 1618442036607], 
	300:["Item 3", 1618442036607], 
	400:["Item 4", 1618442036607], 
	500:["Item5", 1618442991205], 
	600:["Item6", 1618445996551]}')
name: "container"
```

The result of the call is the result of the last read operation in the list of calls if one exists, otherwise it is the last write operation. So in this case, the result will be the result of the `getByKeyRange` call, which is 3 items: 100, 300, 400.

However, if we changed the call to be:

```java
List<Item> results = (List<Item>) list.beginMultiOperation()
		.append(new Item(600, new Date(), "Item6"))
		.removeByKey(200)
	.end();
```

Then the result would be the result of the `removeByKey`, which by default is null. (Write operations pass a ReturnType of NONE to CDT operations by default)

However, if we wanted a particular operation in the list to return its result, we can flag it with `asResult()`. For example:

```java
List<Item> results = (List<Item>) list.beginMultiOperation()
		.append(new Item(600, new Date(), "Item6"))
		.removeByKey(200).asResult()
		.removeByKey(500)
	.end();
```

In this case, the element removed with with the `removeByKey(200)` will be returned, giving the data associated with item 200.

The type of the result (where supported) can also be changed with a call to `asResultOfType()`. For example:

```java
long count = (long)list.beginMultiOperation()
		.append(new Item(600, new Date(), "Item6"))
		.removeByKey(200)
		.removeByKeyRange(20, 350).asResultOfType(ReturnType.COUNT)
		.getByKeyRange(100, 450)
	.end();
```

The return type of the method is now going to be a long as it represents the count of elements removed (2 in this case). Note that this example is not very practical -- there is no point in calling `getByKeyRange(...)` in this call as the result is ignored.

Also note that virtual lists allow operations only on the list, not on other bins on the same record. To do this, you would have to use the underlying native Aerospike API. There are however convenience methods on the AeroMapper which can help map between Aerospike and Java formats.. For example:

```java
public <T> List<Object> convertToList(@NotNull T instance);
public <T> Map<String, Object> convertToMap(@NotNull T instance);
public <T> T convertToObject(Class<T> clazz, List<Object> record);
public <T> T convertToObject(Class<T> clazz, Map<String,Object> record);
public <T> T convertToObject(Class<T> clazz, Record record);
```

Note: At the moment not all CDT operations are supported, and if the underlying CDTs are of the wrong type, a different API call may be used. For example, if you invoke `getByKeyRange` on items represented in the database as a list, `getByValueRange` is invoked instead as a list has no key.

## Scans
Scans can be used to process every record in a set. The scan iterates through every item in the set and invokes a callback for every item in the set. For example:

```java
mapper.scan(Person.class, (person) -> {
	// ... process person
	return true;
});
```

If the processing method returns true, the scan continues. However, if the processing method returns false the scan will abort. Note that if the scan policy calls for multi-threading of the scans, the callback method may be invoked by multiple threads at once and hence must be thread safe. If one thread aborts the scan, other threads already in the processing method will finish processing their records.

Note that if you want to process only some records in a set you can attach an Expression on the optional policy passed to the scan. For example, if there is a `Person` class:

```java
@AerospikeRecord(namespace = "test", set = "testScan")
public class Person {
	@AerospikeKey
	private int id;
	private String name;
	private int age;
	
	public Person(@ParamFrom("id") int id, @ParamFrom("name") String name, @ParamFrom("age") int age) {
		super();
		this.id = id;
		this.name = name;
		this.age = age;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getAge() {
		return age;
	}
}
```
 
and then several people are inserted:
 
 ```java
mapper.save(new Person(1, "Tim", 312),
			new Person(2, "Bob", 44),
			new Person(3, "Sue", 56),
			new Person(4, "Rob", 23),
			new Person(5, "Jim", 32),
			new Person(6, "Bob", 78));
 ```

As a contrived example, let's say we want to count the number of people in the set called "Bob". We can simply do:

```java
AtomicInteger counter = new AtomicInteger(0);
ScanPolicy scanPolicy = new ScanPolicy(mapper.getScanPolicy(Person.class));
scanPolicy.filterExp = Exp.build(Exp.eq(Exp.stringBin("name"), Exp.val("Bob")));
mapper.scan(scanPolicy, Person.class, (person) -> {
	counter.incrementAndGet();
	return true;
});
```

Note that when we altered the ScanPolicy, we had to make a copy of it first. If we fail to do this, the ScanPolicy will be altered for all subsequent calls. To clarify, the **wrong** way to set the scan policy is

```java
ScanPolicy scanPolicy = mapper.getScanPolicy(Person.class);
scanPolicy.filterExp = Exp.build(Exp.eq(Exp.stringBin("name"), Exp.val("Bob")));
```

and the **right** way to set an expression is

```java
ScanPolicy scanPolicy = new ScanPolicy(mapper.getScanPolicy(Person.class));
scanPolicy.filterExp = Exp.build(Exp.eq(Exp.stringBin("name"), Exp.val("Bob")));
```

## Queries

Similar to Scans, Queries can processed using the AeroMapper. Syntactically, the only difference between a query and a scan is the addition of a `Filter` on the Query which dictates the criteria of the query. A secondary index must be defined on the Bin referenced in the Filter or an error will be thrown. If no filter is passed, the query will be turned into a scan.

Similar to Scans, returning `false` on the processing method will abort the Query and process no further records, and additional filter criteria can be added using Expressions on the QueryPolicy.

```java
mapper.query(A.class, (a) -> {
	System.out.println(a);
	counter.incrementAndGet();
	return true;
}, Filter.range("age", 30, 54));

```

