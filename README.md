# Aerospike Java Mapper

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
public static class Account {
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


