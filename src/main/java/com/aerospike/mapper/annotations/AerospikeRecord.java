package com.aerospike.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AerospikeRecord {
    String namespace() default "";

    String set() default "";

    String shortName() default "";

    /**
     * The TTL for the record. As this must be a primitive value <code>Integer.MIN_VALUE</code> is used to indicate that the 
     * value has not been explicitly set.
     * @return
     */
    int ttl() default Integer.MIN_VALUE;

    /**
     * Determine whether to add all the bins or not. If true, all bins will be added without having to map them via @AerospikeBin
     */
    boolean mapAll() default true;

    int version() default 1;

    boolean sendKey() default false;

    boolean durableDelete() default false;

    String factoryClass() default "";

    String factoryMethod() default "";
}
