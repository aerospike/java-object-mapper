package com.aerospike.mapper.tools;

import java.util.HashMap;
import java.util.Map;

import com.aerospike.client.Key;

public class LoadedObjectResolver {
	
	private static class LoadedObjectMap {
		private int referenceCount = 0;
		private final Map<Key, Object> objectMap = new HashMap<>(); 
	}
	
	private static final ThreadLocal<LoadedObjectMap> threadLocalObjects = ThreadLocal.withInitial(LoadedObjectMap::new);
	
	public static void begin() {
		LoadedObjectMap map = threadLocalObjects.get();
		map.referenceCount++;
	}

	public static void end() {
		LoadedObjectMap map = threadLocalObjects.get();
		map.referenceCount--;
		if (map.referenceCount == 0) {
			map.objectMap.clear();
		}
	}
	
	public static void setObjectForCurrentKey(Object object) {
		Key currentKey = ThreadLocalKeySaver.get();
		LoadedObjectMap map = threadLocalObjects.get();
		if (currentKey != null) {
			map.objectMap.put(currentKey, object);
		}
	}
//	public static void add(Object key, Object object) {
//		threadLocalObjects.get().objectMap.put(key, object);
//	}
	
	public static Object get(Key key) {
		LoadedObjectMap map = threadLocalObjects.get();
		return map.objectMap.get(key);
	}
}