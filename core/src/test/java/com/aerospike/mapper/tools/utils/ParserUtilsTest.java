package com.aerospike.mapper.tools.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ParserUtils} placeholder resolution.
 * Tests ${prop} system-property syntax and #{ENV} environment-variable syntax.
 */
public class ParserUtilsTest {

    private static final String TEST_PROP = "aerospike.test.namespace";

    @AfterEach
    public void cleanup() {
        System.clearProperty(TEST_PROP);
    }

    // ── Plain string passthrough ──────────────────────────────────────────────

    @Test
    public void plainString_returnedAsIs() {
        assertEquals("test", ParserUtils.getInstance().get("test"));
    }

    @Test
    public void longPlainString_returnedAsIs() {
        String value = "some-namespace";
        assertEquals(value, ParserUtils.getInstance().get(value));
    }

    // ── Short strings bypass (≤3 chars) ──────────────────────────────────────

    @Test
    public void nullInput_returnsNull() {
        assertNull(ParserUtils.getInstance().get(null));
    }

    @Test
    public void emptyString_returnedAsIs() {
        assertEquals("", ParserUtils.getInstance().get(""));
    }

    @Test
    public void threeCharString_returnedAsIs() {
        assertEquals("abc", ParserUtils.getInstance().get("abc"));
    }

    // ── System property resolution (${...}) ───────────────────────────────────

    @Test
    public void systemProperty_resolved() {
        System.setProperty(TEST_PROP, "mynamespace");
        String result = ParserUtils.getInstance().get("${" + TEST_PROP + "}");
        assertEquals("mynamespace", result);
    }

    @Test
    public void systemProperty_missing_noDefault_returnsNull() {
        // Ensure property is not set
        System.clearProperty(TEST_PROP);
        assertNull(ParserUtils.getInstance().get("${" + TEST_PROP + "}"));
    }

    @Test
    public void systemProperty_missing_withDefault_returnsDefault() {
        System.clearProperty(TEST_PROP);
        String result = ParserUtils.getInstance().get("${" + TEST_PROP + ":defaultNs}");
        assertEquals("defaultNs", result);
    }

    @Test
    public void systemProperty_present_defaultIgnored() {
        System.setProperty(TEST_PROP, "realValue");
        String result = ParserUtils.getInstance().get("${" + TEST_PROP + ":defaultValue}");
        assertEquals("realValue", result);
    }

    // ── Environment variable resolution (#{...}) ──────────────────────────────

    @Test
    public void envVariable_missing_noDefault_returnsNull() {
        // Use a variable name that is virtually guaranteed to not exist
        assertNull(ParserUtils.getInstance().get("#{__AEROSPIKE_MAPPER_TEST_VAR_THAT_DOES_NOT_EXIST__}"));
    }

    @Test
    public void envVariable_missing_withDefault_returnsDefault() {
        String result = ParserUtils.getInstance().get("#{__AEROSPIKE_MAPPER_ABSENT__:fallback}");
        assertEquals("fallback", result);
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    public void incompleteOpenBrace_returnedAsIs() {
        // Starts with ${ but doesn't end with }
        String value = "${no-closing-brace";
        assertEquals(value, ParserUtils.getInstance().get(value));
    }

    @Test
    public void singletonAlwaysReturnsSameInstance() {
        assertSame(ParserUtils.getInstance(), ParserUtils.getInstance());
    }
}
