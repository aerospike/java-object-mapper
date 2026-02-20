package com.aerospike.mapper.tools.mappers;

import com.aerospike.mapper.exceptions.AerospikeMapperException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for all individual TypeMapper implementations.
 * No Aerospike server or mocking required - these are pure Java function tests.
 */
public class MapperUnitTests {

    // ── DateMapper ────────────────────────────────────────────────────────────

    @Test
    public void dateMapper_nullRoundTrip() {
        DateMapper m = new DateMapper();
        assertNull(m.toAerospikeFormat(null));
        assertNull(m.fromAerospikeFormat(null));
    }

    @Test
    public void dateMapper_roundTrip() {
        DateMapper m = new DateMapper();
        Date original = new Date(1_700_000_000_000L);
        Long serialized = (Long) m.toAerospikeFormat(original);
        assertEquals(original.getTime(), serialized);
        assertEquals(original, m.fromAerospikeFormat(serialized));
    }

    @Test
    public void dateMapper_epochZero() {
        DateMapper m = new DateMapper();
        Date epoch = new Date(0L);
        assertEquals(new Date(0L), m.fromAerospikeFormat(m.toAerospikeFormat(epoch)));
    }

    // ── InstantMapper ─────────────────────────────────────────────────────────

    @Test
    public void instantMapper_nullRoundTrip() {
        InstantMapper m = new InstantMapper();
        assertNull(m.toAerospikeFormat(null));
        assertNull(m.fromAerospikeFormat(null));
    }

    @Test
    public void instantMapper_roundTrip() {
        InstantMapper m = new InstantMapper();
        Instant original = Instant.parse("2024-06-15T12:34:56.123456789Z");
        Long serialized = (Long) m.toAerospikeFormat(original);
        Instant restored = (Instant) m.fromAerospikeFormat(serialized);
        assertEquals(original, restored);
    }

    @Test
    public void instantMapper_epochZero() {
        InstantMapper m = new InstantMapper();
        Instant epoch = Instant.EPOCH;
        assertEquals(epoch, m.fromAerospikeFormat(m.toAerospikeFormat(epoch)));
    }

    @Test
    public void instantMapper_nanosecondPrecision() {
        InstantMapper m = new InstantMapper();
        // Verify that nanosecond component is preserved exactly
        Instant original = Instant.ofEpochSecond(1_000_000L, 999_999_999L);
        assertEquals(original, m.fromAerospikeFormat(m.toAerospikeFormat(original)));
    }

    // ── LocalDateMapper ───────────────────────────────────────────────────────

    @Test
    public void localDateMapper_nullRoundTrip() {
        LocalDateMapper m = new LocalDateMapper();
        assertNull(m.toAerospikeFormat(null));
        assertNull(m.fromAerospikeFormat(null));
    }

    @Test
    public void localDateMapper_roundTrip() {
        LocalDateMapper m = new LocalDateMapper();
        LocalDate original = LocalDate.of(2024, Month.NOVEMBER, 5);
        Long serialized = (Long) m.toAerospikeFormat(original);
        assertEquals(original, m.fromAerospikeFormat(serialized));
    }

    @Test
    public void localDateMapper_epochDay() {
        LocalDateMapper m = new LocalDateMapper();
        LocalDate epoch = LocalDate.ofEpochDay(0);
        assertEquals(epoch, m.fromAerospikeFormat(m.toAerospikeFormat(epoch)));
    }

    // ── LocalTimeMapper ───────────────────────────────────────────────────────

    @Test
    public void localTimeMapper_nullRoundTrip() {
        LocalTimeMapper m = new LocalTimeMapper();
        assertNull(m.toAerospikeFormat(null));
        assertNull(m.fromAerospikeFormat(null));
    }

    @Test
    public void localTimeMapper_roundTrip() {
        LocalTimeMapper m = new LocalTimeMapper();
        LocalTime original = LocalTime.of(13, 45, 22, 123_456_789);
        Long serialized = (Long) m.toAerospikeFormat(original);
        assertEquals(original, m.fromAerospikeFormat(serialized));
    }

