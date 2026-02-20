package com.aerospike.mapper.tools;

public class ConfigurationUtils {
    public static boolean validateFieldOnClass(Class<?> clazz, String fieldName) {
        try {
            clazz.getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException nsfe) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null) {
                return validateFieldOnClass(superclass, fieldName);
            }
        }
        return false;
    }
}
