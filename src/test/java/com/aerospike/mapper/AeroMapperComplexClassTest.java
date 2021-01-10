package com.aerospike.mapper;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;

public class AeroMapperComplexClassTest extends AeroMapperBaseTest {

	@AerospikeRecord(namespace = "test", set = "testSet", mapAll = true, sendKey = true)
	public static class ComplexClass {
		private long trid;
		private String cnid;
		private double amt;
		@AerospikeKey
		private Instant trts;
		private String res;
		private String meid;
		private List<Integer> byte4Fields;
		private List<Short> byte2Fields;
		private List<Byte> byte1Fields;
		private List<Character> charFields;
		private List<String> strFields;
		private boolean testData = false;
		public ComplexClass() {}
	}
	
    private AeroMapper mapper;

    @Before
    public void setup() {
        mapper = new AeroMapper.Builder(client).build();
        client.truncate(null, "test", "testSet",null);
    }

	@Test
	public void test() {

		ComplexClass complex = new ComplexClass();
		complex.amt = 100.1;
		complex.trts = Instant.now();
		complex.trid = 18;
		complex.cnid = "CN19";
		complex.res = "result";
		complex.meid = "ME11";
		complex.byte4Fields = Arrays.asList(new Integer[] {1,2,3,4,5});
		complex.byte2Fields = Arrays.asList(new Short[] {1,2,3,4,5,6,7});
		complex.byte1Fields = Arrays.asList(new Byte[] {1,2,3});
        mapper.save(complex);
        
        ComplexClass complex2 = mapper.read(ComplexClass.class, complex.trts);
        System.out.println("Complex2 loaded");
	}
}
