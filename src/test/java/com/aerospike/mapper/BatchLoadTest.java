package com.aerospike.mapper;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.tools.AeroMapper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BatchLoadTest extends AeroMapperBaseTest {

	@AerospikeRecord(namespace = "test", set = "B")
	public static class B {
		@AerospikeKey
		public int id;
		public String name;
		
		public B(@ParamFrom("id") int id, @ParamFrom("name") String name) {
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
			this.id = id;
			this.name = name;
			data = new ArrayList<>();
		}

		public void setBList(List<B> bees) {
			data = bees;
		}
		
		public A() {}
	}

	@Test
	public void testBatchLoad() {
		AeroMapper mapper = new AeroMapper.Builder(client).build();

		B[] bees = new B[100];
		for (int i = 0; i < 100; i++) {
			bees[i] = new B(i, "B-" + i);
		}

		A[] as = new A[10];
		for (int i = 0; i < 10; i++) {
			as[i] = new A(100 + i, "A-" + i);
			as[i].setBList(Arrays.asList(
					Arrays.copyOfRange(bees, i * 10, (i + 1) * 10)));
		}

		mapper.save((Object[])bees);
		mapper.save((Object[])as);

		System.out.println("--- Reading single object (bees[1]) ---");
		B resultB = mapper.read(B.class, bees[1].id);
		compare(bees[1], resultB);

		System.out.println("--- Reading single object (a[1]) ---");
		A resultA = mapper.read(A.class, as[1].id);
		compare(as[1], resultA);

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
