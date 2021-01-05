package com.aerospike.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tags a field in a class to be mapped to an Aerospike bin.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AerospikeBin {
	/**
	 * The name of the bin to use. If not specified, the field name is used for the bin name.
	 * @return
	 */
	String name() default "";
}
