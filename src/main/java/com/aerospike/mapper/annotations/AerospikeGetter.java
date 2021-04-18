package com.aerospike.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tags a field in a class to be mapped to an Aerospike bin.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AerospikeGetter {
	/**
	 * The name of the bin to use.
	 */
	String name();
}