    @Test
    public void localTimeMapper_midnight() {
        LocalTimeMapper m = new LocalTimeMapper();
        LocalTime midnight = LocalTime.MIDNIGHT;
        assertEquals(midnight, m.fromAerospikeFormat(m.toAerospikeFormat(midnight)));
    }

    // ── LocalDateTimeMapper ───────────────────────────────────────────────────

    @Test
    public void localDateTimeMapper_nullRoundTrip() {
        LocalDateTimeMapper m = new LocalDateTimeMapper();
        assertNull(m.toAerospikeFormat(null));
        assertNull(m.fromAerospikeFormat(null));
    }

    @Test
    public void localDateTimeMapper_roundTrip() {
        LocalDateTimeMapper m = new LocalDateTimeMapper();
        LocalDateTime original = LocalDateTime.of(2024, 6, 15, 10, 30, 0, 987_654_321);
        @SuppressWarnings("unchecked")
        List<Long> serialized = (List<Long>) m.toAerospikeFormat(original);
        assertEquals(2, serialized.size());
        assertEquals(original, m.fromAerospikeFormat(serialized));
    }

    @Test
    public void localDateTimeMapper_storesDateAndTimeSeparately() {
        LocalDateTimeMapper m = new LocalDateTimeMapper();
        LocalDateTime dt = LocalDateTime.of(2000, 1, 1, 0, 0, 0, 0);
        @SuppressWarnings("unchecked")
        List<Long> serialized = (List<Long>) m.toAerospikeFormat(dt);
        // First element = epoch days, second = nano of day
        assertEquals(LocalDate.of(2000, 1, 1).toEpochDay(), (long) serialized.get(0));
        assertEquals(LocalTime.MIDNIGHT.toNanoOfDay(), (long) serialized.get(1));
    }

    // ── BigDecimalMapper ──────────────────────────────────────────────────────

    @Test
    public void bigDecimalMapper_nullRoundTrip() {
        BigDecimalMapper m = new BigDecimalMapper();
        assertNull(m.toAerospikeFormat(null));
        assertNull(m.fromAerospikeFormat(null));
    }

    @Test
    public void bigDecimalMapper_roundTrip() {
        BigDecimalMapper m = new BigDecimalMapper();
        BigDecimal original = new BigDecimal("123456789.987654321");
        String serialized = (String) m.toAerospikeFormat(original);
        assertEquals(original, m.fromAerospikeFormat(serialized));
    }

    @Test
    public void bigDecimalMapper_preservesScale() {
        BigDecimalMapper m = new BigDecimalMapper();
        BigDecimal value = new BigDecimal("1.000");
        BigDecimal restored = (BigDecimal) m.fromAerospikeFormat(m.toAerospikeFormat(value));
        assertEquals(0, value.compareTo(restored));
        assertEquals(value.scale(), restored.scale());
    }

    @Test
    public void bigDecimalMapper_negative() {
        BigDecimalMapper m = new BigDecimalMapper();
        BigDecimal value = new BigDecimal("-9876543210.123456789");
        assertEquals(value, m.fromAerospikeFormat(m.toAerospikeFormat(value)));
    }

    // ── BigIntegerMapper ──────────────────────────────────────────────────────

    @Test
    public void bigIntegerMapper_nullRoundTrip() {
        BigIntegerMapper m = new BigIntegerMapper();
        assertNull(m.toAerospikeFormat(null));
        assertNull(m.fromAerospikeFormat(null));
    }

    @Test
    public void bigIntegerMapper_roundTrip() {
        BigIntegerMapper m = new BigIntegerMapper();
        BigInteger original = new BigInteger("99999999999999999999999999999");
        String serialized = (String) m.toAerospikeFormat(original);
        assertEquals(original, m.fromAerospikeFormat(serialized));
    }

    @Test
    public void bigIntegerMapper_negative() {
        BigIntegerMapper m = new BigIntegerMapper();
        BigInteger value = new BigInteger("-12345678901234567890");
        assertEquals(value, m.fromAerospikeFormat(m.toAerospikeFormat(value)));
    }

