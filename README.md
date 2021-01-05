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

This saves the record with 4 bins, a,c,d. If a field is marked with both @AerospikeExclude and @AerospikeBin, the bin will _not_ be mapped to the database.

## Properties
A pair of methods comprising a getter and setter can also be mapped to a field in the database. These should be annotated with @AerospikeGetter and @AerospikeSetter respectively and the name attribute of these annotations must be provided. The getter must take no arguments and return something, and the setter must return void and take 1 parameter of the same time as the getter return value. Both a setter and a getter must be provided, an exception will be thrown otherwise.

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

### Associating by Embedding

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


