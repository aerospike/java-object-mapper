package com.aerospike.mapper.tools.mappers;

import com.aerospike.client.AerospikeException;
import com.aerospike.mapper.tools.TypeMapper;

import java.lang.reflect.Field;

public class EnumMapper extends TypeMapper {

	private final Class<? extends Enum<?>> clazz;
	private final String enumField;
	private final Field enumRequestedField;

	public EnumMapper(Class<? extends Enum<?>> clazz, String enumField) {
		this.clazz = clazz;
		this.enumField = enumField;
		if (!enumField.equals("")) {
			try {
				this.enumRequestedField = clazz.getDeclaredField(enumField);
				this.enumRequestedField.setAccessible(true);
			} catch (NoSuchFieldException e) {
				throw new AerospikeException("Cannot Map requested enum, issue with the requested enumField.");
			}
		} else {
			this.enumRequestedField = null;
		}
	}

	@Override
	public Object toAerospikeFormat(Object value) {
		if (!enumField.equals("")) {
			try {
				return enumRequestedField.get(value).toString();
			} catch (IllegalAccessException e) {
				throw new AerospikeException("Cannot Map requested enum, issue with the requested enumField.");
			}
		}
		return value.toString();
	}

	@Override
	public Object fromAerospikeFormat(Object value) {
		if (value == null) {
			return null;
		}

		String stringValue = (String) value;
		Enum<?>[] constants = clazz.getEnumConstants();

		if (!enumField.equals("")) {
			try {
				for (Enum<?> thisEnum : constants) {
					if (enumRequestedField.get(thisEnum).equals(stringValue)) {
						return thisEnum;
					}
				}
			} catch (IllegalAccessException e) {
				throw new AerospikeException("Cannot Map requested enum, issue with the requested enumField.");
			}
		} else {
			for (Enum<?> thisEnum : constants) {
				if (thisEnum.toString().equals(stringValue)) {
					return thisEnum;
				}
			}
		}
		throw new AerospikeException(String.format("Enum value of \"%s\" not found in type %s", stringValue, clazz.toString()));
	}
}
