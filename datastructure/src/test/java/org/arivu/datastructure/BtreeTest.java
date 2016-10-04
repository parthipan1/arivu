/**
 * 
 */
package org.arivu.datastructure;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.locks.Lock;

import org.arivu.utils.lock.AtomicWFReentrantLock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author P
 *
 */
public class BtreeTest {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDirectionGet() {
		assertTrue(Direction.left.get(null) == null);
		assertTrue(Direction.right.get(null) == null);
		Lock lock = new AtomicWFReentrantLock();
		LinkedReference l = new LinkedReference(CompareStrategy.EQUALS, lock);
		LinkedReference ll = new LinkedReference(CompareStrategy.EQUALS, lock);
		LinkedReference lr = new LinkedReference(CompareStrategy.EQUALS, lock);

		// System.out.println(" l "+l);
		// System.out.println(" ll "+ll);
		// System.out.println(" lr "+lr);

		ll.obj = "l";
		lr.obj = "r";

		assertFalse(l.add(null));
		assertTrue(l.isEmpty());
		assertTrue(Direction.left.remove(l) == null);
		assertTrue(Direction.right.remove(l) == null);

		Direction.left.set(null, null);
		Direction.left.set(l, null);
		Direction.left.set(l, ll);
		Direction.right.set(null, null);
		Direction.right.set(l, null);
		Direction.right.set(l, lr);

		Direction.right.set(lr, ll);

		assertFalse(l.isEmpty());

		assertTrue(Direction.left.get(l) == ll);
		assertTrue(Direction.right.get(l) == lr);

		assertTrue(Direction.left.getOther() == Direction.right);
		assertTrue(Direction.right.getOther() == Direction.left);

		assertTrue(Direction.left.remove(null) == null);
		assertTrue(Direction.left.remove(l) == ll);
		
		assertTrue(Btree.getAll(null).size()==0);

	}

	@Test
	public void testSearch() {
		Lock lock = new AtomicWFReentrantLock();
		LinkedReference l = new LinkedReference(CompareStrategy.EQUALS, lock);
		l.add("r");
		l.add("l");

		assertTrue(l.search(null) == null);
		assertTrue(l.search("1") == null);
		assertTrue(l.search("l") == l.left);
		assertTrue(l.search("r") == l.right);
	}

	// @Test
	// @Ignore
	// public void testNodeLeaves() {
	// Btree bt = new Btree();
	//// Btree.Node n = new Btree.Node();
	// bt.add("1", new int[]{0});
	// Btree.Node n = bt.root;
	// int cnt = 0;
	// for (int i = 0; i < n.refs.length; i++) {
	// if(n.refs[i] == null){
	// cnt++;
	// }
	// }
	// assertTrue(cnt==1);
	// bt.resetLeaves(n);
	// assertTrue(n.refs==null);
	//// cnt = 0;
	//// for (int i = 0; i < n.refs.length; i++) {
	//// if(n.refs[i] == null){
	//// cnt++;
	//// }
	//// }
	//// assertTrue(cnt==2);
	// }

	@Test
	public void testNodeLeavesRemove() {
		Btree bt = new Btree();
		// Btree.Node n = bt.root;

		// List<Node> rns = new DoublyLinkedList<Btree.Node>();

		assertTrue(bt.removeObj("1", new int[] { 0 }) == null);

		bt.addObj("1", new int[] { 0 });

		assertTrue(bt.removeObj("2", new int[] { 0 }) == null);
		assertFalse(bt.removeObj("1", new int[] { 0 }) == null);

	}

	@Test
	public void testNodeDirection() {
		assertTrue(Direction.valueOf("right") == Direction.right);
		assertTrue(Direction.valueOf("left") == Direction.left);
		assertTrue(Direction.values().length == 2);
	}

	@Test
	public void testBtreeConstruction1() {
		try {
			new Btree(0);
			fail("failed on powerbase 0");
		} catch (IllegalArgumentException e) {
			assertTrue(e != null);
		}
		try {
			new Btree(5);
			fail("failed on powerbase 5");
		} catch (IllegalArgumentException e) {
			assertTrue(e != null);
		}

		Btree b = new Btree();
		assertTrue(b.getHeight() == 8);
		assertTrue(b.remove(null) == null);
		assertTrue(b.get(null) == null);

		assertTrue(b.size() == 0);
		b.add(null);
		assertTrue(b.size() == 0);
	}

	static class TestAdd {
		final String val;

		/**
		 * @param val
		 */
		public TestAdd(String val) {
			super();
			this.val = val;
		}

		@Override
		public int hashCode() {
			// final int prime = 31;
			// int result = 1;
			// result = prime * result + ((val == null) ? 0 : val.hashCode());
			// return result;
			return 0;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestAdd other = (TestAdd) obj;
			if (val == null) {
				if (other.val != null)
					return false;
			} else if (!val.equals(other.val))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "TestAdd [val=" + val + "]";
		}

	}

