package com.aerospike.mapper.tools;

import com.aerospike.client.Key;

public class ThreadLocalKeySaver {
	private static ThreadLocal<Key> threadLocalKey = new ThreadLocal<>();
	
	public static void save(Key key) {
		threadLocalKey.set(key);
	}
	
	public static void clear() {
		threadLocalKey.set(null);
	}
	
	public static Key get() {
		return threadLocalKey.get();
	}
}
