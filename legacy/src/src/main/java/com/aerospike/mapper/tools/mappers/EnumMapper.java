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
        if (!enumField.isEmpty()) {
            try {
                this.enumRequestedField = clazz.getDeclaredField(enumField);
                this.enumRequestedField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw toAerospikeException(e);
            }
        } else {
            this.enumRequestedField = null;
        }
    }

    @Override
    public Object toAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }

        if (!enumField.isEmpty()) {
            if (enumRequestedField == null) {
                return null;
            }
            try {
                Object enumValue = enumRequestedField.get(value);
                return enumValue == null ? null : enumValue.toString();
            } catch (IllegalAccessException e) {
                throw toAerospikeException(e);
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

        if (!enumField.isEmpty()) {
            try {
                for (Enum<?> thisEnum : constants) {
                    if (enumRequestedField.get(thisEnum).equals(stringValue)) {
                        return thisEnum;
                    }
                }
            } catch (IllegalAccessException e) {
                throw toAerospikeException(e);
            }
        } else {
            for (Enum<?> thisEnum : constants) {
                if (thisEnum.toString().equals(stringValue)) {
                    return thisEnum;
                }
            }
        }
        throw new AerospikeException(String.format("Enum value of \"%s\" not found in type %s", stringValue, clazz));
    }

    private AerospikeException toAerospikeException(Exception e) {
        return new AerospikeException("Cannot Map requested enum, issue with the requested enumField.", e);
    }
}
