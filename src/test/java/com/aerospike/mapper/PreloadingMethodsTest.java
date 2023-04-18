package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.model.preload.Animal;
import com.aerospike.mapper.model.preload.Cat;
import com.aerospike.mapper.model.preload.Dog;
import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.mapper.tools.ClassCache;

import lombok.Data;
import lombok.NoArgsConstructor;

public class PreloadingMethodsTest extends AeroMapperBaseTest {

    @AerospikeRecord(namespace = "test", set = "zoo", mapAll = false)
    @NoArgsConstructor
    @Data
    public static class Zoo {
        @AerospikeBin
        @AerospikeKey
        private String zooId;

        @AerospikeBin
        @AerospikeEmbed(type = EmbedType.LIST, elementType = EmbedType.MAP)
        private List<Animal> animalsList;

        public Zoo(String zooId, List<Animal> animalsList) {
            super();
            this.zooId = zooId;
            this.animalsList = animalsList;
        }
    }

    private Zoo createAnimalList() {
        Zoo zoo = new Zoo();
        zoo.setZooId("103");

        List<Animal> listOfAnimals = new ArrayList<>();
        listOfAnimals.add(new Cat("1", "Persian", "30"));
        listOfAnimals.add(new Cat("2", "Indian", "21"));
        listOfAnimals.add(new Dog("3", "Lab"));
        listOfAnimals.add(new Cat("4", "Korean", "15"));
        listOfAnimals.add(new Dog("5", "Desi"));

        zoo.setAnimalsList(listOfAnimals);
        return zoo;
    }

    @Test
    public void TestWriteThenRead() {
        IAerospikeClient client = new AerospikeClient("127.0.0.1", 3000);
        AeroMapper mapper = new AeroMapper.Builder(client).build();

        Zoo zoo = createAnimalList();
        mapper.save(zoo);
        Zoo readZoo = mapper.read(Zoo.class, "103");
        assertEquals(zoo, readZoo);
    }

    @Test
    public void TestWriteThenClearThenRead() {
        IAerospikeClient client = new AerospikeClient("127.0.0.1", 3000);
        AeroMapper mapper = new AeroMapper.Builder(client).build();

        Zoo zoo = createAnimalList();
        mapper.save(zoo);
        ClassCache.getInstance().clear();

        try {
            mapper.read(Zoo.class, "103");
            // Animal, Dog, Cat are unknown at this point, and not fully qualified by the
            // type in the database, so we expect failure
            fail("Classes should be unknown at this point");
        } catch (AerospikeException ignored) {
        }
    }

    @Test
    public void TestWriteThenClearThenReadWithPreload() {
        IAerospikeClient client = new AerospikeClient("127.0.0.1", 3000);
        AeroMapper mapper = new AeroMapper.Builder(client).build();

        Zoo zoo = createAnimalList();
        mapper.save(zoo);
        ClassCache.getInstance().clear();

        mapper = new AeroMapper.Builder(client).preLoadClass(Cat.class).preLoadClass(Dog.class).build();

        Zoo readZoo = mapper.read(Zoo.class, "103");
        assertEquals(zoo, readZoo);
    }

    @Test
    public void TestWriteThenClearThenReadWithPreloadClasses() {
        IAerospikeClient client = new AerospikeClient("127.0.0.1", 3000);
        AeroMapper mapper = new AeroMapper.Builder(client).build();

        Zoo zoo = createAnimalList();
        mapper.save(zoo);
        ClassCache.getInstance().clear();

        mapper = new AeroMapper.Builder(client).preLoadClasses(Cat.class, Dog.class).build();

        Zoo readZoo = mapper.read(Zoo.class, "103");
        assertEquals(zoo, readZoo);
    }

    @Test
    public void TestWriteThenClearThenReadWithPreloadClassesFromPackage() {
        IAerospikeClient client = new AerospikeClient("127.0.0.1", 3000);
        AeroMapper mapper = new AeroMapper.Builder(client).build();

        Zoo zoo = createAnimalList();
        mapper.save(zoo);
        ClassCache.getInstance().clear();

        mapper = new AeroMapper.Builder(client).preLoadClassesFromPackage(Cat.class).build();

        Zoo readZoo = mapper.read(Zoo.class, "103");
        assertEquals(zoo, readZoo);
    }
}
