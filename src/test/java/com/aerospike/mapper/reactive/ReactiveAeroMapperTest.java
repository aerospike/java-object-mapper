package com.aerospike.mapper.reactive;

import com.aerospike.client.Key;
import com.aerospike.client.query.KeyRecord;
import com.aerospike.mapper.model.Person;
import com.aerospike.mapper.model.PersonDifferentNames;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import org.junit.Before;
import org.junit.Test;
import reactor.core.scheduler.Schedulers;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ReactiveAeroMapperTest extends ReactiveAeroMapperBaseTest {

    private ReactiveAeroMapper reactiveMapper;

    @Before
    public void setup() {
        reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();
        client.truncate(null, NAMESPACE, "people", null);
        client.truncate(null, NAMESPACE, "account", null);
        client.truncate(null, NAMESPACE, "product", null);
    }

    @Test
    public void testSaveRead() {
        Person writePerson = new Person();
        writePerson.setFirstName("John");
        writePerson.setLastName("Doe");
        writePerson.setAge(17);
        writePerson.setBalance(123.10f);
        writePerson.setDateOfBirth(new Date());
        writePerson.setHeight(1.93);
        writePerson.setPhoto("Photo bytes".getBytes());
        writePerson.setSsn("123-456-7890");
        writePerson.setValid(true);

        reactiveMapper.save(writePerson).subscribeOn(Schedulers.parallel()).block();

        Person readPerson = reactiveMapper
                .read(Person.class, writePerson.getSsn())
                .subscribeOn(Schedulers.parallel()).block();

        assertNotNull(readPerson);
        assertEquals(writePerson.getSsn(), readPerson.getSsn());
        assertEquals(writePerson.toString(), readPerson.toString());

        // Check the column names
        KeyRecord keyRecord = reactorClient
                .get(null, new Key(NAMESPACE, "people", writePerson.getSsn()))
                .subscribeOn(Schedulers.parallel()).block();

        assertNotNull(keyRecord);
        assertEquals(keyRecord.record.getString("ssn"), writePerson.getSsn());
        assertEquals(keyRecord.record.getInt("age"), writePerson.getAge());
    }

    @Test
    public void testSaveDifferentNames() {
        PersonDifferentNames writePerson = new PersonDifferentNames();
        writePerson.setFirstName("John");
        writePerson.setLastName("Doe");
        writePerson.setAge(17);
        writePerson.setBalance(123.10f);
        writePerson.setDateOfBirth(new Date());
        writePerson.setHeight(1.93);
        writePerson.setPhoto("Photo bytes".getBytes());
        writePerson.setSsn("123-456-7890");
        writePerson.setValid(true);

        reactiveMapper.save(writePerson).subscribeOn(Schedulers.parallel()).block();

        PersonDifferentNames readPerson = reactiveMapper
                .read(PersonDifferentNames.class, writePerson.getSsn())
                .subscribeOn(Schedulers.parallel()).block();

        assertNotNull(readPerson);
        assertEquals(writePerson.toString(), readPerson.toString());

        // Check the column names
        KeyRecord keyRecord = reactorClient
                .get(null, new Key(NAMESPACE, "people", writePerson.getSsn()))
                .subscribeOn(Schedulers.parallel()).block();

        assertNotNull(keyRecord);
        assertEquals(keyRecord.record.getString("s"), writePerson.getSsn());
        assertEquals(keyRecord.record.getInt("a"), writePerson.getAge());
    }
}
