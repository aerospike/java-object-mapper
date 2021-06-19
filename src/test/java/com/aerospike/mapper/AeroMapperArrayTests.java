package com.aerospike.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.mapper.tools.ClassCache;
import org.junit.jupiter.api.Test;

public class AeroMapperArrayTests extends AeroMapperBaseTest {
	@AerospikeRecord(namespace = "test", set = "testSet")
	public static class ChildClass {
		private int a;
		private String b;
		private float c;
		
		public ChildClass() {
		}

		public ChildClass(int a, String b, float c) {
			super();
			this.a = a;
			this.b = b;
			this.c = c;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			ChildClass other = (ChildClass) obj;
			return this.a == other.a && this.c == other.c && ((this.b == null && other.b == null) || (this.b != null && this.b.equals(other.b)));
		}
	}
	
	@AerospikeRecord(namespace = "test", set = "testSet")
	public static class AnnotatedClass {
		@AerospikeKey
		private int key;
		private byte[] bytes;
		private short[] shorts;
		private int[] ints;
		private long[] longs;
		private float[] floats;
		private double[] doubles;
		private String[] strings;
		@AerospikeEmbed
		private ChildClass[] children;
		@AerospikeEmbed(elementType = EmbedType.LIST)
		private ChildClass[] listChildren;
		@AerospikeEmbed(elementType = EmbedType.MAP)
		private ChildClass[] mapChildren;
	}

	public static class UnAnnotatedChildClass {
		private int a;
		private String b;
		private float c;
		
		public UnAnnotatedChildClass() {
		}

		public UnAnnotatedChildClass(int a, String b, float c) {
			super();
			this.a = a;
			this.b = b;
			this.c = c;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			UnAnnotatedChildClass other = (UnAnnotatedChildClass) obj;
			return this.a == other.a && this.c == other.c && ((this.b == null && other.b == null) || (this.b != null && this.b.equals(other.b)));
		}
	}

	public static class UnAnnotatedClass {
		private int key;
		private byte[] bytes;
		private short[] shorts;
		private int[] ints;
		private long[] longs;
		private float[] floats;
		private double[] doubles;
		private String[] strings;
		private UnAnnotatedChildClass[] children;
	}

	@Test
	public void testByteArray() {
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		AnnotatedClass annotatedClass = new AnnotatedClass();
		annotatedClass.key = 0;
		annotatedClass.bytes = new byte[] {1,2,3,4,5};
		mapper.save(annotatedClass);
		AnnotatedClass class2 = mapper.read(AnnotatedClass.class, annotatedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotatedClass.key));
		byte[] bytes = (byte[])record.getValue("bytes");

