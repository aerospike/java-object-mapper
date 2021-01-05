package com.aerospike.mapper.tools.mappers;

import com.aerospike.client.AerospikeException;
import com.aerospike.mapper.tools.TypeMapper;

public class EnumMapper implements TypeMapper {

	private Class<? extends Enum<?>> clazz;
	
	public EnumMapper(Class<? extends Enum<?>> clazz) {
		this.clazz = clazz;
	}
	@Override
	public Object toAerospikeFormat(Object value) {
		return ((Enum<?>)value).toString();
	}

	@Override
	public Object fromAerospikeFormat(Object value) {
		if (value == null) {
			return null;
		}
		String stringValue = (String)value;
		Enum<?>[] constants=clazz.getEnumConstants();
		for (Enum<?> thisEnum : constants) {
			if (thisEnum.toString().equals(stringValue)) {
				return thisEnum;
			}
		}
		
		throw new AerospikeException(String.format("Enum value of \"%s\" not found in type %s", stringValue, clazz.toString()));
	}

}
