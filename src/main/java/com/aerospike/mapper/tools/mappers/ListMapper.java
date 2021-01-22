package com.aerospike.mapper.tools.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.aerospike.client.AerospikeException;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.mapper.tools.ClassCache;
import com.aerospike.mapper.tools.ClassCacheEntry;
import com.aerospike.mapper.tools.TypeMapper;
import com.aerospike.mapper.tools.TypeUtils;

public class ListMapper implements TypeMapper {

	@SuppressWarnings("unused")
	private final Class<?> referencedClass;
	private final Class<?> instanceClass;
	private final AeroMapper mapper;
	private final boolean supportedWithoutTranslation;
	private final TypeMapper instanceClassMapper;
	private final EmbedType embedType;
	private final ClassCacheEntry subTypeEntry;
	
	public ListMapper(final Class<?> clazz, final Class<?> instanceClass, final TypeMapper instanceClassMapper, final AeroMapper mapper, final EmbedType embedType) {
		this.referencedClass = clazz;
		this.mapper = mapper;
		this.instanceClass = instanceClass;
		this.supportedWithoutTranslation = TypeUtils.isAerospikeNativeType(instanceClass);
		this.instanceClassMapper = instanceClassMapper;
		if (embedType == EmbedType.DEFAULT) {
			this.embedType = EmbedType.LIST;
		}
		else {
			this.embedType = embedType;
		}
		if (this.embedType == EmbedType.MAP && (instanceClassMapper == null || (!ObjectMapper.class.isAssignableFrom(instanceClassMapper.getClass())))) {
			subTypeEntry = null;
			// TODO: Should this throw an exception or just change the embedType back to LIST?
			throw new AerospikeException("Annotations embedding lists of objects can only map those objects to maps instead of lists if the object is an AerospikeRecord on instance of class " + clazz.getSimpleName());
		}
		else {
			subTypeEntry = ClassCache.getInstance().loadClass(instanceClass, mapper);
		}
	}
	
	@Override
	public Object toAerospikeFormat(Object value) {
		if (value == null) {
			return null;
		}
		List<?> list = (List<?>)value;
		if (list.size() == 0 || this.supportedWithoutTranslation) {
			return value;
		}
		if (embedType == null || embedType == EmbedType.LIST) {
			List<Object> results = new ArrayList<>();
			if (instanceClass == null) {
				// We don't have any hints as to how to translate them, we have to look up each type
				for (Object obj : list) {
					if (obj == null) {
						results.add(null);
					}
					else {
						TypeMapper thisMapper = TypeUtils.getMapper(obj.getClass(), null, null, mapper);
						results.add(thisMapper == null ? obj : thisMapper.toAerospikeFormat(obj));
					}
				}
			}
			else {
				for (Object obj : list) {
					// TODO: Handle subclasses of the objects
					results.add(this.instanceClassMapper.toAerospikeFormat(obj));
				}
			}
			return results;
		}
		else {
			Map<Object, Object> results = new TreeMap<>();
			for (Object obj : list) {
				// TODO: Handle subclasses of the objects
				// TODO: The key will potentially get stored twice here, once as the key and once in the object. Optimise this.
				Object key = subTypeEntry.getKey(obj);
				Object item = this.instanceClassMapper.toAerospikeFormat(obj);
				results.put(key, item);
			}
			return results;

		}
	}

	@Override
	public Object fromAerospikeFormat(Object value) {
		if (value == null) {
			return null;
		}
		List<Object> results = new ArrayList<>();
		if (embedType == EmbedType.DEFAULT || embedType == EmbedType.LIST) {
			List<?> list = (List<?>)value;
			if (list.size() == 0 || this.supportedWithoutTranslation) {
				return value;
			}

			if (instanceClass == null) {
				// We don't have any hints as to how to translate them, we have to look up each type
				for (Object obj : list) {
					if (obj == null) {
						results.add(null);
					}
					else {
						TypeMapper thisMapper = TypeUtils.getMapper(obj.getClass(), null, null, mapper);
						results.add(thisMapper == null ? obj : thisMapper.fromAerospikeFormat(obj));
					}
				}
			}
			else {
				for (Object obj : list) {
					results.add(this.instanceClassMapper.fromAerospikeFormat(obj));
				}
			}
		}
		else {
			Map<?, ?> map = (Map<?,?>)value;
			for (Object key : map.keySet()) {
				Object item = map.get(key);
				
				Object result = this.instanceClassMapper.fromAerospikeFormat(item);
				subTypeEntry.setKey(result, key);
				results.add(result);
			}
		}
		return results;
	}
}
