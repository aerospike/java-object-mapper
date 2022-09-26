package com.aerospike.mapper;

import com.aerospike.mapper.annotations.AerospikeExclude;
import com.aerospike.mapper.annotations.AerospikeGetter;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeSetter;
import com.aerospike.mapper.tools.AeroMapper;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FinalFieldMappingTest extends AeroMapperBaseTest {

    @Test
    void test() {
        AeroMapper mapper = new AeroMapper.Builder(client).build();
        Customer customer = new Customer();
        customer.id = 1;
        customer.name = "tim";
        customer.dob = new Date();

        Doc doc = new Doc();
        doc.id = 1;
        doc.data = "This is some data";

        mapper.save(customer, doc);
        Customer readCustomer = mapper.read(Customer.class, customer.getKey());
        Doc readDoc = mapper.read(Doc.class, doc.getKey());
        assertEquals(doc.toString(), readDoc.toString());

        ReferencingClass ref = new ReferencingClass();
        ref.id = 2;
        ref.cust = readCustomer;
        ref.doc = doc;
        mapper.save(null, ref);
        ReferencingClass readRef = mapper.read(ReferencingClass.class, ref.id);
        assertEquals(ref.toString(), readRef.toString());
    }

    @AerospikeRecord(namespace = "test", set = "common")
    public static class Customer {
        @AerospikeExclude
        public static final String PREFIX = "CUST:";
        @AerospikeExclude
        private int id;
        private String name;
        private Date dob;

        @AerospikeKey
        @AerospikeGetter(name = "key")
        public String getKey() {
            return PREFIX + this.id;
        }

        @AerospikeKey(setter = true)
        @AerospikeSetter(name = "key")
        public void setKey(String key) {
            this.id = Integer.parseInt(key.substring(PREFIX.length()));
        }

        @Override
        public String toString() {
            return String.format("{id=%d,name='%s',dob=%s}", id, name, dob);
        }
    }

    @AerospikeRecord(namespace = "test", set = "common")
    public static class Doc {
        private static final String PREFIX = "DOC:";
        @AerospikeExclude
        public int id;
        private String data;

        @AerospikeKey
        @AerospikeGetter(name = "key")
        public String getKey() {
            return PREFIX + this.id;
        }

        @AerospikeKey(setter = true)
        @AerospikeSetter(name = "key")
        public void setKey(String key) {
            this.id = Integer.parseInt(key.substring(PREFIX.length()));
        }

        @Override
        public String toString() {
            return String.format("{id=%d,data='%s'}", id, data);
        }
    }

    @AerospikeRecord(namespace = "test", set = "ref")
    public static class ReferencingClass {
        @AerospikeKey
        public int id;
        public Customer cust;
        public Doc doc;

        @Override
        public String toString() {
            return String.format("{id=%d,cust=%s,doc=%s}", id, cust, doc);
        }
    }
}
