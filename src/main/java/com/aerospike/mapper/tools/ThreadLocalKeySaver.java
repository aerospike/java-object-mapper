package com.aerospike.mapper.tools;

import com.aerospike.client.Key;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Save the keys. Note that this is effectively a stack of keys, as A can load B which can load C, and C needs B's key,
 * not A's.
 */
public class ThreadLocalKeySaver {

    private static final ThreadLocal<Deque<Key>> threadLocalKeys = ThreadLocal.withInitial(ArrayDeque::new);

    private ThreadLocalKeySaver() {
    }

    public static void save(Key key) {
        threadLocalKeys.get().addLast(key);
    }

    public static void clear() {
        threadLocalKeys.get().removeLast();
        if (threadLocalKeys.get().isEmpty()) {
            threadLocalKeys.remove();
        }
    }

    public static Key get() {
        Deque<Key> keys = threadLocalKeys.get();
        if (keys.isEmpty()) {
            return null;
        }
        return keys.getLast();
    }
}
