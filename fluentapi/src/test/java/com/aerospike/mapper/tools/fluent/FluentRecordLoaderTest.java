package com.aerospike.mapper.tools.fluent;

import com.aerospike.client.fluent.Key;
import com.aerospike.client.fluent.Record;
import com.aerospike.client.fluent.RecordStream;
import com.aerospike.client.fluent.Session;
import com.aerospike.client.fluent.query.KeyBasedQueryBuilderInterface;
import com.aerospike.mapper.exceptions.AerospikeMapperException;
import com.aerospike.mapper.tools.RecordKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public class FluentRecordLoaderTest {

    private Session session;
    private FluentRecordLoader loader;

    @BeforeEach
    public void setUp() {
        session = mock(Session.class);
        loader = new FluentRecordLoader(session);
    }

    private void mockSingleRecordQuery(Record record) {
        KeyBasedQueryBuilderInterface qb = mock(KeyBasedQueryBuilderInterface.class);
        RecordStream rs = mock(RecordStream.class);
        when(session.query(any(Key.class))).thenReturn(qb);
        when(qb.executeSync()).thenReturn(rs);
        when(rs.getFirstRecord()).thenReturn(record);
    }

    // ── getRecord ─────────────────────────────────────────────────────

    @Test
    public void getRecord_returnsNull_whenNotFound() {
        mockSingleRecordQuery(null);
        Map<String, Object> result = loader.getRecord("ns", "set", "key1");
        assertNull(result);
    }

    @Test
    public void getRecord_returnsBins_whenFound() {
        Map<String, Object> bins = new HashMap<>();
        bins.put("name", "Alice");
        Record record = new Record(bins, 1, 0);
        mockSingleRecordQuery(record);

        Map<String, Object> result = loader.getRecord("ns", "set", "key1");

        assertNotNull(result);
        assertEquals("Alice", result.get("name"));
        assertNotSame(bins, result, "Should return a defensive copy");
    }

    // ── getRecordByDigest ─────────────────────────────────────────────

    @Test
    public void getRecordByDigest_returnsNull() {
        mockSingleRecordQuery(null);
        Map<String, Object> result = loader.getRecordByDigest("ns", "set", new byte[20]);
        assertNull(result);
    }

    // ── getBatchRecords ───────────────────────────────────────────────

    @Test
    public void getBatchRecords_emptyList_returnsEmpty() {
        List<Map<String, Object>> results = loader.getBatchRecords(new ArrayList<>());
        assertTrue(results.isEmpty());
    }

    @Test
    public void getBatchRecords_exceptionWrapped() {
        KeyBasedQueryBuilderInterface qb = mock(KeyBasedQueryBuilderInterface.class);
        when(session.query(any(List.class))).thenReturn(qb);
        when(qb.executeSync()).thenThrow(new RuntimeException("connection failed"));

        List<RecordKey> keys = List.of(new RecordKey("ns", "set", "k1"));

        AerospikeMapperException ex = assertThrows(
            AerospikeMapperException.class,
            () -> loader.getBatchRecords(keys));
        assertTrue(ex.getMessage().contains("Batch record fetch failed"));
        assertNotNull(ex.getCause());
    }

    @Test
    public void getBatchRecords_mapperExceptionNotWrapped() {
        KeyBasedQueryBuilderInterface qb = mock(KeyBasedQueryBuilderInterface.class);
        when(session.query(any(List.class))).thenReturn(qb);
        AerospikeMapperException original = new AerospikeMapperException("mapper error");
        when(qb.executeSync()).thenThrow(original);

        List<RecordKey> keys = List.of(new RecordKey("ns", "set", "k1"));

        AerospikeMapperException ex = assertThrows(
            AerospikeMapperException.class,
            () -> loader.getBatchRecords(keys));
        assertSame(original, ex, "AerospikeMapperException should not be wrapped");
    }

    // ── computeDigest ─────────────────────────────────────────────────

    @Test
    public void computeDigest_returnsNonNullBytes() {
        byte[] digest = loader.computeDigest("mySet", "myKey");
        assertNotNull(digest);
        assertTrue(digest.length > 0);
    }

    @Test
    public void computeDigest_sameInputSameOutput() {
        byte[] d1 = loader.computeDigest("set", 42);
        byte[] d2 = loader.computeDigest("set", 42);
        assertArrayEquals(d1, d2);
    }
}
