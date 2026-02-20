package com.aerospike.mapper.tools;

import com.aerospike.mapper.exceptions.AerospikeMapperException;
import com.aerospike.mapper.tools.configuration.ClassConfig;
import com.aerospike.mapper.tools.utils.TypeUtils;
import com.aerospike.mapper.tools.utils.TypeUtils.AnnotatedType;

import lombok.Getter;
import lombok.Setter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

public class PropertyDefinition {

    public enum SetterParamType {
        NONE,
        KEY,
        VALUE
    }

    private final String name;
    private final IObjectMapper mapper;
    @Getter @Setter
    private Method getter;
    @Getter @Setter
    private Method setter;
    private Class<?> clazz;
    @Getter
    private TypeMapper typeMapper;
    @Getter
    private SetterParamType setterParamType = SetterParamType.NONE;

    public PropertyDefinition(String name, IObjectMapper mapper) {
        this.name = name;
        this.mapper = mapper;
    }

    /**
     * Get the type of this property. The getter and setter must agree on the property and this method
     * is only valid after the <code>validate</code> method has been called.
     */
    public Class<?> getType() {
        return this.clazz;
    }

    public Annotation[] getAnnotations() {
        return getter != null ? getter.getAnnotations() : setter.getAnnotations();
    }

    public Type getGenericType() {
        return this.getter.getGenericReturnType();
    }

    /**
     * Validate that this is a valid property
     */
    public void validate(String className, ClassConfig config, boolean allowNoSetter) {
        if (this.getter == null) {
            throw new AerospikeMapperException(String.format("Property %s on class %s must have a getter", this.name, className));
        }
        if (getter.getParameterCount() != 0) {
            throw new AerospikeMapperException(String.format("Getter for property %s on class %s must take 0 arguments", this.name, className));
        }
        Class<?> getterClazz = getter.getReturnType();
        if (TypeUtils.isVoidType(getterClazz)) {
            throw new AerospikeMapperException(String.format("Getter for property %s on class %s cannot return void", this.name, className));
        }
        this.getter.setAccessible(true);

        Class<?> setterClazz = null;
        if (this.setter != null || !allowNoSetter) {
            if (this.setter == null) {
                throw new AerospikeMapperException(String.format("Property %s on class %s must have a setter", this.name, className));
            }

            if (setter.getParameterCount() == 2) {
                Parameter param = setter.getParameters()[1];
                String paramTypeName = param.getType().getName();
                if ("com.aerospike.client.Key".equals(paramTypeName)) {
                    this.setterParamType = SetterParamType.KEY;
                } else if ("com.aerospike.client.Value".equals(paramTypeName)) {
                    this.setterParamType = SetterParamType.VALUE;
                } else {
                    throw new AerospikeMapperException(String.format("Property %s on class %s has a setter with 2 arguments," +
                            " but the second one is neither a Key nor a Value", this.name, className));
                }
            } else if (setter.getParameterCount() != 1) {
                throw new AerospikeMapperException(String.format("Setter for property %s on class %s must take 1 or 2 arguments",
                        this.name, className));
            }
            setterClazz = setter.getParameterTypes()[0];
            this.setter.setAccessible(true);
        }

        if (setterClazz != null && !getterClazz.equals(setterClazz)) {
            throw new AerospikeMapperException(String.format("Getter (%s) and setter (%s) for property %s on class %s differ in type",
                    getterClazz.getName(), setterClazz.getName(), this.name, className));
        }
        this.clazz = getterClazz;

        this.typeMapper = TypeUtils.getMapper(clazz, new AnnotatedType(config, getter), this.mapper);
    }
}
