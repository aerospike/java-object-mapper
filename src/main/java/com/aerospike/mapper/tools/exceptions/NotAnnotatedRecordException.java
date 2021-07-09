package com.aerospike.mapper.tools.exceptions;

import com.aerospike.client.AerospikeException;

public class NotAnnotatedRecordException extends AerospikeException {
	private static final long serialVersionUID = 6218131114335403885L;

	public NotAnnotatedRecordException(String message) {
		super(message);
	}
}
