package com.aerospike.mapper.tools;

import com.aerospike.client.*;
import com.aerospike.client.policy.*;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import com.aerospike.mapper.tools.ClassCache.PolicyType;
import com.aerospike.mapper.tools.DeferredObjectLoader.DeferredObject;
import com.aerospike.mapper.tools.DeferredObjectLoader.DeferredObjectSetter;
import com.aerospike.mapper.tools.TypeUtils.AnnotatedType;
import com.aerospike.mapper.tools.configuration.ClassConfig;
import com.aerospike.mapper.tools.configuration.Configuration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AeroMapper {

    final IAerospikeClient mClient;

    public static class Builder {
        private final AeroMapper mapper;
        private List<Class<?>> classesToPreload = null;

        public Builder(IAerospikeClient client) {
            this.mapper = new AeroMapper(client);
            ClassCache.getInstance().setDefaultPolicies(client);
        }

        /**
         * Add in a custom type converter. The converter must have methods which implement the ToAerospike and FromAerospike annotation.
         * @param converter The custom converter
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

        public Builder withConfigurationFile(File file) throws IOException {
        	return this.withConfigurationFile(file, false);
        }
        
        public Builder withConfigurationFile(File file, boolean allowsInvalid) throws IOException {
        	ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        	Configuration configuration = objectMapper.readValue(file, Configuration.class);
        	this.loadConfiguration(configuration, allowsInvalid);
        	return this;
        }

        public Builder withConfiguration(String configurationYaml) throws JsonProcessingException {
        	return this.withConfiguration(configurationYaml, false);
        }
        
        public Builder withConfiguration(String configurationYaml, boolean allowsInvalid) throws JsonProcessingException {
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
							throw new AerospikeException("Cannot find a class with name " + name);
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
            throw new AerospikeException("Namespace not specified to perform database operation on a record of type " + clazz.getName());
        }
        return entry;
    }

    private <T> void save(WritePolicy writePolicy, @NotNull T object, RecordExistsAction recordExistsAction, String[] binNames) {
    	Class<T> clazz = (Class<T>) object.getClass();
    	ClassCacheEntry<T> entry = getEntryAndValidateNamespace(clazz);
    	if (writePolicy == null) {
        	writePolicy = new WritePolicy(entry.getWritePolicy());
        	if (recordExistsAction != null) {
        		writePolicy.recordExistsAction = recordExistsAction;
        	}
    	}
    	
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
        Key key = new Key(entry.getNamespace(), set, Value.get(entry.getKey(object)));

        Bin[] bins = entry.getBins(object, writePolicy.recordExistsAction != RecordExistsAction.REPLACE, binNames);

        mClient.put(writePolicy, key, bins);
    }

    /**
     * Translate a Java object to an Aerospike format object. Note that this could potentially have performance issues as
     * the type information of the passed object must be determined on every call.
     * @param obj A given Java object.
     * @return An Aerospike format object.
     */
    public Object translateToAerospike(Object obj) {
    	if (obj == null) {
    		return null;
    	}
    	TypeMapper thisMapper = TypeUtils.getMapper(obj.getClass(), AnnotatedType.getDefaultAnnotateType(), this);
    	return thisMapper == null ? obj : thisMapper.toAerospikeFormat(obj);
    }

    /**
     * Translate an Aerospike object to a Java object. Note that this could potentially have performance issues as
     * the type information of the passed object must be determined on every call.
     * @param obj A given Java object.
     * @return An Aerospike format object.
     */
    @SuppressWarnings("unchecked")
	public <T> T translateFromAerospike(@NotNull Object obj, @NotNull Class<T> expectedClazz) {
    	TypeMapper thisMapper = TypeUtils.getMapper(expectedClazz, AnnotatedType.getDefaultAnnotateType(), this);
    	T result = (T)(thisMapper == null ? obj : thisMapper.fromAerospikeFormat(obj));
		resolveDependencies(ClassCache.getInstance().loadClass(expectedClazz, this));
		return result;
    }

    /**
     * Save each object in the database. This method will perform a REPLACE on the existing record so any existing
     * data will be overwritten by the data in the passed object. This is a convenience method for
     * <pre>
     * save(A);
     * save(B);
     * save(C);
     * </pre>
     * Not that no transactionality is implied by this method -- if any of the save methods fail, the exception will be
     * thrown without trying the other objects, nor attempting to roll back previously saved objects
     * @param objects One or two objects to save.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    public void save(@NotNull Object ... objects) throws AerospikeException {
    	for (Object thisObject : objects) {
    		this.save(thisObject);
    	}
    }

    /**
     * Save an object in the database. This method will perform a REPLACE on the existing record so any existing
     * data will be overwritten by the data in the passed object
     * @param object The object to save.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    public void save(@NotNull Object object, String ...binNames) throws AerospikeException {
        save(null, object, RecordExistsAction.REPLACE, binNames);
    }

    /**
     * Save an object in the database with the given WritePolicy. This write policy will override any other set writePolicy so
     * is effectively an upsert operation
     * @param writePolicy The write policy for the save operation.
     * @param object The object to save.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    public void save(@NotNull WritePolicy writePolicy, @NotNull Object object, String ...binNames) throws AerospikeException {
        save(writePolicy, object, null, binNames);
    }

    /**
     * Updates the object in the database, merging the record with the existing record. This uses the RecordExistsAction
     * of UPDATE. If bins are specified, only bins with the passed names will be updated (or all of them if null is passed)
     * @param object The object to update.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    public void update(@NotNull Object object, String ... binNames) throws AerospikeException {
        save(null, object, RecordExistsAction.UPDATE, binNames);
    }

    public <T> T readFromDigest(Policy readPolicy, @NotNull Class<T> clazz, @NotNull byte[] digest) throws AerospikeException {
    	return this.readFromDigest(readPolicy, clazz, digest, true);
    }
    
    /**
     * This method should not be used except by mappers
     */
    public <T> T readFromDigest(Policy readPolicy, @NotNull Class<T> clazz, @NotNull byte[] digest, boolean resolveDependencies) throws AerospikeException {
        ClassCacheEntry<T> entry = getEntryAndValidateNamespace(clazz);
        Key key = new Key(entry.getNamespace(), digest, entry.getSetName(), null);
        return this.read(readPolicy, clazz, key, entry, resolveDependencies);
    }

    public <T> T readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest) throws AerospikeException {
    	return this.readFromDigest(clazz, digest, true);
    }
    
    /**
     * This method should not be used except by mappers
     */
    public <T> T readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest, boolean resolveDependencies) throws AerospikeException {
        ClassCacheEntry<T> entry = getEntryAndValidateNamespace(clazz);
        Key key = new Key(entry.getNamespace(), digest, entry.getSetName(), null);
        return this.read(null, clazz, key, entry, resolveDependencies);
    }

    public <T> T read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Object userKey) throws AerospikeException {
    	return this.read(readPolicy, clazz, userKey, true);
    }
    
    /**
     * This method should not be used except by mappers
     */
    public <T> T read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Object userKey, boolean resolveDependencies) throws AerospikeException {
        ClassCacheEntry<T> entry = getEntryAndValidateNamespace(clazz);
        String set = entry.getSetName();
        Key key = new Key(entry.getNamespace(), set, Value.get(entry.translateKeyToAerospikeKey(userKey)));
        return read(readPolicy, clazz, key, entry, resolveDependencies);
    }

    /**
     * Read a record from the repository and map it to an instance of the passed class.
     * @param clazz - The type of be returned.
     * @param userKey - The key of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return The returned mapped record.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    public <T> T read(@NotNull Class<T> clazz, @NotNull Object userKey) throws AerospikeException {
    	return this.read(clazz, userKey, true);
    }
    
    /**
     * This method should not be used: It is used by mappers to correctly resolved dependencies. Use read(clazz, userKey) instead
     */
    public <T> T read(@NotNull Class<T> clazz, @NotNull Object userKey, boolean resolveDependencies) throws AerospikeException {
        ClassCacheEntry<T> entry = getEntryAndValidateNamespace(clazz);
        String set = entry.getSetName();
        Key key = new Key(entry.getNamespace(), set, Value.get(entry.translateKeyToAerospikeKey(userKey)));
        return read(null, clazz, key, entry, resolveDependencies);
    }

    private <T> T read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Key key, @NotNull ClassCacheEntry<T> entry, boolean resolveDependencies) {
    	if (readPolicy == null) {
    		readPolicy = entry.getReadPolicy();
    	}
        Record record = mClient.get(readPolicy, key);

        if (record == null) {
            return null;
        } else {
            try {
            	ThreadLocalKeySaver.save(key);
                return convertToObject(clazz, record, entry, resolveDependencies);
            } catch (ReflectiveOperationException e) {
                throw new AerospikeException(e);
            }
            finally {
            	ThreadLocalKeySaver.clear();
            }
        }
    }

    /**
     * Read a batch of records from the repository and map them to an instance of the passed class.
     * @param clazz - The type of be returned.
     * @param userKeys - The keys of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return The returned mapped records.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    public <T> T[] read(@NotNull Class<T> clazz, @NotNull Object ... userKeys) throws AerospikeException {
    	return this.read(null, clazz, userKeys);
    }

    /**
     * Read a batch of records from the repository and map them to an instance of the passed class.
     * @param batchPolicy A given batch policy.
     * @param clazz - The type of be returned.
     * @param userKeys - The keys of the record. The namespace and set will be derived from the values specified on the passed class.
     * @return The returned mapped records.
     * @throws AerospikeException an AerospikeException will be thrown in case of an error.
     */
    public <T> T[] read(BatchPolicy batchPolicy, @NotNull Class<T> clazz, @NotNull Object ... userKeys) throws AerospikeException {
        ClassCacheEntry<T> entry = getEntryAndValidateNamespace(clazz);
        String set = entry.getSetName();
        Key[] keys = new Key[userKeys.length];
        for (int i = 0; i < userKeys.length; i++) {
        	if (userKeys[i] == null) {
        		throw new AerospikeException("Cannot pass null to object " + i + " in multi-read call");
        	}
        	else {
        		keys[i] = new Key(entry.getNamespace(), set, Value.get(entry.translateKeyToAerospikeKey(userKeys[i])));
        	}
        }

    	return this.readBatch(batchPolicy, clazz, keys, entry);
    }

    private <T> T[] readBatch(BatchPolicy batchPolicy, @NotNull Class<T> clazz, @NotNull Key[] keys, @NotNull ClassCacheEntry<T> entry) {
    	if (batchPolicy == null) {
    		batchPolicy = entry.getBatchPolicy();
    	}
        Record[] records = mClient.get(batchPolicy, keys);
        T[] results = (T[])Array.newInstance(clazz, records.length);
        for (int i = 0; i < records.length; i++) {
        	if (records[i] == null) {
        		results[i] = null;
        	}
        	else {
                try {
                	ThreadLocalKeySaver.save(keys[i]);
                    T result = convertToObject(clazz, records[i], entry, false);
                    results[i] = result;
                } catch (ReflectiveOperationException e) {
                    throw new AerospikeException(e);
                }
                finally {
                	ThreadLocalKeySaver.clear();
                }
        	}
        }
        resolveDependencies(entry);
        return results;
    }

    public <T> boolean delete(@NotNull Class<T> clazz, @NotNull Object userKey) throws AerospikeException {
    	return this.delete(null, clazz, userKey);
    }
    
    public <T> boolean delete(WritePolicy writePolicy, @NotNull Class<T> clazz, @NotNull Object userKey) throws AerospikeException {
        ClassCacheEntry<T> entry = getEntryAndValidateNamespace(clazz);
        Object asKey = entry.translateKeyToAerospikeKey(userKey);

        if (writePolicy == null) {
            writePolicy = entry.getWritePolicy();
            if (entry.getDurableDelete() != null) {
            	// Clone the write policy so we're not changing the original one
                writePolicy = new WritePolicy(writePolicy);
                writePolicy.durableDelete = entry.getDurableDelete();
            }
    	}
        Key key = new Key(entry.getNamespace(), entry.getSetName(), Value.get(asKey));

        return mClient.delete(writePolicy, key);
    }

    public boolean delete(@NotNull Object object) throws AerospikeException {
    	return this.delete((WritePolicy)null, object);
    }
    
    public boolean delete(WritePolicy writePolicy, @NotNull Object object) throws AerospikeException {
        ClassCacheEntry<?> entry = getEntryAndValidateNamespace(object.getClass());
        Key key = new Key(entry.getNamespace(), entry.getSetName(), Value.get(entry.getKey(object)));

        if (writePolicy == null) {
	        writePolicy = entry.getWritePolicy();
	        if (entry.getDurableDelete() != null) {
	            writePolicy = new WritePolicy(writePolicy);
	            writePolicy.durableDelete = entry.getDurableDelete();
	        }
        }
        return mClient.delete(writePolicy, key);
    }
    
    /**
     * Create a virtual list against an attribute on a class. The list does all operations to the database and does not affect the underlying
     * class, and is useful for situation when operations are needed to affect the database without having to return all the elements on the
     * list each time.
     * <p/>
     * For example, consider a set of transactions associated with a credit card. Common operations might be
     * <ul>
     * 	<li>Return the last N transactions </li>
     * 	<li>insert a new transaction into the list</li>
     * </ul>
     * These operation can all be done without having the full set of transactions
     * @param <T> the type of the elements in the list.
     * @param object The object that will use as a base for the virtual list.
     * @param binName The Aerospike bin name.
     * @param elementClazz The class of the elements in the list.
     * @return A virtual list.
     */
    public <T> VirtualList<T> asBackedList(@NotNull Object object, @NotNull String binName, Class<T> elementClazz) {
    	return new VirtualList<>(this, object, binName, elementClazz);
    }
    
    /**
     * Create a virtual list against an attribute on a class. The list does all operations to the database and does not affect the underlying
     * class, and is useful for situation when operations are needed to affect the database without having to return all the elements on the
     * list each time.
     * <p/>
     * Note that the object being mapped does not need to actually exist in this case. The owning class is used purely for the definitions
     * of how to map the list elements (are they to be mapped in the database as a list or a map, is each element a list or a map, etc), as
     * well as using the namespace / set definition for the location to map into the database.  The 
     * passed key is used to map the object to the database. 
     * <p/>
     * For example, consider a set of transactions associated with a credit card. Common operations might be
     * <ul>
     * 	<li>Return the last N transactions </li>
     * 	<li>insert a new transaction into the list</li>
     * </ul>
     * These operation can all be done without having the full set of transactions
     * @param <T> the type of the elements in the list.
     * @param owningClazz Used for the definitions of how to map the list elements.
     * @param key The key to map the object to the database.
     * @param binName The Aerospike bin name.
     * @param elementClazz The class of the elements in the list.
     * @return A virtual list.
     */
    public <T> VirtualList<T> asBackedList(@NotNull Class<?> owningClazz, @NotNull Object key, @NotNull String binName, Class<T> elementClazz) {
    	return new VirtualList<>(this, owningClazz, key, binName, elementClazz);
    }
    
    public <T> void find(@NotNull Class<T> clazz, Function<T, Boolean> function) throws AerospikeException {
        ClassCacheEntry<T> entry = getEntryAndValidateNamespace(clazz);

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
     * @param clazz The class type to convert the Aerospike record to.
     * @param record The Aerospike record to convert.
     * @return A virtual list.
     * @throws AerospikeException an AerospikeException will be thrown in case of an encountering a ReflectiveOperationException.
     */
    public <T> T convertToObject(Class<T> clazz, Record record) {
    	try {
    		return convertToObject(clazz, record, null);
		} catch (ReflectiveOperationException e) {
			throw new AerospikeException(e);
		}    		
    }

    public <T> T convertToObject(Class<T> clazz, Record record, ClassCacheEntry<T> entry) throws ReflectiveOperationException {
    	return this.convertToObject(clazz, record, entry, true);
    }
    
    /**
     * This method should not be used, it is public only to allow mappers to see it.
     */
    public <T> T convertToObject(Class<T> clazz, Record record, ClassCacheEntry<T> entry, boolean resolveDependencies) throws ReflectiveOperationException {
        if (entry == null) {
            entry = ClassCache.getInstance().loadClass(clazz, this);
        }
        T result = entry.constructAndHydrate(record);
        if (resolveDependencies) {
        	resolveDependencies(entry);
        }
		return result;
    }

    public <T> T convertToObject(Class<T> clazz, List<Object> record)  {
    	return this.convertToObject(clazz, record, true);
    }

    /**
     * This method should not be used, it is public only to allow mappers to see it.
     */
    public <T> T convertToObject(Class<T> clazz, List<Object> record, boolean resolveDependencies)  {
		try {
	        ClassCacheEntry<T> entry = ClassCache.getInstance().loadClass(clazz, this);
	        T result;
			result = clazz.getConstructor().newInstance();
			entry.hydrateFromList(record, result);
			if (resolveDependencies) {
				resolveDependencies(entry);
			}
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
    
    /**
     * If an object refers to other objects (eg A has a list of B via references), then reading the object will populate the
     * ids. If configured to do so, these objects can be loaded via a batch load and populated back into the references which
     * contain them. This method performs this batch load, translating the records to objects and mapping them back to the
     * references.
     * <p/>
     * These loaded child objects can themselves have other references to other objects, so we iterate through this until
     * the list of deferred objects is empty. The deferred objects are stored in a <pre>ThreadLocalData<pre> list, so are thread safe
     * @param parentEntity - the ClassCacheEntry of the parent entity. This is used to get the batch policy to use.
     */
    void resolveDependencies(ClassCacheEntry<?> parentEntity) {
    	List<DeferredObjectSetter> deferredObjects = DeferredObjectLoader.getAndClear();
    	
    	if (deferredObjects.size() == 0) {
    		return;
    	}
    	
    	BatchPolicy batchPolicy = parentEntity == null ? mClient.getBatchPolicyDefault() : parentEntity.getBatchPolicy();
    	BatchPolicy batchPolicyClone = new BatchPolicy(batchPolicy);
    	
    	while (!deferredObjects.isEmpty()) {
    		int size = deferredObjects.size();
    		
    		ClassCacheEntry<?>[] classCaches = new ClassCacheEntry<?>[size];
    		Key[] keys = new Key[size];
    		
    		for (int i = 0; i < size; i++) {
    			DeferredObjectSetter thisObjectSetter = deferredObjects.get(i);
    			DeferredObject deferredObject = thisObjectSetter.getObject();
    			Class<?> clazz = deferredObject.getType();
    			ClassCacheEntry<?> entry = getEntryAndValidateNamespace(clazz);
    			classCaches[i] = entry; 
    			
    			if (deferredObject.isDigest()) {
    				keys[i] = new Key(entry.getNamespace(), (byte[])deferredObject.getKey(), entry.getSetName(), null);
    			}
    			else {
    				keys[i] = new Key(entry.getNamespace(), entry.getSetName(), Value.get(entry.translateKeyToAerospikeKey(deferredObject.getKey())));
    			}
    		}
    		
    		// Load the data
    		if (keys.length <= 2) {
    			// Just single-thread these keys for speed
    			batchPolicyClone.maxConcurrentThreads = 1;
    		}
    		else {
    			batchPolicyClone.maxConcurrentThreads = batchPolicy.maxConcurrentThreads;
    		}
    		Record[] records = this.mClient.get(batchPolicyClone, keys);
    		
    		for (int i = 0; i < size; i++) {
    			DeferredObjectSetter thisObjectSetter = deferredObjects.get(i);
    			try {
                	ThreadLocalKeySaver.save(keys[i]);
                	Object result = records[i] == null ? null : this.convertToObject((Class)thisObjectSetter.getObject().getType(), records[i], classCaches[i], false);
                	thisObjectSetter.getSetter().setValue(result);
	            } catch (ReflectiveOperationException e) {
	                throw new AerospikeException(e);
	            }
	            finally {
	            	ThreadLocalKeySaver.clear();
	            }
    		}
        	deferredObjects = DeferredObjectLoader.getAndClear();
    	}
    }
}
