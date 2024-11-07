package com.aerospike.mapper.tools.mappers;

import com.aerospike.mapper.tools.ClassCache;
import com.aerospike.mapper.tools.TypeMapper;
import com.aerospike.mapper.tools.configuration.ClassConfig;

import static com.aerospike.client.Value.UseBoolBin;

public class BooleanMapper extends TypeMapper {

    public enum Encoding {
        Numeric,
        Boolean
    }

    private final ClassConfig classConfig;

    public BooleanMapper(final ClassConfig config) {
        super();
        classConfig = config;
    }

    @Override
    public Object toAerospikeFormat(Object value) {
        if (ClassCache.getInstance().hasClassConfig(Boolean.class)) {
            UseBoolBin = ClassCache.getInstance()
                    .getClassConfig(Boolean.class)
                    .getBoolEncoding()
                    .equals(Encoding.Boolean);
        } else {
            UseBoolBin = false;
        }


        if (value == null) {
            return null;
        }
        if (UseBoolBin) {
            return value;
        }
        return ((Boolean) value) ? 1 : 0;
    }

    @Override
    public Object fromAerospikeFormat(Object value) {
        if (value == null) {
            return null;
        }
        if (UseBoolBin) {
            return value;
        }
        return !Long.valueOf(0).equals(value);
    }
}
