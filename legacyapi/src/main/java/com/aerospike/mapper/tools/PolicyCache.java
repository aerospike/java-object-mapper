package com.aerospike.mapper.tools;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.reactor.IAerospikeReactorClient;
import lombok.Getter;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores per-class and default Aerospike client policies.
 * Thread-safe singleton for policy resolution across mapper instances.
 */
public class PolicyCache {

    public enum PolicyType {
        READ,
        WRITE,
        BATCH,
        SCAN,
        QUERY
    }

    @Getter
    private static final PolicyCache instance = new PolicyCache();
    private final Map<PolicyType, Policy> defaultPolicies = new ConcurrentHashMap<>();
    private final EnumMap<PolicyType, ConcurrentHashMap<Class<?>, Policy>> childrenPolicies =
        new EnumMap<>(PolicyType.class);
    private final EnumMap<PolicyType, ConcurrentHashMap<Class<?>, Policy>> specificPolicies =
        new EnumMap<>(PolicyType.class);

    private PolicyCache() {
        for (PolicyType thisType : PolicyType.values()) {
            this.childrenPolicies.put(thisType, new ConcurrentHashMap<>());
            this.specificPolicies.put(thisType, new ConcurrentHashMap<>());
        }
    }

    public void clear() {
        defaultPolicies.clear();
        for (PolicyType type : PolicyType.values()) {
            childrenPolicies.get(type).clear();
            specificPolicies.get(type).clear();
        }
    }

    public void setDefaultPolicies(IAerospikeClient client) {
        if (client != null) {
            setDefault(client.getReadPolicyDefault(), client.getWritePolicyDefault(), client.getBatchPolicyDefault(),
                    client.getQueryPolicyDefault(), client.getScanPolicyDefault());
        }
    }

    private void setDefault(Policy readPolicyDefault, WritePolicy writePolicyDefault, BatchPolicy batchPolicyDefault,
                            QueryPolicy queryPolicyDefault, ScanPolicy scanPolicyDefault) {
        this.defaultPolicies.put(PolicyType.READ, readPolicyDefault);
        this.defaultPolicies.put(PolicyType.WRITE, writePolicyDefault);
        this.defaultPolicies.put(PolicyType.BATCH, batchPolicyDefault);
        this.defaultPolicies.put(PolicyType.QUERY, queryPolicyDefault);
        this.defaultPolicies.put(PolicyType.SCAN, scanPolicyDefault);
    }

    public void setReactiveDefaultPolicies(IAerospikeReactorClient reactorClient) {
        setDefault(reactorClient.getReadPolicyDefault(), reactorClient.getWritePolicyDefault(),
                reactorClient.getBatchPolicyDefault(), reactorClient.getQueryPolicyDefault(),
                reactorClient.getScanPolicyDefault());
    }

    public void setDefaultPolicy(PolicyType policyType, Policy policy) {
        this.defaultPolicies.put(policyType, policy);
    }

    public void setChildrenPolicy(PolicyType policyType, Class<?> parentClass, Policy policy) {
        this.childrenPolicies.get(policyType).put(parentClass, policy);
    }

    public void setSpecificPolicy(PolicyType policyType, Class<?> clazz, Policy policy) {
        this.specificPolicies.get(policyType).put(clazz, policy);
    }

    public Policy determinePolicy(Class<?> clazz, PolicyType policyType) {
        if (clazz != null) {
            Policy result = specificPolicies.get(policyType).get(clazz);
            if (result != null) {
                return result;
            }
            Class<?> thisClass = clazz;
            while (thisClass != null) {
                Policy aPolicy = childrenPolicies.get(policyType).get(thisClass);
                if (aPolicy != null) {
                    return aPolicy;
                }
                thisClass = thisClass.getSuperclass();
            }
        }
        return this.defaultPolicies.get(policyType);
    }

    /** Returns the effective write policy for the given class, falling back to clientDefault. */
    public WritePolicy getEffectiveWritePolicy(Class<?> clazz, WritePolicy clientDefault) {
        Policy p = determinePolicy(clazz, PolicyType.WRITE);
        return new WritePolicy(p != null ? (WritePolicy) p : clientDefault);
    }

    /** Returns the effective read policy for the given class, falling back to clientDefault. */
    public Policy getEffectiveReadPolicy(Class<?> clazz, Policy clientDefault) {
        Policy p = determinePolicy(clazz, PolicyType.READ);
        return p != null ? p : clientDefault;
    }

    /** Returns the effective batch policy for the given class, falling back to clientDefault. */
    public BatchPolicy getEffectiveBatchPolicy(Class<?> clazz, BatchPolicy clientDefault) {
        Policy p = determinePolicy(clazz, PolicyType.BATCH);
        return p != null ? (BatchPolicy) p : clientDefault;
    }

    /** Returns the effective scan policy for the given class, falling back to clientDefault. */
    public ScanPolicy getEffectiveScanPolicy(Class<?> clazz, ScanPolicy clientDefault) {
        Policy p = determinePolicy(clazz, PolicyType.SCAN);
        return p != null ? (ScanPolicy) p : clientDefault;
    }

    /** Returns the effective query policy for the given class, falling back to clientDefault. */
    public QueryPolicy getEffectiveQueryPolicy(Class<?> clazz, QueryPolicy clientDefault) {
        Policy p = determinePolicy(clazz, PolicyType.QUERY);
        return p != null ? (QueryPolicy) p : clientDefault;
    }
}
