package com.aerospike.mapper.tools;

import com.aerospike.mapper.annotations.AerospikeVersion;
import com.aerospike.mapper.exceptions.AerospikeMapperException;
import com.aerospike.mapper.tools.DeferredObjectLoader.DeferredObject;
import com.aerospike.mapper.tools.DeferredObjectLoader.DeferredObjectSetter;
import com.aerospike.mapper.tools.DeferredObjectLoader.DeferredSetter;
import com.aerospike.mapper.tools.utils.TypeUtils.AnnotatedType;
import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Implementation of a value, which can be either a method on a class (getter) or a field
 *
 * @author timfaulkes
 */
public abstract class ValueType {
    @Getter
    private int minimumVersion = 1;
    @Getter
    private int maximumVersion = Integer.MAX_VALUE;
    private final TypeMapper mapper;
    @Getter
    private final AnnotatedType annotatedType;

    public ValueType(@NotNull final TypeMapper mapper, final AnnotatedType annotatedType) {
        this.mapper = mapper;
        this.annotatedType = annotatedType;
    }

    public abstract Object get(Object obj) throws ReflectiveOperationException;

    public abstract void set(Object obj, Object value) throws ReflectiveOperationException;

    public abstract Class<?> getType();

    public abstract Annotation[] getAnnotations();

    protected void setVersion(AerospikeVersion version) {
        if (version.min() <= 0) {
            throw new AerospikeMapperException("Minimum version must be greater than or equal to 1, not " + version.min());
        }
        if (version.max() <= 0) {
            throw new AerospikeMapperException("Maximum version must be greater than or equal to 1, not " + version.max());
        }
        if (version.min() > version.max()) {
            throw new AerospikeMapperException("Maximum version must be greater than or equal to the minumum version, not " + version.max());
        }
        this.maximumVersion = version.max();
        this.minimumVersion = version.min();
    }

    public TypeMapper getTypeMapper() {
        return this.mapper;
    }

    public static class FieldValue extends ValueType {
        private final Field field;

        public FieldValue(Field field, TypeMapper typeMapper, AnnotatedType annotatedType) {
            super(typeMapper, annotatedType);
            this.field = field;
            this.field.setAccessible(true);
            if (this.field.isAnnotationPresent(AerospikeVersion.class)) {
                AerospikeVersion version = this.field.getAnnotation(AerospikeVersion.class);
                super.setVersion(version);
            }
        }

        @Override
        public Object get(Object obj) throws ReflectiveOperationException {
            return this.field.get(obj);
        }

        @Override
        public void set(final Object obj, final Object value) throws ReflectiveOperationException {
            if (value instanceof DeferredObject) {
                DeferredSetter setter = object -> {
                    try {
                        field.set(obj, object);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new AerospikeMapperException(String.format("Could not set field %s on %s to %s. Error is %s (%s)", field, obj, value, e.getMessage(), e.getClass()));
                    }
                };
                DeferredObjectSetter objectSetter = new DeferredObjectSetter(setter, (DeferredObject) value);
                DeferredObjectLoader.add(objectSetter);
            } else {
                this.field.set(obj, value);
            }
        }

        @Override
        public Class<?> getType() {
            return this.field.getType();
        }

        @Override
        public Annotation[] getAnnotations() {
            return this.field.getAnnotations();
        }

        @Override
        public String toString() {
            return String.format("Value(Field): %s (%s)", this.field.getName(), this.field.getType().getSimpleName());
        }
    }

    public static class MethodValue extends ValueType {
        private final PropertyDefinition property;

        public MethodValue(PropertyDefinition property, TypeMapper typeMapper, AnnotatedType annotatedType) {
            super(typeMapper, annotatedType);
            this.property = property;
        }

        @Override
        public Object get(Object obj) throws ReflectiveOperationException {
            if (obj == null) {
                return null;
            }
            return this.property.getGetter().invoke(obj);
        }

        @Override
        public void set(final Object obj, final Object value) throws ReflectiveOperationException {
            if (this.property.getSetter() == null) {
                throw new AerospikeMapperException("Lazy loading cannot be used on objects with a property key type and no annotated key setter method");
            } else {
                switch (this.property.getSetterParamType()) {
                    case KEY: {
                        final Object key = ThreadLocalKeySaver.getKeyContext();
                        if (value instanceof DeferredObject) {
                            DeferredSetter setter = resolved -> {
                                try {
                                    property.getSetter().invoke(obj, resolved, key);
                                } catch (ReflectiveOperationException e) {
                                    throw new AerospikeMapperException(String.format("Could not set field %s on %s to" +
                                        " %s", property, obj, resolved));
                                }
                            };
                            DeferredObjectSetter objectSetter = new DeferredObjectSetter(setter, (DeferredObject) value);
                            DeferredObjectLoader.add(objectSetter);
                        } else {
                            this.property.getSetter().invoke(obj, value, key);
                        }
                        break;
                    }

                    case VALUE: {
                        final Object userKeyValue = ThreadLocalKeySaver.getUserKeyValue();
                        if (value instanceof DeferredObject) {
                            DeferredSetter setter = resolved -> {
                                try {
                                    property.getSetter().invoke(obj, resolved, userKeyValue);
                                } catch (ReflectiveOperationException e) {
                                    throw new AerospikeMapperException(String.format("Could not set field %s on %s to" +
                                        " %s", property, obj, resolved));
                                }
                            };
                            DeferredObjectSetter objectSetter = new DeferredObjectSetter(setter, (DeferredObject) value);
                            DeferredObjectLoader.add(objectSetter);
                        } else {
                            this.property.getSetter().invoke(obj, value, ThreadLocalKeySaver.getUserKeyValue());
                        }
                        break;
                    }

                    default:
                        if (value instanceof DeferredObject) {
                            DeferredSetter setter = resolved -> {
                                try {
                                    property.getSetter().invoke(obj, resolved);
                                } catch (ReflectiveOperationException e) {
                                    throw new AerospikeMapperException(String.format("Could not set field %s on %s to" +
                                        " %s", property, obj, resolved));
                                }
                            };
                            DeferredObjectSetter objectSetter = new DeferredObjectSetter(setter, (DeferredObject) value);
                            DeferredObjectLoader.add(objectSetter);
                        } else {
                            this.property.getSetter().invoke(obj, value);
                        }
                }
            }
        }

        @Override
        public Class<?> getType() {
            return this.property.getType();
        }

        @Override
        public Annotation[] getAnnotations() {
            return this.property.getAnnotations();
        }

        @Override
        public String toString() {
            return String.format("Value(Method): %s/%s (%s)", this.property.getGetter(), this.property.getSetter(), this.property.getType().getSimpleName());
        }
    }
}
