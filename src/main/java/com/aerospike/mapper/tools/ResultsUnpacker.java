package com.aerospike.mapper.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public interface ResultsUnpacker {
	Object unpack(Object object);
	
    class ListUnpacker implements ResultsUnpacker {
    	private ListUnpacker() {
		}
    	@Override
    	public Object unpack(Object object) {
    		if (object == null) {
    			return null;
    		}
    		else {
    			List<?> list = (List<?>)object;
    			return list.isEmpty() ? null : list.get(0);
    		}
    	}
    	public final static ListUnpacker instance = new ListUnpacker();
    }
    
    class IdentityUnpacker implements ResultsUnpacker {
    	private IdentityUnpacker() {
    	}
    	@Override
    	public Object unpack(Object object) {
    		return object;
    	}
    	public final static IdentityUnpacker instance = new IdentityUnpacker();
    }
    
    class ElementUnpacker implements ResultsUnpacker {
    	Function<Object, Object> function;
    	public ElementUnpacker(Function<Object, Object> itemMapper) {
    		this.function = itemMapper;
    	}
    	@Override
    	public Object unpack(Object object) {
    		return function.apply(object);
    	}
    }

    class ArrayUnpacker implements ResultsUnpacker {
    	Function<Object, Object> function;
    	public ArrayUnpacker(Function<Object, Object> itemMapper) {
    		this.function = itemMapper;
    	}
    	@Override
    	public Object unpack(Object object) {
    		if (object == null) {
    			return null;
    		}

    		List<Object> source = (List<Object>)object;
    		List<Object> results = new ArrayList<>(source.size());
    		for (Object thisObject : source) {
    			results.add(function.apply(thisObject));
    		}
    		return results;
    	}
    }
}
