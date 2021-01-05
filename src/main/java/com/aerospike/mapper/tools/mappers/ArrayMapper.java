package com.aerospike.mapper.tools.mappers;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import com.aerospike.mapper.tools.TypeMapper;
import com.aerospike.mapper.tools.TypeUtils;

public class ArrayMapper implements TypeMapper {

	private final Class<?> instanceClass;
	private final boolean supportedWithoutTranslation;
	private final TypeMapper instanceClassMapper;
	
	public ArrayMapper(final Class<?> instanceClass, final TypeMapper instanceClassMapper) {
		this.instanceClass = instanceClass;
		this.supportedWithoutTranslation = TypeUtils.isByteType(instanceClass);
		this.instanceClassMapper = instanceClassMapper;
	}
	
	@Override
	public Object toAerospikeFormat(Object value) {
		if (value == null) {
			return null;
		}
		int length = Array.getLength(value);
		if (this.supportedWithoutTranslation) {
			return value;
		}
		
		List<Object> results = new ArrayList<>();
		for (int i = 0; i < length; i++) {
			results.add(this.instanceClassMapper.toAerospikeFormat(Array.get(value, i)));
		}
		return results;
	}

	@Override
	public Object fromAerospikeFormat(Object value) {
		if (value == null) {
			return null;
		}
		List<?> list = (List<?>)value;
		if (this.supportedWithoutTranslation) {
			return value;
		}

		Object result = Array.newInstance(instanceClass, list.size());
		for (int i = 0; i < list.size(); i++) {
			Array.set(result, i, this.instanceClassMapper.fromAerospikeFormat(list.get(i)));
		}
		return result;
	}
}
