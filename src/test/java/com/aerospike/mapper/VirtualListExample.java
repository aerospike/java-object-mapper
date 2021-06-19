package com.aerospike.mapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.mapper.tools.ReturnType;
import com.aerospike.mapper.tools.VirtualList;

public class VirtualListExample extends AeroMapperBaseTest {
	
	@AerospikeRecord(namespace = "test", set = "item")
	public static class Item {
		@AerospikeKey
		private int id;
		private Date due;
		private String desc;
		public Item(int id, Date due, String desc) {
			super();
			this.id = id;
			this.due = due;
			this.desc = desc;
		}
		
		public Item() {
		}
	}
	
	@AerospikeRecord(namespace = "test", set = "container")
	public static class Container {
		@AerospikeKey
		private int id;
		private String name;
		@AerospikeEmbed(type = EmbedType.MAP, elementType = EmbedType.LIST)
		private final List<Item> items;
		
		public Container() {
			this.items = new ArrayList<>();
		}
	}
	
	@Test
	public void testListOfReferences() {
		Container container = new Container();
		container.id = 1;
		container.name = "container";
		
		container.items.add(new Item(100, new Date(), "Item 1"));
		container.items.add(new Item(200, new Date(), "Item 2"));
		container.items.add(new Item(300, new Date(), "Item 3"));
		container.items.add(new Item(400, new Date(), "Item 4"));
		
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		mapper.save(container);
		
		VirtualList<Item> list = mapper.asBackedList(container, "items", Item.class);
		// perform a single operation. NOTE: This does NOT change the backed item, just the database
		list.append(new Item(500, new Date(), "Item5"));
		
		/*
		List<Item> results = (List<Item>) list.beginMultiOperation()
				.append(new Item(600, new Date(), "Item6"))
				.removeByKey(200)
				.getByKeyRange(100, 450)
			.end();
		
		System.out.println(results.size());
		*/
		long count = (long)list.beginMultiOperation()
				.append(new Item(600, new Date(), "Item6"))
				.removeByKey(200)
				.removeByKeyRange(20, 350).asResultOfType(ReturnType.COUNT)
				.getByKeyRange(100, 450)
			.end();
		
		System.out.println(count);
	}
}
