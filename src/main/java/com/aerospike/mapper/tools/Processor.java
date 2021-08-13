package com.aerospike.mapper.tools;

public interface Processor<T> {
    /**
     * Process the given record.
     * @param data - the record to be processed
     * @return true if further records should be processed, false if the processing loop should abort.
     */
    boolean process(T data);
}
