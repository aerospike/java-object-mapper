package com.aerospike.mapper.reactive;

import com.aerospike.client.Operation;
import com.aerospike.client.cdt.ListOperation;
import com.aerospike.client.cdt.ListReturnType;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReactiveBatchLoadTest extends ReactiveAeroMapperBaseTest {

    private static final String DATA_BIN = "data";
    private final B[] bees = new B[100];
    private final A[] as = new A[10];

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

    @AfterAll
    public void clear() {
        client.truncate(null, "test", "batchA", null);
        client.truncate(null, "test", "batchB", null);
    }

    @SneakyThrows
    private ReactiveAeroMapper populate() {
        client.truncate(null, "test", "batchA", null);
        client.truncate(null, "test", "batchB", null);
        Thread.sleep(1000);

        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();

        reactiveMapper.save((Object[]) bees).subscribeOn(Schedulers.single()).collectList().block();
        reactiveMapper.save((Object[]) as).subscribeOn(Schedulers.single()).collectList().block();

        return reactiveMapper;
    }

    @Test
    void testBatchLoad() {
        ReactiveAeroMapper reactiveMapper = populate();

        B resultB = reactiveMapper.read(B.class, bees[1].id).subscribeOn(Schedulers.single()).block();
        compare(bees[1], resultB);

        A resultA = reactiveMapper.read(A.class, as[1].id).subscribeOn(Schedulers.single()).block();
        compare(as[1], resultA);

        Integer[] ids = new Integer[6];
        ids[0] = as[4].id;
        ids[1] = as[7].id;
        ids[2] = as[5].id;
        ids[3] = as[0].id;
        ids[4] = as[1].id;
        ids[5] = 3000;

        List<A> expected = Stream.of(as[4], as[7], as[5], as[0], as[1])
                .sorted(Comparator.nullsFirst(A::compareTo))
                .collect(Collectors.toList());

        List<A> resultsList = reactiveMapper.read(A.class, ids)
                .subscribeOn(Schedulers.single()).collectList().block();

        assertNotNull(resultsList);
        resultsList.sort(Comparator.nullsFirst(A::compareTo));
        compare(expected, resultsList);
    }

    @Test
    void testBatchLoadWithOperations() {
        ReactiveAeroMapper reactiveMapper = populate();

        B resultB = reactiveMapper.read(B.class, bees[1].id).subscribeOn(Schedulers.single()).block();
        compare(bees[1], resultB);

        A resultA = reactiveMapper.read(A.class, as[1].id).subscribeOn(Schedulers.single()).block();
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

        List<List<B>> expected = Stream.of(as[4], as[7], as[5], as[0], as[1])
                .map(a -> a.data)
                .sorted(Comparator.comparing((List<B> o) -> o.get(1)))
                .collect(Collectors.toList());

        List<List<B>> resultsList = reactiveMapper.read(A.class, userKeys, ops)
                .subscribeOn(Schedulers.parallel()).collectList().block()
                .stream().map(a -> a.data)
                .sorted(Comparator.comparing((List<B> o) -> o.get(1)))
                .collect(Collectors.toList());

        assertNotNull(resultsList);
        for (int i = 0; i < expected.size(); i++) {
            compare(resultsList.get(i).get(0).id, 10);
            compare(expected.get(i).get(expected.get(i).size() - 1), resultsList.get(i).get(1));
        }
    }

    @AerospikeRecord(namespace = "test", set = "batchB")
    public static class B implements Comparable<B> {
        @AerospikeKey
        public int id;
        public String name;

        public B(@ParamFrom("id") int id, @ParamFrom("name") String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public int compareTo(B o) {
            if (o == null) return 1;
            return Integer.compare(id, o.id);
        }
    }

    @AerospikeRecord(namespace = "test", set = "batchA")
    public static class A implements Comparable<A> {
        @AerospikeKey
        public int id;
        public String name;
        public List<B> data;

        public A(int id, String name) {
            this.id = id;
            this.name = name;
            data = new ArrayList<>();
        }

        public A() {
        }

        public void setBList(List<B> bees) {
            data = bees;
        }

        @Override
        public int compareTo(A o) {
            if (o == null) return 1;
            return Integer.compare(id, o.id);
        }
    }
}
