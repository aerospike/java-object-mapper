package com.aerospike.mapper;

import com.aerospike.client.Operation;
import com.aerospike.client.cdt.ListOperation;
import com.aerospike.client.cdt.ListReturnType;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.tools.AeroMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BatchLoadTest extends AeroMapperBaseTest {

    private static final String DATA_BIN = "data";
    private final B[] bees = new B[100];
    private final A[] as = new A[10];

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

    @BeforeAll
    public void populateStaticData() {
        for (int i = 0; i < 100; i++) {
            bees[i] = new B(i, "B-" + i);
        }

        for (int i = 0; i < 10; i++) {
            as[i] = new A(100 + i, "A-" + i);
            as[i].setBList(Arrays.asList(
                    Arrays.copyOfRange(bees, i * 10, (i + 1) * 10)));
        }
    }

    private AeroMapper populate() {
        client.truncate(null, "test", "a", null);
        client.truncate(null, "test", "b", null);

        AeroMapper mapper = new AeroMapper.Builder(client).build();

        mapper.save((Object[]) bees);
        mapper.save((Object[]) as);

        return mapper;
    }

    @Test
    public void testBatchLoad() {
        AeroMapper mapper = populate();

        B resultB = mapper.read(B.class, bees[1].id);
        compare(bees[1], resultB);

        A resultA = mapper.read(A.class, as[1].id);
        compare(as[1], resultA);

        Integer[] ids = new Integer[6];
        ids[0] = as[4].id;
        ids[1] = as[7].id;
        ids[2] = as[5].id;
        ids[3] = as[0].id;
        ids[4] = as[1].id;
        ids[5] = 3000;

        A[] results = mapper.read(A.class, ids);
        compare(results[0], as[4]);
        compare(results[1], as[7]);
        compare(results[2], as[5]);
        compare(results[3], as[0]);
        compare(results[4], as[1]);
        compare(results[5], null);
    }

    @Test
    public void testBatchLoadWithOperations() {
        AeroMapper mapper = populate();

        B resultB = mapper.read(B.class, bees[1].id);
        compare(bees[1], resultB);

        A resultA = mapper.read(A.class, as[1].id);
        compare(as[1], resultA);

        Integer[] userKeys = new Integer[6];
        userKeys[0] = as[4].id;
        userKeys[1] = as[7].id;
        userKeys[2] = as[5].id;
        userKeys[3] = as[0].id;
        userKeys[4] = as[1].id;
        userKeys[5] = 3000;

        Operation[] ops = new Operation[2];
        ops[0] = ListOperation.size(DATA_BIN);
        ops[1] = ListOperation.getByIndex(DATA_BIN, -1, ListReturnType.VALUE);

        A[] results = mapper.read(A.class, userKeys, ops);
        compare(results[0].data.get(0).id, 10);
        compare(results[0].data.get(1).id, as[4].data.get(as[4].data.size() - 1).id);
        compare(results[1].data.get(0).id, 10);
        compare(results[1].data.get(1).id, as[7].data.get(as[7].data.size() - 1).id);
        compare(results[2].data.get(0).id, 10);
        compare(results[2].data.get(1).id, as[5].data.get(as[5].data.size() - 1).id);
        compare(results[3].data.get(0).id, 10);
        compare(results[3].data.get(1).id, as[0].data.get(as[0].data.size() - 1).id);
        compare(results[4].data.get(0).id, 10);
        compare(results[4].data.get(1).id, as[1].data.get(as[1].data.size() - 1).id);
        compare(results[5], null);
    }
}
