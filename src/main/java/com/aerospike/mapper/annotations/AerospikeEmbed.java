package com.aerospike.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bins marked with AerospikeEmbed will have the objects they reference embedded in the parent object, either as a list or a map
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AerospikeEmbed {

    enum EmbedType {
        LIST,
        MAP,
        DEFAULT
    }

    EmbedType type() default EmbedType.DEFAULT;

    /**
     * The elementType is used for sub-elements. For example, if there is:
     * <pre>
     * @AerospikeBin
     * @AerospikeEmbed(elementType = EmbedType.LIST)
     * private List<Account> accounts;
     * </pre>
     * then the objects will be stored in the database as lists of lists, rather than lists of maps.
     */
    EmbedType elementType() default EmbedType.DEFAULT;

    /**
     * Determine whether the key should be saved in the sub-object. This is used only for translating
     * a list of objects in Java into a map of objects which contains a list. For example:<p/>
     * <pre>
     * public class Transaction {
     *    private String name;
     *    private int value;
     *    &#064;AerospikeKey
     *    private long time;
     *
     *    public Transaction() {}
     *    public Transaction(String name, int value, long time) {
     *        super();
     *        this.name = name;
     *        this.value = value;
     *        this.time = time;
     *    }
     * }
     *
     * ...
     *
     * &#064;AerospikeEmbed(type = EmbedType.MAP, elementType = EmbedType.LIST)
     * public List&lt;Transaction&gt; txns;
     * </pre>
     * <p>
     * Assume elements have been inserted into the list as follows:
     * <pre>
     *  account.txns.add(new Transaction("details1", 100, 101));
     *  account.txns.add(new Transaction("details2", 200, 99));
     *  account.txns.add(new Transaction("details3", 300, 1010));
     * </pre>
     * <p>
     * Since these elements are stored in a map, the AerospikeKey field (time in this case) will be the key to the map.
     * By default, the key is then dropped from the list as it is not needed, resulting in:
     * <pre>
     * KEY_ORDERED_MAP('{99:["details2", 200], 101:["details1", 100], 1010:["details3", 300]}')
     * </pre>
     * <p>
     * If it is desired for the key to be part of the list as well, this value can be set to <code>true</code>:
     *
     * <pre>
     * &#064;AerospikeEmbed(type = EmbedType.MAP, elementType = EmbedType.LIST, saveKey = true)
     * public List&lt;Transaction&gt; txns;
     * </pre>
     * <p>
     * This would result in:
     * <pre>
     * KEY_ORDERED_MAP('{99:["details2", 99, 200], 101:["details1", 101, 100], 1010:["details3", 1010, 300]}')
     * </pre>
     */
    boolean saveKey() default false;
}
