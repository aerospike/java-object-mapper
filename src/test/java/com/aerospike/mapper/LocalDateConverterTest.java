package com.aerospike.mapper;

import com.aerospike.client.Key;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocalDateConverterTest extends AeroMapperBaseTest {

    @Test
    public void testLocalDateTypes() {
        AeroMapper mapper = new AeroMapper.Builder(client).build();

        LocalDateTimeContainer container = new LocalDateTimeContainer();
        container.id = 1;
        container.localDate = LocalDate.now();
        container.localDateTime = LocalDateTime.now();
        container.localTime = LocalTime.now();

        mapper.save(container);

        LocalDateTimeContainer readContainer = mapper.read(LocalDateTimeContainer.class, 1);
        assertEquals(container.id, readContainer.id);
        assertEquals(container.localDate, readContainer.localDate);
        assertEquals(container.localTime, readContainer.localTime);
        assertEquals(container.localDateTime, readContainer.localDateTime);

        com.aerospike.client.Record record = client.get(null, new Key("test", "local", 1));
        long value = record.getLong("localDate");
        long timeValue = record.getLong("localTime");
        @SuppressWarnings("unchecked")
        List<Long> dateTimeValues = (List<Long>) record.getList("localDateTime");
        assertTrue(value > 0);
        assertTrue(timeValue > 0);
        assertTrue(dateTimeValues.get(0) > 0);
    }

    @Test
    public void testEmbeddedLocalDateTypes() {
        AeroMapper mapper = new AeroMapper.Builder(client).build();

        LocalDateTimeContainer container = new LocalDateTimeContainer();
        container.id = 10;
        container.localDate = LocalDate.now();
        container.localDateTime = LocalDateTime.now();

        LocalDateTimeTester tester = new LocalDateTimeTester();
        tester.id = 1;
        tester.cont = container;
        tester.contList = new ArrayList<LocalDateTimeContainer>();
        tester.contList.add(container);
        mapper.save(tester);

        LocalDateTimeTester readTester = mapper.read(LocalDateTimeTester.class, 1);
        assertEquals(tester.id, readTester.id);
        assertEquals(tester.cont.localDate, readTester.cont.localDate);
        assertEquals(tester.cont.localDateTime, readTester.cont.localDateTime);

        assertEquals(tester.id, readTester.id);
        assertEquals(tester.contList.get(0).localDate, readTester.contList.get(0).localDate);
        assertEquals(tester.contList.get(0).localDateTime, readTester.contList.get(0).localDateTime);
    }

    @AerospikeRecord(namespace = "test", set = "local")
    public static class LocalDateTimeContainer {
        @AerospikeKey
        public int id;
        public LocalTime localTime;
        public LocalDate localDate;
        public LocalDateTime localDateTime;
    }

    @AerospikeRecord(namespace = "test", set = "localNest")
    public static class LocalDateTimeTester {
        @AerospikeKey
        public int id = 1;
        @AerospikeEmbed
        public LocalDateTimeContainer cont;
        @AerospikeEmbed
        public List<LocalDateTimeContainer> contList;
    }
}
