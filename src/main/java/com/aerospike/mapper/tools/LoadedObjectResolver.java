package com.aerospike.mapper.tools;

import com.aerospike.client.Key;

import java.util.HashMap;
import java.util.Map;

public class LoadedObjectResolver {

    private static final ThreadLocal<LoadedObjectMap> threadLocalObjects = ThreadLocal.withInitial(LoadedObjectMap::new);

    private LoadedObjectResolver() {
    }

    public static void begin() {
        LoadedObjectMap map = threadLocalObjects.get();
        map.referenceCount++;
    }

    public static void end() {
        LoadedObjectMap map = threadLocalObjects.get();
        map.referenceCount--;
        if (map.referenceCount == 0) {
            threadLocalObjects.remove();
        }
    }

    public static void setObjectForCurrentKey(Object object) {
        Key currentKey = ThreadLocalKeySaver.get();
        LoadedObjectMap map = threadLocalObjects.get();
        if (currentKey != null) {
            map.objectMap.put(currentKey, object);
        }
    }

    public static Object get(Key key) {
        LoadedObjectMap map = threadLocalObjects.get();
        return map.objectMap.get(key);
    }

    private static class LoadedObjectMap {
        private final Map<Key, Object> objectMap = new HashMap<>();
        private int referenceCount = 0;
    }
}
