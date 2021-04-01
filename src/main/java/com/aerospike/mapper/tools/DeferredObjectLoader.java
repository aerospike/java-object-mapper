package com.aerospike.mapper.tools;

import java.util.ArrayList;
import java.util.List;

public class DeferredObjectLoader {
	public static interface DeferredSetter {
		public void setValue(Object object);
	}
	
	public static class DeferredObject {
		private final Object key;
		private final Class<?> type;
		private final boolean isDigest;
		public DeferredObject(Object key, Class<?> type, boolean isDigest) {
			super();
			this.key = key;
			this.type = type;
			this.isDigest = isDigest;
		}
		public Object getKey() {
			return key;
		}
		public Class<?> getType() {
			return type;
		}
		public boolean isDigest() {
			return isDigest;
		}
	}
	
	public static class DeferredObjectSetter {
		private final DeferredSetter setter;
		private final DeferredObject object;
		
		public DeferredObjectSetter(DeferredSetter setter, DeferredObject object) {
			super();
			this.setter = setter;
			this.object = object;
		}
		public DeferredSetter getSetter() {
			return setter;
		}
		public DeferredObject getObject() {
			return object;
		}
	}
	
	
	private static ThreadLocal<List<DeferredObjectSetter>> threadLocalLoader = new ThreadLocal<List<DeferredObjectSetter>>() {
		@Override
		public List<DeferredObjectSetter> initialValue() {
			return new ArrayList<DeferredObjectSetter>();
		}
	};
	
	public static void save(DeferredObjectSetter object) {
		threadLocalLoader.get().add(object);
	}
	
	public static void clear() {
		threadLocalLoader.get().clear();
	}
	
	public static List<DeferredObjectSetter> get() {
		return threadLocalLoader.get();
	}
	
	public static void add(DeferredObjectSetter deferredSetter) {
		threadLocalLoader.get().add(deferredSetter);
	}

	public static List<DeferredObjectSetter> getAndClear() {
		List<DeferredObjectSetter> localArray = threadLocalLoader.get();
		List<DeferredObjectSetter> setters = new ArrayList<DeferredObjectSetter>(localArray);
		localArray.clear();
		return setters;
	}
}
