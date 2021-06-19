package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.aerospike.client.Key;
import com.aerospike.client.Value;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeGetter;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeSetter;
import com.aerospike.mapper.tools.AeroMapper;

public class MultipleParameterSetterTest extends AeroMapperBaseTest {

	@AerospikeRecord(namespace = "test", set = "A", mapAll = false)
	public static class A {
		private String key;
		private String value1;
		private long value2;
		
		@AerospikeKey
		public String getKey() {
			return key;
		}
		@AerospikeKey(setter = true)
		public void setKey(String key) {
			this.key = key;
		}
		
		@AerospikeGetter(name = "v1")
		public String getValue1() {
			return value1;
		}
		@AerospikeSetter(name = "v1")
		public void setValue1(String value1, Value owningKey) {
			assertEquals("B-1", owningKey.getObject());
			this.value1 = value1;
		}
		
		@AerospikeGetter(name = "v2")
		public long getValue2() {
			return value2;
		}
		
		@AerospikeSetter(name = "v2")
		public void setValue2(long value2, Key key) {
			assertEquals("test", key.namespace);
			assertEquals("B", key.setName);
			assertEquals("B-1", key.userKey.getObject());
			this.value2 = value2;
		}
	}
	
	@AerospikeRecord(namespace = "test", set = "B")
	public static class B {
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
		
		compare(b, b2);
	}
}
