package com.aerospike.mapper.tools;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.aerospike.client.AerospikeException;
import com.aerospike.mapper.tools.TypeUtils.AnnotatedType;
import com.aerospike.mapper.tools.configuration.ClassConfig;

public class PropertyDefinition {
	private Method getter;
	private Method setter;
	private String name;
	private Class<?> clazz;
	private TypeMapper typeMapper;
	private AeroMapper mapper;
	
	public PropertyDefinition(String name, AeroMapper mapper) {
		this.name = name;
		this.mapper = mapper;
	}
	public Method getGetter() {
		return getter;
	}
	public void setGetter(Method getter) {
		this.getter = getter;
	}
	public Method getSetter() {
		return setter;
	}
	public void setSetter(Method setter) {
		this.setter = setter;
	}
	/**
	 * Get the type of this property. The getter and setter must agree on the property and this method
	 * is only valid after the <code>validate</code> method has been called.
	 * @return
	 */
	public Class<?> getType() {
		return this.clazz;
	}
	
	public TypeMapper getTypeMapper() {
		return typeMapper;
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
			throw new AerospikeException(String.format("Property %s on class %s must have a getter", this.name, className));
		}
		if (getter.getParameterCount() != 0) {
			throw new AerospikeException(String.format("Getter for property %s on class %s must take 0 arguments", this.name, className));
		}
		Class<?> getterClazz = getter.getReturnType();
		if (TypeUtils.isVoidType(getterClazz)) {
			throw new AerospikeException(String.format("Getter for property %s on class %s cannot return void", this.name, className));
		}
		this.getter.setAccessible(true);

		Class<?> setterClazz = null;
		if (this.setter != null || !allowNoSetter) {
			if (this.setter == null) {
				throw new AerospikeException(String.format("Property %s on class %s must have a setter", this.name, className));
			}
			if (!TypeUtils.isVoidType(setter.getReturnType())) {
				throw new AerospikeException(String.format("Setter for property %s on class %s must return void", this.name, className));
			}
			if (setter.getParameterCount() != 1) {
				throw new AerospikeException(String.format("Setter for property %s on class %s must take 1 argument", this.name, className));
			}
			setterClazz = setter.getParameterTypes()[0];
			this.setter.setAccessible(true);
		}
		
		if (setterClazz != null && !getterClazz.equals(setterClazz)) {
			throw new AerospikeException(String.format("Getter (%s) and setter (%s) for property %s on class %s differ in type", getterClazz.getName(), setterClazz.getName(), this.name, className));
		}
		this.clazz = getterClazz;
		
		this.typeMapper = TypeUtils.getMapper(clazz, new AnnotatedType(config, getter), this.mapper);
	}
}
