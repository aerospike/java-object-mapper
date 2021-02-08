package com.aerospike.mapper.tools;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import javax.validation.constraints.NotNull;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.mapper.annotations.AerospikeVersion;

/**
 * Implementation of a value, which can be either a method on a class (getter) or a field
 * @author timfaulkes
 */
public abstract class ValueType {
	private int minimumVersion = 1;
	private int maximumVersion = Integer.MAX_VALUE;
	private final TypeMapper mapper;
	
	public ValueType(@NotNull final TypeMapper mapper) {
		this.mapper = mapper;
	}
	public abstract Object get(Object obj) throws ReflectiveOperationException;
	public abstract void set(Object obj, Object value) throws ReflectiveOperationException;
	public abstract Class<?> getType();
	public abstract Annotation[] getAnnotations();
	
	public int getMinimumVersion() {return minimumVersion;}
	public int getMaximumVersion() {return maximumVersion;}
	protected void setVersion(AerospikeVersion version) {
		if (version.min() <= 0) {
			throw new IllegalArgumentException("Minimum version must be greater than or equal to 1, not " +version.min());
		}
		if (version.max() <= 0) {
			throw new IllegalArgumentException("Maximum version must be greater than or equal to 1, not " +version.max());
		}
		this.maximumVersion = version.max();
		this.minimumVersion = version.min();
	}
	
	public TypeMapper getTypeMapper() {
		return this.mapper;
	}
	
	
	public static class FieldValue extends ValueType {
		private Field field;
		public FieldValue(Field field, TypeMapper typeMapper) {
			super(typeMapper);
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
		public void set(Object obj, Object value) throws ReflectiveOperationException {
			this.field.set(obj, value);
		}

		@Override
		public Class<?> getType() {
			return this.field.getType();
		}

		@Override
		public Annotation[] getAnnotations() {
			return this.field.getAnnotations();
		}
	}

	public static class MethodValue extends ValueType {
		private PropertyDefinition property;
		
		public MethodValue(PropertyDefinition property, TypeMapper typeMapper) {
			super(typeMapper);
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
		public void set(Object obj, Object value) throws ReflectiveOperationException {
			if (this.property.getSetter() == null) {
				throw new AerospikeException("Lazy loading cannot be used on objects with a property key type and no annotated key setter method");
			}
			else {
				switch (this.property.getSetterParamType()) {
				case KEY:
					Key key = ThreadLocalKeySaver.get();
					this.property.getSetter().invoke(obj, value, key);
					break;
					
				case VALUE:
					key = ThreadLocalKeySaver.get();
					this.property.getSetter().invoke(obj, value, key.userKey);
					break;
					
				default:
					this.property.getSetter().invoke(obj, value);
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
	}
}
