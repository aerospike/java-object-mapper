package com.aerospike.mapper.tools.mappers;

import java.util.Map;
import java.util.TreeMap;

import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.mapper.tools.TypeMapper;
import com.aerospike.mapper.tools.TypeUtils;
import com.aerospike.mapper.tools.TypeUtils.AnnotatedType;

public class MapMapper implements TypeMapper {

	@SuppressWarnings("unused")
	private final Class<?> referencedClass;
	@SuppressWarnings("unused")
	private final Class<?> itemClass;
	@SuppressWarnings("unused")
	private final Class<?> keyClass;
	private final AeroMapper mapper;
	private final boolean supportedWithoutTranslation;
	private final TypeMapper itemMapper;
	private final TypeMapper keyMapper;
	
	public MapMapper(final Class<?> clazz, final Class<?> keyClass, final Class<?> itemClass, 
			final TypeMapper keyMapper, final TypeMapper itemMapper, final AeroMapper mapper) {
		this.referencedClass = clazz;
		this.mapper = mapper;
		this.keyMapper = keyMapper;
		this.keyClass = keyClass;
		this.itemMapper = itemMapper;
		this.itemClass = itemClass;
		this.supportedWithoutTranslation = TypeUtils.isAerospikeNativeType(itemClass) && TypeUtils.isAerospikeNativeType(keyClass);
	}
	
	@Override
	public Object toAerospikeFormat(Object value) {
		if (value == null) {
			return null;
		}
		Map<?, ?> map = (Map<?, ?>)value;
		if (map.size() == 0 || this.supportedWithoutTranslation) {
			return value;
		}
		
		Map<Object, Object> results = new TreeMap<>();
		for (Object key : map.keySet()) {
			Object item = map.get(key);
			
			TypeMapper keyMap = keyMapper != null ? keyMapper : TypeUtils.getMapper(key.getClass(), AnnotatedType.getDefaultAnnotateType(), mapper);
			TypeMapper itemMap = itemMapper != null ? itemMapper : TypeUtils.getMapper(item.getClass(), AnnotatedType.getDefaultAnnotateType(), mapper);
			results.put(keyMap.toAerospikeFormat(key), itemMap.toAerospikeFormat(item));
		}
		return results;
	}

	@Override
	public Object fromAerospikeFormat(Object value) {
		if (value == null) {
			return null;
		}
		Map<?, ?> map = (Map<?, ?>)value;
		if (map.size() == 0 || this.supportedWithoutTranslation) {
			return value;
		}
		
		Map<Object, Object> results = new TreeMap<>();
		for (Object key : map.keySet()) {
			Object item = map.get(key);
			
			TypeMapper keyMap = keyMapper != null ? keyMapper : TypeUtils.getMapper(key.getClass(), AnnotatedType.getDefaultAnnotateType(), mapper);
			TypeMapper itemMap = itemMapper != null ? itemMapper : TypeUtils.getMapper(item.getClass(), AnnotatedType.getDefaultAnnotateType(), mapper);
			results.put(keyMap.fromAerospikeFormat(key), itemMap.fromAerospikeFormat(item));
		}
		return results;
	}
}
