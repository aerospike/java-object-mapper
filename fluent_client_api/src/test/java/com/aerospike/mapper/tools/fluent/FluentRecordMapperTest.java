package com.aerospike.mapper.tools.fluent;

import com.aerospike.client.fluent.Key;
import com.aerospike.client.fluent.Value;
import com.aerospike.mapper.tools.ClassCacheEntry;
import com.aerospike.mapper.tools.converters.MappingConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import org.mockito.ArgumentMatchers;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Unit tests for {@link FluentRecordMapper}.
 * Uses mocked ClassCacheEntry and MappingConverter — no Aerospike server needed.
 */
@SuppressWarnings("unchecked")
public class FluentRecordMapperTest {

    private static class Pojo {
        int id;
        String name;
    }

    private ClassCacheEntry<Pojo> entry;
    private MappingConverter converter;
    private FluentRecordMapper<Pojo> mapper;

    @BeforeEach
    public void setUp() {
        entry = mock(ClassCacheEntry.class);
        converter = mock(MappingConverter.class);
        mapper = new FluentRecordMapper<>(entry, converter);
    }

    // ── id ────────────────────────────────────────────────────────────────────

    @Test
    public void id_delegatesToEntry() {
        Pojo pojo = new Pojo();
        when(entry.getKey(pojo)).thenReturn(42);

        Object result = mapper.id(pojo);

        assertEquals(42, result);
        verify(entry).getKey(pojo);
    }

    // ── toMap ─────────────────────────────────────────────────────────────────

    @Test
    public void toMap_convertsRawValuesToFluentValues() {
        Pojo pojo = new Pojo();
        Map<String, Object> rawMap = new HashMap<>();
        rawMap.put("id", 1);
        rawMap.put("name", "Alice");
        when(entry.getMap(pojo, false)).thenReturn(rawMap);

        Map<String, Value> result = mapper.toMap(pojo);

        assertEquals(rawMap.size(), result.size());
        assertTrue(result.containsKey("id"));
        assertTrue(result.containsKey("name"));
        // Each value must be a proper fluent Value instance
        assertNotNull(result.get("id"));
        assertNotNull(result.get("name"));
    }

    @Test
    public void toMap_emptyObject_returnsEmptyMap() {
        Pojo pojo = new Pojo();
        when(entry.getMap(pojo, false)).thenReturn(new HashMap<>());

        Map<String, Value> result = mapper.toMap(pojo);

        assertTrue(result.isEmpty());
    }

    // ── fromMap ───────────────────────────────────────────────────────────────

    @Test
    public void fromMap_noKeyInjection_whenKeyFieldNameIsNull() {
        // When there is no key field name, the bins map goes to converter as-is
        Map<String, Object> bins = new HashMap<>();
        bins.put("name", "Alice");
        Key key = new Key("test", "s", "k1");
        Pojo expected = new Pojo();

        when(entry.getKeyFieldName()).thenReturn(null);
        when(entry.getUnderlyingClass()).thenReturn((Class) Pojo.class);
        when(converter.convertToObject(Pojo.class, bins)).thenReturn(expected);

        Pojo result = mapper.fromMap(bins, key, 1);

        assertSame(expected, result);
        // The exact same map is passed (no copy was made)
        verify(converter).convertToObject(Pojo.class, bins);
    }

    @Test
    public void fromMap_noKeyInjection_whenKeyStoredAsBin() {
        Map<String, Object> bins = new HashMap<>();
        bins.put("id", 7);
        Key key = new Key("test", "s", "k2");
        Pojo expected = new Pojo();

        when(entry.getKeyFieldName()).thenReturn("id");
        when(entry.isKeyFieldStoredAsBin()).thenReturn(true);
        when(entry.getUnderlyingClass()).thenReturn((Class) Pojo.class);
        when(converter.convertToObject(Pojo.class, bins)).thenReturn(expected);

        Pojo result = mapper.fromMap(bins, key, 0);

        assertSame(expected, result);
        // bins passed unchanged — key already present as a bin
        verify(converter).convertToObject(Pojo.class, bins);
    }

    @Test
    public void fromMap_injectsKeyWhenSendKeyScenario() {
        // sendKey scenario: keyFieldName != null, not stored as bin, key.userKey != null
        Map<String, Object> bins = new HashMap<>();
        bins.put("name", "Bob");
        Key key = new Key("test", "s", "bob-key");   // userKey = StringValue("bob-key")
        Pojo expected = new Pojo();

        when(entry.getKeyFieldName()).thenReturn("id");
        when(entry.isKeyFieldStoredAsBin()).thenReturn(false);
        when(entry.getUnderlyingClass()).thenReturn((Class) Pojo.class);
        when(converter.convertToObject(eq(Pojo.class),
                ArgumentMatchers.<Map<String, Object>>argThat(m ->
                        m.containsKey("id") && "bob-key".equals(m.get("id")))
        )).thenReturn(expected);

        Pojo result = mapper.fromMap(bins, key, 3);

        assertSame(expected, result);
        // Original bins map must not be mutated
        assertFalse(bins.containsKey("id"));
    }

    @Test
    public void fromMap_setsGenerationOnResult() {
        Map<String, Object> bins = new HashMap<>();
        Key key = new Key("test", "s", "gkey");
        Pojo expected = new Pojo();

        when(entry.getKeyFieldName()).thenReturn(null);
        when(entry.getUnderlyingClass()).thenReturn((Class) Pojo.class);
        when(converter.convertToObject(Pojo.class, bins)).thenReturn(expected);

        mapper.fromMap(bins, key, 7);

        verify(entry).setGenerationValue(expected, 7);
    }

    @Test
    public void fromMap_noKeyInjection_whenUserKeyIsNull() {
        // Key created from a digest has no userKey
        Map<String, Object> bins = new HashMap<>();
        Key key = new Key("test", new byte[20], "digestSet", null);
        Pojo expected = new Pojo();

        when(entry.getKeyFieldName()).thenReturn("id");
        when(entry.isKeyFieldStoredAsBin()).thenReturn(false);
        when(entry.getUnderlyingClass()).thenReturn((Class) Pojo.class);
        when(converter.convertToObject(Pojo.class, bins)).thenReturn(expected);

        Pojo result = mapper.fromMap(bins, key, 0);

        assertSame(expected, result);
        // No key injected because userKey is null
        verify(converter).convertToObject(Pojo.class, bins);
    }
}
