package com.aerospike.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface AerospikeKey {
    /**
     * The setter attribute is used only on Methods where the method is used to set the key on lazy object instantiation
     */
    boolean setter() default false;
    
    boolean storeInPkOnly() default false;
}
