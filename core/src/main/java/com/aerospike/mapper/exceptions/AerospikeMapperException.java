package com.aerospike.mapper.exceptions;

/**
 * Base unchecked exception for Aerospike Object Mapper errors.
 */
public class AerospikeMapperException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AerospikeMapperException(String message) {
        super(message);
    }

    public AerospikeMapperException(String message, Throwable cause) {
        super(message, cause);
    }

    public AerospikeMapperException(Throwable cause) {
        super(cause);
    }
}
