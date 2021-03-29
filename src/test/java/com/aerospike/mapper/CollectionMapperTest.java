package com.aerospike.mapper;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.aerospike.mapper.annotations.AerospikeConstructor;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.ParamFrom;
import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.mapper.tools.ReturnType;
import com.aerospike.mapper.tools.VirtualList;

public class CollectionMapperTest extends AeroMapperBaseTest {
	@AerospikeRecord
	public static class CollectionElement {
		@AerospikeKey
		public int id;
		public String name;
		public long date;
		
		public CollectionElement(@ParamFrom("id") int id, @ParamFrom("name") String name, @ParamFrom("date") long date) {
			super();
			this.id = id;
			this.name = name;
			this.date = date;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public long getDate() {
			return date;
		}
		@Override
		public String toString() {
			return String.format("{id=%d, name=%s, date=%d}",  id, name, date);
		}
	}
	
	@AerospikeRecord(namespace = "test", set = "testSet1")
	public static class Collection {
		@AerospikeEmbed(type = EmbedType.MAP, elementType = EmbedType.LIST)
		public List<CollectionElement> elements;
		
		@AerospikeKey 
		public int id;

		public Collection() {
			elements = new ArrayList<>();
		}
	}
	
	@Test
	public void test() {
		Collection collection = new Collection();
		collection.id = 1;
		
		collection.elements.add(new CollectionElement(102, "bob", 12345));
		collection.elements.add(new CollectionElement(101, "joe", 23456));
		collection.elements.add(new CollectionElement(100, "sue", 34567));

		AeroMapper mapper = new AeroMapper.Builder(client).build();
		mapper.save(collection);
		
		VirtualList<CollectionElement> list = mapper.asBackedList(collection, "elements", CollectionElement.class);
//		list.append(new CollectionElement(103, "tom", 45678));
//		System.out.println("Get by index returned: " + list.get(2));
//		System.out.println("Delete by Key Range returned: " + list.removeByKeyRange(100, 102, true));
		Object results = list.beginMultiOperation()
				.append(new CollectionElement(103, "tom", 45678))
				.append(new CollectionElement(104, "tim", 22222))
				.append(new CollectionElement(105, "sam", 33333))
				.append(new CollectionElement(106, "rob", 44444))
				.getByKeyRange(101, 105)
//				.removeByKeyRange(100, 102).asResult()
//				.get(0)
//				.size()
			.end();
		
		System.out.println(results);
	}
}


