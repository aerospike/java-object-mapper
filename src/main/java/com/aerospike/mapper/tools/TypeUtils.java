package com.aerospike.mapper.tools;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aerospike.client.AerospikeException;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.annotations.AerospikeReference.ReferenceType;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeReference;
import com.aerospike.mapper.tools.mappers.ArrayMapper;
import com.aerospike.mapper.tools.mappers.BooleanMapper;
import com.aerospike.mapper.tools.mappers.ByteMapper;
import com.aerospike.mapper.tools.mappers.DateMapper;
import com.aerospike.mapper.tools.mappers.DefaultMapper;
import com.aerospike.mapper.tools.mappers.EnumMapper;
import com.aerospike.mapper.tools.mappers.FloatMapper;
import com.aerospike.mapper.tools.mappers.IntMapper;
import com.aerospike.mapper.tools.mappers.ListMapper;
import com.aerospike.mapper.tools.mappers.ObjectEmbedMapper;
import com.aerospike.mapper.tools.mappers.ObjectReferenceMapper;
import com.aerospike.mapper.tools.mappers.ShortMapper;

public class TypeUtils {
	private static Map<Class<?>, TypeMapper> mappers = new HashMap<>();
	
	@SuppressWarnings("unchecked")
	private static TypeMapper getMapper(Class<?> clazz, Type instanceType, Annotation[] annotations, AeroMapper mapper, boolean isForSubType) {
		TypeMapper typeMapper = mappers.get(clazz);
		boolean addToMap = true;
		if (typeMapper == null) {
			if (Date.class.isAssignableFrom(clazz)) {
				typeMapper = new DateMapper();
			}
			else if (Byte.class.isAssignableFrom(clazz) || Byte.TYPE.isAssignableFrom(clazz)) {
				typeMapper = new ByteMapper();
			}
			else if (Short.class.isAssignableFrom(clazz) || Short.TYPE.isAssignableFrom(clazz)) {
				typeMapper = new ShortMapper();
			}
			else if (Integer.class.isAssignableFrom(clazz) || Integer.TYPE.isAssignableFrom(clazz)) {
				typeMapper = new IntMapper();
			}
			else if (Boolean.class.isAssignableFrom(clazz) || Boolean.TYPE.isAssignableFrom(clazz)) {
				typeMapper = new BooleanMapper();
			}
			else if (Float.class.isAssignableFrom(clazz) || Float.TYPE.isAssignableFrom(clazz)) {
				typeMapper = new FloatMapper();
			}
			else if (clazz.isEnum()) {
				typeMapper = new EnumMapper((Class<? extends Enum<?>>) clazz);
			}
			else if (clazz.isArray()) {
				Class<?> elementType = clazz.getComponentType();
				if (isByteType(elementType)) {
					// Byte arrays are natively supported
					typeMapper = new DefaultMapper();
				}
				else {
					TypeMapper subMapper = getMapper(elementType, instanceType, annotations, mapper, true);
					typeMapper = new ArrayMapper(elementType, subMapper);
				}
			}
			else if (Map.class.isAssignableFrom(clazz)) {
				if (instanceType instanceof ParameterizedType) {
					ParameterizedType paramType = (ParameterizedType)instanceType;
					Type[] types = paramType.getActualTypeArguments();
					System.out.println(instanceType);
					for (int i = 0; i < types.length; i++) {
						Class<?> clzz = (Class<?>)types[i];
						System.out.printf("   %d - class = %s, typename = %s\n", i, clzz.getName(), types[i].getTypeName());
					}
					
				}
				else {
					System.out.println("non parameterized map - " + instanceType);
				}
				addToMap = false;
			}
			else if (List.class.isAssignableFrom(clazz)) {
				if (instanceType instanceof ParameterizedType) {
					ParameterizedType paramType = (ParameterizedType)instanceType;
					Type[] types = paramType.getActualTypeArguments();
					if (types.length != 1) {
						throw new AerospikeException(String.format("Type %s is a parameterized type as expected, but has %d type parameters, not the expected 1", clazz.getName(), types.length));
					}
					
					Class<?> subClazz = (Class<?>)types[0];
					TypeMapper subMapper = getMapper(subClazz, instanceType, annotations, mapper, true);
					typeMapper = new ListMapper(clazz, subClazz, subMapper,  mapper);
				}
				else {
					System.out.println("non parameterized map - " + instanceType);
					typeMapper = new ListMapper(clazz, null, null, mapper);
				}
				addToMap = false;
			}
			else if (clazz.isAnnotationPresent(AerospikeRecord.class)) {
				for (Annotation annotation : annotations) {
					if (annotation.annotationType().equals(AerospikeReference.class)) {
						AerospikeReference ref = (AerospikeReference)annotation;
						typeMapper = new ObjectReferenceMapper(clazz, ref.lazy(), ref.type(), mapper);
						addToMap = false;
						break;
					}
					if (annotation.annotationType().equals(AerospikeEmbed.class)) {
						AerospikeEmbed embed = (AerospikeEmbed)annotation;
						EmbedType type = isForSubType ? embed.elementType() : embed.type();
						typeMapper = new ObjectEmbedMapper(clazz, type, mapper);
						addToMap = false;
						break;
					}
				}
				if (typeMapper == null) {
					// No annotations were specified, so use the ObjectReferenceMapper with non-lazy references
					typeMapper = new ObjectReferenceMapper(clazz, false, ReferenceType.ID, mapper);
					addToMap = false;
				}
			}
			if (typeMapper == null) {
				typeMapper = new DefaultMapper();
			}
			if (addToMap) {
				mappers.put(clazz, typeMapper);
			}
		}
		return typeMapper;
	}

	public static TypeMapper getMapper(Class<?> clazz, Type instanceType, Annotation[] annotations, AeroMapper mapper) {
		return getMapper(clazz, instanceType, annotations, mapper, false);
	}


	public static boolean isByteType(Class<?> clazz) {
		return  Byte.class.equals(clazz) ||
				Byte.TYPE.equals(clazz);
	}

	public static boolean isVoidType(Class<?> clazz) {
		return clazz == null ||
				Void.class.equals(clazz) ||
				Void.TYPE.equals(clazz);
	}

	public static boolean isAerospikeNativeType(Class<?> clazz) {
		if (clazz == null) {
			return false;
		}
		return Integer.TYPE.equals(clazz) ||
				Integer.class.equals(clazz) ||
				Long.TYPE.equals(clazz) ||
				Long.class.equals(clazz) ||
				Short.TYPE.equals(clazz) ||
				Short.class.equals(clazz) ||
				Byte.TYPE.equals(clazz) ||
				Byte.class.equals(clazz) ||
				String.class.equals(clazz);
	}
}
