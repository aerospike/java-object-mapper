package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;

import lombok.Data;
import lombok.NoArgsConstructor;

public class InterfaceHierarchyTest extends AeroMapperBaseTest {
    @AerospikeRecord(set = "testSet", namespace = "test")
    public interface BaseInterface {
        String getName();
    }

    @AerospikeRecord
    @NoArgsConstructor
    @Data
    public static class SubClass1 implements BaseInterface {
        @AerospikeKey
        private String myName;
        public SubClass1(String myName) {
            this.myName = myName;
        }
        @Override
        public String getName() {
            return myName;
        }
        @Override
        public String toString() {
            return "SubClass1: " + myName;
        }
    }
    @AerospikeRecord
    @NoArgsConstructor
    @Data
    public static class SubClass2 implements BaseInterface {
        @AerospikeKey
        private String myName;
        public SubClass2(String myName) {
            this.myName = myName;
        }
        @Override
        public String getName() {
            return myName;
        }
        @Override
        public String toString() {
            return "SubClass2: " + myName;
        }
    }
    
    @AerospikeRecord(set="container", namespace = "test")
    @Data
    public static class Container {
        @AerospikeKey
        private long id;
        private final List<BaseInterface> children;
        public Container() {
            this.children = new ArrayList<>();
            this.id = 1;
        }
        public Container(BaseInterface firstChild, BaseInterface ... otherChildren) {
            this();
            this.children.add(firstChild);
            this.children.addAll(Arrays.asList(otherChildren));
        }
    }

    @AerospikeRecord(set="container", namespace = "test")
    @Data
    public static class NestedContainer {
        @AerospikeKey
        private long id;
        @AerospikeEmbed
        private final List<BaseInterface> children;
        public NestedContainer() {
            this.children = new ArrayList<>();
            this.id = 2;
        }
        public NestedContainer(BaseInterface firstChild, BaseInterface ... otherChildren) {
            this();
            this.children.add(firstChild);
            this.children.addAll(Arrays.asList(otherChildren));
        }
    }

    @Test
    public void runTest() {
        AeroMapper mapper = new AeroMapper.Builder(client).build();
        Container container = new Container(
                new SubClass1("Bob"),
                new SubClass2("Fred"),
                new SubClass1("Wilma")
        );
        
        mapper.save(container, container.children.get(0), container.children.get(1), container.children.get(2));
        Container readContainer = mapper.read(Container.class, 1);
        assertEquals(container, readContainer);
    }

    @Test
    public void runNestedTest() {
        AeroMapper mapper = new AeroMapper.Builder(client).build();
        NestedContainer container = new NestedContainer(
                new SubClass1("Bob"),
                new SubClass2("Fred"),
                new SubClass1("Wilma")
        );
        
        mapper.save(container);
        NestedContainer readContainer = mapper.read(NestedContainer.class, 2);
        assertEquals(container, readContainer);
    }
}
