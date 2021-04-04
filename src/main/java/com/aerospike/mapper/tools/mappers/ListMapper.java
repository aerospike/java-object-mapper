package com.aerospike.mapper.tools.mappers;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.aerospike.client.AerospikeException;
import com.aerospike.mapper.annotations.AerospikeReference;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.mapper.tools.ClassCache;
import com.aerospike.mapper.tools.ClassCacheEntry;
import com.aerospike.mapper.tools.DeferredObjectLoader;
import com.aerospike.mapper.tools.DeferredObjectLoader.DeferredObject;
import com.aerospike.mapper.tools.DeferredObjectLoader.DeferredObjectSetter;
import com.aerospike.mapper.tools.DeferredObjectLoader.DeferredSetter;
import com.aerospike.mapper.tools.TypeMapper;
import com.aerospike.mapper.tools.TypeUtils;
import com.aerospike.mapper.tools.TypeUtils.AnnotatedType;

public class ListMapper extends TypeMapper {

	@SuppressWarnings("unused")
	private final Class<?> referencedClass;
	private final Class<?> instanceClass;
	private final AeroMapper mapper;
	private final boolean supportedWithoutTranslation;
	private final TypeMapper instanceClassMapper;
	private final EmbedType embedType;
	private final ClassCacheEntry<?> subTypeEntry;
	private final boolean saveKey;
	private final boolean allowBatchLoad;
	
