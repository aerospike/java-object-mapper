package com.aerospike.mapper.tools.mappers;

import java.util.ArrayList;
import java.util.List;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Value;
import com.aerospike.client.util.Crypto;
import com.aerospike.mapper.annotations.AerospikeReference.ReferenceType;
import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.mapper.tools.ClassCache;
import com.aerospike.mapper.tools.ClassCacheEntry;
import com.aerospike.mapper.tools.TypeMapper;

public class ObjectReferenceMapper extends ObjectMapper {

	private final ClassCacheEntry<?> referencedClass;
	private final AeroMapper mapper;
	private final boolean lazy;
	private final ReferenceType type;
	
	public ObjectReferenceMapper(ClassCacheEntry<?> entry, boolean lazy, ReferenceType type, AeroMapper mapper) {
		this.referencedClass = entry;
		this.mapper = mapper;
		this.lazy = lazy;
		this.type = type;
		
		if (ReferenceType.DIGEST.equals(this.type) && this.lazy) {
			throw new AerospikeException("An object reference to a " + entry.getClass().getSimpleName() + " cannot be both lazy and map to a digest");
		}
	}
	
	@Override
	public Object toAerospikeFormat(Object value) {
		return toAerospikeFormat(value, true);
	}
	
	@Override
	public Object toAerospikeFormat(Object value, boolean expectedType) {
		if (value == null) {
			return null;
		}
		// In this case we want to store a reference to the object.
		ClassCacheEntry<?> classToUse;
		if (value.getClass().equals(referencedClass.getUnderlyingClass())) {
			classToUse = referencedClass;
		}
		else {
			classToUse = ClassCache.getInstance().loadClass(value.getClass(), mapper);
			expectedType = false;
		}
		Object key = classToUse.getKey(value);
		if (ReferenceType.DIGEST.equals(type)) {
			key = Crypto.computeDigest(classToUse.getSetName(), Value.get(key));
		}
		if (/*classToUse.isChildClass() || */ !expectedType) {
			// Need to put the class name in the key so we can recreate the class
			List<Object> keyParts = new ArrayList<>();
			keyParts.add(key);
			keyParts.add(classToUse.getShortenedClassName());
			return keyParts;
		}
		return key;
	}

	@Override
	public Object fromAerospikeFormat(Object value) {
		// The object should be the primary key of the referencing object
		if (value == null) {
			return null;
		}
		ClassCacheEntry<?> classToUse = referencedClass;
		
		Object key;
		if (value instanceof List) {
			List<?> list = (List<?>)value;
			key = list.get(0);
			classToUse = ClassCache.getInstance().getCacheEntryFromStoredName((String)list.get(1));
		}
		else {
			key = value;
		}
		
		if (this.lazy) {
			Object instance;
			try {
				instance = classToUse.getUnderlyingClass().newInstance();
				classToUse.setKey(instance, key);
				return instance;
			} catch (InstantiationException | IllegalAccessException e) {
				throw new AerospikeException(e);
			}
		}
		else {
			if (ReferenceType.DIGEST.equals(type)) {
				return mapper.readFromDigest(classToUse.getUnderlyingClass(), (byte[]) key);
			}
			else {
				return mapper.read(classToUse.getUnderlyingClass(), key);
			}
		}
	}

}
