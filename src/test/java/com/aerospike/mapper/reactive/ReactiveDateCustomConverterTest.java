package com.aerospike.mapper.reactive;

import com.aerospike.client.Key;
import com.aerospike.client.query.KeyRecord;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.FromAerospike;
import com.aerospike.mapper.annotations.ToAerospike;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReactiveDateCustomConverterTest extends ReactiveAeroMapperBaseTest {
    public static class DateConverter {
        private static final ThreadLocal<SimpleDateFormat> dateFormatter = ThreadLocal.withInitial(() -> new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS zzzZ"));

        @ToAerospike
        public String toAerospike(Date date) {
            if (date == null) {
                return null;
            }
            return dateFormatter.get().format(date);
        }

        @FromAerospike
        public Date fromAerospike(String dateStr) throws ParseException {
            if (dateStr == null) {
                return null;
            }
            return dateFormatter.get().parse(dateStr);
        }
    }

    @AerospikeRecord(namespace = "test", set="dateFormat")
    public static class DateContainer {
        @AerospikeKey
        public long key;
        public Date date;
    }

    @Test
    public void testSave() throws ParseException {
        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).addConverter(new DateConverter()).build();

        Date date = new Date();

        DateContainer container = new DateContainer();
        container.key = 1;
        container.date = date;

        reactiveMapper.save(container).subscribeOn(Schedulers.parallel()).block();
        DateContainer container2 = reactiveMapper.read(DateContainer.class, container.key).subscribeOn(Schedulers.parallel()).block();
        compare(container, container2, true);

        KeyRecord keyRecord = reactorClient.get(null, new Key("test", "dateFormat", 1)).subscribeOn(Schedulers.parallel()).block();
        assert keyRecord != null;
        String dateStr = keyRecord.record.getString("date");
        Date date2 = DateConverter.dateFormatter.get().parse(dateStr);
        System.out.println("Expected: " + date + ", received " + date2);
        assertEquals(date, date2);
    }
}
