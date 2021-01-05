package com.aerospike.mapper.tools;

import java.lang.reflect.Field;
import java.util.function.Function;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;


public class AeroMapper {

	private IAerospikeClient mClient;
	
	public AeroMapper(@NotNull IAerospikeClient client) {
		this.mClient = client;
	}
	
	public void preLoadClass(Class<?> clazz) {
		ClassCache.getInstance().loadClass(clazz, this);
	}

	public void save(@NotNull Object object) throws AerospikeException {
		this.save(null, object);
	}

	public void save(String namespace, @NotNull Object object) throws AerospikeException {
        Class<?> clazz = object.getClass();
        ClassCacheEntry entry = ClassCache.getInstance().loadClass(clazz, this);

    	if (StringUtils.isBlank(namespace)) {
    		namespace = entry.getNamespace();
        	if ( StringUtils.isBlank(namespace) ) {
        		throw new AerospikeException("Namespace not specified in annotation.");
        	}
    	}
    	
    	String set = entry.getSetName();
    	int ttl = entry.getTtl();
    	
		long now = System.nanoTime();
		Key key = new Key(namespace, set, Value.get(entry.getKey(object)));
		
		Bin[] bins = entry.getBins(object);
		System.out.printf("Convert to bins in %,.3fms\n", ((System.nanoTime() - now) / 1_000_000.0));
		
		WritePolicy writePolicy = null;
		if ( ttl != 0 ) {
			writePolicy = new WritePolicy();
			writePolicy.expiration = ttl;
		}
		now = System.nanoTime();
		mClient.put(null, key, bins);
		System.out.printf("Saved to database in %,.3fms\n", ((System.nanoTime() - now) / 1_000_000.0));
	}
	
	
	public <T> T read(@NotNull Class<T> clazz, @NotNull Object userKey) throws AerospikeException {
		return read(clazz, null, userKey);
	}
	