	@Test
	public void testBtreeAddExpand() {
		Btree bt = new Btree();

		TestAdd obj0 = new TestAdd("0");
		TestAdd obj1 = new TestAdd("1");
		TestAdd obj2 = new TestAdd("2");
		TestAdd obj3 = new TestAdd("3");
		TestAdd obj4 = new TestAdd("4");
		TestAdd obj5 = new TestAdd("5");
		TestAdd obj6 = new TestAdd("6");
		TestAdd obj7 = new TestAdd("7");
		TestAdd obj8 = new TestAdd("8");
		TestAdd obj9 = new TestAdd("9");
		TestAdd obj10 = new TestAdd("10");
		TestAdd obj11 = new TestAdd("11");
		TestAdd obj12 = new TestAdd("12");
		TestAdd obj13 = new TestAdd("13");
		TestAdd obj14 = new TestAdd("14");
		TestAdd obj15 = new TestAdd("15");

		bt.add(obj0);
		bt.add(obj1);
		bt.add(obj2);
		bt.add(obj3);
		bt.add(obj4);
		bt.add(obj5);
		bt.add(obj6);
		bt.add(obj7);
		bt.add(obj8);
		bt.add(obj9);
		bt.add(obj10);
		bt.add(obj11);
		bt.add(obj12);
		bt.add(obj13);
		bt.add(obj14);
		bt.add(obj15);

		assertTrue(bt.size == 16);

		Object[] findLeaf = bt.findLeaf(obj0, bt.getPathObj(obj0), null);

		assertTrue(findLeaf.length == 16);

		assertTrue(Btree.getSize(findLeaf) == 16);

		TestAdd obj16 = new TestAdd("16");
		
		bt.add(obj16);

		assertTrue(bt.size == 17);

		findLeaf = bt.findLeaf(obj0, bt.getPathObj(obj0), null);

		assertTrue(findLeaf.length == 32);

		assertTrue(Btree.getSize(findLeaf) == 17);
		assertTrue(Btree.getSize(null) == 0);
	}
	
	@Test
	public void testBtreeAddExpand_Case2() {
		Btree bt = new Btree();

		TestAdd obj0 = new TestAdd("0");
//		TestAdd obj1 = new TestAdd("1");
//		TestAdd obj2 = new TestAdd("2");
//		TestAdd obj3 = new TestAdd("3");
//		TestAdd obj4 = new TestAdd("4");
//		TestAdd obj5 = new TestAdd("5");
//		TestAdd obj6 = new TestAdd("6");
//		TestAdd obj7 = new TestAdd("7");
//		TestAdd obj8 = new TestAdd("8");
//		TestAdd obj9 = new TestAdd("9");
//		TestAdd obj10 = new TestAdd("10");
//		TestAdd obj11 = new TestAdd("11");
//		TestAdd obj12 = new TestAdd("12");
//		TestAdd obj13 = new TestAdd("13");
//		TestAdd obj14 = new TestAdd("14");
//		TestAdd obj15 = new TestAdd("15");

		bt.add(obj0);
		bt.add(obj0);

		assertTrue(bt.size == 1);

		Object[] findLeaf = bt.findLeaf(obj0, bt.getPathObj(obj0), null);

		assertTrue(findLeaf.length == 16);

		assertTrue(Btree.getSize(findLeaf) == 1);

	}
	
	@Test
	public void testBtreeAddExpand_Case3() {
		Btree bt = new Btree();

		TestAdd obj0 = new TestAdd("0");
		TestAdd obj1 = new TestAdd("1");
		TestAdd obj2 = new TestAdd("2");
		TestAdd obj3 = new TestAdd("3");
		TestAdd obj4 = new TestAdd("4");
		TestAdd obj5 = new TestAdd("5");
		TestAdd obj6 = new TestAdd("6");
		TestAdd obj7 = new TestAdd("7");
		TestAdd obj8 = new TestAdd("8");
		TestAdd obj9 = new TestAdd("9");
		TestAdd obj10 = new TestAdd("10");
		TestAdd obj11 = new TestAdd("11");
		TestAdd obj12 = new TestAdd("12");
		TestAdd obj13 = new TestAdd("13");
		TestAdd obj14 = new TestAdd("14");
		TestAdd obj15 = new TestAdd("15");

		bt.add(obj0);
		bt.add(obj1);
		bt.add(obj2);
		bt.add(obj3);
		bt.add(obj4);
		bt.add(obj5);
		bt.add(obj6);
		bt.add(obj7);
		bt.add(obj8);
		bt.add(obj9);
		bt.add(obj10);
		bt.add(obj11);
		bt.add(obj12);
		bt.add(obj13);
		bt.add(obj14);
		bt.add(obj15);

		assertTrue(bt.size == 16);

		Object[] findLeaf = bt.findLeaf(obj0, bt.getPathObj(obj0), null);

		assertTrue(findLeaf.length == 16);

		assertTrue(Btree.getSize(findLeaf) == 16);

		bt.remove(obj8);
		
		findLeaf = bt.findLeaf(obj0, bt.getPathObj(obj0), null);

		assertTrue(findLeaf.length == 16);

		assertTrue(Btree.getSize(findLeaf) == 15);

		bt.add(obj8);
		
		findLeaf = bt.findLeaf(obj0, bt.getPathObj(obj0), null);

		assertTrue(findLeaf.length == 16);

		assertTrue(Btree.getSize(findLeaf) == 16);
		
		TestAdd obj16 = new TestAdd("16");
		
		bt.add(obj16);

		assertTrue(bt.size == 17);

		findLeaf = bt.findLeaf(obj0, bt.getPathObj(obj0), null);

		assertTrue(findLeaf.length == 32);

		assertTrue(Btree.getSize(findLeaf) == 17);
		assertTrue(Btree.getSize(null) == 0);
	}
	

