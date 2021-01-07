package com.aerospike.mapper.examples;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.mapper.tools.AeroMapper;

public class AeroMapperExample {
	
	public static void main(String[] args) {
		
		IAerospikeClient client = new AerospikeClient("localhost",3000);
		
		AeroMapper mapper = new AeroMapper.Builder(client)
				// The following lines are for performance reasons only, they are not required.
				.preLoadClass(Account.class)
				.preLoadClass(Person.class)
				.preLoadClass(Product.class)
				.build();
		
		for (int i = 0; i < 100; i++) {
		
		Account a = new Account();
		a.setBalance(100);
		a.setId(1);
		a.setTitle("My Account");
		a.setUnmapped(200);
		mapper.save(a);
		System.out.println(a);
		Account b = mapper.read(Account.class, a.getId());
		System.out.println(b);
		//mapper.delete(a);
			
		Product product = new Product();
		product.setName("Checking account product");
		product.setType(ProductType.CHECKING);
		product.setVersion(3);
		product.setId(1000);
		mapper.save(product);
		mapper.read(Product.class, "1000:3");
		
		Account c = new Account();
		c.setBalance(1000);
		c.setId(2);
		c.setTitle("John Doe's primary account");
		c.setUnmapped(250);
		c.setProduct(product);
		mapper.save(c);

		Person p = new Person();
		p.setFirstName("John");
		p.setLastName("Doe");
		p.setSsn("123456789");
		p.setAge(17);
		p.setDateOfBirth(new Date());
		p.setValid(true);
		p.setBalance(1.23f);
		p.setHeight(12.321);
		p.setPhoto("This is a test byte string".getBytes());
		List<String> strings = new ArrayList<>();
		strings.add("String 1");
		strings.add("String 2");
		strings.add("String 3");
		
		p.setStringList(strings);
		p.getAccounts().add(a);
		p.getAccounts().add(c);
		
		p.setLongData(new long[] {1,2,3,4,5});
		
		p.setAccountArray(new Account[] {a, b, c});
		
		Map<Integer, String> testMap = new HashMap<>();
		testMap.put(4, "a");
		testMap.put(3, "b");
		testMap.put(2, "c");
		testMap.put(1, "d");
		p.setTestMap(testMap);
		
		Map<String, Product> products = new HashMap<>();
		products.put("Product 1", new Product(200, 1, "Product 200v1", ProductType.CHECKING));
		products.put("Product 2", new Product(200, 2, "Product 200v2", ProductType.CHECKING));
		products.put("Product 3", new Product(300, 1, "Product 300v1", ProductType.SAVINGS));
		p.setProductMap(products);
		
		Account otherAccount = new Account();
		otherAccount.setBalance(150);
		otherAccount.setCrazyness(22);
		otherAccount.setId(17);
		otherAccount.setProduct(product);
		otherAccount.setTitle("This is an account title");
		
		// Primary account is saved by embedding, so does not need saving in the database.
		p.setPrimaryAccount(a);
		// Secondary account is saved by embedding, so does not need saving in the database.
		p.setSecondaryAccount(otherAccount);
		// tertiary account is saved by reference, so must have the account explicitly saved to the database
		p.setTertiaryAccount(c);
		System.out.printf("Original person: %s\n", p.toString());

		long now = System.nanoTime();
		mapper.save("test",p);
		System.out.printf("Saved in %,.3fms\n", ((System.nanoTime() - now) / 1_000_000.0));
		}
		System.out.println(client.get(null, new Key("test", "people", "123456789")));
		
		Person p1 = mapper.read(Person.class, "123456789");
		System.out.printf("Loaded person: %s\n", p1.toString());
      	
    	Function<Person,Boolean> function = person -> {
    		System.out.println(String.format("\n\n:::: %s ::::", person.getSsn()));
			System.out.println(person.toString());
			
			//mapper.delete(person);
    		return true;
    	};
    	
    	mapper.find(Person.class, function);
	}
}
