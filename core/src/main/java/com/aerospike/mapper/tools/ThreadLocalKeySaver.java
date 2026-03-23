package com.aerospike.mapper.tools;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Save the keys. Note that this is effectively a stack of keys, as A can load B which can load C, and C needs B's key,
 * not A's.
 */
public class ThreadLocalKeySaver {

    private static final ThreadLocal<Deque<Object[]>> threadLocalKeys = ThreadLocal.withInitial(ArrayDeque::new);

    private ThreadLocalKeySaver() {
    }

    /**
     * @param keyContext   the full key context (e.g. a legacy Key object or RecordKey)
     * @param userKeyValue the user key value (e.g. key.userKey for legacy, or RecordKey.keyValue)
     */
    public static void save(Object keyContext, Object userKeyValue) {
        threadLocalKeys.get().addLast(new Object[]{keyContext, userKeyValue});
        LoadedObjectResolver.begin();
    }

    public static void clear() {
        LoadedObjectResolver.end();
        threadLocalKeys.get().removeLast();
        if (threadLocalKeys.get().isEmpty()) {
            threadLocalKeys.remove();
        }
    }

    /** Returns the full key context stored for the current load (e.g. a legacy Key or RecordKey). */
    public static Object getKeyContext() {
        Deque<Object[]> keys = threadLocalKeys.get();
        if (keys.isEmpty()) {
            return null;
        }
        return keys.getLast()[0];
    }

    /** Returns the user-key value stored for the current load (e.g. a String or Integer). */
    public static Object getUserKeyValue() {
        Deque<Object[]> keys = threadLocalKeys.get();
        if (keys.isEmpty()) {
            return null;
        }
        return keys.getLast()[1];
    }
}