	public ListMapper(final Class<?> clazz, final Class<?> instanceClass, final TypeMapper instanceClassMapper, final AeroMapper mapper, final EmbedType embedType, final boolean saveKey, boolean allowBatchLoad) {
		this.referencedClass = clazz;
		this.mapper = mapper;
		this.instanceClass = instanceClass;
		this.supportedWithoutTranslation = TypeUtils.isAerospikeNativeType(instanceClass);
		this.instanceClassMapper = instanceClassMapper;
		this.saveKey = saveKey;
		this.allowBatchLoad = allowBatchLoad;
		
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
			if (instanceClass != null) {
				subTypeEntry = ClassCache.getInstance().loadClass(instanceClass, mapper);
			}
			else {
				subTypeEntry = null;
			}
		}
	}
	
	public Object toAerospikeInstanceFormat(Object obj) {
		if (embedType == null || embedType == EmbedType.LIST) {
			if (instanceClass == null) {
			// We don't have any hints as to how to translate them, we have to look up each type
				if (obj == null) {
					return null;
				}
				else {
					TypeMapper thisMapper = TypeUtils.getMapper(obj.getClass(), null, mapper);
					return thisMapper == null ? obj : thisMapper.toAerospikeFormat(obj, true, false);
				}
			}
			else {
				if (obj == null || obj.getClass().equals(instanceClass)) {
					return this.instanceClassMapper.toAerospikeFormat(obj);
				}
				else {
					// This class must be a subclass of the annotated type
					return this.instanceClassMapper.toAerospikeFormat(obj, false, true);
				}
			}
		}
		else {
			Object key = subTypeEntry.getKey(obj);
			Object item;
			if (obj == null || obj.getClass().equals(instanceClass)) {
				item = this.instanceClassMapper.toAerospikeFormat(obj);
			}
			else {
				// This class must be a subclass of the annotated type
				item = this.instanceClassMapper.toAerospikeFormat(obj, false, true);
			}
			return new AbstractMap.SimpleEntry<Object, Object>(key, item);
		}
	}
	
	
	@Override
	public Object toAerospikeFormat(Object value) {
		if (value == null) {
			return null;
		}
		List<?> list = (List<?>)value;
		if (embedType == null || embedType == EmbedType.LIST) {
			List<Object> results = new ArrayList<>();
			for (Object obj : list) {
				results.add(this.toAerospikeInstanceFormat(obj));
			}
			return results;
		}
		else {
			Map<Object, Object> results = new TreeMap<>();
			for (Object obj : list) {
				Object key = subTypeEntry.getKey(obj);
				Object item;
				if (obj == null || obj.getClass().equals(instanceClass)) {
					item = this.instanceClassMapper.toAerospikeFormat(obj);
				}
				else {
					// This class must be a subclass of the annotated type
					item = this.instanceClassMapper.toAerospikeFormat(obj, false, true);
				}
				results.put(key, item);
			}
			return results;

		}
	}

	private Class<?> getClassToUse(Object obj) {
		if (List.class.isAssignableFrom(obj.getClass())) {
			List<Object> list = (List<Object>) obj;
			int lastElementIndex = list.size()-1;
			if ((!list.isEmpty()) && (list.get(lastElementIndex) instanceof String)) {
				String lastElement = (String)list.get(lastElementIndex);
				if (lastElement.startsWith(ClassCacheEntry.TYPE_PREFIX)) {
					String className = lastElement.substring(ClassCacheEntry.TYPE_PREFIX.length());
					ClassCacheEntry<?> thisClass = ClassCache.getInstance().getCacheEntryFromStoredName(className);
					if (thisClass != null) {
						return thisClass.getUnderlyingClass();
					}
				}
			}
		}
		return obj.getClass();
	}
	
	public Object fromAerospikeInstanceFormat(Object obj) {
		if (embedType == null || embedType == EmbedType.LIST) {
			if (instanceClass == null) {
			// We don't have any hints as to how to translate them, we have to look up each type
				if (obj == null) {
					return null;
				}
				else {
					TypeMapper thisMapper = TypeUtils.getMapper(getClassToUse(obj), AnnotatedType.getDefaultAnnotateType(), mapper);
					return thisMapper == null ? obj : thisMapper.fromAerospikeFormat(obj);
				}
			}
			else {
				return this.instanceClassMapper.fromAerospikeFormat(obj);
			}
		}
		else {
			Entry<Object, Object> entry = (Entry<Object, Object>) obj;
			Object result = this.instanceClassMapper.fromAerospikeFormat(entry.getValue());
			subTypeEntry.setKey(result, entry.getKey());
			return result;
		}
	}
	

	@Override
	public Object fromAerospikeFormat(Object value) {
		if (value == null) {
			return null;
		}
		List<Object> results = new ArrayList<>();
		if (embedType == null || embedType == EmbedType.LIST) {
			List<?> list = (List<?>)value;
			if (list.size() == 0 || this.supportedWithoutTranslation) {
				return value;
			}

			if (instanceClass == null) {
				// We don't have any hints as to how to translate them, we have to look up each type
				int index = 0;
				for (Object obj : list) {
					if (obj == null) {
						results.add(null);
					}
					else {
						TypeMapper thisMapper = TypeUtils.getMapper(getClassToUse(obj), AnnotatedType.getDefaultAnnotateType(), mapper);
						Object result = thisMapper == null ? obj : thisMapper.fromAerospikeFormat(obj);
						if (result instanceof DeferredObject) {
							final int thisIndex = index;
							DeferredSetter setter = new DeferredSetter() {
								@Override
								public void setValue(Object object) {
									results.set(thisIndex, object);
								}
							};
							DeferredObjectSetter objectSetter = new DeferredObjectSetter(setter, (DeferredObject)result);
							DeferredObjectLoader.add(objectSetter);
							// add a placeholder to maintain the index
							results.add(null);
						}
						else {
							results.add(result);
						}
					}
					index++;
				}
			}
			else {
				int index = 0;
				for (Object obj : list) {
					if (!allowBatchLoad) {
						results.add(this.instanceClassMapper.fromAerospikeFormat(obj));
					}
					else {
						Object result = this.instanceClassMapper.fromAerospikeFormat(obj);
						if (result instanceof DeferredObject) {
							final int thisIndex = index;
							DeferredSetter setter = new DeferredSetter() {
								@Override
								public void setValue(Object object) {
									results.set(thisIndex, object);
								}
							};
							DeferredObjectSetter objectSetter = new DeferredObjectSetter(setter, (DeferredObject)result);
							DeferredObjectLoader.add(objectSetter);
							// add a placeholder to maintain the index
							results.add(null);
						}
						else {
							results.add(result);
						}
					}
					index++;
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
