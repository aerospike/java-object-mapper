package com.aerospike.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify the version of a record. Records without the @AerospikeVersion annotation are assumed to be version 1 of a record.
 * <p/>
 * Versions are typically used for embedded records to be stored in a list. Since a list item has no name associated with it,
 * the order of the attributes determines which part of the object to map the value to. By default the items are mapped
 * alphabetically, but this presents issues is an item is added or removed from the object.
 * <p/>
 * For example, consider a Person object with an Account stored as a list: <p/>
 * <pre>
 * &#64;AerospikeRecord(namespace = "test", set = "account")
 * public class Account {
 *     &#64;AerospikeBin
 *     private String name;
 *     &#64;AerospikeBin
 *     private int balance
 * }
 *
 * &#64;AerospikeRecord(namespace = "test", set = "people")
 * public class Person {
 *     &#64;AerospikeBin
 *     private String name;
 *     &#64;AerospikeBin
 *     &#64;AerospikeEmbed(type = EmbedType.LIST)
 *     private Account account;
 * }
 * </pre>
 *
 * @author timfaulkes
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface AerospikeVersion {
    int min() default 1;

    int max() default Integer.MAX_VALUE;
}
