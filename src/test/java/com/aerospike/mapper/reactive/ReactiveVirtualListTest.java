package com.aerospike.mapper.reactive;

import com.aerospike.mapper.annotations.*;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import com.aerospike.mapper.tools.ReactiveVirtualList;
import org.junit.Test;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReactiveVirtualListTest extends ReactiveAeroMapperBaseTest {

    @AerospikeRecord(namespace = "test", set = "C")
    public static class C {
        @AerospikeKey
        public int a;
        public String b;
        public C(@ParamFrom("a") int a, @ParamFrom("b") String b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return String.format("{%d,\"%s\"}", a, b);
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && a == ((C)obj).a && b.equals(((C)obj).b);
        }
        @Override
        public int hashCode() {
            return 17*a + (b == null ? 0 : b.hashCode());
        }
    }

    @AerospikeRecord
    public static class B {
        @AerospikeKey
        public int id;
        public String name;
        public long date;
        public C thisC;
        public List<C> Cs;
        public List<C> otherCs;
        public C anonC;

        @AerospikeConstructor
        public B(@ParamFrom("id") int id, @ParamFrom("name") String name, @ParamFrom("date") long date) {
            super();
            this.id = id;
            this.name = name;
            this.date = date;
            this.Cs = new ArrayList<>();
            this.otherCs = new ArrayList<>();
        }

        public B(int id, String name, long date, C thisC, C... listCs) {
            this(id, name, date);
            this.thisC = thisC;
            this.anonC = thisC;
            for (C aC : listCs) {
                this.Cs.add(aC);
                this.otherCs.add(aC);
            }
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
        public List<C> getCs() {
            return Cs;
        }
        public C getThisC() {
            return thisC;
        }
        public void setThisC(C thisC) {
            this.thisC = thisC;
        }

        @Override
        public String toString() {
            return String.format("{id=%d, name=%s, date=%d, thisC=%s, listC=%s}",  id, name, date, thisC, Cs);
        }

        private boolean compare(Object a, Object b) {
            if (a == null && b == null) {
                return true;
            }
            if ((a != null && b == null) || (a == null && b != null)) {
                return false;
            }
            return a.equals(b);
        }

        @Override
        public boolean equals(Object obj) {
            if ((!(obj instanceof B))) {
                return false;
            }
            B b2 = (B)obj;
            if (id != b2.id || date != b2.date) {
                return false;
            }
            return compare(name, b2.name) && compare(thisC, b2.thisC) && compare(Cs, b2.Cs) && compare(otherCs, b2.otherCs) && compare(anonC, b2.anonC);
        }
    }

    @AerospikeRecord(namespace = "test", set = "A")
    public static class A {
        @AerospikeEmbed(type = AerospikeEmbed.EmbedType.MAP, elementType = AerospikeEmbed.EmbedType.MAP)
        public List<B> elements;

        @AerospikeKey
        public int id;

        public A() {
            elements = new ArrayList<>();
        }
    }

    @Test
    public void test() {
        C a = new C(1, "a");
        C b = new C(2, "b");
        C c = new C(3, "c");
        C d = new C(4, "d");
        C e = new C(5, "e");
        C f = new C(6, "f");
        C g = new C(7, "g");
        C h = new C(8, "h");
        C i = new C(9, "i");
        C j = new C(10, "j");

        A collection = new A();
        collection.id = 1;

        collection.elements.add(new B(102, "bob", 12345, a, b, c));
        collection.elements.add(new B(101, "joe", 23456, b, d, e, f));
        collection.elements.add(new B(100, "sue", 34567, c));

        ReactiveAeroMapper reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();
        reactiveMapper.save(collection).subscribeOn(Schedulers.parallel()).block();
        reactiveMapper.save(a,b,c,d,e,f,g,h,i,j).subscribeOn(Schedulers.parallel()).collectList().block();

        ReactiveVirtualList<B> list = reactiveMapper.asBackedList(collection, "elements", B.class);
//		list.append(new CollectionElement(103, "tom", 45678));
//		System.out.println("Get by index returned: " + list.get(2));
//		System.out.println("Delete by Key Range returned: " + list.removeByKeyRange(100, 102, true));
        List<B> results = list.beginMultiOperation()
                .append(new B(104, "tim", 22222, i, e, f))
                .append(new B(103, "tom", 45678, h, g, g))
                .append(new B(105, "sam", 33333, j, a, b))
                .append(new B(106, "rob", 44444, j, g))
                .getByKeyRange(101, 105)
//				.removeByKeyRange(100, 102).asResult()
//				.get(0)
//				.size()
                .end();

        assertEquals(4, results.size());
        // Note that the results will be sorted by the id as we're using a K_ORDERED map
        assertEquals(101, results.get(0).id);
        assertEquals("joe", results.get(0).name);
        assertEquals(23456, results.get(0).date);
        assertEquals(b, results.get(0).thisC);
        assertEquals(d, results.get(0).Cs.get(0));
        assertEquals(e, results.get(0).Cs.get(1));
        assertEquals(f, results.get(0).Cs.get(2));

        assertEquals(102, results.get(1).id);
        assertEquals(103, results.get(2).id);
        assertEquals(104, results.get(3).id);

        A result = reactiveMapper.read(A.class, collection.id).subscribeOn(Schedulers.parallel()).block();
        assert result != null;
        assertEquals(collection.id, result.id);
        assertEquals(7, result.elements.size());

        // Note that the returned results will be sorted, the inputs will not be.
        for (int x = 0; x < collection.elements.size(); x++) {
            boolean found = false;
            for (int y = 0; y < result.elements.size(); y++) {
                if (collection.elements.get(x).id == result.elements.get(y).id) {
                    assertEquals(collection.elements.get(x), result.elements.get(y));
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
    }
}
