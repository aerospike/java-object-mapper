package com.aerospike.mapper.tools;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aerospike.client.AerospikeException;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeReference;
import com.aerospike.mapper.annotations.AerospikeReference.ReferenceType;
import com.aerospike.mapper.tools.mappers.ArrayMapper;
import com.aerospike.mapper.tools.mappers.BooleanMapper;
import com.aerospike.mapper.tools.mappers.ByteMapper;
import com.aerospike.mapper.tools.mappers.DateMapper;
import com.aerospike.mapper.tools.mappers.DefaultMapper;
import com.aerospike.mapper.tools.mappers.EnumMapper;
import com.aerospike.mapper.tools.mappers.FloatMapper;
import com.aerospike.mapper.tools.mappers.IntMapper;
import com.aerospike.mapper.tools.mappers.ListMapper;
import com.aerospike.mapper.tools.mappers.MapMapper;
import com.aerospike.mapper.tools.mappers.ObjectEmbedMapper;
import com.aerospike.mapper.tools.mappers.ObjectReferenceMapper;
import com.aerospike.mapper.tools.mappers.ShortMapper;

public class TypeUtils {
	private static Map<Class<?>, TypeMapper> mappers = new HashMap<>();
	
	// package visibility
	/**
	 * This method adds a new type mapper into the system. This type mapper will replace any other mappers
	 * registered for the same class. If there was another mapper for the same type already registered,
	 * the old mapper will be replaced with this mapper and the old mapper returned.
	 * @param clazz
	 * @param mapper
	 * @return
	 */
	static TypeMapper addTypeMapper(Class<?> clazz, TypeMapper mapper) {
		TypeMapper returnValue = mappers.get(clazz);
		mappers.put(clazz, mapper);
		return returnValue;
	}
	
	@SuppressWarnings("unchecked")
	private static TypeMapper getMapper(Class<?> clazz, Type instanceType, Annotation[] annotations, AeroMapper mapper, boolean isForSubType) {
		if (clazz == null) {
			return null;
		}
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
					if (types.length != 2) {
						throw new AerospikeException(String.format("Type %s is a parameterized type as expected, but has %d type parameters, not the expected 2", clazz.getName(), types.length));
					}
					
					Class<?> keyClazz = (Class<?>)types[0];
					Class<?> itemClazz = (Class<?>)types[1];
					TypeMapper keyMapper = getMapper(keyClazz, instanceType, annotations, mapper, true);
					TypeMapper itemMapper = getMapper(itemClazz, instanceType, annotations, mapper, true);
					typeMapper = new MapMapper(clazz, keyClazz, itemClazz, keyMapper, itemMapper,  mapper);

				}
				else {
					typeMapper = new MapMapper(clazz, null, null, null, null, mapper);
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
					typeMapper = new ListMapper(clazz, null, null, mapper);
				}
				addToMap = false;
			}
			else if (clazz.isAnnotationPresent(AerospikeRecord.class)) {
				for (Annotation annotation : annotations) {
					boolean throwError = false;
					if (annotation.annotationType().equals(AerospikeReference.class)) {
						if (typeMapper != null) {
							throwError = true;
						}
						else {
							AerospikeReference ref = (AerospikeReference)annotation;
							typeMapper = new ObjectReferenceMapper(clazz, ref.lazy(), ref.type(), mapper);
							addToMap = false;
						}
					}
					if (annotation.annotationType().equals(AerospikeEmbed.class)) {
						AerospikeEmbed embed = (AerospikeEmbed)annotation;
						if (typeMapper != null) {
							throwError = true;
						}
						else {
							EmbedType type = isForSubType ? embed.elementType() : embed.type();
							typeMapper = new ObjectEmbedMapper(clazz, type, mapper);
							addToMap = false;
						}
					}
					if (throwError) {
						throw new AerospikeException(String.format("A class with a reference to %s specifies multiple annotations for storing the reference", clazz.getName()));
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
				Double.TYPE.equals(clazz) ||
				Double.class.equals(clazz) ||
				String.class.equals(clazz);
	}
}
