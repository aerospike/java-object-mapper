package com.aerospike.mapper.tools;

public class OperationParameters {
	private boolean needsResult;
	
	public OperationParameters() {
	}
	public OperationParameters(boolean needsResult) {
		super();
		this.needsResult = needsResult;
	}
	public boolean needsResult() {
		return needsResult;
	}
	public void setNeedsResult(boolean needsResult) {
		this.needsResult = needsResult;
	}
}