		assertEquals(annotatedClass.bytes.length, class2.bytes.length);
		for (int i = 0; i < annotatedClass.bytes.length; i++) {
			assertEquals(annotatedClass.bytes[i], class2.bytes[i]);
			assertEquals(annotatedClass.bytes[i], bytes[i]);
		}
		assertEquals(annotatedClass.bytes.length, bytes.length);
	}

	@Test
	public void testByteArrayUnannotated() throws Exception {
		String config = 
				"---\n" +
				"classes:\n" +
				"  - class: com.aerospike.mapper.AeroMapperArrayTests$UnAnnotatedClass\n" +
				"    namespace: test\n" +
				"    set: testSet\n" +
				"    key:\n" +
				"      field: key\n";

		AeroMapper mapper = new AeroMapper.Builder(client).withConfiguration(config).build();
		
		UnAnnotatedClass unAnnotatedClass = new UnAnnotatedClass();
		unAnnotatedClass.key = 1;
		unAnnotatedClass.bytes = new byte[] {1,2,3,4,5};
		mapper.save(unAnnotatedClass);
		UnAnnotatedClass class2 = mapper.read(UnAnnotatedClass.class, unAnnotatedClass.key);
		Record record = client.get(null, new Key("test", "testSet", unAnnotatedClass.key));
		byte[] bytes = (byte[])record.getValue("bytes");

		assertEquals(unAnnotatedClass.bytes.length, class2.bytes.length);
		for (int i = 0; i < unAnnotatedClass.bytes.length; i++) {
			assertEquals(unAnnotatedClass.bytes[i], class2.bytes[i]);
			assertEquals(unAnnotatedClass.bytes[i], bytes[i]);
		}
		assertEquals(unAnnotatedClass.bytes.length, bytes.length);
	}

	
	@Test
	public void testShortArray() {
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		AnnotatedClass annotatedClass = new AnnotatedClass();
		annotatedClass.key = 2;
		annotatedClass.shorts = new short[] {1,2,3,4,5};
		mapper.save(annotatedClass);
		AnnotatedClass class2 = mapper.read(AnnotatedClass.class, annotatedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotatedClass.key));
		List<Long> shorts = (List<Long>)record.getList("shorts");

		assertEquals(annotatedClass.shorts.length, class2.shorts.length);
		for (int i = 0; i < annotatedClass.shorts.length; i++) {
			assertEquals(annotatedClass.shorts[i], class2.shorts[i]);
			assertEquals(annotatedClass.shorts[i], (long)shorts.get(i));
		}
		assertEquals(annotatedClass.shorts.length, shorts.size());
	}

	@Test
	public void testShortArrayUnannotated() throws Exception {
		String config = 
				"---\n" +
				"classes:\n" +
				"  - class: com.aerospike.mapper.AeroMapperArrayTests$UnAnnotatedClass\n" +
				"    namespace: test\n" +
				"    set: testSet\n" +
				"    key:\n" +
				"      field: key\n";

		AeroMapper mapper = new AeroMapper.Builder(client).withConfiguration(config).build();
		
		UnAnnotatedClass unAnnotatedClass = new UnAnnotatedClass();
		unAnnotatedClass.key = 3;
		unAnnotatedClass.shorts = new short[] {1,2,3,4,5};
		mapper.save(unAnnotatedClass);
		UnAnnotatedClass class2 = mapper.read(UnAnnotatedClass.class, unAnnotatedClass.key);
		Record record = client.get(null, new Key("test", "testSet", unAnnotatedClass.key));
		List<Long> shorts = (List<Long>)record.getList("shorts");

		assertEquals(unAnnotatedClass.shorts.length, class2.shorts.length);
		for (int i = 0; i < unAnnotatedClass.shorts.length; i++) {
			assertEquals(unAnnotatedClass.shorts[i], class2.shorts[i]);
			assertEquals(unAnnotatedClass.shorts[i], (long)shorts.get(i));
		}
		assertEquals(unAnnotatedClass.shorts.length, shorts.size());
	}

	
	@Test
	public void testIntArray() {
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		AnnotatedClass annotatedClass = new AnnotatedClass();
		annotatedClass.key = 4;
		annotatedClass.ints = new int[] {1,2,3,4,5};
		mapper.save(annotatedClass);
		AnnotatedClass class2 = mapper.read(AnnotatedClass.class, annotatedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotatedClass.key));
		List<Long> ints = (List<Long>)record.getList("ints");

		assertEquals(annotatedClass.ints.length, class2.ints.length);
		for (int i = 0; i < annotatedClass.ints.length; i++) {
			assertEquals(annotatedClass.ints[i], class2.ints[i]);
			assertEquals(annotatedClass.ints[i], (long)ints.get(i));
		}
		assertEquals(annotatedClass.ints.length, ints.size());
	}

	@Test
	public void testIntArrayUnannotated() throws Exception {
		String config = 
				"---\n" +
				"classes:\n" +
				"  - class: com.aerospike.mapper.AeroMapperArrayTests$UnAnnotatedClass\n" +
				"    namespace: test\n" +
				"    set: testSet\n" +
				"    key:\n" +
				"      field: key\n";

		AeroMapper mapper = new AeroMapper.Builder(client).withConfiguration(config).build();
		
		UnAnnotatedClass unAnnotatedClass = new UnAnnotatedClass();
		unAnnotatedClass.key = 5;
		unAnnotatedClass.ints = new int[] {1,2,3,4,5};
		mapper.save(unAnnotatedClass);
		UnAnnotatedClass class2 = mapper.read(UnAnnotatedClass.class, unAnnotatedClass.key);
		Record record = client.get(null, new Key("test", "testSet", unAnnotatedClass.key));
		List<Long> ints = (List<Long>)record.getList("ints");

		assertEquals(unAnnotatedClass.ints.length, class2.ints.length);
		for (int i = 0; i < unAnnotatedClass.ints.length; i++) {
			assertEquals(unAnnotatedClass.ints[i], class2.ints[i]);
			assertEquals(unAnnotatedClass.ints[i], (long)ints.get(i));
		}
		assertEquals(unAnnotatedClass.ints.length, ints.size());
	}

	
	@Test
	public void testLongArray() {
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		AnnotatedClass annotatedClass = new AnnotatedClass();
		annotatedClass.key = 6;
		annotatedClass.longs = new long[] {1,2,3,4,5};
		mapper.save(annotatedClass);
		AnnotatedClass class2 = mapper.read(AnnotatedClass.class, annotatedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotatedClass.key));
		List<Long> longs = (List<Long>)record.getList("longs");

		assertEquals(annotatedClass.longs.length, class2.longs.length);
		for (int i = 0; i < annotatedClass.longs.length; i++) {
			assertEquals(annotatedClass.longs[i], class2.longs[i]);
			assertEquals(annotatedClass.longs[i], (long)longs.get(i));
		}
		assertEquals(annotatedClass.longs.length, longs.size());
	}

	@Test
	public void testLongArrayUnannotated() throws Exception {
		String config = 
				"---\n" +
				"classes:\n" +
				"  - class: com.aerospike.mapper.AeroMapperArrayTests$UnAnnotatedClass\n" +
				"    namespace: test\n" +
				"    set: testSet\n" +
				"    key:\n" +
				"      field: key\n";

		AeroMapper mapper = new AeroMapper.Builder(client).withConfiguration(config).build();
		
		UnAnnotatedClass unAnnotatedClass = new UnAnnotatedClass();
		unAnnotatedClass.key = 7;
		unAnnotatedClass.longs = new long[] {1,2,3,4,5};
		mapper.save(unAnnotatedClass);
		UnAnnotatedClass class2 = mapper.read(UnAnnotatedClass.class, unAnnotatedClass.key);
		Record record = client.get(null, new Key("test", "testSet", unAnnotatedClass.key));
		List<Long> longs = (List<Long>)record.getList("longs");

		assertEquals(unAnnotatedClass.longs.length, class2.longs.length);
		for (int i = 0; i < unAnnotatedClass.longs.length; i++) {
			assertEquals(unAnnotatedClass.longs[i], class2.longs[i]);
			assertEquals(unAnnotatedClass.longs[i], (long)longs.get(i));
		}
		assertEquals(unAnnotatedClass.longs.length, longs.size());
	}

	
	@Test
	public void testFloatArray() {
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		AnnotatedClass annotatedClass = new AnnotatedClass();
		annotatedClass.key = 8;
		annotatedClass.floats = new float[] {1,2,3,4,5};
		mapper.save(annotatedClass);
		AnnotatedClass class2 = mapper.read(AnnotatedClass.class, annotatedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotatedClass.key));
		List<Double> floats = (List<Double>)record.getList("floats");

		assertEquals(annotatedClass.floats.length, class2.floats.length);
		for (int i = 0; i < annotatedClass.floats.length; i++) {
			assertEquals(annotatedClass.floats[i], class2.floats[i], 0.00001);
			assertEquals(annotatedClass.floats[i], floats.get(i), 0.00001);
		}
		assertEquals(annotatedClass.floats.length, floats.size());
	}

	@Test
	public void testFloatArrayUnannotated() throws Exception {
		String config = 
				"---\n" +
				"classes:\n" +
				"  - class: com.aerospike.mapper.AeroMapperArrayTests$UnAnnotatedClass\n" +
				"    namespace: test\n" +
				"    set: testSet\n" +
				"    key:\n" +
				"      field: key\n";

		AeroMapper mapper = new AeroMapper.Builder(client).withConfiguration(config).build();
		
		UnAnnotatedClass unAnnotatedClass = new UnAnnotatedClass();
		unAnnotatedClass.key = 9;
		unAnnotatedClass.floats = new float[] {1,2,3,4,5};
		mapper.save(unAnnotatedClass);
		UnAnnotatedClass class2 = mapper.read(UnAnnotatedClass.class, unAnnotatedClass.key);
		Record record = client.get(null, new Key("test", "testSet", unAnnotatedClass.key));
		List<Double> floats = (List<Double>)record.getList("floats");

		assertEquals(unAnnotatedClass.floats.length, class2.floats.length);
		for (int i = 0; i < unAnnotatedClass.floats.length; i++) {
			assertEquals(unAnnotatedClass.floats[i], class2.floats[i], 0.0001);
			assertEquals(unAnnotatedClass.floats[i], floats.get(i), 0.0001);
		}
		assertEquals(unAnnotatedClass.floats.length, floats.size());
	}

	
	@Test
	public void testDoubleArray() {
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		AnnotatedClass annotatedClass = new AnnotatedClass();
		annotatedClass.key = 10;
		annotatedClass.doubles = new double[] {1,2,3,4,5};
		mapper.save(annotatedClass);
		AnnotatedClass class2 = mapper.read(AnnotatedClass.class, annotatedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotatedClass.key));
		List<Double> doubles = (List<Double>)record.getList("doubles");

		assertEquals(annotatedClass.doubles.length, class2.doubles.length);
		for (int i = 0; i < annotatedClass.doubles.length; i++) {
			assertEquals(annotatedClass.doubles[i], class2.doubles[i], 0.00001);
			assertEquals(annotatedClass.doubles[i], doubles.get(i), 0.00001);
		}
		assertEquals(annotatedClass.doubles.length, doubles.size());
	}

	@Test
	public void testDoubleArrayUnannotated() throws Exception {
		String config = 
				"---\n" +
				"classes:\n" +
				"  - class: com.aerospike.mapper.AeroMapperArrayTests$UnAnnotatedClass\n" +
				"    namespace: test\n" +
				"    set: testSet\n" +
				"    key:\n" +
				"      field: key\n";

		AeroMapper mapper = new AeroMapper.Builder(client).withConfiguration(config).build();
		
		UnAnnotatedClass unAnnotatedClass = new UnAnnotatedClass();
		unAnnotatedClass.key = 11;
		unAnnotatedClass.doubles = new double[] {1,2,3,4,5};
		mapper.save(unAnnotatedClass);
		UnAnnotatedClass class2 = mapper.read(UnAnnotatedClass.class, unAnnotatedClass.key);
		Record record = client.get(null, new Key("test", "testSet", unAnnotatedClass.key));
		List<Double> doubles = (List<Double>)record.getList("doubles");

		assertEquals(unAnnotatedClass.doubles.length, class2.doubles.length);
		for (int i = 0; i < unAnnotatedClass.doubles.length; i++) {
			assertEquals(unAnnotatedClass.doubles[i], class2.doubles[i], 0.0001);
			assertEquals(unAnnotatedClass.doubles[i], doubles.get(i), 0.0001);
		}
		assertEquals(unAnnotatedClass.doubles.length, doubles.size());
	}


	@Test
	public void testStringArray() {
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		AnnotatedClass annotatedClass = new AnnotatedClass();
		annotatedClass.key = 12;
		annotatedClass.strings = new String[] {"the", "quick", "brown", "fox"};
		mapper.save(annotatedClass);
		AnnotatedClass class2 = mapper.read(AnnotatedClass.class, annotatedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotatedClass.key));
		List<String> strings = (List<String>)record.getList("strings");

		assertEquals(annotatedClass.strings.length, class2.strings.length);
		for (int i = 0; i < annotatedClass.strings.length; i++) {
			assertEquals(annotatedClass.strings[i], class2.strings[i]);
			assertEquals(annotatedClass.strings[i], strings.get(i));
		}
		assertEquals(annotatedClass.strings.length, strings.size());
	}

	@Test
	public void testStringArrayUnannotated() throws Exception {
		String config = 
				"---\n" +
				"classes:\n" +
				"  - class: com.aerospike.mapper.AeroMapperArrayTests$UnAnnotatedClass\n" +
				"    namespace: test\n" +
				"    set: testSet\n" +
				"    key:\n" +
				"      field: key\n";

		AeroMapper mapper = new AeroMapper.Builder(client).withConfiguration(config).build();
		
		UnAnnotatedClass unAnnotatedClass = new UnAnnotatedClass();
		unAnnotatedClass.key = 13;
		unAnnotatedClass.strings = new String[] {"the", "quick", "brown", "fox"};
		mapper.save(unAnnotatedClass);
		UnAnnotatedClass class2 = mapper.read(UnAnnotatedClass.class, unAnnotatedClass.key);
		Record record = client.get(null, new Key("test", "testSet", unAnnotatedClass.key));
		List<String> strings = (List<String>)record.getList("strings");

		assertEquals(unAnnotatedClass.strings.length, class2.strings.length);
		for (int i = 0; i < unAnnotatedClass.strings.length; i++) {
			assertEquals(unAnnotatedClass.strings[i], class2.strings[i]);
			assertEquals(unAnnotatedClass.strings[i], strings.get(i));
		}
		assertEquals(unAnnotatedClass.strings.length, strings.size());
	}


	@Test
	public void testClassArray() {
		ClassCache.getInstance().clear();
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		AnnotatedClass annotatedClass = new AnnotatedClass();
		annotatedClass.key = 14;
		annotatedClass.children = new ChildClass[] {new ChildClass(1, "a", 2), new ChildClass(2, "b", 4), new ChildClass(3, "c", 6)};
		mapper.save(annotatedClass);
		AnnotatedClass class2 = mapper.read(AnnotatedClass.class, annotatedClass.key);

		assertEquals(annotatedClass.children.length, class2.children.length);
		for (int i = 0; i < annotatedClass.children.length; i++) {
			assertEquals(annotatedClass.children[i], class2.children[i]);
		}
	}

	@Test
	public void testClassArrayUnannotated() throws Exception {
		ClassCache.getInstance().clear();
		String config = 
				"---\n" +
				"classes:\n" +
				"  - class: com.aerospike.mapper.AeroMapperArrayTests$UnAnnotatedChildClass\n" +
				"    namespace: test\n" +
				"    set: testSet\n" +
				"    key:\n" +
				"      field: a\n" +
				"  - class: com.aerospike.mapper.AeroMapperArrayTests$UnAnnotatedClass\n" +
				"    namespace: test\n" +
				"    set: testSet\n" +
				"    bins:\n" +
				"    - field: children\n" +
				"      embed:\n" +
				"        elementType: DEFAULT\n" +
				"    key:\n" +
				"      field: key\n";

		AeroMapper mapper = new AeroMapper.Builder(client).withConfiguration(config).build();
		
		UnAnnotatedClass unAnnotatedClass = new UnAnnotatedClass();
		unAnnotatedClass.key = 15;
		unAnnotatedClass.children = new UnAnnotatedChildClass[] {new UnAnnotatedChildClass(1, "a", 2), new UnAnnotatedChildClass(2, "b", 4), new UnAnnotatedChildClass(3, "c", 6)};
		mapper.save(unAnnotatedClass);
		UnAnnotatedClass class2 = mapper.read(UnAnnotatedClass.class, unAnnotatedClass.key);

		assertEquals(unAnnotatedClass.children.length, class2.children.length);
		for (int i = 0; i < unAnnotatedClass.children.length; i++) {
			assertEquals(unAnnotatedClass.children[i], class2.children[i]);
		}
	}
}
