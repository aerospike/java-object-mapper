package com.aerospike.mapper.tools.virtuallist;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Value;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.tools.ClassCache;
import com.aerospike.mapper.tools.ClassCacheEntry;
import com.aerospike.mapper.tools.IBaseAeroMapper;
import com.aerospike.mapper.tools.TypeMapper;
import com.aerospike.mapper.tools.ValueType;
import com.aerospike.mapper.tools.mappers.ListMapper;
import com.aerospike.mapper.tools.utils.TypeUtils;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class BaseVirtualList<E> {

    protected final ClassCacheEntry<?> owningEntry;
    protected final String binName;
    protected final ListMapper listMapper;
    protected Key key;
    protected final VirtualListInteractors virtualListInteractors;

    protected BaseVirtualList(@NotNull IBaseAeroMapper mapper, Object object, Class<?> owningClazz, Object key,
                              @NotNull String binName, @NotNull Class<E> clazz) {
        if (object != null) {
            owningClazz = object.getClass();
        }
        this.owningEntry = ClassCache.getInstance().loadClass(owningClazz, mapper);
        Object aerospikeKey;
        if (key == null) {
            aerospikeKey = owningEntry.getKey(object);
        } else {
            aerospikeKey = owningEntry.translateKeyToAerospikeKey(key);
        }

        ClassCacheEntry<?> elementEntry = ClassCache.getInstance().loadClass(clazz, mapper);
        this.binName = binName;
        ValueType value = owningEntry.getValueFromBinName(binName);
        if (value == null) {
            throw new AerospikeException(String.format("Class %s has no bin called %s", clazz.getSimpleName(), binName));
        }
        String set = alignedSet();
        this.key = new Key(owningEntry.getNamespace(), set, Value.get(aerospikeKey));

        TypeUtils.AnnotatedType annotatedType = value.getAnnotatedType();
        AerospikeEmbed embed = annotatedType.getAnnotation(AerospikeEmbed.class);
        if (embed == null) {
            throw new AerospikeException(String.format("Bin %s on class %s is not specified as a embedded",
                    binName, clazz.getSimpleName()));
        }

        AerospikeEmbed.EmbedType listType = embed.type() == AerospikeEmbed.EmbedType.DEFAULT ?
                AerospikeEmbed.EmbedType.LIST : embed.type();
        Class<?> binClazz = value.getType();
        if (!(binClazz.isArray() || (Map.class.isAssignableFrom(binClazz)) || List.class.isAssignableFrom(binClazz))) {
            throw new AerospikeException(String.format("Bin %s on class %s is not a collection class",
                    binName, clazz.getSimpleName()));
        }

        TypeMapper typeMapper = value.getTypeMapper();
        if (typeMapper instanceof ListMapper) {
            listMapper = ((ListMapper) typeMapper);
        } else {
            throw new AerospikeException(String.format(
                    "Bin %s on class %s is not mapped via a listMapper. This is unexpected", binName, clazz.getSimpleName()));
        }

        Function<Object, Object> instanceMapper = listMapper::fromAerospikeInstanceFormat;
        this.virtualListInteractors = new VirtualListInteractors(binName, listType, elementEntry, instanceMapper, mapper);
    }

    protected String alignedSet() {
        String set = owningEntry.getSetName();
        if ("".equals(set)) {
            // Use the null set
            return null;
        }
        return set;
    }

    protected WritePolicy getWritePolicy(Policy policy) {
        if (policy == null) {
            return new WritePolicy(owningEntry.getWritePolicy());
        }
        return new WritePolicy(policy);
    }
}
