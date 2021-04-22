package com.aerospike.mapper.tools.mappers;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import com.aerospike.mapper.tools.DeferredObjectLoader;
import com.aerospike.mapper.tools.TypeMapper;
import com.aerospike.mapper.tools.TypeUtils;
import com.aerospike.mapper.tools.DeferredObjectLoader.DeferredObject;
import com.aerospike.mapper.tools.DeferredObjectLoader.DeferredObjectSetter;
import com.aerospike.mapper.tools.DeferredObjectLoader.DeferredSetter;

public class ArrayMapper extends TypeMapper {

	private final Class<?> instanceClass;
	private final boolean supportedWithoutTranslation;
	private final TypeMapper instanceClassMapper;
	private final Boolean allowBatch;
	
	public ArrayMapper(final Class<?> instanceClass, final TypeMapper instanceClassMapper, final boolean allowBatch) {
		this.instanceClass = instanceClass;
		this.supportedWithoutTranslation = TypeUtils.isByteType(instanceClass);
		this.instanceClassMapper = instanceClassMapper;
		this.allowBatch = allowBatch;
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
			
			Object obj = list.get(i);
			Object item = this.instanceClassMapper.fromAerospikeFormat(obj);
			if (!allowBatch || (!(item instanceof DeferredObject)) ){
				Array.set(result, i, item);
			}
			else {
				final int thisIndex = i;
				DeferredSetter setter = object -> Array.set(result, thisIndex, object);
				DeferredObjectLoader.add(new DeferredObjectSetter(setter, (DeferredObject)item));
			}
		}
		return result;
	}
}
