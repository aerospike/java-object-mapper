package com.aerospike.mapper.tools;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.tools.configuration.ClassConfig;
import com.aerospike.mapper.tools.configuration.Configuration;

public class ClassCache {
	private static ClassCache instance = new ClassCache();

	public static ClassCache getInstance() {
		return instance;
	}
	
	enum PolicyType {
		READ,
		WRITE
	}
	
	private Map<Class<?>, ClassCacheEntry> cacheMap = new HashMap<>();
	private Map<String, ClassConfig> classesConfig = new HashMap<>();
	private Map<PolicyType, Policy> defaultPolicies = new HashMap<>();
	private Map<PolicyType, Map<Class<?>, Policy>> childrenPolicies = new HashMap<>();  
	private Map<PolicyType, Map<Class<?>, Policy>> specificPolicies = new HashMap<>();  

	private ClassCache() {
		for (PolicyType thisType : PolicyType.values()) {
			this.childrenPolicies.put(thisType, new HashMap<>());
			this.specificPolicies.put(thisType, new HashMap<>());
		}
	}

	public ClassCacheEntry loadClass(@NotNull Class<?> clazz, AeroMapper mapper) {
		ClassCacheEntry entry = cacheMap.get(clazz);
		if (entry == null) {
			try {
				entry = new ClassCacheEntry(clazz, mapper, getClassConfig(clazz), determinePolicy(clazz, PolicyType.READ), (WritePolicy)determinePolicy(clazz, PolicyType.WRITE));
			}
			catch (IllegalArgumentException iae) {
				return null;
			}
			cacheMap.put(clazz, entry);
		}
		return entry;
	}

	// package visibility
	void setDefaultPolicies(Policy readPolicy, WritePolicy writePolicy) {
		this.defaultPolicies.put(PolicyType.READ, readPolicy);
		this.defaultPolicies.put(PolicyType.WRITE, writePolicy);
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
		// Otherwise, iterate up class heirarchy looking for the policy.
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
