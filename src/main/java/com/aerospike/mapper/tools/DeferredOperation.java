package com.aerospike.mapper.tools;

import com.aerospike.client.Operation;

public interface DeferredOperation {
	public Operation getOperation(OperationParameters operationParams);
	public ResultsUnpacker[] getUnpackers(OperationParameters operationParams);
}
