package com.aerospike.mapper.tools.mappers;

import com.aerospike.mapper.tools.ClassCache;
import com.aerospike.mapper.tools.TypeMapper;
import com.aerospike.mapper.tools.configuration.ClassConfig;

import static com.aerospike.client.Value.UseBoolBin;

public class BooleanMapper extends TypeMapper {

    public enum Encoding {
        Numeric,
        Object
    }

    private final ClassConfig classConfig;

    public BooleanMapper(final ClassConfig config) {
        super();
        classConfig = config;
    }

    @Override
    public Object toAerospikeFormat(Object value) {
        final boolean useObjectEncoding;
        if (ClassCache.getInstance().hasClassConfig(Boolean.class)) {
            useObjectEncoding = ClassCache.getInstance()
                    .getClassConfig(Boolean.class)
                    .getBoolEncoding()
                    .equals(Encoding.Object);
        } else {
            useObjectEncoding = false;
        }


        if (value == null) {
            return null;
        }
        if (UseBoolBin && useObjectEncoding) {
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
