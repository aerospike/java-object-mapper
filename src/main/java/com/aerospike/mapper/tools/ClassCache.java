package com.aerospike.mapper.tools;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.reactor.IAerospikeReactorClient;
import com.aerospike.mapper.exceptions.NotAnnotatedClass;
import com.aerospike.mapper.tools.configuration.ClassConfig;
import com.aerospike.mapper.tools.configuration.Configuration;
import com.aerospike.mapper.tools.utils.TypeUtils;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class ClassCache {

    private static final ClassCache instance = new ClassCache();
    private final Map<Class<?>, ClassCacheEntry<?>> cacheMap = new HashMap<>();
    private final Map<String, ClassConfig> classesConfig = new HashMap<>();
    private final Map<PolicyType, Policy> defaultPolicies = new HashMap<>();
    private final Map<String, ClassCacheEntry<?>> storedNameToCacheEntry = new HashMap<>();
    private final Map<PolicyType, Map<Class<?>, Policy>> childrenPolicies = new HashMap<>();
    private final Map<PolicyType, Map<Class<?>, Policy>> specificPolicies = new HashMap<>();
    private final Object lock = new Object();

    private ClassCache() {
        for (PolicyType thisType : PolicyType.values()) {
            this.childrenPolicies.put(thisType, new HashMap<>());
            this.specificPolicies.put(thisType, new HashMap<>());
        }
    }

    public static ClassCache getInstance() {
        return instance;
    }

    public <T> ClassCacheEntry<T> loadClass(@NotNull Class<T> clazz, IBaseAeroMapper mapper) {
        return loadClass(clazz, mapper, true);
    }

    @SuppressWarnings("unchecked")
    public <T> ClassCacheEntry<T> loadClass(@NotNull Class<T> clazz, IBaseAeroMapper mapper, boolean requireRecord) {
        if (clazz.isPrimitive() || clazz.equals(Object.class) || clazz.equals(String.class)
                || clazz.equals(Character.class) || Number.class.isAssignableFrom(clazz)) {
            return null;
        }

        ClassCacheEntry<T> entry = (ClassCacheEntry<T>) cacheMap.get(clazz);
        if (entry == null || entry.isNotConstructed()) {
            synchronized (lock) {
                entry = (ClassCacheEntry<T>) cacheMap.get(clazz);
                if (entry == null) {
                    try {
                        // Construct a class cache entry. This must be done in 2 steps, one creating the entry
                        // and the other finalizing construction of it.
                        // This is to cater for classes  which recursively refer to themselves, such as
                        // 	public static class A {
                        //      @AerospikeKey
                        //      public int id;
                        //      public A a;
                        //  }
                        entry = new ClassCacheEntry<>(clazz, mapper, getClassConfig(clazz), requireRecord,
                                determinePolicy(clazz, PolicyType.READ),
                                (WritePolicy) determinePolicy(clazz, PolicyType.WRITE),
                                (BatchPolicy) determinePolicy(clazz, PolicyType.BATCH),
                                (QueryPolicy) determinePolicy(clazz, PolicyType.QUERY),
                                (ScanPolicy) determinePolicy(clazz, PolicyType.SCAN));

                    } catch (NotAnnotatedClass nae) {
                        return null;
                    }
                    cacheMap.put(clazz, entry);
                    try {
                        entry.construct();
                    } catch (IllegalArgumentException iae) {
                        cacheMap.remove(clazz);
                        return null;
                    } catch (Exception e) {
                        cacheMap.remove(clazz);
                        throw e;
                    }
                }
            }
        }
        return entry;
    }

    // package visibility
    void setStoredName(@NotNull ClassCacheEntry<?> entry, @NotNull String name) {
        ClassCacheEntry<?> existingEntry = storedNameToCacheEntry.get(name);
        if (existingEntry != null && !(existingEntry.equals(entry))) {
            String errorMessage = String.format("Stored name of \"%s\" is used for both %s and %s",
                    name, existingEntry.getUnderlyingClass().getName(), entry.getUnderlyingClass().getName());
            throw new AerospikeException(errorMessage);
        } else {
            storedNameToCacheEntry.put(name, entry);
        }
    }

    public ClassCacheEntry<?> getCacheEntryFromStoredName(@NotNull String name) {
        return storedNameToCacheEntry.get(name);
    }

    void setDefaultPolicies(IAerospikeClient client) {
        if (client != null) {
            this.defaultPolicies.put(PolicyType.READ, client.getReadPolicyDefault());
            this.defaultPolicies.put(PolicyType.WRITE, client.getWritePolicyDefault());
            this.defaultPolicies.put(PolicyType.BATCH, client.getBatchPolicyDefault());
            this.defaultPolicies.put(PolicyType.QUERY, client.getQueryPolicyDefault());
            this.defaultPolicies.put(PolicyType.SCAN, client.getScanPolicyDefault());
        }
    }

    void setReactiveDefaultPolicies(IAerospikeReactorClient reactorClient) {
        this.defaultPolicies.put(PolicyType.READ, reactorClient.getReadPolicyDefault());
        this.defaultPolicies.put(PolicyType.WRITE, reactorClient.getWritePolicyDefault());
        this.defaultPolicies.put(PolicyType.BATCH, reactorClient.getBatchPolicyDefault());
        this.defaultPolicies.put(PolicyType.QUERY, reactorClient.getQueryPolicyDefault());
        this.defaultPolicies.put(PolicyType.SCAN, reactorClient.getScanPolicyDefault());
    }

    void setDefaultPolicy(PolicyType policyType, Policy policy) {
        this.defaultPolicies.put(policyType, policy);
    }

    void setChildrenPolicy(PolicyType policyType, Class<?> parentClass, Policy policy) {
        this.childrenPolicies.get(policyType).put(parentClass, policy);
    }

    void setSpecificPolicy(PolicyType policyType, Class<?> parentClass, Policy policy) {
        this.childrenPolicies.get(policyType).put(parentClass, policy);
    }

    public boolean hasClass(Class<?> clazz) {
        return cacheMap.containsKey(clazz);
    }

    private Policy determinePolicy(@NotNull Class<?> clazz, @NotNull PolicyType policyType) {
        // Specific classes have the highest precedence
        Policy result = specificPolicies.get(policyType).get(clazz);
        if (result != null) {
            return result;
        }
        // Otherwise, iterate up class hierarchy looking for the policy.
        Class<?> thisClass = clazz;
        while (thisClass != null) {
            Policy aPolicy = childrenPolicies.get(policyType).get(thisClass);
            if (aPolicy != null) {
                return aPolicy;
            }
            thisClass = thisClass.getSuperclass();
        }
        // To get here, must have nothing specified, use the default
        return this.defaultPolicies.get(policyType);
    }

    /**
     * This method is typically only used for testing
     */
    public void clear() {
        this.cacheMap.clear();
        this.classesConfig.clear();
        TypeUtils.clear();
        this.storedNameToCacheEntry.clear();
    }

    public void addConfiguration(@NotNull Configuration configuration) {
        for (ClassConfig thisConfig : configuration.getClasses()) {
            classesConfig.put(thisConfig.getClassName(), thisConfig);
        }
    }

    public ClassConfig getClassConfig(String className) {
        return classesConfig.get(className);
    }

    public ClassConfig getClassConfig(Class<?> clazz) {
        return classesConfig.get(clazz.getName());
    }

    public boolean hasClassConfig(String className) {
        return classesConfig.containsKey(className);
    }

    public boolean hasClassConfig(Class<?> clazz) {
        return classesConfig.containsKey(clazz.getName());
    }

    enum PolicyType {
        READ,
        WRITE,
        BATCH,
        SCAN,
        QUERY
    }
}
