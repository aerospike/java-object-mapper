package com.aerospike.mapper.tools;

import java.util.ArrayDeque;
import java.util.Deque;

import com.aerospike.client.Key;

/**
 * Save the keys. Note that this is effectively a stack of keys, as A can load B which can load C, and C needs B's key, not A's.
 * @author timfaulkes
 *
 */
public class ThreadLocalKeySaver {
	private static ThreadLocal<Deque<Key>> threadLocalKeys = new ThreadLocal<Deque<Key>>() {
		@Override
		public Deque<Key> initialValue() {
			return new ArrayDeque();
		}
	};
	
	public static void save(Key key) {
		threadLocalKeys.get().addLast(key);
	}
	
	public static void clear() {
		threadLocalKeys.get().removeLast();
	}
	
	public static Key get() {
		return threadLocalKeys.get().getLast();
	}
}
