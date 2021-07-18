package com.aerospike.mapper.exceptions;

import com.aerospike.client.AerospikeException;

public class NotAnnotatedClass extends AerospikeException {

	private static final long serialVersionUID = -4781097961894057707L;
	public static final int REASON_CODE = -109;
	public NotAnnotatedClass(String description) {
		super(REASON_CODE, description);
	}

}
