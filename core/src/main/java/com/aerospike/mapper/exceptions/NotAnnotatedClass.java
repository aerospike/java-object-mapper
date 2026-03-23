package com.aerospike.mapper.exceptions;

public class NotAnnotatedClass extends AerospikeMapperException {

    private static final long serialVersionUID = -4781097961894057707L;
    public static final int REASON_CODE = -109;

    public NotAnnotatedClass(String description) {
        super(description);
    }
}
