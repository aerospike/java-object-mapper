package com.aerospike.mapper.tools.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.cdt.ListReturnType;
import com.aerospike.client.cdt.MapReturnType;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.annotations.AerospikeEnum;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.AerospikeReference;
import com.aerospike.mapper.annotations.AerospikeReference.ReferenceType;
import com.aerospike.mapper.tools.ClassCache;
import com.aerospike.mapper.tools.IBaseAeroMapper;
import com.aerospike.mapper.tools.TypeMapper;
import com.aerospike.mapper.tools.configuration.BinConfig;
import com.aerospike.mapper.tools.configuration.ClassConfig;
import com.aerospike.mapper.tools.configuration.EmbedConfig;
import com.aerospike.mapper.tools.configuration.ReferenceConfig;
import com.aerospike.mapper.tools.mappers.ArrayMapper;
import com.aerospike.mapper.tools.mappers.BigDecimalMapper;
import com.aerospike.mapper.tools.mappers.BigIntegerMapper;
import com.aerospike.mapper.tools.mappers.BooleanMapper;
import com.aerospike.mapper.tools.mappers.ByteMapper;
import com.aerospike.mapper.tools.mappers.CharacterMapper;
import com.aerospike.mapper.tools.mappers.DateMapper;
import com.aerospike.mapper.tools.mappers.DefaultMapper;
import com.aerospike.mapper.tools.mappers.EnumMapper;
import com.aerospike.mapper.tools.mappers.FloatMapper;
import com.aerospike.mapper.tools.mappers.InstantMapper;
import com.aerospike.mapper.tools.mappers.IntMapper;
import com.aerospike.mapper.tools.mappers.ListMapper;
import com.aerospike.mapper.tools.mappers.LocalDateMapper;
import com.aerospike.mapper.tools.mappers.LocalDateTimeMapper;
import com.aerospike.mapper.tools.mappers.LocalTimeMapper;
import com.aerospike.mapper.tools.mappers.MapMapper;
import com.aerospike.mapper.tools.mappers.ObjectEmbedMapper;
import com.aerospike.mapper.tools.mappers.ObjectReferenceMapper;
import com.aerospike.mapper.tools.mappers.ShortMapper;
import com.aerospike.mapper.tools.virtuallist.ReturnType;

public class TypeUtils {

    private static final Map<Class<?>, TypeMapper> mappers = new ConcurrentHashMap<>();

    private TypeUtils() {
    }

    /**
     * This method adds a new type mapper into the system. This type mapper will replace any other mappers
     * registered for the same class. If there was another mapper for the same type already registered,
     * the old mapper will be replaced with this mapper and the old mapper returned.
     *
     * @param clazz  The class to register for the new type mapper.
     * @param mapper The new type mapper to create.
     * @return Return existing mapper registered for the requested class, null in case there isn't one.
     */
    public static TypeMapper addTypeMapper(Class<?> clazz, TypeMapper mapper) {
        TypeMapper returnValue = mappers.get(clazz);
        mappers.put(clazz, mapper);
        return returnValue;
    }

