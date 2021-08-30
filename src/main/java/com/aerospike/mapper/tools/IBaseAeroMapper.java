package com.aerospike.mapper.tools;

import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.mapper.tools.converters.MappingConverter;

public interface IBaseAeroMapper {

    MappingConverter getMappingConverter();

    IAeroMapper asMapper();

    /**
     * Return the read policy to be used for the passed class. This is a convenience method only and should rarely be needed
     *
     * @param clazz - the class to return the read policy for.
     * @return - the appropriate read policy. If none is set, the client's readPolicyDefault is returned.
     */
    Policy getReadPolicy(Class<?> clazz);

    /**
     * Return the write policy to be used for the passed class. This is a convenience method only and should rarely be needed
     *
     * @param clazz - the class to return the write policy for.
     * @return - the appropriate write policy. If none is set, the client's writePolicyDefault is returned.
     */
    WritePolicy getWritePolicy(Class<?> clazz);

    /**
     * Return the batch policy to be used for the passed class. This is a convenience method only and should rarely be needed
     *
     * @param clazz - the class to return the batch policy for.
     * @return - the appropriate batch policy. If none is set, the client's batchPolicyDefault is returned.
     */
    BatchPolicy getBatchPolicy(Class<?> clazz);

    /**
     * Return the scan policy to be used for the passed class. This is a convenience method only and should rarely be needed
     *
     * @param clazz - the class to return the scan policy for.
     * @return - the appropriate scan policy. If none is set, the client's scanPolicyDefault is returned.
     */
    ScanPolicy getScanPolicy(Class<?> clazz);

    /**
     * Return the query policy to be used for the passed class. This is a convenience method only and should rarely be needed
     *
     * @param clazz - the class to return the query policy for.
     * @return - the appropriate query policy. If none is set, the client's queryPolicyDefault is returned.
     */
    QueryPolicy getQueryPolicy(Class<?> clazz);
}
