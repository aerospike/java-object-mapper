package com.aerospike.mapper.tools;

import com.aerospike.mapper.exceptions.AerospikeMapperException;
import com.aerospike.mapper.exceptions.NotAnnotatedClass;
import com.aerospike.mapper.tools.configuration.ClassConfig;
import com.aerospike.mapper.tools.configuration.Configuration;
import com.aerospike.mapper.tools.utils.TypeUtils;
import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.util.concurrent.ConcurrentHashMap;

public class ClassCache {

    @Getter
    private static final ClassCache instance = new ClassCache();
    private final ConcurrentHashMap<Class<?>, ClassCacheEntry<?>> cacheMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ClassConfig> classesConfig = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ClassCacheEntry<?>> storedNameToCacheEntry = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    private ClassCache() {
    }

    public <T> ClassCacheEntry<T> loadClass(@NotNull Class<T> clazz, IObjectMapper mapper) {
        return loadClass(clazz, mapper, true);
    }

    @SuppressWarnings("unchecked")
    public <T> ClassCacheEntry<T> loadClass(@NotNull Class<T> clazz, IObjectMapper mapper, boolean requireRecord) {
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

    void setStoredName(@NotNull ClassCacheEntry<?> entry, @NotNull String name) {
        ClassCacheEntry<?> existingEntry = storedNameToCacheEntry.get(name);
        if (existingEntry != null && !(existingEntry.equals(entry))) {
            String errorMessage = String.format("Stored name of \"%s\" is used for both %s and %s",
                    name, existingEntry.getUnderlyingClass().getName(), entry.getUnderlyingClass().getName());
            throw new AerospikeMapperException(errorMessage);
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

    public void clear() {
        synchronized (lock) {
            this.cacheMap.clear();
            this.classesConfig.clear();
            TypeUtils.clear();
            this.storedNameToCacheEntry.clear();
        }
    }

    public void addConfiguration(@NotNull Configuration configuration) {
        for (ClassConfig thisConfig : configuration.getClasses()) {
            classesConfig.put(thisConfig.getClassName(), thisConfig);
        }
    }

    @SuppressWarnings("unused")
    public ClassConfig getClassConfig(String className) {
        return classesConfig.get(className);
    }

    public ClassConfig getClassConfig(Class<?> clazz) {
        return classesConfig.get(clazz.getName());
    }

    @SuppressWarnings("unused")
    public boolean hasClassConfig(String className) {
        return classesConfig.containsKey(className);
    }

    public boolean hasClassConfig(Class<?> clazz) {
        return classesConfig.containsKey(clazz.getName());
    }
}
