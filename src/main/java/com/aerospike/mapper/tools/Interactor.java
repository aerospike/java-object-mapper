package com.aerospike.mapper.tools;

import javax.validation.constraints.NotNull;

import com.aerospike.client.Operation;

public class Interactor {
	private Operation operation;
	private DeferredOperation deferredOperation;
	private final OperationParameters deferredParameters;
	private ResultsUnpacker []resultsUnpackers;

	public Interactor(@NotNull Operation operation, @NotNull ResultsUnpacker... resultsUnpackers) {
		super();
		this.operation = operation;
		this.resultsUnpackers = resultsUnpackers;
		this.deferredParameters = null;
	}
	public Interactor(@NotNull DeferredOperation deferredOperation) {
		super();
		this.deferredOperation = deferredOperation;
		this.deferredParameters = new OperationParameters();
	}
	
	public void setNeedsResultOfType(ReturnType returnType) {
		if (this.deferredParameters != null) {
			this.deferredParameters.setNeedsResultOfType(returnType);
		}
	}
	public Operation getOperation() {
		if (operation == null && deferredOperation != null) {
			operation = deferredOperation.getOperation(this.deferredParameters);
			resultsUnpackers = deferredOperation.getUnpackers(this.deferredParameters);
		}
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
		if (this.operation != null) {
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
		else {
			return !this.deferredOperation.isGetOperation();
		}
	}
}