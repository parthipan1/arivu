/**
 * 
 */
package org.arivu.datastructure;

import static org.junit.Assert.*;

import java.util.Collection;

import org.arivu.datastructure.Graph.CyclicException;
import org.arivu.datastructure.Graph.Identity;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author P
 *
 */
public class GraphTest {

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

//	/**
//	 * Test method for {@link org.arivu.datastructure.Graph#size()}.
//	 */
//	@Test
//	public void testSize() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link org.arivu.datastructure.Graph#isEmpty()}.
//	 */
//	@Test
//	public void testIsEmpty() {
//		fail("Not yet implemented");
//	}

	static class TestIdentity implements Graph.Identity{

		final String val;
		final Collection<TestIdentity> children = new DoublyLinkedList<GraphTest.TestIdentity>();
		final Collection<TestIdentity> parents = new DoublyLinkedList<GraphTest.TestIdentity>();
		
		TestIdentity(String val) {
			super();
			this.val = val;
		}

		@Override
		public Collection<? extends Identity> getChildren() {
			return children;
		}

		@Override
		public Collection<? extends Identity> getParents() {
			return parents;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((val == null) ? 0 : val.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestIdentity other = (TestIdentity) obj;
			if (val == null) {
				if (other.val != null)
					return false;
			} else if (!val.equals(other.val))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "TestIdentity [val=" + val + "]";
		}
		
	} 
	
	/**
	 * Test method for {@link org.arivu.datastructure.Graph#add(org.arivu.datastructure.Graph.Identity)}.
	 * @throws CyclicException 
	 */
	@Test
	public void testAdd() throws CyclicException {
		TestIdentity one = new TestIdentity("1");
		TestIdentity two = new TestIdentity("2");
		TestIdentity three = new TestIdentity("3");
		
		Graph graph = new Graph();
		
		graph.add(one);
		graph.add(two);
		graph.add(three);
		
		assertTrue("Failed in max Level",graph.getMaxLevel()==0);
		assertTrue("Failed in resolve",graph.get(0).size()==3);
		
	}

	@Test
	public void testAdd_Case1() throws CyclicException {
		TestIdentity one = new TestIdentity("1");
		TestIdentity two = new TestIdentity("2");
		TestIdentity three = new TestIdentity("3");
		
		two.parents.add(one);
		
		Graph graph = new Graph();
		
		graph.add(one);
		graph.add(two);
		graph.add(three);
		
		assertTrue("Failed in max Level GOT :: "+graph.getMaxLevel(),graph.getMaxLevel()==1);
		
		assertTrue("Failed in resolve",graph.get(0).size()==2);
		assertTrue("Failed in resolve",graph.get(1).size()==1);
		
		assertTrue("Failed in resolve",graph.get(0).contains(one));
		assertTrue("Failed in resolve",graph.get(0).contains(three));
		assertTrue("Failed in resolve",graph.get(1).contains(two));
		
		
	}
	
	@Test
	public void testAdd_Case2() throws CyclicException {
		TestIdentity one = new TestIdentity("1");
		TestIdentity two = new TestIdentity("2");
		TestIdentity three = new TestIdentity("3");
		
		one.children.add(two);
		
		Graph graph = new Graph();
		
		graph.add(one);
		graph.add(two);
		graph.add(three);
		
		assertTrue("Failed in max Level",graph.getMaxLevel()==1);
		
		assertTrue("Failed in resolve",graph.get(0).size()==2);
		assertTrue("Failed in resolve",graph.get(1).size()==1);
		
		assertTrue("Failed in resolve",graph.get(0).contains(one));
		assertTrue("Failed in resolve",graph.get(0).contains(three));
		assertTrue("Failed in resolve",graph.get(1).contains(two));
		
		
	}
	
//	/**
//	 * Test method for {@link org.arivu.datastructure.Graph#remove(java.lang.Object)}.
//	 */
//	@Test
//	public void testRemove() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link org.arivu.datastructure.Graph#addAll(java.util.Collection)}.
//	 */
//	@Test
//	public void testAddAll() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link org.arivu.datastructure.Graph#removeAll(java.util.Collection)}.
//	 */
//	@Test
//	public void testRemoveAll() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link org.arivu.datastructure.Graph#clear()}.
//	 */
//	@Test
//	public void testClear() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link org.arivu.datastructure.Graph#getMaxLevel()}.
//	 */
//	@Test
//	public void testGetMaxLevel() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link org.arivu.datastructure.Graph#get(int)}.
//	 */
//	@Test
//	public void testGet() {
//		fail("Not yet implemented");
//	}

}
