package com.aerospike.mapper.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import com.aerospike.mapper.tools.ClassCache.PolicyType;
import com.aerospike.mapper.tools.configuration.ClassConfig;
import com.aerospike.mapper.tools.configuration.Configuration;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class AeroMapper {

    private IAerospikeClient mClient;

    public static class Builder {
        private AeroMapper mapper;
        private List<Class<?>> classesToPreload = null;

        public Builder(IAerospikeClient client) {
            this.mapper = new AeroMapper(client);
            ClassCache.getInstance().setDefaultPolicies(client);
        }

        /**
         * Add in a custom type converter. The converter must have methods which implement the ToAerospike and FromAerospike annotation
         *
         * @param converter
         * @return this object
         */
        public Builder addConverter(Object converter) {
            GenericTypeMapper mapper = new GenericTypeMapper(converter);
            TypeUtils.addTypeMapper(mapper.getMappedClass(), mapper);

            return this;
        }

        public Builder preLoadClass(Class<?> clazz) {
            if (classesToPreload == null) {
                classesToPreload = new ArrayList<>();
            }
            classesToPreload.add(clazz);
            return this;
        }

        public Builder withConfigurationFile(File file) throws JsonParseException, JsonMappingException, IOException {
        	return this.withConfigurationFile(file, false);
        }
        
        public Builder withConfigurationFile(File file, boolean allowsInvalid) throws JsonParseException, JsonMappingException, IOException {
        	ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        	Configuration configuration = objectMapper.readValue(file, Configuration.class);
        	this.loadConfiguration(configuration, allowsInvalid);
        	return this;
        }

        public Builder withConfiguration(String configurationYaml) throws JsonMappingException, JsonProcessingException {
        	return this.withConfiguration(configurationYaml, false);
        }
        
        public Builder withConfiguration(String configurationYaml, boolean allowsInvalid) throws JsonMappingException, JsonProcessingException {
        	ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        	Configuration configuration = objectMapper.readValue(configurationYaml, Configuration.class);
        	this.loadConfiguration(configuration, allowsInvalid);
        	return this;
        }

        private void loadConfiguration(@NotNull Configuration configuration, boolean allowsInvalid) {
        	for (ClassConfig config : configuration.getClasses()) {
        		try {
	        		String name = config.getClassName();
	        		if (StringUtils.isBlank(name)) {
	        			throw new AerospikeException("Class with blank name in configuration file");
	        		}
	        		else {
	        			try {
	        				Class.forName(config.getClassName());
						} catch (ClassNotFoundException e) {
							throw new AerospikeException("Canot find a class with name " + name);
						}
	        		}
        		}
        		catch (RuntimeException re) {
        			if (allowsInvalid) {
        				System.err.println("Ignoring issue with configuration: " + re.getMessage());
        			}
        			else {
        				throw re;
        			}
        		}
        	}
        	ClassCache.getInstance().addConfiguration(configuration);
        }
        
        public static class AeroPolicyMapper {
        	private final Builder builder;
        	private final Policy policy;
        	private final PolicyType policyType;

        	public AeroPolicyMapper(Builder builder, PolicyType policyType, Policy policy) {
        		this.builder = builder;
        		this.policyType = policyType;
        		this.policy = policy;
			}
        	public Builder forClasses(Class<?> ... classes) {
        		for (Class<?> thisClass : classes) {
        			ClassCache.getInstance().setSpecificPolicy(policyType, thisClass, policy);
        		}
        		return builder;
        	}
        	public Builder forThisOrChildrenOf(Class<?> clazz) {
        		ClassCache.getInstance().setChildrenPolicy(this.policyType, clazz, this.policy);
        		return builder;
        	}
        	public Builder forAll() {
        		ClassCache.getInstance().setDefaultPolicy(policyType, policy);
        		return builder;
        	}
        }
        
        public AeroPolicyMapper withReadPolicy(Policy policy) {
        	return new AeroPolicyMapper(this, PolicyType.READ, policy);
        }
        public AeroPolicyMapper withWritePolicy(Policy policy) {
        	return new AeroPolicyMapper(this, PolicyType.WRITE, policy);
        }
        public AeroPolicyMapper withBatchPolicy(BatchPolicy policy) {
        	return new AeroPolicyMapper(this, PolicyType.BATCH, policy);
        }
        public AeroPolicyMapper withScanPolicy(ScanPolicy policy) {
        	return new AeroPolicyMapper(this, PolicyType.SCAN, policy);
        }
        public AeroPolicyMapper withQueryPolicy(QueryPolicy policy) {
        	return new AeroPolicyMapper(this, PolicyType.QUERY, policy);
        }
        
        public AeroMapper build() {
            if (classesToPreload != null) {
                for (Class<?> clazz : classesToPreload) {
                    ClassCache.getInstance().loadClass(clazz, this.mapper);
                }
            }
            return this.mapper;
        }
    }

    private AeroMapper(@NotNull IAerospikeClient client) {
        this.mClient = client;
    }

    
    private <T> ClassCacheEntry<T> getEntryAndValidateNamespace(Class<T> clazz) {
        ClassCacheEntry<T> entry = ClassCache.getInstance().loadClass(clazz, this);
        String namespace = null;
        if (entry != null) {
	        namespace = entry.getNamespace();
        }
        if (StringUtils.isBlank(namespace)) {
            throw new AerospikeException("Namespace not specified to save a record of type " + clazz.getName());
        }
        return entry;
    }

    private <T> void save(@NotNull T object, @NotNull RecordExistsAction recordExistsAction, String[] binNames) {
    	Class<T> clazz = (Class<T>) object.getClass();
    	ClassCacheEntry<T> entry = getEntryAndValidateNamespace(clazz);
        WritePolicy writePolicy = new WritePolicy(entry.getWritePolicy());
        String set = entry.getSetName();
        if ("".equals(set)) {
        	// Use the null set
        	set = null;
        }
        Integer ttl = entry.getTtl();
        Boolean sendKey = entry.getSendKey();

        if (ttl != null) {
        	writePolicy.expiration = ttl;
        }
        if (sendKey != null) {
        	writePolicy.sendKey = sendKey;
        }
        writePolicy.recordExistsAction = recordExistsAction;
        
        Key key = new Key(entry.getNamespace(), set, Value.get(entry.getKey(object)));

        Bin[] bins = entry.getBins(object, recordExistsAction != RecordExistsAction.REPLACE, binNames);

        mClient.put(writePolicy, key, bins);
    }

    /**
     * Save an object in the database. This method will perform a REPLACE on the existing record so any existing
     * data will be overwritten by the data in the passed object
     * @param object
     * @throws AerospikeException
     */
    public void save(@NotNull Object object, String ...binNames) throws AerospikeException {
        save(object, RecordExistsAction.REPLACE, binNames);
    }

    /**
     * Updates the object in the database, merging the record with the existing record. This uses the RecordExistsAction
     * of UPDATE. If bins are specified, only bins with the passed names will be updated (or all of them if null is passed)
     * @param object
     * @throws AerospikeException
     */
    public void update(@NotNull Object object, String ... binNames) throws AerospikeException {
        save(object, RecordExistsAction.UPDATE, binNames);
    }


    public <T> T readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest) throws AerospikeException {
        ClassCacheEntry entry = getEntryAndValidateNamespace(clazz);
        Key key = new Key(entry.getNamespace(), digest, entry.getSetName(), null);
        return this.read(clazz, key, entry);
    }

    public <T> T read(@NotNull Class<T> clazz, @NotNull Object userKey) throws AerospikeException {

        ClassCacheEntry entry = getEntryAndValidateNamespace(clazz);
        String set = entry.getSetName();
        Key key = new Key(entry.getNamespace(), set, Value.get(entry.translateKeyToAerospikeKey(userKey)));
        return read(clazz, key, entry);
    }

    private <T> T read(@NotNull Class<T> clazz, @NotNull Key key, @NotNull ClassCacheEntry entry) {
        Record record = mClient.get(entry.getReadPolicy(), key);

        if (record == null) {
            return null;
        } else {
            try {
            	ThreadLocalKeySaver.save(key);
                T result = (T) convertToObject(clazz, record, entry);
                return result;
            } catch (ReflectiveOperationException e) {
                throw new AerospikeException(e);
            }
            finally {
            	ThreadLocalKeySaver.clear();
            }
        }
    }

    public <T> boolean delete(@NotNull Class<T> clazz, @NotNull Object userKey) throws AerospikeException {
        ClassCacheEntry<T> entry = getEntryAndValidateNamespace(clazz);
        Object asKey = entry.translateKeyToAerospikeKey(userKey);
        Key key = new Key(entry.getNamespace(), entry.getSetName(), Value.get(asKey));

        WritePolicy writePolicy = entry.getWritePolicy();
        if (entry.getDurableDelete() != null) {
        	// Clone the write policy so we're not changing the original one
            writePolicy = new WritePolicy(writePolicy);
            writePolicy.durableDelete = entry.getDurableDelete();
        }

        return mClient.delete(writePolicy, key);
    }

    public boolean delete(@NotNull Object object) throws AerospikeException {
        ClassCacheEntry entry = getEntryAndValidateNamespace(object.getClass());
        Key key = new Key(entry.getNamespace(), entry.getSetName(), Value.get(entry.getKey(object)));

        WritePolicy writePolicy = entry.getWritePolicy();
        if (entry.getDurableDelete() != null) {
            writePolicy = new WritePolicy(writePolicy);
            writePolicy.durableDelete = entry.getDurableDelete();
        }
        return mClient.delete(writePolicy, key);
    }

    public <T> void find(@NotNull Class<T> clazz, Function<T, Boolean> function) throws AerospikeException {
        ClassCacheEntry entry = getEntryAndValidateNamespace(clazz);

        Statement statement = new Statement();
        statement.setNamespace(entry.getNamespace());
        statement.setSetName(entry.getSetName());

        RecordSet recordSet = null;
        try {
        	// TODO: set the policy (If this statement is thought to be useful, which is dubious)
            recordSet = mClient.query(null, statement);
            T result;
            while (recordSet.next()) {
                result = clazz.getConstructor().newInstance();
                entry.hydrateFromRecord(recordSet.getRecord(), result);
                if (!function.apply(result)) {
                    break;
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new AerospikeException(e);
        } finally {
            if (recordSet != null) {
                recordSet.close();
            }
        }

    }

    // --------------------------------------------------------------------------------------------------
    // The following are convenience methods to convert objects to / from lists / maps / records in case
    // it is needed to perform this operation manually. They will not be needed in most use cases.
    // --------------------------------------------------------------------------------------------------
    /**
     * Given a record loaded from Aerospike and a class type, attempt to convert the record to 
     * an instance of the passed class.
     * @param <T>
     * @param clazz
     * @param record
     * @return
     * @throws ReflectiveOperationException
     */
    public <T> T convertToObject(Class<T> clazz, Record record) {
    	try {
    		return convertToObject(clazz, record, null);
		} catch (ReflectiveOperationException e) {
			throw new AerospikeException(e);
		}    		
    }

    public <T> T convertToObject(Class<T> clazz, Record record, ClassCacheEntry<T> entry) throws ReflectiveOperationException {
        if (entry == null) {
            entry = ClassCache.getInstance().loadClass(clazz, this);
        }
        return entry.constructAndHydrate(clazz, record);
    }

    public <T> T convertToObject(Class<T> clazz, List<Object> record)  {
		try {
	        ClassCacheEntry<T> entry = ClassCache.getInstance().loadClass(clazz, this);
	        T result;
			result = clazz.getConstructor().newInstance();
			entry.hydrateFromList(record, result);
			return result;
		} catch (ReflectiveOperationException e) {
			throw new AerospikeException(e);
		}
    }

    public <T> List<Object> convertToList(@NotNull T instance) {
    	ClassCacheEntry<T> entry = (ClassCacheEntry<T>) ClassCache.getInstance().loadClass(instance.getClass(), this);
    	return entry.getList(instance, false, false);
    }

    public <T> T convertToObject(Class<T> clazz, Map<String,Object> record) {
    	try {
	        ClassCacheEntry<T> entry = ClassCache.getInstance().loadClass(clazz, this);
	        T result = clazz.getConstructor().newInstance();
	        entry.hydrateFromMap(record, result);
	        return result;
		} catch (ReflectiveOperationException e) {
			throw new AerospikeException(e);
		}
    }

    public <T> Map<String, Object> convertToMap(@NotNull T instance) {
    	ClassCacheEntry<T> entry = (ClassCacheEntry<T>) ClassCache.getInstance().loadClass(instance.getClass(), this);
    	return entry.getMap(instance, false);
    }
}