    // ── CharacterMapper ───────────────────────────────────────────────────────

    @Test
    public void characterMapper_nullRoundTrip() {
        CharacterMapper m = new CharacterMapper();
        assertNull(m.toAerospikeFormat(null));
        assertNull(m.fromAerospikeFormat(null));
    }

    @Test
    public void characterMapper_roundTrip() {
        CharacterMapper m = new CharacterMapper();
        for (char c : new char[]{'A', 'z', '0', ' ', '\t', '\u00e9'}) {
            Long serialized = (Long) m.toAerospikeFormat(c);
            assertEquals(c, (long) serialized);
            assertEquals(c, m.fromAerospikeFormat(serialized));
        }
    }

    @Test
    public void characterMapper_fromLong() {
        CharacterMapper m = new CharacterMapper();
        // fromAerospikeFormat should accept any Number
        assertEquals('A', m.fromAerospikeFormat(65L));
        assertEquals('a', m.fromAerospikeFormat(97L));
    }

    // ── ByteMapper ────────────────────────────────────────────────────────────

    @Test
    public void byteMapper_nullRoundTrip() {
        ByteMapper m = new ByteMapper();
        assertNull(m.toAerospikeFormat(null));
        assertNull(m.fromAerospikeFormat(null));
    }

    @Test
    public void byteMapper_roundTrip() {
        ByteMapper m = new ByteMapper();
        for (byte b : new byte[]{Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE}) {
            Long serialized = (Long) m.toAerospikeFormat(b);
            assertEquals(b, (long) serialized);
            assertEquals(b, m.fromAerospikeFormat(serialized));
        }
    }

    // ── ShortMapper ───────────────────────────────────────────────────────────

    @Test
    public void shortMapper_nullRoundTrip() {
        ShortMapper m = new ShortMapper();
        assertNull(m.toAerospikeFormat(null));
        assertNull(m.fromAerospikeFormat(null));
    }

    @Test
    public void shortMapper_roundTrip() {
        ShortMapper m = new ShortMapper();
        for (short s : new short[]{Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE}) {
            Long serialized = (Long) m.toAerospikeFormat(s);
            assertEquals(s, (long) serialized);
            assertEquals(s, m.fromAerospikeFormat(serialized));
        }
    }

    // ── IntMapper ─────────────────────────────────────────────────────────────

    @Test
    public void intMapper_nullFromAerospike() {
        IntMapper m = new IntMapper();
        assertNull(m.fromAerospikeFormat(null));
    }

    @Test
    public void intMapper_toAerospikePassthrough() {
        IntMapper m = new IntMapper();
        // toAerospikeFormat returns the value as-is
        assertEquals(42, m.toAerospikeFormat(42));
        assertNull(m.toAerospikeFormat(null));
    }

    @Test
    public void intMapper_fromLong() {
        IntMapper m = new IntMapper();
        // Aerospike returns longs; fromAerospikeFormat must convert to int
        assertEquals(100, m.fromAerospikeFormat(100L));
        assertEquals(Integer.MAX_VALUE, m.fromAerospikeFormat((long) Integer.MAX_VALUE));
    }

    // ── LongMapper ────────────────────────────────────────────────────────────

    @Test
    public void longMapper_passthrough() {
        LongMapper m = new LongMapper();
        assertNull(m.toAerospikeFormat(null));
        assertNull(m.fromAerospikeFormat(null));
        assertEquals(Long.MAX_VALUE, m.toAerospikeFormat(Long.MAX_VALUE));
        assertEquals(Long.MIN_VALUE, m.fromAerospikeFormat(Long.MIN_VALUE));
    }

    // ── FloatMapper ───────────────────────────────────────────────────────────

    @Test
    public void floatMapper_nullFromAerospike() {
        FloatMapper m = new FloatMapper();
        assertNull(m.fromAerospikeFormat(null));
    }