    @SuppressWarnings("unchecked")
    private static TypeMapper getMapper(Class<?> clazz, AnnotatedType type, IBaseAeroMapper mapper, boolean isForSubType) {
        if (clazz == null) {
            return null;
        }
        boolean isEmbedded = type != null && type.annotations != null && Arrays.stream(type.annotations)
                .anyMatch(a -> a.annotationType().isAssignableFrom(AerospikeEmbed.class));

        TypeMapper typeMapper = mappers.get(clazz);
        boolean addToMap = true;
        if (typeMapper == null) {
            if (Date.class.isAssignableFrom(clazz)) {
                typeMapper = new DateMapper(); 
            } else if (LocalDateTime.class.isAssignableFrom(clazz)) {
                typeMapper = new LocalDateTimeMapper();
            } else if (LocalDate.class.isAssignableFrom(clazz)) {
                typeMapper = new LocalDateMapper();
            } else if (LocalTime.class.isAssignableFrom(clazz)) {
                typeMapper = new LocalTimeMapper();
            } else if (Instant.class.isAssignableFrom(clazz)) {
                typeMapper = new InstantMapper();
            } else if (BigInteger.class.isAssignableFrom(clazz)) {
                typeMapper = new BigIntegerMapper();
            } else if (BigDecimal.class.isAssignableFrom(clazz)) {
                typeMapper = new BigDecimalMapper();
            } else if (Byte.class.isAssignableFrom(clazz) || Byte.TYPE.isAssignableFrom(clazz)) {
                typeMapper = new ByteMapper();
            } else if (Character.class.isAssignableFrom(clazz) || Character.TYPE.isAssignableFrom(clazz)) {
                typeMapper = new CharacterMapper();
            } else if (Short.class.isAssignableFrom(clazz) || Short.TYPE.isAssignableFrom(clazz)) {
                typeMapper = new ShortMapper();
            } else if (Integer.class.isAssignableFrom(clazz) || Integer.TYPE.isAssignableFrom(clazz)) {
                typeMapper = new IntMapper();
            } else if (Boolean.class.isAssignableFrom(clazz) || Boolean.TYPE.isAssignableFrom(clazz)) {
                typeMapper = new BooleanMapper();
            } else if (Float.class.isAssignableFrom(clazz) || Float.TYPE.isAssignableFrom(clazz)) {
                typeMapper = new FloatMapper();
            } else if (clazz.isEnum()) {
                String aeroEnumField = "";
                if (type != null && type.getAnnotations() != null) {
                    AerospikeEnum aeroEnum = type.getAnnotation(AerospikeEnum.class);
                    if (aeroEnum != null) {
                        aeroEnumField = aeroEnum.enumField();
                    }
                }
                typeMapper = new EnumMapper((Class<? extends Enum<?>>) clazz, aeroEnumField);
                addToMap = false;
            } else if (clazz.isArray()) {
                Class<?> elementType = clazz.getComponentType();
                if (isByteType(elementType)) {
                    // Byte arrays are natively supported
                    typeMapper = new DefaultMapper();
                } else {
                    // TODO: The type mapped into this type mapper should be the element type
//					ClassConfig config = ClassCache.getInstance().getClassConfig(elementType.getClass());
//					AnnotatedType elementAnnotateType = type.replaceClassConfig(config);
                    boolean allowBatch = true;
                    if (type != null) {
                        AerospikeReference reference = type.getAnnotation(AerospikeReference.class);
                        if (reference != null) {
                            allowBatch = reference.batchLoad();
                        }
                    }
                    TypeMapper subMapper = getMapper(elementType, type, mapper, true);
                    typeMapper = new ArrayMapper(elementType, subMapper, allowBatch);
                    addToMap = false;
                }
            } else if (Map.class.isAssignableFrom(clazz)) {
                if (type.isParameterizedType()) {
                    ParameterizedType paramType = type.getParameterizedType();
                    Type[] types = paramType.getActualTypeArguments();
                    if (types.length != 2) {
                        throw new AerospikeException(String.format("Type %s is a parameterized type as expected, but has %d type parameters, not the expected 2",
                                clazz.getName(), types.length));
                    }

                    Class<?> keyClazz = (Class<?>) types[0];
                    Class<?> itemClazz = (Class<?>) types[1];
                    TypeMapper keyMapper = getMapper(keyClazz, type, mapper, true);
                    TypeMapper itemMapper = getMapper(itemClazz, type, mapper, true);
                    typeMapper = new MapMapper(clazz, keyClazz, itemClazz, keyMapper, itemMapper, mapper);

                } else {
                    typeMapper = new MapMapper(clazz, null, null, null, null, mapper);
                }
                addToMap = false;
            } else if (List.class.isAssignableFrom(clazz)) {
                EmbedType embedType = EmbedType.DEFAULT;
                boolean saveKey = true;
                boolean allowBatch = true;
                if (type != null && type.getAnnotations() != null) {
                    AerospikeEmbed embed = type.getAnnotation(AerospikeEmbed.class);
                    if (embed != null) {
                        embedType = embed.type();
                        saveKey = embed.saveKey();
                    }
                    AerospikeReference reference = type.getAnnotation(AerospikeReference.class);
                    if (reference != null) {
                        allowBatch = reference.batchLoad();
                    }
                }
                BinConfig binConfig = type != null ? type.getBinConfig() : null;
                if (binConfig != null && binConfig.getEmbed() != null) {
                    if (binConfig.getEmbed().getSaveKey() != null) {
                        saveKey = binConfig.getEmbed().getSaveKey();
                    }
                    if (binConfig.getEmbed().getType() != null) {
                        embedType = binConfig.getEmbed().getType();
                    }
                }
                if (binConfig != null && binConfig.getReference() != null) {
                    if (binConfig.getReference().getBatchLoad() != null) {
                        allowBatch = binConfig.getReference().getBatchLoad();
                    }
                }

                if (type != null && type.isParameterizedType()) {
                    ParameterizedType paramType = type.getParameterizedType();
                    Type[] types = paramType.getActualTypeArguments();
                    if (types.length != 1) {
                        throw new AerospikeException(String.format("Type %s is a parameterized type as expected, but has %d type parameters, not the expected 1",
                                clazz.getName(), types.length));
                    }

                    Class<?> subClazz = (Class<?>) types[0];
                    TypeMapper subMapper = getMapper(subClazz, type, mapper, true);
                    typeMapper = new ListMapper(clazz, subClazz, subMapper, mapper, embedType, saveKey, allowBatch);
                } else {
                    typeMapper = new ListMapper(clazz, null, null, mapper, embedType, saveKey, allowBatch);
                }
                addToMap = false;
            } else if (clazz.isAnnotationPresent(AerospikeRecord.class) || ClassCache.getInstance().hasClassConfig(clazz) || isEmbedded) {
                boolean throwError = false;
                if (type != null) {
                    BinConfig binConfig = type.getBinConfig();
                    if (binConfig != null && (binConfig.getEmbed() != null || binConfig.getReference() != null)) {
                        // The config parameters take precedence over the annotations.
                        if (binConfig.getEmbed() != null && binConfig.getReference() != null) {
                            throwError = true;
                        } else if (binConfig.getEmbed() != null) {
                            EmbedConfig embedConfig = binConfig.getEmbed();
                            EmbedType embedType = isForSubType ? embedConfig.getElementType() : embedConfig.getType();
                            if (embedType == null) {
                                embedType = EmbedType.MAP;
                            }
                            boolean saveKey = embedConfig.getSaveKey() != null;
                            boolean skipKey = isForSubType && (embedConfig.getType() == EmbedType.MAP && embedConfig.getElementType() == EmbedType.LIST && (!saveKey));
                            typeMapper = new ObjectEmbedMapper(clazz, embedType, mapper, skipKey);
                            addToMap = false;
                        } else {
                            // Reference
                            ReferenceConfig ref = binConfig.getReference();
                            typeMapper = new ObjectReferenceMapper(ClassCache.getInstance().loadClass(clazz, mapper), ref.getLazy(), ref.getBatchLoad(), ref.getType(), mapper);
                            addToMap = false;
                        }
                    } else {
                        if (type.getAnnotations() != null) {
                            for (Annotation annotation : type.getAnnotations()) {
                                if (annotation.annotationType().equals(AerospikeReference.class)) {
                                    if (typeMapper != null) {
                                        throwError = true;
                                        break;
                                    } else {
                                        AerospikeReference ref = (AerospikeReference) annotation;
                                        typeMapper = new ObjectReferenceMapper(ClassCache.getInstance().loadClass(clazz, mapper), ref.lazy(), ref.batchLoad(), ref.type(), mapper);
                                        addToMap = false;
                                    }
                                }
                                if (annotation.annotationType().equals(AerospikeEmbed.class)) {
                                    AerospikeEmbed embed = (AerospikeEmbed) annotation;
                                    if (typeMapper != null) {
                                        throwError = true;
                                        break;
                                    } else {
                                        EmbedType embedType = isForSubType ? embed.elementType() : embed.type();
                                        boolean skipKey = isForSubType && (embed.type() == EmbedType.MAP && embed.elementType() == EmbedType.LIST && (!embed.saveKey()));
                                        typeMapper = new ObjectEmbedMapper(clazz, embedType, mapper, skipKey);
                                        addToMap = false;
                                    }
                                }
                            }
                        }
                    }
                }
                if (throwError) {
                    throw new AerospikeException(String.format("A class with a reference to %s specifies multiple annotations for storing the reference", clazz.getName()));
                }
                if (typeMapper == null) {
                    // No annotations were specified, so use the ObjectReferenceMapper with non-lazy references
                    typeMapper = new ObjectReferenceMapper(ClassCache.getInstance().loadClass(clazz, mapper), false, true, ReferenceType.ID, mapper);
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

    public static TypeMapper getMapper(Class<?> clazz, AnnotatedType type, IBaseAeroMapper mapper) {
        return getMapper(clazz, type, mapper, false);
    }

    public static boolean isByteType(Class<?> clazz) {
        return Byte.class.equals(clazz) ||
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
        return
                Long.TYPE.equals(clazz) ||
                        Long.class.equals(clazz) ||
                        Double.TYPE.equals(clazz) ||
                        Double.class.equals(clazz) ||
                        String.class.equals(clazz);
    }

    public static void clear() {
        mappers.clear();
    }

    public static int returnTypeToListReturnType(ReturnType returnType) {
        switch (returnType) {
            case DEFAULT:
            case ELEMENTS:
                return ListReturnType.VALUE;
            case COUNT:
                return ListReturnType.COUNT;
            case INDEX:
                return ListReturnType.INDEX;
            case NONE:
            default:
                return ListReturnType.NONE;
        }
    }

    public static int returnTypeToMapReturnType(ReturnType returnType) {
        switch (returnType) {
            case DEFAULT:
            case ELEMENTS:
                return MapReturnType.KEY_VALUE;
            case COUNT:
                return MapReturnType.COUNT;
            case INDEX:
                return MapReturnType.INDEX;
            case NONE:
            default:
                return MapReturnType.NONE;
        }
    }

    public static class AnnotatedType {

        private static final AnnotatedType defaultAnnotatedType = new AnnotatedType(null, null, null);
        private final Annotation[] annotations;
        private final ParameterizedType parameterizedType;
        private final BinConfig binConfig;

        private AnnotatedType(BinConfig binConfig, Type type, Annotation[] annotations) {
            this.binConfig = binConfig;
            this.annotations = annotations;
            if (type instanceof ParameterizedType) {
                this.parameterizedType = (ParameterizedType) type;
            } else {
                this.parameterizedType = null;
            }
        }

        public AnnotatedType(ClassConfig config, Field field) {
            this(config == null ? null : config.getBinByFieldName(field.getName()), field.getGenericType(), field.getAnnotations());
        }

        public AnnotatedType(ClassConfig config, Method getter) {
            this(config == null ? null : config.getBinByGetterName(getter.getName()), getter.getGenericReturnType(), getter.getAnnotations());
        }

        public static AnnotatedType getDefaultAnnotateType() {
            return defaultAnnotatedType;
        }

        public Annotation[] getAnnotations() {
            return annotations;
        }

        public BinConfig getBinConfig() {
            return binConfig;
        }

        public ParameterizedType getParameterizedType() {
            return parameterizedType;
        }

        public boolean isParameterizedType() {
            return parameterizedType != null;
        }

        @SuppressWarnings("unchecked")
        public <T> T getAnnotation(Class<T> clazz) {
            if (this.annotations == null) {
                return null;
            }
            for (Annotation annotation : this.annotations) {
                if (annotation.annotationType().equals(clazz)) {
                    return (T) annotation;
                }
            }
            return null;
        }
    }
}
