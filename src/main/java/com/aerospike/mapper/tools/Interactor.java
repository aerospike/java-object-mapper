package com.aerospike.mapper.tools;

import javax.validation.constraints.NotNull;

import com.aerospike.client.Operation;

public class Interactor {
	private final Operation operation;
	private final ResultsUnpacker []resultsUnpackers;

	public Interactor(@NotNull Operation operation, @NotNull ResultsUnpacker ... resultsUnpackers) {
		super();
		this.operation = operation;
		this.resultsUnpackers = resultsUnpackers;
	}
	public Operation getOperation() {
		return operation;
	}
	public Object getResult(Object rawResult) {
		Object result = rawResult;
		for (ResultsUnpacker thisUnpacker : resultsUnpackers) {
			result = thisUnpacker.unpack(result);
		}
		return result;
	}
	public boolean isWriteOperation() {
		switch (operation.type) {
		case ADD:
		case APPEND:
		case BIT_MODIFY:
		case CDT_MODIFY:
		case DELETE:
		case MAP_MODIFY:
		case PREPEND:
		case TOUCH:
		case WRITE:
			return true;
		default:
			return false;
		}
	}
}