    @Test
    public void floatMapper_toAerospikePassthrough() {
        FloatMapper m = new FloatMapper();
        assertEquals(3.14f, m.toAerospikeFormat(3.14f));
        assertNull(m.toAerospikeFormat(null));
    }

    @Test
    public void floatMapper_fromDouble() {
        FloatMapper m = new FloatMapper();
        // Aerospike may return Double; fromAerospikeFormat must return float
        float result = (float) m.fromAerospikeFormat(3.14);
        assertEquals(3.14f, result, 0.001f);
    }

    // ── DoubleMapper ──────────────────────────────────────────────────────────

    @Test
    public void doubleMapper_passthrough() {
        DoubleMapper m = new DoubleMapper();
        assertNull(m.toAerospikeFormat(null));
        assertNull(m.fromAerospikeFormat(null));
        assertEquals(Math.PI, m.toAerospikeFormat(Math.PI));
        assertEquals(Math.E, m.fromAerospikeFormat(Math.E));
    }

    // ── BooleanMapper ─────────────────────────────────────────────────────────

    @Test
    public void booleanMapper_nullRoundTrip() {
        BooleanMapper m = new BooleanMapper();
        assertNull(m.toAerospikeFormat(null));
        assertNull(m.fromAerospikeFormat(null));
    }

    @Test
    public void booleanMapper_toAerospikePassthrough() {
        BooleanMapper m = new BooleanMapper();
        assertEquals(Boolean.TRUE, m.toAerospikeFormat(true));
        assertEquals(Boolean.FALSE, m.toAerospikeFormat(false));
    }

    @Test
    public void booleanMapper_fromNativeBoolean() {
        BooleanMapper m = new BooleanMapper();
        assertTrue((Boolean) m.fromAerospikeFormat(Boolean.TRUE));
        assertFalse((Boolean) m.fromAerospikeFormat(Boolean.FALSE));
    }

    @Test
    public void booleanMapper_backwardCompatibleFromLong() {
        // Older Aerospike servers stored booleans as 0/1 longs
        BooleanMapper m = new BooleanMapper();
        assertFalse((Boolean) m.fromAerospikeFormat(0L));
        assertTrue((Boolean) m.fromAerospikeFormat(1L));
        assertTrue((Boolean) m.fromAerospikeFormat(-1L));
    }

    // ── EnumMapper ────────────────────────────────────────────────────────────

    private enum Color { RED, GREEN, BLUE }

    private enum Status {
        ACTIVE("active"), INACTIVE("inactive");

        private final String code;

        Status(String code) {
            this.code = code;
        }
    }

    @Test
    public void enumMapper_nullRoundTrip() {
        EnumMapper m = new EnumMapper(Color.class, "");
        assertNull(m.toAerospikeFormat(null));
        assertNull(m.fromAerospikeFormat(null));
    }

    @Test
    public void enumMapper_byName_roundTrip() {
        EnumMapper m = new EnumMapper(Color.class, "");
        assertEquals("RED", m.toAerospikeFormat(Color.RED));
        assertEquals("BLUE", m.toAerospikeFormat(Color.BLUE));
        assertEquals(Color.GREEN, m.fromAerospikeFormat("GREEN"));
    }

    @Test
    public void enumMapper_byField_roundTrip() {
        EnumMapper m = new EnumMapper(Status.class, "code");
        assertEquals("active", m.toAerospikeFormat(Status.ACTIVE));
        assertEquals("inactive", m.toAerospikeFormat(Status.INACTIVE));
        assertEquals(Status.ACTIVE, m.fromAerospikeFormat("active"));
        assertEquals(Status.INACTIVE, m.fromAerospikeFormat("inactive"));
    }

    @Test
    public void enumMapper_unknownValue_throwsException() {
        EnumMapper m = new EnumMapper(Color.class, "");
        assertThrows(AerospikeMapperException.class, () -> m.fromAerospikeFormat("PURPLE"));
    }

    @Test
    public void enumMapper_invalidField_throwsOnConstruction() {
        assertThrows(AerospikeMapperException.class,
                () -> new EnumMapper(Color.class, "nonExistentField"));
    }
}