	public <T> T read(@NotNull Class<T> clazz, String namespace, @NotNull Object userKey) throws AerospikeException {
		
        ClassCacheEntry entry = ClassCache.getInstance().loadClass(clazz, this);

    	if (StringUtils.isBlank(namespace)) {
    		namespace = entry.getNamespace();
        	if ( StringUtils.isBlank(namespace) ) {
        		throw new AerospikeException("Namespace not specified in annotation.");
        	}
    	}
    	
    	String set = entry.getSetName();
		Key key = new Key(namespace, set, Value.get(userKey));
		Record record = mClient.get(null, key);
        	
    	if ( record == null ) {
    		return null;
    	} else {
        	try {
        		T result = convertRecord(clazz, record, entry);
            	return result;
        	} catch (ReflectiveOperationException e) {
        		throw new AerospikeException(e);
        	}
    	}
	}
	
	
	public boolean delete(@NotNull Class<?> clazz, @NotNull Object userKey) throws AerospikeException {
		
        if ( clazz.isAnnotationPresent(AerospikeRecord.class) ) {
        	AerospikeRecord recordAnnotation = clazz.getAnnotation(AerospikeRecord.class);

        	String namespace = recordAnnotation.namespace();
        	if ( StringUtils.isBlank(namespace) ) {
        		throw new AerospikeException("Namespace not specified in annotation.");
        	}
        	String set = recordAnnotation.set();

        	Key key = new Key(namespace, set, Value.get(userKey));
        	return mClient.delete(null, key);      	
        } else {
        	throw new AerospikeException("No annotations specified");
        }
	}
	
	
	public boolean delete(@NotNull Class<?> clazz, @NotNull String namespace, @NotNull Object userKey) throws AerospikeException {
		
        if ( clazz.isAnnotationPresent(AerospikeRecord.class) ) {
        	AerospikeRecord recordAnnotation = clazz.getAnnotation(AerospikeRecord.class);

        	if ( StringUtils.isBlank(namespace) ) {
        		throw new AerospikeException("Namespace not specified in annotation.");
        	}
        	String set = recordAnnotation.set();

        	Key key = new Key(namespace, set, Value.get(userKey));
        	return mClient.delete(null, key);      	
        } else {
        	throw new AerospikeException("No annotations specified");
        }
	}
	
	
	public boolean delete(@NotNull Object object) throws AerospikeException {
		
        Class<?> clazz = object.getClass();
        if ( clazz.isAnnotationPresent(AerospikeRecord.class) ) {
        	AerospikeRecord recordAnnotation = clazz.getAnnotation(AerospikeRecord.class);

        	String namespace = recordAnnotation.namespace();
        	if ( StringUtils.isBlank(namespace) ) {
        		throw new AerospikeException("Namespace not specified in annotation.");
        	}
        	
        	String set = recordAnnotation.set();
        	
        	Key key = null;
        	try {
            	for (Field field: clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                
                    if (field.isAnnotationPresent(AerospikeKey.class)) {      	
                    	key = new Key(namespace, set, Value.get(field.get(object)));
                    }
                }
        	} catch (IllegalAccessException e) {
        		throw new AerospikeException(e);
        	}
        	
        	if ( key == null ) {
        		throw new AerospikeException("Null key from annotated object.");
        	} else {
        		return mClient.delete(null, key);	
        	}
        } else {
        	throw new AerospikeException("No annotations specified");
        }
	}
	
	
	public boolean delete(@NotNull String namespace, @NotNull Object object) throws AerospikeException {
		
        Class<?> clazz = object.getClass();
        if ( clazz.isAnnotationPresent(AerospikeRecord.class) ) {
        	AerospikeRecord recordAnnotation = clazz.getAnnotation(AerospikeRecord.class);

        	if ( StringUtils.isBlank(namespace) ) {
        		throw new AerospikeException("Namespace not specified in annotation.");
        	}
        	
        	String set = recordAnnotation.set();
        	
        	Key key = null;
        	try {
            	for (Field field: clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                
                    if (field.isAnnotationPresent(AerospikeKey.class)) {      	
                    	key = new Key(namespace, set, Value.get(field.get(object)));
                    }
                }
        	} catch (IllegalAccessException e) {
        		throw new AerospikeException(e);
        	}
        	
        	if ( key == null ) {
        		throw new AerospikeException("Null key from annotated object.");
        	} else {
        		return mClient.delete(null, key);	
        	}
        } else {
        	throw new AerospikeException("No annotations specified");
        }
	}
	
	
	
	public <T> void find(@NotNull Class<T> clazz, Function<T,Boolean> function) throws AerospikeException {
		this.find(clazz, null, function);
	}
	
	
	public <T> void find(@NotNull Class<T> clazz, String namespace, Function<T,Boolean> function) throws AerospikeException {
		ClassCacheEntry entry = ClassCache.getInstance().loadClass(clazz, this);

    	if (StringUtils.isBlank(namespace)) {
    		namespace = entry.getNamespace();
        	if ( StringUtils.isBlank(namespace) ) {
        		throw new AerospikeException("Namespace not specified in annotation.");
        	}
    	}

    	Statement statement = new Statement();
    	statement.setNamespace(namespace);
    	statement.setSetName(entry.getSetName());
    	
    	RecordSet recordSet = null;
    	try {
        	recordSet = mClient.query(null, statement);
        	T result;
        	while ( recordSet.next() ) {
        		result = clazz.getConstructor().newInstance();
        		entry.hydrateFromRecord(recordSet.getRecord(), result);
        		if  ( !function.apply(result) ) {
        			break;
        		}
        	}  
    	} catch (ReflectiveOperationException e) {
			throw new AerospikeException(e);
		} finally {
    		if ( recordSet != null ) {
    			recordSet.close();
    		}
    	}
        	
	}

	public <T> T convertRecord(Class<T> clazz, Record record) throws ReflectiveOperationException {
		return convertRecord(clazz, record, null);
	}

	public <T> T convertRecord(Class<T> clazz, Record record, ClassCacheEntry entry) throws ReflectiveOperationException {
		if (entry == null) {
			entry = ClassCache.getInstance().loadClass(clazz, this);
		}
		T result = clazz.getConstructor().newInstance();
		entry.hydrateFromRecord(record, result);
    	return result;
	}
}
