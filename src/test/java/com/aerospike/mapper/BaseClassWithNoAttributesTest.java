package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aerospike.mapper.annotations.AerospikeBin;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

public class BaseClassWithNoAttributesTest extends AeroMapperBaseTest {

    @Data
    @SuperBuilder
    @NoArgsConstructor
    public static class Animal {
    }

    @Data
    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    @AerospikeRecord(namespace = "test", set = "zoo")
    public static class Dog extends Animal {

        @AerospikeKey
        private String dogId;
        private String breed;

        public Dog() {
            dogId = "" + ((int) (Math.random() * 1000));
        }

        public Dog(String breed) {
            this.breed = breed;
            dogId = "" + ((int) (Math.random() * 1000));
        }

        public String getBreed() {
            return breed;
        }

        public String getDogId() {
            return dogId;
        }
    }

    @Data
    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    @AerospikeRecord(namespace = "test", set = "zoo")
    public static class Cat extends Animal {

        @AerospikeKey
        private String catId;
        private String breed;
        private String lifeSpan;

        public Cat(String breed, String lifeSpan) {
            this.breed = breed;
            this.lifeSpan = lifeSpan;

            catId = "" + ((int) (Math.random() * 1000));
        }

        public Cat() {
            catId = "" + ((int) (Math.random() * 1000));
        }

        public String getBreed() {
            return breed;
        }

        public String getCatId() {
            return catId;
        }

        public String getLifeSpan() {
            return lifeSpan;
        }
    }

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

    @Test
    public void runTest() {
        AeroMapper mapper = new AeroMapper.Builder(client).build();

        Zoo zoo = new Zoo();
        zoo.setZooId("103");

        List<Animal> listOfAnimals = new ArrayList<>();
        listOfAnimals.add(new Cat("Persian", "30"));
        listOfAnimals.add(new Cat("Indian", "21"));
        listOfAnimals.add(new Dog("Lab"));
        listOfAnimals.add(new Cat("Korean", "15"));
        listOfAnimals.add(new Dog("Desi"));

        zoo.setAnimalsList(listOfAnimals);
        mapper.save(zoo);

        Zoo readZoo = mapper.read(Zoo.class, "103");
        assertEquals(zoo.zooId, readZoo.zooId);
        assertEquals(zoo, readZoo);
    }
}
