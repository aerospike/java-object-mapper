package com.aerospike.mapper;

import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.tools.PolicyCache;
import com.aerospike.mapper.tools.PolicyCache.PolicyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PolicyCacheTest {

    private PolicyCache cache;

    @BeforeEach
    public void setUp() {
        cache = PolicyCache.getInstance();
        cache.clear();
    }

    // ── Default policy fallback ──────────────────────────────────────

    @Test
    public void noPolicy_returnsNull() {
        assertDoesNotThrow(() -> cache.determinePolicy(getClass(), PolicyType.READ));
    }

    @Test
    public void defaultPolicy_returnedForUnknownClass() {
        WritePolicy defaultWrite = new WritePolicy();
        defaultWrite.totalTimeout = 9999;
        cache.setDefaultPolicy(PolicyType.WRITE, defaultWrite);

        Policy result = cache.determinePolicy(PolicyCacheTest.class, PolicyType.WRITE);

        assertSame(defaultWrite, result);
    }

    // ── Specific policy precedence ───────────────────────────────────

    @Test
    public void specificPolicy_overridesDefault() {
        Policy defaultRead = new Policy();
        Policy specificRead = new Policy();
        specificRead.totalTimeout = 1234;
        cache.setDefaultPolicy(PolicyType.READ, defaultRead);
        cache.setSpecificPolicy(PolicyType.READ, PolicyCacheTest.class, specificRead);

        Policy result = cache.determinePolicy(PolicyCacheTest.class, PolicyType.READ);

        assertSame(specificRead, result);
    }

    // ── Children policy hierarchy ────────────────────────────────────

    static class ParentEntity {

    }

    static class ChildEntity extends ParentEntity {

    }

    static class GrandchildEntity extends ChildEntity {

    }

    @Test
    public void childrenPolicy_matchesSuperclass() {
        Policy parentPolicy = new Policy();
        parentPolicy.totalTimeout = 555;
        cache.setChildrenPolicy(PolicyType.READ, ParentEntity.class, parentPolicy);

        Policy result = cache.determinePolicy(ChildEntity.class, PolicyType.READ);

        assertSame(parentPolicy, result);
    }

    @Test
    public void childrenPolicy_matchesGrandchild() {
        Policy parentPolicy = new Policy();
        cache.setChildrenPolicy(PolicyType.READ, ParentEntity.class, parentPolicy);

        Policy result = cache.determinePolicy(GrandchildEntity.class, PolicyType.READ);

        assertSame(parentPolicy, result);
    }

    @Test
    public void specificPolicy_overridesChildren() {
        Policy childrenPolicy = new Policy();
        Policy specificPolicy = new Policy();
        specificPolicy.totalTimeout = 777;
        cache.setChildrenPolicy(PolicyType.READ, ParentEntity.class, childrenPolicy);
        cache.setSpecificPolicy(PolicyType.READ, ChildEntity.class, specificPolicy);

        Policy result = cache.determinePolicy(ChildEntity.class, PolicyType.READ);

        assertSame(specificPolicy, result);
    }

    // ── Effective policy methods ─────────────────────────────────────

    @Test
    public void effectiveWritePolicy_returnsNewInstance() {
        WritePolicy clientDefault = new WritePolicy();
        WritePolicy result = cache.getEffectiveWritePolicy(PolicyCacheTest.class, clientDefault);
        assertNotNull(result);
        assertNotSame(clientDefault, result, "Must return a defensive copy");
    }

    @Test
    public void effectiveReadPolicy_fallsBackToDefault() {
        Policy clientDefault = new Policy();
        clientDefault.totalTimeout = 3000;
        Policy result = cache.getEffectiveReadPolicy(String.class, clientDefault);
        assertNotNull(result);
    }

    @Test
    public void effectiveBatchPolicy_fallsBackToDefault() {
        BatchPolicy clientDefault = new BatchPolicy();
        BatchPolicy result = cache.getEffectiveBatchPolicy(String.class, clientDefault);
        assertSame(clientDefault, result);
    }

    @Test
    public void effectiveScanPolicy_fallsBackToDefault() {
        ScanPolicy clientDefault = new ScanPolicy();
        ScanPolicy result = cache.getEffectiveScanPolicy(String.class, clientDefault);
        assertSame(clientDefault, result);
    }

    @Test
    public void effectiveQueryPolicy_fallsBackToDefault() {
        QueryPolicy clientDefault = new QueryPolicy();
        QueryPolicy result = cache.getEffectiveQueryPolicy(String.class, clientDefault);
        assertSame(clientDefault, result);
    }

    // ── Null class handling ──────────────────────────────────────────

    @Test
    public void determinePolicy_nullClass_returnsDefault() {
        WritePolicy defaultWrite = new WritePolicy();
        cache.setDefaultPolicy(PolicyType.WRITE, defaultWrite);

        Policy result = cache.determinePolicy(null, PolicyType.WRITE);

        assertSame(defaultWrite, result);
    }
}
