package com.aerospike.mapper;

import com.aerospike.client.Operation;
import com.aerospike.client.cdt.ListOperation;
import com.aerospike.client.cdt.ListReturnType;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.tools.AeroMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BatchLoadTest extends AeroMapperBaseTest {

    private static final String DATA_BIN = "data";
    private final TestData testData = populateTestData();

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

        public A() {
        }
    }

    private static class TestData {
        public B[] bees;
        public A[] as;

        public TestData(B[] bees, A[] as) {
            this.bees = bees;
            this.as = as;
        }
    }

    private TestData populateTestData() {
        B[] bees = new B[100];
        A[] as = new A[10];

        for (int i = 0; i < 100; i++) {
            bees[i] = new B(i, "B-" + i);
        }

        for (int i = 0; i < 10; i++) {
            as[i] = new A(100 + i, "A-" + i);
            as[i].setBList(Arrays.asList(
                    Arrays.copyOfRange(bees, i * 10, (i + 1) * 10)));
        }

        return new TestData(bees, as);
    }

    private AeroMapper populate() {
        client.truncate(null, "test", "A", null);
        client.truncate(null, "test", "B", null);

        AeroMapper mapper = new AeroMapper.Builder(client).build();

        mapper.save((Object[]) testData.bees);
        mapper.save((Object[]) testData.as);

        return mapper;
    }

    @Test
    public void testBatchLoad() {
        AeroMapper mapper = populate();

        B resultB = mapper.read(B.class, testData.bees[1].id);
        compare(testData.bees[1], resultB);

        A resultA = mapper.read(A.class, testData.as[1].id);
        compare(testData.as[1], resultA);

        Integer[] ids = new Integer[6];
        ids[0] = testData.as[4].id;
        ids[1] = testData.as[7].id;
        ids[2] = testData.as[5].id;
        ids[3] = testData.as[0].id;
        ids[4] = testData.as[1].id;
        ids[5] = 3000;

        A[] results = mapper.read(A.class, ids);
        compare(results[0], testData.as[4]);
        compare(results[1], testData.as[7]);
        compare(results[2], testData.as[5]);
        compare(results[3], testData.as[0]);
        compare(results[4], testData.as[1]);
        compare(results[5], null);
    }

    @Test
    public void testBatchLoadWithOperations() {
        AeroMapper mapper = populate();

        B resultB = mapper.read(B.class, testData.bees[1].id);
        compare(testData.bees[1], resultB);

        A resultA = mapper.read(A.class, testData.as[1].id);
        compare(testData.as[1], resultA);

        Integer[] userKeys = new Integer[6];
        userKeys[0] = testData.as[4].id;
        userKeys[1] = testData.as[7].id;
        userKeys[2] = testData.as[5].id;
        userKeys[3] = testData.as[0].id;
        userKeys[4] = testData.as[1].id;
        userKeys[5] = 3000;

        Operation[] ops = new Operation[2];
        ops[0] = ListOperation.size(DATA_BIN);
        ops[1] = ListOperation.getByIndex(DATA_BIN, -1, ListReturnType.VALUE);

        A[] results = mapper.read(A.class, userKeys, ops);
        compare(results[0].data.get(0).id, 10);
        compare(results[0].data.get(1).id, testData.as[4].data.get(testData.as[4].data.size() - 1).id);
        compare(results[1].data.get(0).id, 10);
        compare(results[1].data.get(1).id, testData.as[7].data.get(testData.as[7].data.size() - 1).id);
        compare(results[2].data.get(0).id, 10);
        compare(results[2].data.get(1).id, testData.as[5].data.get(testData.as[5].data.size() - 1).id);
        compare(results[3].data.get(0).id, 10);
        compare(results[3].data.get(1).id, testData.as[0].data.get(testData.as[0].data.size() - 1).id);
        compare(results[4].data.get(0).id, 10);
        compare(results[4].data.get(1).id, testData.as[1].data.get(testData.as[1].data.size() - 1).id);
        compare(results[5], null);
    }
}
