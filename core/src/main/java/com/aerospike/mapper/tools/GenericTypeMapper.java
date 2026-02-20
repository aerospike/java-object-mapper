package com.aerospike.mapper.tools;

import com.aerospike.mapper.exceptions.AerospikeMapperException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.aerospike.mapper.annotations.FromAerospike;
import com.aerospike.mapper.annotations.ToAerospike;
import com.aerospike.mapper.tools.utils.TypeUtils;
import lombok.Getter;

public class GenericTypeMapper extends TypeMapper {
    @Getter
    private final Class<?> mappedClass;
    private final Object converter;
    private Method toAerospike;
    private Method fromAerospike;

    public GenericTypeMapper(Object converter) {
        for (Method method : converter.getClass().getMethods()) {
            if (method.isAnnotationPresent(ToAerospike.class)) {
                if (toAerospike != null) {
                    throw new AerospikeMapperException(String.format("Multiple methods annotated with @ToAerospike: %s, %s",
                            toAerospike.toGenericString(), method.toGenericString()));
                }
                toAerospike = method;
            }
            if (method.isAnnotationPresent(FromAerospike.class)) {
                if (fromAerospike != null) {
                    throw new AerospikeMapperException(String.format("Multiple methods annotated with @FromAerospike: %s, %s",
                            fromAerospike.toGenericString(), method.toGenericString()));
                }
                fromAerospike = method;
            }
        }
        this.converter = converter;
        mappedClass = validateAndGetClass();
    }

    @Override
    public Object toAerospikeFormat(Object value) {
        try {
            return this.toAerospike.invoke(converter, value);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new AerospikeMapperException(e);
        }
    }

    @Override
    public Object fromAerospikeFormat(Object value) {
        try {
            return this.fromAerospike.invoke(converter, value);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new AerospikeMapperException(e);
        }
    }

    public Class<?> validateAndGetClass() {
        if (this.toAerospike == null) {
            throw new AerospikeMapperException(String.format("Converter class %s must have a @ToAerospike annotated method.", this.converter.getClass()));
        }
        if (this.toAerospike.getParameterCount() != 1) {
            throw new AerospikeMapperException(String.format("@ToAerospike method on Converter class %s must take 1 argument", this.converter.getClass()));
        }
        if (TypeUtils.isVoidType(this.toAerospike.getReturnType())) {
            throw new AerospikeMapperException(String.format("@ToAerospike method on Converter class %s cannot return void", this.converter.getClass()));
        }
        this.toAerospike.setAccessible(true);

        if (this.fromAerospike == null) {
            throw new AerospikeMapperException(String.format("Converter class %s must have a @FromAerospike annotated method.", this.converter.getClass()));
        }
        if (this.fromAerospike.getParameterCount() != 1) {
            throw new AerospikeMapperException(String.format("@FromAerospike method on Converter class %s must take 1 argument", this.converter.getClass()));
        }
        if (TypeUtils.isVoidType(this.fromAerospike.getReturnType())) {
            throw new AerospikeMapperException(String.format("@FromAerospike method on Converter class %s cannot return void", this.converter.getClass()));
        }
        this.fromAerospike.setAccessible(true);

        if (!this.toAerospike.getParameters()[0].getType().equals(this.fromAerospike.getReturnType())) {
            throw new AerospikeMapperException(String.format("@FromAerospike method on Converter class %s returns %s, but the @ToAerospike method takes %s. These should be the same class",
                    this.converter.getClass().getSimpleName(), this.fromAerospike.getReturnType().getSimpleName(), this.toAerospike.getParameters()[0].getType().getSimpleName()));
        }
        if (!this.fromAerospike.getParameters()[0].getType().equals(this.toAerospike.getReturnType())) {
            throw new AerospikeMapperException(String.format("@ToAerospike method on Converter class %s returns %s, but the @FromAerospike method takes %s. These should be the same class",
                    this.converter.getClass().getSimpleName(), this.toAerospike.getReturnType().getSimpleName(), this.fromAerospike.getParameters()[0].getType().getSimpleName()));
        }
        // We need to return the Java type, which is the result of the FromAerospike
        return this.fromAerospike.getReturnType();
    }
}
