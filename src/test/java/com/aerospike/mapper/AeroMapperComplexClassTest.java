package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AeroMapperComplexClassTest extends AeroMapperBaseTest {

	@AerospikeRecord(namespace = "test", set = "testSet", sendKey = true)
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

    @BeforeEach
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
		complex.byte4Fields = Arrays.asList(1,2,3,4,5);
		complex.byte2Fields = Arrays.asList(new Short[] {1,2,3,4,5,6,7});
		complex.byte1Fields = Arrays.asList(new Byte[] {1,2,3,-1,-2,127,-128,0});
		complex.charFields = Arrays.asList(new Character[] {9,8,7,6,0,255,245,128,127});
        mapper.save(complex);
        
        ComplexClass complex2 = mapper.read(ComplexClass.class, complex.trts);
        for (int i= 0; i < complex.charFields.size(); i++) {
        	assertEquals(complex.charFields.get(i), complex2.charFields.get(i));
        }
        for (int i= 0; i < complex.byte1Fields.size(); i++) {
        	assertEquals(complex.byte1Fields.get(i), complex2.byte1Fields.get(i));
        }
        for (int i= 0; i < complex.byte2Fields.size(); i++) {
        	assertEquals(complex.byte2Fields.get(i), complex2.byte2Fields.get(i));
        }
        for (int i= 0; i < complex.byte4Fields.size(); i++) {
        	assertEquals(complex.byte4Fields.get(i), complex2.byte4Fields.get(i));
        }
        assertEquals(complex.trts, complex2.trts);
        System.out.println("Complex2 loaded");
	}
}
