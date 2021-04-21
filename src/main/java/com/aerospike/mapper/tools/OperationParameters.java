package com.aerospike.mapper.tools;

public class OperationParameters {
	private ReturnType needsResultOfType = ReturnType.NONE;
	
	public OperationParameters() {
	}

	public OperationParameters(ReturnType needsResultOfType) {
		super();
		this.needsResultOfType = needsResultOfType;
	}

	public ReturnType getNeedsResultOfType() {
		return needsResultOfType;
	}

	public void setNeedsResultOfType(ReturnType needsResultOfType) {
		this.needsResultOfType = needsResultOfType;
	}
}
