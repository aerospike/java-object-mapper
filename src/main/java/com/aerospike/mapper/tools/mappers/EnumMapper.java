package com.aerospike.mapper.tools.mappers;

import com.aerospike.client.AerospikeException;
import com.aerospike.mapper.tools.TypeMapper;

import java.lang.reflect.Field;

public class EnumMapper extends TypeMapper {

	private final Class<? extends Enum<?>> clazz;
	private final String enumField;

	public EnumMapper(Class<? extends Enum<?>> clazz, String enumField) {
		this.clazz = clazz;
		this.enumField = enumField;
	}

	@Override
	public Object toAerospikeFormat(Object value) {
		if (!enumField.equals("")) {
			try {
				Field enumRequestedField = clazz.getDeclaredField(enumField);
				enumRequestedField.setAccessible(true);
				return enumRequestedField.get(value).toString();
			} catch (NoSuchFieldException | IllegalAccessException e) {
				throw new AerospikeException("Cannot Map requested enum, issue with requested field.");
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
				Field enumRequestedField = clazz.getDeclaredField(enumField);
				enumRequestedField.setAccessible(true);
				for (Enum<?> thisEnum : constants) {
					if (enumRequestedField.get(thisEnum).equals(stringValue)) {
						return thisEnum;
					}
				}
			} catch (NoSuchFieldException | IllegalAccessException e) {
				throw new AerospikeException("Cannot Map requested enum, issue with requested field.");
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
