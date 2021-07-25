package com.aerospike.mapper.tools;

import com.aerospike.client.policy.BatchPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.mapper.tools.converters.MappingConverter;

public interface IBaseAeroMapper {
    Policy getReadPolicy(Class<?> clazz);

    WritePolicy getWritePolicy(Class<?> clazz);

    BatchPolicy getBatchPolicy(Class<?> clazz);

    ScanPolicy getScanPolicy(Class<?> clazz);

    Policy getQueryPolicy(Class<?> clazz);

    IAeroMapper asMapper();

    MappingConverter getMappingConverter();
}
