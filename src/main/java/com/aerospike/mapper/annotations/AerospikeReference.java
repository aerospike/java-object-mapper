package com.aerospike.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bins marked with AerospikeExclude will not be mapped to the database, irrespective of other annotations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AerospikeReference {
	/**
	 * Fields marked as being lazy references will not be read from Aerospike at runtime when the parent class is
	 * read. Instead, a new object with just the key populated will be created
	 */
	boolean lazy() default false;
	/**
	 * When a reference is to be loaded, it can either be loaded inline or it can be loaded via a batch load. The
	 * batch load is typically significantly more efficient. Set this flag to <pre>false</pre> to prevent the batch load
	 */
	boolean batchLoad() default true;
	
	enum ReferenceType {
		ID,
		DIGEST
	}

	ReferenceType type() default ReferenceType.ID;
}
