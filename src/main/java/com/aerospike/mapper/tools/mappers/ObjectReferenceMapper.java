package com.aerospike.mapper.tools.mappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Value;
import com.aerospike.client.util.Crypto;
import com.aerospike.mapper.annotations.AerospikeReference.ReferenceType;
import com.aerospike.mapper.tools.*;
import com.aerospike.mapper.tools.DeferredObjectLoader.DeferredObject;

public class ObjectReferenceMapper extends ObjectMapper {

    // Package visibility
    private final ClassCacheEntry<?> referencedClass;
    private final IBaseAeroMapper mapper;
    private final boolean lazy;
    private final boolean allowBatch;
    private final ReferenceType type;

    public ObjectReferenceMapper(ClassCacheEntry<?> entry, boolean lazy, boolean allowBatch,
                                 ReferenceType type, IBaseAeroMapper mapper) {
        this.referencedClass = entry;
        this.mapper = mapper;
        this.lazy = lazy;
        this.type = type;
        this.allowBatch = allowBatch;

        if (ReferenceType.DIGEST.equals(this.type) && this.lazy) {
            throw new AerospikeException("An object reference to a " + entry.getClass().getSimpleName()
                    + " cannot be both lazy and map to a digest");
        }
    }

    @Override
    public Object toAerospikeFormat(Object value) {
        return toAerospikeFormat(value, false, false);
    }

    @Override
    public Object toAerospikeFormat(Object value, boolean isUnknownType, boolean isSubclassOfKnownType) {
        if (value == null) {
            return null;
        }
        // In this case we want to store a reference to the object.
        ClassCacheEntry<?> classToUse;
        if (value.getClass().equals(referencedClass.getUnderlyingClass())) {
            classToUse = referencedClass;
        } else {
            classToUse = ClassCache.getInstance().loadClass(value.getClass(), mapper);
            isSubclassOfKnownType = true;
        }
        Object key = classToUse.getKey(value);
        if (ReferenceType.DIGEST.equals(type)) {
            key = Crypto.computeDigest(classToUse.getSetName(), Value.get(key));
        }
        if (isSubclassOfKnownType || isUnknownType) {
            // Need to put the class name in the key so we can recreate the class
            List<Object> keyParts = new ArrayList<>();
            keyParts.add(key);
            if (isUnknownType) {
                // Must put in an identifier to mark this as an unknown type
                keyParts.add(ClassCacheEntry.TYPE_PREFIX + classToUse.getShortenedClassName());
            } else {
                keyParts.add(classToUse.getShortenedClassName());
            }
            return keyParts;
        }
        return key;
    }

    @Override
    public Object fromAerospikeFormat(Object value) {
        // The object should be the primary key of the referencing object
        if (value == null) {
            return null;
        }
        ClassCacheEntry<?> classToUse = referencedClass;

        Object key;
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            key = list.get(0);
            String typeName = (String) list.get(1);
            if (typeName.startsWith(ClassCacheEntry.TYPE_PREFIX)) {
                typeName = typeName.substring(ClassCacheEntry.TYPE_PREFIX.length());
            }
            classToUse = ClassCache.getInstance().getCacheEntryFromStoredName(typeName);
        } else {
            key = value;
        }

        if (this.lazy) {
            Map<String, Object> map = new HashMap<>();
            Object instance = classToUse.constructAndHydrate(map);
            classToUse.setKey(instance, key);
            return instance;
        } else if (allowBatch) {
            return new DeferredObject(key, classToUse.getUnderlyingClass(), ReferenceType.DIGEST.equals(type));
        } else if (ReferenceType.DIGEST.equals(type)) {
            return mapper.asMapper().readFromDigest(classToUse.getUnderlyingClass(), (byte[]) key, false);
        } else {
            return mapper.asMapper().read(classToUse.getUnderlyingClass(), key, false);
        }
    }
}
