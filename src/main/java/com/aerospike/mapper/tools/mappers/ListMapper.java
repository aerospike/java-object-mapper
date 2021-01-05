package com.aerospike.mapper.tools.mappers;

import java.util.ArrayList;
import java.util.List;

import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.mapper.tools.TypeMapper;
import com.aerospike.mapper.tools.TypeUtils;

public class ListMapper implements TypeMapper {

	@SuppressWarnings("unused")
	private final Class<?> referencedClass;
	private final Class<?> instanceClass;
	private final AeroMapper mapper;
	private final boolean supportedWithoutTranslation;
	private final TypeMapper instanceClassMapper;
	
	public ListMapper(final Class<?> clazz, final Class<?> instanceClass, final TypeMapper instanceClassMapper, final AeroMapper mapper) {
		this.referencedClass = clazz;
		this.mapper = mapper;
		this.instanceClass = instanceClass;
		this.supportedWithoutTranslation = TypeUtils.isAerospikeNativeType(instanceClass);
		this.instanceClassMapper = instanceClassMapper;
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
				results.add(this.instanceClassMapper.toAerospikeFormat(obj));
			}
		}
		return results;
	}

	@Override
	public Object fromAerospikeFormat(Object value) {
		if (value == null) {
			return null;
		}
		List<?> list = (List<?>)value;
		if (list.size() == 0 || this.supportedWithoutTranslation) {
			return value;
		}

		List<Object> results = new ArrayList<>();
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
		return results;
	}
}
