package com.aerospike.mapper;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.mapper.annotations.AerospikeEmbed;
import com.aerospike.mapper.annotations.AerospikeEmbed.EmbedType;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.tools.AeroMapper;
import com.aerospike.mapper.tools.ClassCache;

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
		AnnotatedClass annotedClass = new AnnotatedClass();
		annotedClass.key = 0;
		annotedClass.bytes = new byte[] {1,2,3,4,5};
		mapper.save(annotedClass);
		AnnotatedClass class2 = mapper.read(AnnotatedClass.class, annotedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotedClass.key));
		byte[] bytes = (byte[])record.getValue("bytes");

		assertEquals(annotedClass.bytes.length, class2.bytes.length);
		for (int i = 0; i < annotedClass.bytes.length; i++) {
			assertEquals(annotedClass.bytes[i], class2.bytes[i]);
			assertEquals(annotedClass.bytes[i], bytes[i]);
		}
		assertEquals(annotedClass.bytes.length, bytes.length);
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
		
		UnAnnotatedClass annotedClass = new UnAnnotatedClass();
		annotedClass.key = 1;
		annotedClass.bytes = new byte[] {1,2,3,4,5};
		mapper.save(annotedClass);
		UnAnnotatedClass class2 = mapper.read(UnAnnotatedClass.class, annotedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotedClass.key));
		byte[] bytes = (byte[])record.getValue("bytes");

		assertEquals(annotedClass.bytes.length, class2.bytes.length);
		for (int i = 0; i < annotedClass.bytes.length; i++) {
			assertEquals(annotedClass.bytes[i], class2.bytes[i]);
			assertEquals(annotedClass.bytes[i], bytes[i]);
		}
		assertEquals(annotedClass.bytes.length, bytes.length);
	}

	
	@Test
	public void testShortArray() {
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		AnnotatedClass annotedClass = new AnnotatedClass();
		annotedClass.key = 2;
		annotedClass.shorts = new short[] {1,2,3,4,5};
		mapper.save(annotedClass);
		AnnotatedClass class2 = mapper.read(AnnotatedClass.class, annotedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotedClass.key));
		List<Long> shorts = (List<Long>)record.getList("shorts");

		assertEquals(annotedClass.shorts.length, class2.shorts.length);
		for (int i = 0; i < annotedClass.shorts.length; i++) {
			assertEquals(annotedClass.shorts[i], class2.shorts[i]);
			assertEquals((long)annotedClass.shorts[i], (long)shorts.get(i));
		}
		assertEquals(annotedClass.shorts.length, shorts.size());
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
		
		UnAnnotatedClass annotedClass = new UnAnnotatedClass();
		annotedClass.key = 3;
		annotedClass.shorts = new short[] {1,2,3,4,5};
		mapper.save(annotedClass);
		UnAnnotatedClass class2 = mapper.read(UnAnnotatedClass.class, annotedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotedClass.key));
		List<Long> shorts = (List<Long>)record.getList("shorts");

		assertEquals(annotedClass.shorts.length, class2.shorts.length);
		for (int i = 0; i < annotedClass.shorts.length; i++) {
			assertEquals(annotedClass.shorts[i], class2.shorts[i]);
			assertEquals((long)annotedClass.shorts[i], (long)shorts.get(i));
		}
		assertEquals(annotedClass.shorts.length, shorts.size());
	}

	
	@Test
	public void testIntArray() {
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		AnnotatedClass annotedClass = new AnnotatedClass();
		annotedClass.key = 4;
		annotedClass.ints = new int[] {1,2,3,4,5};
		mapper.save(annotedClass);
		AnnotatedClass class2 = mapper.read(AnnotatedClass.class, annotedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotedClass.key));
		List<Long> ints = (List<Long>)record.getList("ints");

		assertEquals(annotedClass.ints.length, class2.ints.length);
		for (int i = 0; i < annotedClass.ints.length; i++) {
			assertEquals(annotedClass.ints[i], class2.ints[i]);
			assertEquals((long)annotedClass.ints[i], (long)ints.get(i));
		}
		assertEquals(annotedClass.ints.length, ints.size());
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
		
		UnAnnotatedClass annotedClass = new UnAnnotatedClass();
		annotedClass.key = 5;
		annotedClass.ints = new int[] {1,2,3,4,5};
		mapper.save(annotedClass);
		UnAnnotatedClass class2 = mapper.read(UnAnnotatedClass.class, annotedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotedClass.key));
		List<Long> ints = (List<Long>)record.getList("ints");

		assertEquals(annotedClass.ints.length, class2.ints.length);
		for (int i = 0; i < annotedClass.ints.length; i++) {
			assertEquals(annotedClass.ints[i], class2.ints[i]);
			assertEquals((long)annotedClass.ints[i], (long)ints.get(i));
		}
		assertEquals(annotedClass.ints.length, ints.size());
	}

	
	@Test
	public void testLongArray() {
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		AnnotatedClass annotedClass = new AnnotatedClass();
		annotedClass.key = 6;
		annotedClass.longs = new long[] {1,2,3,4,5};
		mapper.save(annotedClass);
		AnnotatedClass class2 = mapper.read(AnnotatedClass.class, annotedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotedClass.key));
		List<Long> longs = (List<Long>)record.getList("longs");

		assertEquals(annotedClass.longs.length, class2.longs.length);
		for (int i = 0; i < annotedClass.longs.length; i++) {
			assertEquals(annotedClass.longs[i], class2.longs[i]);
			assertEquals((long)annotedClass.longs[i], (long)longs.get(i));
		}
		assertEquals(annotedClass.longs.length, longs.size());
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
		
		UnAnnotatedClass annotedClass = new UnAnnotatedClass();
		annotedClass.key = 7;
		annotedClass.longs = new long[] {1,2,3,4,5};
		mapper.save(annotedClass);
		UnAnnotatedClass class2 = mapper.read(UnAnnotatedClass.class, annotedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotedClass.key));
		List<Long> longs = (List<Long>)record.getList("longs");

		assertEquals(annotedClass.longs.length, class2.longs.length);
		for (int i = 0; i < annotedClass.longs.length; i++) {
			assertEquals(annotedClass.longs[i], class2.longs[i]);
			assertEquals((long)annotedClass.longs[i], (long)longs.get(i));
		}
		assertEquals(annotedClass.longs.length, longs.size());
	}

	
	@Test
	public void testFloatArray() {
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		AnnotatedClass annotedClass = new AnnotatedClass();
		annotedClass.key = 8;
		annotedClass.floats = new float[] {1,2,3,4,5};
		mapper.save(annotedClass);
		AnnotatedClass class2 = mapper.read(AnnotatedClass.class, annotedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotedClass.key));
		List<Double> floats = (List<Double>)record.getList("floats");

		assertEquals(annotedClass.floats.length, class2.floats.length);
		for (int i = 0; i < annotedClass.floats.length; i++) {
			assertEquals(annotedClass.floats[i], class2.floats[i], 0.00001);
			assertEquals((double)annotedClass.floats[i], (double)floats.get(i), 0.00001);
		}
		assertEquals(annotedClass.floats.length, floats.size());
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
		
		UnAnnotatedClass annotedClass = new UnAnnotatedClass();
		annotedClass.key = 9;
		annotedClass.floats = new float[] {1,2,3,4,5};
		mapper.save(annotedClass);
		UnAnnotatedClass class2 = mapper.read(UnAnnotatedClass.class, annotedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotedClass.key));
		List<Double> floats = (List<Double>)record.getList("floats");

		assertEquals(annotedClass.floats.length, class2.floats.length);
		for (int i = 0; i < annotedClass.floats.length; i++) {
			assertEquals(annotedClass.floats[i], class2.floats[i], 0.0001);
			assertEquals((double)annotedClass.floats[i], (double)floats.get(i), 0.0001);
		}
		assertEquals(annotedClass.floats.length, floats.size());
	}

	
	@Test
	public void testDoubleArray() {
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		AnnotatedClass annotedClass = new AnnotatedClass();
		annotedClass.key = 10;
		annotedClass.doubles = new double[] {1,2,3,4,5};
		mapper.save(annotedClass);
		AnnotatedClass class2 = mapper.read(AnnotatedClass.class, annotedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotedClass.key));
		List<Double> doubles = (List<Double>)record.getList("doubles");

		assertEquals(annotedClass.doubles.length, class2.doubles.length);
		for (int i = 0; i < annotedClass.doubles.length; i++) {
			assertEquals(annotedClass.doubles[i], class2.doubles[i], 0.00001);
			assertEquals((double)annotedClass.doubles[i], (double)doubles.get(i), 0.00001);
		}
		assertEquals(annotedClass.doubles.length, doubles.size());
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
		
		UnAnnotatedClass annotedClass = new UnAnnotatedClass();
		annotedClass.key = 11;
		annotedClass.doubles = new double[] {1,2,3,4,5};
		mapper.save(annotedClass);
		UnAnnotatedClass class2 = mapper.read(UnAnnotatedClass.class, annotedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotedClass.key));
		List<Double> doubles = (List<Double>)record.getList("doubles");

		assertEquals(annotedClass.doubles.length, class2.doubles.length);
		for (int i = 0; i < annotedClass.doubles.length; i++) {
			assertEquals(annotedClass.doubles[i], class2.doubles[i], 0.0001);
			assertEquals((double)annotedClass.doubles[i], (double)doubles.get(i), 0.0001);
		}
		assertEquals(annotedClass.doubles.length, doubles.size());
	}


	@Test
	public void testStringArray() {
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		AnnotatedClass annotedClass = new AnnotatedClass();
		annotedClass.key = 12;
		annotedClass.strings = new String[] {"the", "quick", "brown", "fox"};
		mapper.save(annotedClass);
		AnnotatedClass class2 = mapper.read(AnnotatedClass.class, annotedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotedClass.key));
		List<String> strings = (List<String>)record.getList("strings");

		assertEquals(annotedClass.strings.length, class2.strings.length);
		for (int i = 0; i < annotedClass.strings.length; i++) {
			assertEquals(annotedClass.strings[i], class2.strings[i]);
			assertEquals(annotedClass.strings[i], strings.get(i));
		}
		assertEquals(annotedClass.strings.length, strings.size());
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
		
		UnAnnotatedClass annotedClass = new UnAnnotatedClass();
		annotedClass.key = 13;
		annotedClass.strings = new String[] {"the", "quick", "brown", "fox"};
		mapper.save(annotedClass);
		UnAnnotatedClass class2 = mapper.read(UnAnnotatedClass.class, annotedClass.key);
		Record record = client.get(null, new Key("test", "testSet", annotedClass.key));
		List<String> strings = (List<String>)record.getList("strings");

		assertEquals(annotedClass.strings.length, class2.strings.length);
		for (int i = 0; i < annotedClass.strings.length; i++) {
			assertEquals(annotedClass.strings[i], class2.strings[i]);
			assertEquals(annotedClass.strings[i], strings.get(i));
		}
		assertEquals(annotedClass.strings.length, strings.size());
	}


	@Test
	public void testClassArray() {
		ClassCache.getInstance().clear();
		AeroMapper mapper = new AeroMapper.Builder(client).build();
		AnnotatedClass annotedClass = new AnnotatedClass();
		annotedClass.key = 14;
		annotedClass.children = new ChildClass[] {new ChildClass(1, "a", 2), new ChildClass(2, "b", 4), new ChildClass(3, "c", 6)};
		mapper.save(annotedClass);
		AnnotatedClass class2 = mapper.read(AnnotatedClass.class, annotedClass.key);

		assertEquals(annotedClass.children.length, class2.children.length);
		for (int i = 0; i < annotedClass.children.length; i++) {
			assertEquals(annotedClass.children[i], class2.children[i]);
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
		
		UnAnnotatedClass annotedClass = new UnAnnotatedClass();
		annotedClass.key = 15;
		annotedClass.children = new UnAnnotatedChildClass[] {new UnAnnotatedChildClass(1, "a", 2), new UnAnnotatedChildClass(2, "b", 4), new UnAnnotatedChildClass(3, "c", 6)};
		mapper.save(annotedClass);
		UnAnnotatedClass class2 = mapper.read(UnAnnotatedClass.class, annotedClass.key);

		assertEquals(annotedClass.children.length, class2.children.length);
		for (int i = 0; i < annotedClass.children.length; i++) {
			assertEquals(annotedClass.children[i], class2.children[i]);
		}
	}


}
