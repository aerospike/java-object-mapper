package com.aerospike.mapper.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.ListOperation;
import com.aerospike.client.cdt.ListReturnType;
import com.aerospike.client.cdt.MapOperation;
import com.aerospike.client.cdt.MapOrder;
import com.aerospike.client.cdt.MapPolicy;
import com.aerospike.client.cdt.MapReturnType;
import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.tools.ClassCache.PolicyType;
import com.aerospike.mapper.tools.ResultsUnpacker.ArrayUnpacker;
import com.aerospike.mapper.tools.ResultsUnpacker.ElementUnpacker;
import com.aerospike.mapper.tools.ResultsUnpacker.ListUnpacker;
import com.aerospike.mapper.tools.TypeUtils.AnnotatedType;
import com.aerospike.mapper.tools.configuration.ClassConfig;
import com.aerospike.mapper.tools.configuration.Configuration;
import com.aerospike.mapper.tools.mappers.ListMapper;
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
     * Save an object in the database. This method will perform a REPLACE on the existing record so any existing
     * data will be overwritten by the data in the passed object
     * @param object
     * @throws AerospikeException
     */
    public void save(@NotNull Object object, String ...binNames) throws AerospikeException {
        save(null, object, RecordExistsAction.REPLACE, binNames);
    }

    /**
     * Save an object in the database with the given WritePolicy. This write policy will override any other set writePolicy so
     * is effectively an upsert operation
     * @param object
     * @throws AerospikeException
     */
    public void save(@NotNull WritePolicy writePolicy, @NotNull Object object, String ...binNames) throws AerospikeException {
        save(writePolicy, object, null, binNames);
    }


    /**
     * Updates the object in the database, merging the record with the existing record. This uses the RecordExistsAction
     * of UPDATE. If bins are specified, only bins with the passed names will be updated (or all of them if null is passed)
     * @param object
     * @throws AerospikeException
     */
    public void update(@NotNull Object object, String ... binNames) throws AerospikeException {
        save(null, object, RecordExistsAction.UPDATE, binNames);
    }


    public <T> T readFromDigest(Policy readPolicy, @NotNull Class<T> clazz, @NotNull byte[] digest) throws AerospikeException {
        ClassCacheEntry<T> entry = getEntryAndValidateNamespace(clazz);
        Key key = new Key(entry.getNamespace(), digest, entry.getSetName(), null);
        return this.read(readPolicy, clazz, key, entry);
    }

    public <T> T readFromDigest(@NotNull Class<T> clazz, @NotNull byte[] digest) throws AerospikeException {
        ClassCacheEntry<T> entry = getEntryAndValidateNamespace(clazz);
        Key key = new Key(entry.getNamespace(), digest, entry.getSetName(), null);
        return this.read(null, clazz, key, entry);
    }

    public <T> T read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Object userKey) throws AerospikeException {
        ClassCacheEntry<T> entry = getEntryAndValidateNamespace(clazz);
        String set = entry.getSetName();
        Key key = new Key(entry.getNamespace(), set, Value.get(entry.translateKeyToAerospikeKey(userKey)));
        return read(readPolicy, clazz, key, entry);
    }

    public <T> T read(@NotNull Class<T> clazz, @NotNull Object userKey) throws AerospikeException {
        ClassCacheEntry<T> entry = getEntryAndValidateNamespace(clazz);
        String set = entry.getSetName();
        Key key = new Key(entry.getNamespace(), set, Value.get(entry.translateKeyToAerospikeKey(userKey)));
        return read(null, clazz, key, entry);
    }

    private <T> T read(Policy readPolicy, @NotNull Class<T> clazz, @NotNull Key key, @NotNull ClassCacheEntry<T> entry) {
    	if (readPolicy == null) {
    		readPolicy = entry.getReadPolicy();
    	}
        Record record = mClient.get(readPolicy, key);

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
    
    public <T> VirtualList<T> asBackedList(@NotNull Object object, @NotNull String binName, Class<T> clazz) {
    	return new VirtualList<T>(this, object, binName, clazz);
    }
    
    public static class VirtualList<E> {
    	private final AeroMapper mapper;
    	private final ValueType value;
    	private final ClassCacheEntry<?> owningEntry;
    	private final ClassCacheEntry<?> elementEntry;
    	private final String binName;
    	private final ListMapper listMapper;
    	private final Key key;
    	private final EmbedType listType;
    	private final EmbedType elementType;
    	private final Function<Object, Object> instanceMapper; 

    	
    	public VirtualList(@NotNull AeroMapper mapper, @NotNull Object object, @NotNull String binName, @NotNull Class<E> clazz) {
    		Class<?> owningClazz = object.getClass();
            this.owningEntry = (ClassCacheEntry<?>) ClassCache.getInstance().loadClass(owningClazz, mapper);
            this.elementEntry = (ClassCacheEntry<?>) ClassCache.getInstance().loadClass(clazz, mapper);
            this.mapper = mapper;
            this.binName = binName;
            this.value = owningEntry.getValueFromBinName(binName);
            if (value == null) {
            	throw new AerospikeException(String.format("Class %s has no bin called %s", clazz.getSimpleName(), binName));
            }
            String set = owningEntry.getSetName();
            if ("".equals(set)) {
            	// Use the null set
            	set = null;
            }
            key = new Key(owningEntry.getNamespace(), set, Value.get(owningEntry.getKey(object)));

            AnnotatedType annotatedType = value.getAnnotatedType();
            AerospikeEmbed embed = annotatedType.getAnnotation(AerospikeEmbed.class);
            if (embed == null) {
            	throw new AerospikeException(String.format("Bin %s on class %s is not specified as a embedded", binName, clazz.getSimpleName()));
            }
            listType = embed.type() == EmbedType.DEFAULT ? EmbedType.LIST : embed.type();
            elementType = embed.elementType() == EmbedType.DEFAULT ? EmbedType.MAP : embed.elementType();
            Class<?> binClazz = value.getType();
            if (!(binClazz.isArray() || (Map.class.isAssignableFrom(binClazz)) || List.class.isAssignableFrom(binClazz))) {
            	throw new AerospikeException(String.format("Bin %s on class %s is not a collection class", binName, clazz.getSimpleName()));
            }
            
            TypeMapper typeMapper = value.getTypeMapper();
            if (typeMapper instanceof ListMapper) {
            	listMapper = ((ListMapper)typeMapper);
            }
            else {
            	throw new AerospikeException(String.format("Bin %s on class %s is not mapped via a listMapper. This is unexpected", binName, clazz.getSimpleName()));
            }
            this.instanceMapper = src -> listMapper.fromAerospikeInstanceFormat(src);
		}
    	
    	public VirtualList<E> keptInSync(boolean inSync) {
    		return this;
    	}
    	
    	public class MultiOperation<E> {
    		private VirtualList<E> virtualList;
    		private List<Interactor> interactions;
    		private int indexToReturn = -1;
    		private WritePolicy writePolicy;
    		
    		private MultiOperation(@NotNull VirtualList<E> virtualList, @NotNull WritePolicy writePolicy) {
    			this.virtualList = virtualList;
    			this.interactions = new ArrayList<>();
    			this.writePolicy = writePolicy;
			}
    		
    		public MultiOperation<E> append(E item) {
    			Object aerospikeItem = listMapper.toAerospikeInstanceFormat(item);
				this.interactions.add(new Interactor(virtualList.getAppendOperation(aerospikeItem)));
    			return this;
    		}
    		public MultiOperation<E> removeByKeyRange(Object startKey, Object endKey) {
    			// TODO: Be able to change the return type based on the asResult() function call
    			this.interactions.add(getRemoveRangeInteractor(startKey, endKey, true));
    			return this;
    		}
    		
    		public MultiOperation<E> get(int index) {
    			this.interactions.add(getIndexInteractor(index));
    			return this;
    		}
    		
    		public MultiOperation<E> size() {
    			this.interactions.add(getSizeInteractor());
    			return this;
    		}
    		
    		public MultiOperation<E> asResult() {
    			if (interactions.isEmpty()) {
    				throw new AerospikeException("asResult() cannot mark an item as the function result if there are no items to process");
    			}
    			else if (this.indexToReturn >= 0) {
    				throw new AerospikeException("asResult() can only be called once in a multi operation");
    			}
    			else {
    				this.indexToReturn = this.interactions.size() - 1;
    			}
    			return this;
    		}
    		
    		public Object end() {
    			if (this.interactions.isEmpty()) {
    				return null;
    			}
    			this.writePolicy.respondAllOps = true;
    			Operation[] operations = new Operation[this.interactions.size()];
    			int count = 0;
    			for (Interactor thisInteractor : this.interactions) {
    				operations[count++] = thisInteractor.getOperation();
    			}
    			
        		Record record = this.virtualList.mapper.mClient.operate(writePolicy, key, operations);

        		if (count == 1) {
        			Object resultObj = record.getValue(binName);
        			return this.interactions.get(0).getResult(resultObj);
        		}
    			else {
    				List<?> resultList = record.getList(binName);
    				if (indexToReturn < 0) {
    					int listSize = this.interactions.size();
    					indexToReturn = listSize-1;
    					// Determine the last GET operation
    					for (int i = listSize-1; i >= 0; i--) {
    						if (!this.interactions.get(i).isWriteOperation()) {
    							indexToReturn = i;
    							break;
    						}
    					}
    				}
    				return this.interactions.get(indexToReturn).getResult(resultList.get(indexToReturn));
    			}
    		}
    	}

    	public MultiOperation<E> beginMulti() {
    		return this.beginMulti(null);
    	}
    	
    	public MultiOperation<E> beginMulti(WritePolicy writePolicy) {
        	if (writePolicy == null) {
            	writePolicy = new WritePolicy(owningEntry.getWritePolicy());
        		writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        	}
    		return new MultiOperation<E>(this, writePolicy);
    	}
    	

    	/**
    	 * Remove items from the list matching the specified key. If the list is mapped to a MAP in Aerospike, the start key and end key will dictate the range of keys to be removed,
    	 * inclusive of the start, exclusive of the end.
    	 * <p/>
    	 * If the list is mapped to a LIST in Aerospike however, the start and end range represent values to be removed from the list.
    	 * <p/>
    	 * The result of the method is a list of the records which have been removed from the database if returnResults is true, null otherwise.
    	 * @param startKey
    	 * @param endKey
    	 * @param returnResults
    	 * @return
    	 */
    	public List<E> removeByKeyRange(Object startKey, Object endKey, boolean returnResults) {
    		return this.removeByKeyRange(null, startKey, endKey, returnResults);
    	}
    	
    	public List<E> removeByKeyRange(WritePolicy writePolicy, Object startKey, Object endKey, boolean returnResults) {
        	if (writePolicy == null) {
            	writePolicy = new WritePolicy(owningEntry.getWritePolicy());
        		writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        	}
    		Interactor interactor = getRemoveRangeInteractor(startKey, endKey, returnResults);
    		Record record = this.mapper.mClient.operate(writePolicy, key, interactor.getOperation());

    		return (List<E>)interactor.getResult(record.getList(binName));
    	}
    	
    	private Interactor getRemoveRangeInteractor(Object startKey, Object endKey, boolean returnResults) {
    		Object aerospikeStartKey = elementEntry.translateKeyToAerospikeKey(startKey);
    		Object aerospikeEndKey = elementEntry.translateKeyToAerospikeKey(endKey);
    		Interactor interactor;
    		if (listType == EmbedType.LIST) {
    			if (returnResults) {
    				interactor = new Interactor(ListOperation.removeByValueRange(binName, Value.get(aerospikeStartKey), Value.get(aerospikeEndKey), ListReturnType.VALUE), new ArrayUnpacker(instanceMapper));
    			}
    			else {
    				interactor = new Interactor(ListOperation.removeByValueRange(binName, Value.get(aerospikeStartKey), Value.get(aerospikeEndKey), ListReturnType.NONE));
    			}
			}
    		else {
    			if (returnResults) {
    				interactor = new Interactor( MapOperation.removeByKeyRange(binName, Value.get(aerospikeStartKey), Value.get(aerospikeEndKey), MapReturnType.KEY_VALUE), new ArrayUnpacker(instanceMapper));
    			}
    			else {
    				interactor = new Interactor( MapOperation.removeByKeyRange(binName, Value.get(aerospikeStartKey), Value.get(aerospikeEndKey), MapReturnType.NONE));    				
    			}
    		}
    		return interactor;
    	}
    	
    	private Operation getAppendOperation(Object aerospikeObject) {
        	if (aerospikeObject instanceof Entry) {
        		Entry<Object, Object> entry = (Entry) aerospikeObject;
        		return MapOperation.put(new MapPolicy(MapOrder.KEY_ORDERED, 0), binName, Value.get(entry.getKey()), Value.get(entry.getValue()));
        	}
        	else {
        		return ListOperation.append(binName, Value.get(aerospikeObject));
        	}
    	}
    	public long append(E element) {
    		return this.append(null, element);
    	}
    	
    	public long append(WritePolicy writePolicy, E element) {
        	Object result = listMapper.toAerospikeInstanceFormat(element);
        	if (writePolicy == null) {
            	writePolicy = new WritePolicy(owningEntry.getWritePolicy());
        		writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        	}
    		Record record = this.mapper.mClient.operate(writePolicy, key, getAppendOperation(result));
        	return record.getLong(binName);
    	}
    	
    	
    	
    	public E get(int index) {
    		return get(null, index);
    	}
    	
    	private Interactor getIndexInteractor(int index) {
    		if (listType == EmbedType.LIST) {
    			return new Interactor(ListOperation.getByIndex(binName, index, ListReturnType.VALUE), new ElementUnpacker(instanceMapper));
    		}
    		else {
    			return new Interactor(MapOperation.getByIndex(binName, index, MapReturnType.KEY_VALUE), ListUnpacker.instance, new ElementUnpacker(instanceMapper));
    		}
    	}
    	
    	public E get(Policy policy, int index) {
        	if (policy == null) {
        		policy = new Policy(owningEntry.getReadPolicy());
        	}

        	Interactor interactor = getIndexInteractor(index);
    		Record record = this.mapper.mClient.operate(null, key, interactor.getOperation());
    		return (E)interactor.getResult(record.getList(binName));
    	}
    	
    	private Interactor getSizeInteractor() {
    		if (listType == EmbedType.LIST) {
    			 return new Interactor(ListOperation.size(binName));
    		}
    		else {
    			return new Interactor(MapOperation.size(binName));
    		}
    	}

    	public long size(Policy policy) {
        	if (policy == null) {
        		policy = new Policy(owningEntry.getReadPolicy());
        	}
        	Interactor interactor = getSizeInteractor();
    		Record record = this.mapper.mClient.operate(null, key, interactor.getOperation());
    		return record.getLong(binName);
    	}
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
