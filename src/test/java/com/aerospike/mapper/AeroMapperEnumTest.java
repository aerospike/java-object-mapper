package com.aerospike.mapper;

import com.aerospike.mapper.annotations.AerospikeEnum;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AeroMapperEnumTest extends AeroMapperBaseTest {

    enum Status {
        MARRIED,
        SINGLE,
        COMPLICATED
    }

    enum Country {
        ARGENTINA("AR"),
        CHINA("CN"),
        DENMARK("DK"),
        UNITED_STATES("US");

        private final String countryCode;

        Country(String countryCode) {
            this.countryCode = countryCode;
        }

        String getCountryCode() {
            return this.countryCode;
        }
    }

    @AerospikeRecord(namespace = "test", set = "A")
    public static class A {
        @AerospikeKey
        public int id;
        public String name;
        public int age;
        public Status status;
        public Country country;
        @AerospikeEnum(enumField ="countryCode")
        public Country countryAnno;


        public A() {}

        public A(int id, String name, int age, Status status, Country country, Country countryAnno) {
            super();
            this.id = id;
            this.name = name;
            this.age = age;
            this.status = status;
            this.country = country;
            this.countryAnno = countryAnno;
        }
    }

    @Test
    public void runTest() {
        A a1 = new A(1, "a", 10, Status.MARRIED, Country.ARGENTINA, Country.ARGENTINA);
        A a2 = new A(2, "b", 20, Status.SINGLE, Country.DENMARK, Country.DENMARK);
        A a3 = new A(3, "c", 30, Status.COMPLICATED, Country.UNITED_STATES, Country.UNITED_STATES);

        AeroMapper mapper = new AeroMapper.Builder(client).build();
        mapper.save(a1);
        mapper.save(a2);
        mapper.save(a3);

        A a11 = mapper.read(A.class, 1);
        A a12 = mapper.read(A.class, 2);
        A a13 = mapper.read(A.class, 3);

        assertEquals(a1.id, a11.id);
        assertEquals(a1.status, a11.status);
        assertEquals(a1.country, a11.country);
        assertEquals(a2.name, a12.name);
        assertEquals(a2.country, a12.country);
        assertEquals(a2.countryAnno, a12.countryAnno);
        assertEquals(a3.age, a13.age);
        assertEquals(a3.countryAnno, a13.countryAnno);
    }
}
