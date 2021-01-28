package com.aerospike.mapper.tools.mappers;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Value;
import com.aerospike.client.util.Crypto;
import com.aerospike.mapper.annotations.AerospikeReference.ReferenceType;
import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.mapper.tools.ClassCacheEntry;
import com.aerospike.mapper.tools.TypeMapper;

public class ObjectReferenceMapper extends ObjectMapper implements TypeMapper {

	private final ClassCacheEntry referencedClass;
	private final AeroMapper mapper;
	private final boolean lazy;
	private final ReferenceType type;
	
	public ObjectReferenceMapper(ClassCacheEntry entry, boolean lazy, ReferenceType type, AeroMapper mapper) {
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
		if (value == null) {
			return null;
		}
		// In this case we want to store a reference to the object.
		Object key = referencedClass.getKey(value);
		if (ReferenceType.DIGEST.equals(type)) {
			key = Crypto.computeDigest(this.referencedClass.getSetName(), Value.get(key));
		}
		return key;
	}

	@Override
	public Object fromAerospikeFormat(Object value) {
		// The object should be the primary key of the referencing object
		if (value == null) {
			return null;
		}
		if (this.lazy) {
			Object instance;
			try {
				instance = referencedClass.getUnderlyingClass().newInstance();
				referencedClass.setKey(instance, value);
				return instance;
			} catch (InstantiationException | IllegalAccessException e) {
				throw new AerospikeException(e);
			}
		}
		else {
			if (ReferenceType.DIGEST.equals(type)) {
				return mapper.readFromDigest(referencedClass.getUnderlyingClass(), (byte[]) value);
			}
			else {
				return mapper.read(referencedClass.getUnderlyingClass(), value);
			}
		}
	}

}
