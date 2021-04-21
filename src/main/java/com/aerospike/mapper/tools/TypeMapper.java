package com.aerospike.mapper.tools;

public abstract class TypeMapper {
	public abstract Object toAerospikeFormat(Object value);
	public abstract Object fromAerospikeFormat(Object value);
	
	/**
	 * Some types need to know if they're mapped to the correct class. If they do, they can override this method to glean that information
	 */
	public Object toAerospikeFormat(Object value, boolean isUnknownType, boolean isSubclassOfKnownType) {
		return toAerospikeFormat(value);
	}
}
