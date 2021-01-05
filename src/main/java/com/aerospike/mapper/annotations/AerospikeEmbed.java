package com.aerospike.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
/**
 * Bins marked with AerospikeExclude will not be mapped to the database, irrespective of other annotations. 
 */
public @interface AerospikeEmbed {
	public static enum EmbedType {
		LIST,
		MAP
	}
	EmbedType type() default EmbedType.MAP;
	/**
	 * The elementType is used for sub-elements. For example, if there is:
	 * <pre>
	 * @AerospikeBin
	 * @AerospikeEmbed(elementType = EmbedType.LIST)
	 * private List<Account> accounts;
	 * </pre>
	 * then the objects will be stored in the database as lists of lists, rather than lists of maps.
	 * 
	 * @return
	 */
	EmbedType elementType() default EmbedType.MAP;
}
