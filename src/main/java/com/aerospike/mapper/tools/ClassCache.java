package com.aerospike.mapper.tools;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.aerospike.mapper.tools.configuration.ClassConfig;
import com.aerospike.mapper.tools.configuration.Configuration;

public class ClassCache {
	private static ClassCache instance = new ClassCache();

	public static ClassCache getInstance() {
		return instance;
	}
	
	private Map<Class<?>, ClassCacheEntry> cacheMap = new HashMap<>();
	private Map<String, ClassConfig> classesConfig = new HashMap<>();


	private ClassCache() {
	}

	public ClassCacheEntry loadClass(@NotNull Class<?> clazz, AeroMapper mapper) {
		ClassCacheEntry entry = cacheMap.get(clazz);
		if (entry == null) {
			try {
				entry = new ClassCacheEntry(clazz, mapper, getClassConfig(clazz));
			}
			catch (IllegalArgumentException iae) {
				return null;
			}
			cacheMap.put(clazz, entry);
		}
		return entry;
	}
	
	/*
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
	*/
	
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
}
