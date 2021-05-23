package com.aerospike.mapper.reactive;

import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeReference;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import org.junit.Test;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ReactiveListOfReferencesTest extends ReactiveAeroMapperBaseTest {

    @AerospikeRecord(namespace = "test", set = "item")
    public static class Item {
        @AerospikeKey
        private int id;
        private Date due;
        private String desc;
        public Item(int id, Date due, String desc) {
            super();
            this.id = id;
            this.due = due;
            this.desc = desc;
        }

        public Item() {
        }
    }

    @AerospikeRecord(namespace = "test", set = "container")
    public static class Container {
        @AerospikeKey
        private int id;
        private String name;
        @AerospikeReference(type = AerospikeReference.ReferenceType.ID)
        private final List<Item> items;

        public Container() {
            this.items = new ArrayList<>();
        }
    }

    @Test
    public void testListOfReferences() {
        Container container = new Container();
        container.id = 1;
        container.name = "container";

        container.items.add(new Item(100, new Date(), "Item 1"));
        container.items.add(new Item(200, new Date(), "Item 2"));
        container.items.add(new Item(300, new Date(), "Item 3"));
        container.items.add(new Item(400, new Date(), "Item 4"));

        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();
        reactiveMapper.save(container).subscribeOn(Schedulers.parallel()).block();
        for (int i = 0; i < container.items.size(); i++) {
            reactiveMapper.save(container.items.get(i)).subscribeOn(Schedulers.parallel()).block();
        }

        Container newVersion = reactiveMapper.read(Container.class, 1).subscribeOn(Schedulers.parallel()).block();
        assert newVersion != null;
        assertEquals(container.id, newVersion.id);
        assertEquals(container.name, newVersion.name);
        assertEquals(container.items.size(), newVersion.items.size());
        for (int i = 0; i < container.items.size(); i++) {
            assertEquals(container.items.get(i).desc, newVersion.items.get(i).desc);
            assertEquals(container.items.get(i).id, newVersion.items.get(i).id);
            assertEquals(container.items.get(i).due, newVersion.items.get(i).due);
        }
    }
}
