package com.aerospike.mapper.tools;

import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;

public interface IBaseAeroMapper {
    Policy getReadPolicy(Class<?> clazz);

    WritePolicy getWritePolicy(Class<?> clazz);

    BatchPolicy getBatchPolicy(Class<?> clazz);

    ScanPolicy getScanPolicy(Class<?> clazz);

    Policy getQueryPolicy(Class<?> clazz);

    IAeroMapper asMapper();
}
