package com.aerospike.mapper;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.aerospike.client.exp.Exp;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.tools.AeroMapper;

public class ScanTest extends AeroMapperBaseTest {
    @AerospikeRecord(namespace = "test", set = "testScan")
    public static class Person {
        @AerospikeKey
        private final int id;
        private final String name;
        private final int age;

        public Person(@ParamFrom("id") int id, @ParamFrom("name") String name, @ParamFrom("age") int age) {
            super();
            this.id = id;
            this.name = name;
            this.age = age;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        @Override
        public String toString() {
            return String.format("id:%d, name:%s, age:%d", id, name, age);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Person person = (Person) o;
            return id == person.id && age == person.age && Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, age);
        }
    }

    private final List<Person> data = new ArrayList<Person>() {{
        add(new Person(1, "Tim", 312));
        add(new Person(2, "Bob", 44));
        add(new Person(3, "Sue", 56));
        add(new Person(4, "Rob", 23));
        add(new Person(5, "Jim", 32));
        add(new Person(6, "Bob", 78));
    }};

    private AeroMapper populate() {
        client.truncate(null, "test", "testScan", null);
        AeroMapper mapper = new AeroMapper.Builder(client).build();
        mapper.save(data.toArray());
        return mapper;
    }

    @Test
    public void scanTest() {
        AeroMapper mapper = populate();
        AtomicInteger counter = new AtomicInteger(0);
        mapper.scan(Person.class, (a) -> {
            counter.incrementAndGet();
            return true;
        });
        assertEquals(6, counter.get());
    }

    @Test
    public void scanTestWithFilter() {
        AeroMapper mapper = populate();
        AtomicInteger counter = new AtomicInteger(0);
        ScanPolicy scanPolicy = new ScanPolicy(mapper.getScanPolicy(Person.class));
        scanPolicy.filterExp = Exp.build(Exp.eq(Exp.stringBin("name"), Exp.val("Bob")));
        mapper.scan(scanPolicy, Person.class, (a) -> {
            counter.incrementAndGet();
            return true;
        });
        assertEquals(2, counter.get());
    }

    @Test
    public void scanTestWithAbort() {
        AeroMapper mapper = populate();
        ScanPolicy scanPolicy = new ScanPolicy(mapper.getScanPolicy(Person.class));
        scanPolicy.maxConcurrentNodes = 1;
        AtomicInteger counter = new AtomicInteger(0);
        mapper.scan(scanPolicy, Person.class, (a) -> {
            counter.incrementAndGet();
            return false;
        });
        assertEquals(1, counter.get());
    }

    @Test
    public void scanTestReturnsList() {
        AeroMapper mapper = populate();

        List<Person> result = mapper.scan(Person.class);

        List<Person> expected = data.stream()
                .sorted(comparing(Person::getId))
                .collect(toList());
        assertEquals(6, result.size());
        assertEquals(expected, result.stream().sorted(comparing(Person::getId)).collect(toList()));
    }

    @Test
    public void scanWithScanPolicyTestReturnsList() {
        AeroMapper mapper = populate();
        ScanPolicy scanPolicy = new ScanPolicy();
        scanPolicy.filterExp = Exp.build(Exp.eq(Exp.stringBin("name"), Exp.val("Bob")));

        List<Person> result = mapper.scan(scanPolicy, Person.class);

        List<Person> expected = data.stream()
                .filter(d -> d.name.equals("Bob"))
                .sorted(comparing(Person::getId))
                .collect(toList());
        assertEquals(2, result.size());
        assertEquals(expected, result.stream().sorted(comparing(Person::getId)).collect(toList()));
    }
}
