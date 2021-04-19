package com.aerospike.mapper;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.aerospike.client.DebugAerospikeClient;
import com.aerospike.client.DebugAerospikeClient.Granularity;
import com.aerospike.client.DebugAerospikeClient.Options;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.tools.AeroMapper;

public class BatchLoadTest extends AeroMapperBaseTest {
	@AerospikeRecord(namespace = "test", set = "B")
	public static class B {
		@AerospikeKey
		public int id;
		public String name;
		
		public B(@ParamFrom("id") int id, @ParamFrom("name") String name) {
			super();
			this.id = id;
			this.name = name;
		}
	}
	
	@AerospikeRecord(namespace = "test", set = "A")
	public static class A {
		@AerospikeKey
		public int id;
		public String name;
		public List<B> data;
		public A(int id, String name) {
			super();
			this.id = id;
			this.name = name;
			data = new ArrayList<>();
		}
		
		public A addB(B b) {
			data.add(b);
			return this;
		}
		
		public A() {
			super();
		}
	}
	
	@Test
	public void testBatchLoad() {
		AeroMapper mapper = new AeroMapper.Builder(new DebugAerospikeClient(client, new Options(Granularity.EVERY_CALL))).build();
		
		B[] bees = new B[100];
		for (int i = 0; i < 100; i++) {
			bees[i] = new B(i, "B-" + i);
		}
		A[] as = new A[10];
		for (int i = 0; i < 10; i++) {
			as[i] = new A(100+i, "A-" + i);
			for (int j = 0; j < i*8; j++) {
				as[i].addB(bees[i+10+j]);
			}
		}
		
		mapper.save((Object[])bees);
		mapper.save((Object[])as);
		
		System.out.println("--- Reading single object (a[1]) ---");
		A result = mapper.read(A.class, as[1].id);
		compare(as[1], result);
		
		System.out.println("--- Reading batch object with 6 keys ---");
		A[] results = mapper.read(A.class, as[4].id, as[7].id, as[5].id, as[0].id, as[1].id, 3000);
		compare(results[0], as[4]);
		compare(results[1], as[7]);
		compare(results[2], as[5]);
		compare(results[3], as[0]);
		compare(results[4], as[1]);
		compare(results[5], null);

	}
	
}
