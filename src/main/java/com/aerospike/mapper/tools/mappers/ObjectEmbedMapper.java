package com.aerospike.mapper.tools.mappers;

import java.util.List;
import java.util.Map;

import com.aerospike.client.AerospikeException;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.tools.ClassCache;
import com.aerospike.mapper.tools.ClassCacheEntry;
import com.aerospike.mapper.tools.IBaseAeroMapper;

public class ObjectEmbedMapper extends ObjectMapper {

	private final Class<?> referencedClass;
	private final IBaseAeroMapper mapper;
	private final EmbedType type;
	private final boolean skipKey;
	
	public ObjectEmbedMapper(final Class<?> clazz, final EmbedType type, final IBaseAeroMapper mapper, boolean skipKey) {
		this.referencedClass = clazz;
		this.mapper = mapper;
		this.type = type;
		this.skipKey = skipKey;
	}
	
	@Override
	public Object toAerospikeFormat(Object value) {
		if (value == null) {
			return null;
		}
		// In this case we want to store a reference to the object.
		boolean needsType = !(referencedClass.equals(value.getClass()));
		// Use the actual class here in case a sub-class is passed. In that case needsType will be true
		ClassCacheEntry<?> entry = ClassCache.getInstance().loadClass(value.getClass(), mapper);
		switch (type) {
		case LIST:		return entry.getList(value, skipKey, needsType);
		case MAP:		// Fall through
		// If unspecified, default to a MAP for embedded objects
		case DEFAULT:	return entry.getMap(value, needsType);
		default: 		throw new AerospikeException("Unspecified EmbedType");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object fromAerospikeFormat(Object value) {
		if (value == null) {
			return null;
		}
		ClassCacheEntry<?> entry = ClassCache.getInstance().loadClass(referencedClass, mapper);
		try {
			Object instance;
			
			switch (type) {
			case LIST:
				List<Object> listValue = (List<Object>) value;
				instance = entry.constructAndHydrate(listValue, skipKey);
				break;
			case MAP:	// Fall through
			case DEFAULT:
				instance = entry.constructAndHydrate((Map<String,Object>)value);
				break;
			default:
				throw new AerospikeException("Unspecified EmbedType");
			}
			return instance;
		} catch (Exception e) {
			throw new AerospikeException(e);
		}
	}
}
