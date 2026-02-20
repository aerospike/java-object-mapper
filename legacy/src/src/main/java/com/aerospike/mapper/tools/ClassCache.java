package com.aerospike.mapper.tools;

import com.aerospike.client.AerospikeException;
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
    private final Map<String, ClassCacheEntry<?>> storedNameToCacheEntry = new HashMap<>();
    private final Object lock = new Object();

    private ClassCache() {
    }

    public static ClassCache getInstance() {
        return instance;
    }

    public <T> ClassCacheEntry<T> loadClass(@NotNull Class<T> clazz, IObjectMapper mapper) {
        return loadClass(clazz, mapper, true);
    }

    @SuppressWarnings("unchecked")
    public <T> ClassCacheEntry<T> loadClass(@NotNull Class<T> clazz, IObjectMapper mapper, boolean requireRecord) {
        // Clazz can be null if an interface is passed
        if (clazz == null || clazz.isPrimitive() || clazz.equals(Object.class) || clazz.equals(String.class)
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
                        // This is to cater for classes which recursively refer to themselves, such as
                        // 	public static class A {
                        //      @AerospikeKey
                        //      public int id;
                        //      public A a;
                        //  }
                        entry = new ClassCacheEntry<>(clazz, mapper, getClassConfig(clazz), requireRecord);
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

    public boolean hasClass(Class<?> clazz) {
        return cacheMap.containsKey(clazz);
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

    public enum PolicyType {
        READ,
        WRITE,
        BATCH,
        SCAN,
        QUERY
    }
}
