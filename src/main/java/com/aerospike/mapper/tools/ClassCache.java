package com.aerospike.mapper.tools;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.aerospike.mapper.tools.configuration.ClassConfig;

public class ClassCache {
	private static ClassCache instance = new ClassCache();

	public static ClassCache getInstance() {
		return instance;
	}
	
	private Map<Class<?>, ClassCacheEntry> cacheMap = new HashMap<>();

	private ClassCache() {
	}

	public ClassCacheEntry loadClass(@NotNull Class<?> clazz, AeroMapper mapper) {
		ClassCacheEntry entry = cacheMap.get(clazz);
		if (entry == null) {
			try {
				entry = new ClassCacheEntry(clazz, mapper, null);
			}
			catch (IllegalArgumentException iae) {
				return null;
			}
			cacheMap.put(clazz, entry);
		}
		return entry;
	}
	
	public ClassCacheEntry loadClass(AeroMapper mapper, @NotNull ClassConfig config) throws ClassNotFoundException {
		if (StringUtils.isBlank(config.getClassName())) {
			throw new IllegalArgumentException("Class name must be specified");
		}
		Class<?> clazz = Class.forName(config.getClassName());
		ClassCacheEntry entry = cacheMap.get(clazz);
		if (entry == null) {
			try {
				entry = new ClassCacheEntry(clazz, mapper, config);
			}
			catch (IllegalArgumentException iae) {
				return null;
			}
			cacheMap.put(clazz, entry);
		}
		return entry;
	}
	
	public boolean hasClass(Class<?> clazz) {
		return cacheMap.containsKey(clazz);
	}
}
