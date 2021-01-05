package com.aerospike.mapper.tools;

import java.util.HashMap;
import java.util.Map;

public class ClassCache {
	private static ClassCache instance = new ClassCache();

	public static ClassCache getInstance() {
		return instance;
	}
	
	private Map<Class<?>, ClassCacheEntry> cacheMap = new HashMap<>();

	private ClassCache() {
	}

	public ClassCacheEntry loadClass(Class<?> clazz, AeroMapper mapper) {
		ClassCacheEntry entry = cacheMap.get(clazz);
		if (entry == null) {
			try {
				entry = new ClassCacheEntry(clazz, mapper);
			}
			catch (IllegalArgumentException iae) {
				return null;
			}
			cacheMap.put(clazz, entry);
		}
		return entry;
	}
}
