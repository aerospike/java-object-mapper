package com.aerospike.mapper.tools.fluent;

import com.aerospike.client.fluent.RecordMapper;
import com.aerospike.client.fluent.Session;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.ClassCache;
import com.aerospike.mapper.tools.IRecordConverter;
import com.aerospike.mapper.tools.RecordLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link FluentAeroMapper}.
 * Uses a mocked {@link Session} — no Aerospike server needed.
 */
public class FluentAeroMapperTest {

    @AerospikeRecord(namespace = "test", set = "fam_pojo")
    private static class SamplePojo {
        @AerospikeKey
        int id;
        String name;
    }

    private Session mockSession;
    private FluentAeroMapper fluentMapper;

    @BeforeEach
    public void setUp() {
        ClassCache.getInstance().clear();
        mockSession = mock(Session.class);
        fluentMapper = new FluentAeroMapper(mockSession);
    }

    @AfterEach
    public void tearDown() {
        ClassCache.getInstance().clear();
    }

    // ── Construction ──────────────────────────────────────────────────────────

    @Test
    public void getSession_returnsMockedSession() {
        assertSame(mockSession, fluentMapper.getSession());
    }

    @Test
    public void getMappingConverter_returnsNonNull() {
        IRecordConverter converter = fluentMapper.getMappingConverter();
        assertNotNull(converter);
    }

    @Test
    public void getRecordLoader_returnsNonNull() {
        RecordLoader loader = fluentMapper.getRecordLoader();
        assertNotNull(loader);
    }

    // ── getRecordMapper ───────────────────────────────────────────────────────

    @Test
    public void getRecordMapper_returnsFluentRecordMapper() {
        FluentRecordMapper<SamplePojo> recordMapper = fluentMapper.getRecordMapper(SamplePojo.class);
        assertNotNull(recordMapper);
    }

    @Test
    public void getRecordMapper_calledTwice_returnsSameType() {
        FluentRecordMapper<SamplePojo> first = fluentMapper.getRecordMapper(SamplePojo.class);
        FluentRecordMapper<SamplePojo> second = fluentMapper.getRecordMapper(SamplePojo.class);
        assertNotNull(first);
        assertNotNull(second);
        // Both instances wrap the same (cached) ClassCacheEntry
        assertEquals(first.getClass(), second.getClass());
    }

    // ── getMapper (RecordMappingFactory interface) ─────────────────────────────

    @Test
    public void getMapper_returnsRecordMapper() {
        RecordMapper<SamplePojo> rm = fluentMapper.getMapper(SamplePojo.class);
        assertNotNull(rm);
        assertInstanceOf(FluentRecordMapper.class, rm);
    }

    @Test
    public void getMapper_isConsistentWithGetRecordMapper() {
        RecordMapper<SamplePojo> viaInterface = fluentMapper.getMapper(SamplePojo.class);
        FluentRecordMapper<SamplePojo> viaDirect = fluentMapper.getRecordMapper(SamplePojo.class);
        // Both return FluentRecordMapper instances backed by the same ClassCacheEntry
        assertInstanceOf(FluentRecordMapper.class, viaInterface);
        assertNotNull(viaDirect);
    }
}
