package com.aerospike.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a field or property to be used for optimistic concurrency control using Aerospike's generation field.
 * <p/>
 * The field or property must be of Integer or int type. When reading an object which has a field marked 
 * with @Version, the returned record's generation field will be mapped into the @Version field. 
 * When writing the record, if the @Version field is non-zero, the generation will be set in the 
 * writePolicy.generation field and the writePolicy.generationPolicy will be set to 
 * GenerationPolicy.EXPECT_GEN_EQUAL.
 * <p/>
 * Example usage:
 * <pre>
 * &#64;AerospikeRecord(namespace = "test", set = "account")
 * public class Account {
 *     &#64;AerospikeKey
 *     private int id;
 *     &#64;AerospikeBin
 *     private String name;
 *     &#64;Version
 *     private int version;
 * }
 * </pre>
 *
 * @author timfaulkes
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface AerospikeGeneration {
} 