	@Test
	public void testBtreeAddExpand_Case4() {
		Btree bt = new Btree();

		TestAdd obj0 = new TestAdd("0");
		TestAdd obj1 = new TestAdd("1");
		TestAdd obj2 = new TestAdd("2");
		TestAdd obj3 = new TestAdd("3");
		TestAdd obj4 = new TestAdd("4");
		TestAdd obj5 = new TestAdd("5");
		TestAdd obj6 = new TestAdd("6");
		TestAdd obj7 = new TestAdd("7");
		TestAdd obj8 = new TestAdd("8");
		TestAdd obj9 = new TestAdd("9");
		TestAdd obj10 = new TestAdd("10");
		TestAdd obj11 = new TestAdd("11");
		TestAdd obj12 = new TestAdd("12");
		TestAdd obj13 = new TestAdd("13");
		TestAdd obj14 = new TestAdd("14");
		TestAdd obj15 = new TestAdd("15");

		bt.add(obj0);
		bt.add(obj1);
		bt.add(obj2);
		bt.add(obj3);
		bt.add(obj4);
		bt.add(obj5);
		bt.add(obj6);
		bt.add(obj7);
		bt.add(obj8);
		bt.add(obj9);
		bt.add(obj10);
		bt.add(obj11);
		bt.add(obj12);
		bt.add(obj13);
		bt.add(obj14);
		bt.add(obj15);

		assertTrue(bt.size == 16);

		Object[] findLeaf = bt.findLeaf(obj0, bt.getPathObj(obj0), null);

		assertTrue(findLeaf.length == 16);

		assertTrue(Btree.getSize(findLeaf) == 16);

		bt.remove(obj8);
		
		findLeaf = bt.findLeaf(obj0, bt.getPathObj(obj0), null);

		assertTrue(findLeaf.length == 16);

		assertTrue(Btree.getSize(findLeaf) == 15);

		bt.add(obj8);
		
		findLeaf = bt.findLeaf(obj0, bt.getPathObj(obj0), null);

		assertTrue(findLeaf.length == 16);

		assertTrue(Btree.getSize(findLeaf) == 16);
		
		TestAdd obj16 = new TestAdd("16");
		
		bt.add(obj16);

		assertTrue(bt.size == 17);

		findLeaf = bt.findLeaf(obj0, bt.getPathObj(obj0), null);

		assertTrue(findLeaf.length == 32);

		assertTrue(Btree.getSize(findLeaf) == 17);
		assertTrue(Btree.getSize(null) == 0);
	}
	

	@Test
	public void testBtreeAddExpand_Case5() {
		Btree bt = new Btree();

		TestAdd obj0 = new TestAdd("0");
//		TestAdd obj1 = new TestAdd("1");
//		TestAdd obj2 = new TestAdd("2");
//		TestAdd obj3 = new TestAdd("3");
//		TestAdd obj4 = new TestAdd("4");
//		TestAdd obj5 = new TestAdd("5");
//		TestAdd obj6 = new TestAdd("6");
//		TestAdd obj7 = new TestAdd("7");
//		TestAdd obj8 = new TestAdd("8");
//		TestAdd obj9 = new TestAdd("9");
//		TestAdd obj10 = new TestAdd("10");
//		TestAdd obj11 = new TestAdd("11");
//		TestAdd obj12 = new TestAdd("12");
//		TestAdd obj13 = new TestAdd("13");
//		TestAdd obj14 = new TestAdd("14");
//		TestAdd obj15 = new TestAdd("15");

		bt.add(obj0);
		bt.remove(obj0);

//		assertTrue(bt.size == 1);
//
//		Object[] findLeaf = bt.findLeaf(obj0, bt.getPath(obj0), null);
//
//		assertTrue(findLeaf.length == 16);
//
//		assertTrue(Btree.getSize(findLeaf) == 1);

	}
}
