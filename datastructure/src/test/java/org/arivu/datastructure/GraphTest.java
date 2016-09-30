/**
 * 
 */
package org.arivu.datastructure;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.arivu.datastructure.Graph.Algo;
import org.arivu.datastructure.Graph.CyclicException;
import org.arivu.datastructure.Graph.Direction;
import org.arivu.datastructure.Graph.Edges;
import org.arivu.datastructure.Graph.Node;
import org.arivu.datastructure.Graph.Visitor;
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

	// /**
	// * Test method for {@link org.arivu.datastructure.Graph#size()}.
	// */
	// @Test
	// public void testSize() {
	// fail("Not yet implemented");
	// }
	//
	// /**
	// * Test method for {@link org.arivu.datastructure.Graph#isEmpty()}.
	// */
	// @Test
	// public void testIsEmpty() {
	// fail("Not yet implemented");
	// }

	static class TestEdges implements Edges {

		@Override
		public Collection<Object> in(Object obj) {
			if (obj instanceof TestIdentity) {
				return ((TestIdentity) obj).getParents();
			}
			return null;
		}

		@Override
		public Collection<Object> out(Object obj) {
			if (obj instanceof TestIdentity) {
				return ((TestIdentity) obj).getChildren();
			}
			return null;
		}

	}

	static class TestIdentity {

		final String val;
		final Collection<Object> children = new DoublyLinkedList<Object>();
		final Collection<Object> parents = new DoublyLinkedList<Object>();

		TestIdentity(String val) {
			super();
			this.val = val;
		}

		public Collection<Object> getChildren() {
			return children;
		}

		public Collection<Object> getParents() {
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
//			return "TestIdentity [val=" + val + "]";
			return val.toString();
		}

	}

	/**
	 * Test method for
	 * {@link org.arivu.datastructure.Graph#add(org.arivu.datastructure.Graph.Identity)}
	 * .
	 * 
	 * @throws CyclicException
	 */
	@Test
	public void testAdd() throws CyclicException {
		TestIdentity one = new TestIdentity("1");
		TestIdentity two = new TestIdentity("2");
		TestIdentity three = new TestIdentity("3");

		Graph graph = new Graph(new TestEdges());

		graph.add(one);
		graph.add(two);
		graph.add(three);

		assertTrue("Failed in max Level", graph.getMaxLevel() == 0);
		assertTrue("Failed in resolve", graph.get(0).size() == 3);

	}

	@Test
	public void testAdd_Case1() throws CyclicException {
		TestIdentity one = new TestIdentity("1");
		TestIdentity two = new TestIdentity("2");
		TestIdentity three = new TestIdentity("3");

		two.parents.add(one);

		TestEdges edges = new TestEdges();
		Graph graph = new Graph(edges);
		
		assertTrue(graph.size()==0);
		assertTrue(graph.isEmpty());
		
		graph.add(one);
		graph.add(two);
		graph.add(three);
		assertTrue(graph.print()!=null);
		
		graph.resolve();
		
		assertTrue("Failed in max Level GOT :: " + graph.getMaxLevel(), graph.getMaxLevel() == 1);

		assertTrue("Failed in resolve", graph.get(0).size() == 2);
		assertTrue("Failed in resolve", graph.get(1).size() == 1);

		assertTrue("Failed in resolve", graph.get(0).contains(one));
		assertTrue("Failed in resolve", graph.get(0).contains(three));
		assertTrue("Failed in resolve", graph.get(1).contains(two));
		
		graph.setEdges(edges);
		graph.resolve(edges);
		assertTrue(graph.getEdges()==edges);
		
		graph.clear();
		assertTrue(graph.size()==0);
		assertTrue(graph.isEmpty());
	}

	@Test
	public void testAdd_Case2() throws CyclicException {
		TestIdentity one = new TestIdentity("1");
		TestIdentity two = new TestIdentity("2");
		TestIdentity three = new TestIdentity("3");

		one.children.add(two);

		Graph graph = new Graph(new TestEdges());

		graph.add(one);
		graph.add(two);
		graph.add(three);

		assertTrue("Failed in max Level", graph.getMaxLevel() == 1);

		assertTrue("Failed in resolve", graph.get(0).size() == 2);
		assertTrue("Failed in resolve", graph.get(1).size() == 1);

		assertTrue("Failed in resolve", graph.get(0).contains(one));
		assertTrue("Failed in resolve", graph.get(0).contains(three));
		assertTrue("Failed in resolve", graph.get(1).contains(two));
	}

	@Test(expected = CyclicException.class)
	public void testAdd_Case3_Cyclic() throws CyclicException {
		try {
			TestIdentity one = new TestIdentity("1");
			TestIdentity two = new TestIdentity("2");
			TestIdentity three = new TestIdentity("3");
			one.children.add(two);
			two.children.add(one);
			Graph graph = new Graph(new TestEdges());
			assertFalse(graph.add(null));
			graph.add(one);
			graph.add(two);
			graph.add(three);
			assertTrue("Failed in max Level", graph.getMaxLevel() == 1);
			assertTrue("Failed in resolve", graph.get(0).size() == 2);
			assertTrue("Failed in resolve", graph.get(1).size() == 1);
			assertTrue("Failed in resolve", graph.get(0).contains(one));
			assertTrue("Failed in resolve", graph.get(0).contains(three));
			assertTrue("Failed in resolve", graph.get(1).contains(two));
		} catch (CyclicException e) {
			assertFalse(e.getSource().isEmpty());
			throw e;
		}
	}

	@Test
	public void testAdd_Case4() throws CyclicException {
		TestIdentity one = new TestIdentity("1");
		TestIdentity two = new TestIdentity("2");
		TestIdentity three = new TestIdentity("3");

		one.children.add(two);
		two.children.add(three);

		Graph graph = new Graph(new TestEdges());

		graph.add(one);
		graph.add(two);
		graph.add(three);

		assertTrue("Failed in max Level", graph.getMaxLevel() == 2);

		assertTrue("Failed in resolve", graph.get(0).size() == 1);
		assertTrue("Failed in resolve", graph.get(1).size() == 1);
		assertTrue("Failed in resolve", graph.get(2).size() == 1);

		assertTrue("Failed in resolve", graph.get(0).contains(one));
		assertTrue("Failed in resolve", graph.get(1).contains(two));
		assertTrue("Failed in resolve", graph.get(2).contains(three));
	}

	@Test
	public void testAdd_Case5() throws CyclicException {
		TestIdentity a = new TestIdentity("a");
		TestIdentity b = new TestIdentity("b");
		TestIdentity c = new TestIdentity("c");
		TestIdentity d = new TestIdentity("d");
		TestIdentity e = new TestIdentity("e");

		a.children.add(b);
		a.children.add(d);
		b.children.add(c);
		b.children.add(e);
		c.children.add(e);
		d.children.add(b);

		Graph graph = new Graph(new TestEdges());

		graph.add(a);
		// graph.print();
		graph.add(b);
		// graph.print();
		graph.add(c);
		// graph.print();
		graph.add(d);
		// graph.print();
		graph.add(e);
		// graph.print();

		assertTrue("Failed in max Level", graph.getMaxLevel() == 4);

		assertTrue("Failed in resolve", graph.get(0).size() == 1);
		assertTrue("Failed in resolve", graph.get(1).size() == 1);
		assertTrue("Failed in resolve", graph.get(2).size() == 1);
		assertTrue("Failed in resolve", graph.get(3).size() == 1);
		assertTrue("Failed in resolve", graph.get(4).size() == 1);

		assertTrue("Failed in resolve", graph.get(0).contains(a));
		assertTrue("Failed in resolve", graph.get(1).contains(d));
		assertTrue("Failed in resolve", graph.get(2).contains(b));
		assertTrue("Failed in resolve", graph.get(3).contains(c));
		assertTrue("Failed in resolve", graph.get(4).contains(e));
	}

	@Test(expected = CyclicException.class)
	public void testAdd_Case6_Cyclic() throws CyclicException {
		TestIdentity a = new TestIdentity("a");
		TestIdentity b = new TestIdentity("b");
		TestIdentity c = new TestIdentity("c");
		TestIdentity d = new TestIdentity("d");
		TestIdentity e = new TestIdentity("e");

		a.children.add(b);
		a.children.add(d);
		b.children.add(c);
		b.children.add(e);
		c.children.add(e);
		c.children.add(d);
		d.children.add(b);

		Graph graph = new Graph(new TestEdges());

		graph.add(a);
		// graph.print();
		graph.add(b);
		// graph.print();
		graph.add(c);
		// graph.print();
		graph.add(d);
		// graph.print();
		graph.add(e);
		// graph.print();

		assertTrue("Failed in max Level", graph.getMaxLevel() == 4);

		assertTrue("Failed in resolve", graph.get(0).size() == 1);
		assertTrue("Failed in resolve", graph.get(1).size() == 1);
		assertTrue("Failed in resolve", graph.get(2).size() == 1);
		assertTrue("Failed in resolve", graph.get(3).size() == 1);
		assertTrue("Failed in resolve", graph.get(4).size() == 1);

		assertTrue("Failed in resolve", graph.get(0).contains(a));
		assertTrue("Failed in resolve", graph.get(1).contains(d));
		assertTrue("Failed in resolve", graph.get(2).contains(b));
		assertTrue("Failed in resolve", graph.get(3).contains(c));
		assertTrue("Failed in resolve", graph.get(4).contains(e));
	}

	@Test
	public void testAdd_Bfs() throws CyclicException {
		TestIdentity a = new TestIdentity("a");
		TestIdentity b = new TestIdentity("b");
		TestIdentity c = new TestIdentity("c");
		TestIdentity d = new TestIdentity("d");
		TestIdentity e = new TestIdentity("e");

		a.children.add(b);
		a.children.add(d);
		b.children.add(c);
		b.children.add(e);
		c.children.add(e);
		d.children.add(b);

		Graph graph = new Graph(new TestEdges());

		graph.add(a);
		// graph.print();
		graph.add(b);
		// graph.print();
		graph.add(c);
		// graph.print();
		graph.add(d);
		// graph.print();
		graph.add(e);
//		 graph.print();

		assertTrue("Failed in max Level", graph.getMaxLevel() == 4);

		assertTrue("Failed in resolve", graph.get(0).size() == 1);
		assertTrue("Failed in resolve", graph.get(1).size() == 1);
		assertTrue("Failed in resolve", graph.get(2).size() == 1);
		assertTrue("Failed in resolve", graph.get(3).size() == 1);
		assertTrue("Failed in resolve", graph.get(4).size() == 1);

		assertTrue("Failed in resolve", graph.get(0).contains(a));
		assertTrue("Failed in resolve", graph.get(1).contains(d));
		assertTrue("Failed in resolve", graph.get(2).contains(b));
		assertTrue("Failed in resolve", graph.get(3).contains(c));
		assertTrue("Failed in resolve", graph.get(4).contains(e));

		final StringBuffer buf = new StringBuffer();
		final Visitor visitor = new Visitor() {

			@Override
			public void visit(Object obj, int level) {
				buf.append(obj);
			}
		};

		graph.visit(a, visitor, Direction.out, Algo.BFS, true);
		
		assertTrue("Failed in bfs GOT :: "+buf.toString()+" EXP :: adbce", buf.toString().equals("adbce") );
		
		final StringBuffer buf2 = new StringBuffer();
		final Visitor visitor2 = new Visitor() {

			@Override
			public void visit(Object obj, int level) {
				buf2.append(obj);
			}
		};

		// Direction.out , Algo.BFS - default
		graph.visit(a, visitor2, null, null, false);
		
		assertTrue("Failed in bfs GOT :: "+buf2.toString()+" EXP :: adbce", buf2.toString().equals("adbce") );
		
		graph.visit(null, visitor2, null, null, false);
		graph.visit(a, null, null, null, false);
		graph.visit(null, null, null, null, false);
		graph.visit(new TestIdentity("f"), visitor2, null, null, false);
		graph.visit( Graph.getWrapper(new TestIdentity("f")) , visitor2, null, null, false);
	}
	
	@Test
	public void testAdd_Dfs() throws CyclicException {
		TestIdentity a = new TestIdentity("a");
		TestIdentity b = new TestIdentity("b");
		TestIdentity c = new TestIdentity("c");
		TestIdentity d = new TestIdentity("d");
		TestIdentity e = new TestIdentity("e");

		a.children.add(b);
		a.children.add(d);
		b.children.add(c);
		b.children.add(e);
		c.children.add(e);
		d.children.add(b);

		Graph graph = new Graph(new TestEdges());

		graph.add(a);
		// graph.print();
		graph.add(b);
		// graph.print();
		graph.add(c);
		// graph.print();
		graph.add(d);
		// graph.print();
		graph.add(e);
		
		assertFalse(graph.addInternal(null, true));
//		 graph.print();

		assertTrue("Failed in max Level", graph.getMaxLevel() == 4);

		assertTrue("Failed in resolve", graph.get(0).size() == 1);
		assertTrue("Failed in resolve", graph.get(1).size() == 1);
		assertTrue("Failed in resolve", graph.get(2).size() == 1);
		assertTrue("Failed in resolve", graph.get(3).size() == 1);
		assertTrue("Failed in resolve", graph.get(4).size() == 1);

		assertTrue("Failed in resolve", graph.get(0).contains(a));
		assertTrue("Failed in resolve", graph.get(1).contains(d));
		assertTrue("Failed in resolve", graph.get(2).contains(b));
		assertTrue("Failed in resolve", graph.get(3).contains(c));
		assertTrue("Failed in resolve", graph.get(4).contains(e));

		final StringBuffer buf = new StringBuffer();
		final Visitor visitor = new Visitor() {

			@Override
			public void visit(Object obj, int level) {
				buf.append(obj);
			}
		};

		graph.visit(a, visitor, Direction.out, Algo.DFS, true);
		
		assertTrue("Failed in bfs GOT :: "+buf.toString()+" EXP :: ecbda", buf.toString().equals("ecbda") );
		
		final StringBuffer buf2 = new StringBuffer();
		final Visitor visitor2 = new Visitor() {

			@Override
			public void visit(Object obj, int level) {
				buf2.append(obj);
			}
		};

		graph.visit(a, visitor2, Direction.out, Algo.DFS, false);
		
		assertTrue("Failed in bfs GOT :: "+buf2.toString()+" EXP :: ecbda", buf2.toString().equals("ecbda") );
		
	}
	
	@Test
	public void testAdd_Dfs_Case1() throws CyclicException {
		TestIdentity a = new TestIdentity("a");
		TestIdentity b = new TestIdentity("b");
		TestIdentity c = new TestIdentity("c");
		TestIdentity d = new TestIdentity("d");
		TestIdentity e = new TestIdentity("e");

		a.children.add(d);
		b.children.add(d);
		b.children.add(e);
		c.children.add(e);
		d.children.add(e);

		Graph graph = new Graph(new TestEdges());

		graph.add(a);
		// graph.print();
		graph.add(b);
		// graph.print();
		graph.add(c);
		// graph.print();
		graph.add(d);
		// graph.print();
		graph.add(e);
//		graph.print();

		assertTrue("Failed in max Level", graph.getMaxLevel() == 2);

		assertTrue("Failed in resolve", graph.get(0).size() == 3);
		assertTrue("Failed in resolve", graph.get(1).size() == 1);
		assertTrue("Failed in resolve", graph.get(2).size() == 1);

		assertTrue("Failed in resolve", graph.get(0).contains(a));
		assertTrue("Failed in resolve", graph.get(0).contains(b));
		assertTrue("Failed in resolve", graph.get(0).contains(c));
		assertTrue("Failed in resolve", graph.get(1).contains(d));
		assertTrue("Failed in resolve", graph.get(2).contains(e));

		final StringBuffer buf = new StringBuffer();
		final Visitor visitor = new Visitor() {

			@Override
			public void visit(Object obj, int level) {
				buf.append(obj);
			}
		};

		graph.visit(e, visitor, Direction.in, Algo.DFS, true);
		
		assertTrue("Failed in bfs GOT :: "+buf.toString()+" EXP :: abcde", buf.toString().equals("abcde") );
		
		final StringBuffer buf2 = new StringBuffer();
		final Visitor visitor2 = new Visitor() {

			@Override
			public void visit(Object obj, int level) {
				buf2.append(obj);
			}
		};

		graph.visit(e, visitor2, Direction.in, Algo.DFS, false);
		
		assertTrue("Failed in bfs GOT :: "+buf2.toString()+" EXP :: abcde", buf2.toString().equals("abcde") );
		
	}
	
	@Test
	public void testGetWrapper(){
		assertTrue(Graph.getWrapper(null)==null);
		Node<Object> wrapper = Graph.getWrapper("1");
		assertTrue(Graph.getWrapper(wrapper)==wrapper);
		
		List<Object> asList = Arrays.asList(new Object[]{"1","2"});
		String exp = "1,2";
		
		assertTrue(Graph.getStrt(asList).toString().equals(exp));
		
		List<Node<Object>> asListNode = new ArrayList<Graph.Node<Object>>();
		for(Object o:asList)
			asListNode.add(Graph.getWrapper(o));
		
		exp = "1::"+Integer.MIN_VALUE+",2::"+Integer.MIN_VALUE;
		assertTrue(Graph.getStr(asListNode).toString().equals(exp));
	}
	
	@Test
	public void testRemove(){
		TestIdentity one = new TestIdentity("1");
		TestIdentity two = new TestIdentity("2");
		TestIdentity three = new TestIdentity("3");

		two.parents.add(one);

		TestEdges edges = new TestEdges();
		Graph graph = new Graph(edges);
		
		assertTrue(graph.size()==0);
		assertTrue(graph.isEmpty());
		
		Collection<Object> all = new ArrayList<Object>();
		all.add(one);
		all.add(two);
		all.add(three);
		
		try {
			assertTrue(graph.addAll(all));
		} catch (CyclicException e) {
			fail("Failed on allAll");
		}
		assertFalse( graph.removeInternal(new TestIdentity("4")));
		assertFalse( graph.removeInternal( Graph.getWrapper(new TestIdentity("4")) ));
		
		try {
			assertTrue(graph.remove(one));
		} catch (CyclicException e1) {
			fail("Failed on remove");
		}
		
		try {
			assertTrue(graph.removeAll(all));
		} catch (CyclicException e) {
			fail("Failed on removeAll");
		}
	}
	
	@Test
	public void testGetList(){
		List<Object> asList = Arrays.asList(new Object[]{"1","2"});
		
		DoublyLinkedSet<Node<Object>> tempAll = new DoublyLinkedSet<Graph.Node<Object>>();
		for(Object o:asList)
			tempAll.add(Graph.getWrapper(o));
		
		assertTrue(Graph.get(null, tempAll, false)==null);
		assertTrue(Graph.get("1", tempAll, false)!=null);
		
		assertTrue(Graph.get(Graph.getWrapper("1"), tempAll, false).obj.equals("1"));
		assertTrue(Graph.get(Graph.getWrapper("2"), tempAll, false).obj.equals("2"));
		
		assertTrue(Graph.get("3", tempAll, false).obj.equals("3"));
		assertTrue(tempAll.size()==2);
		assertTrue(Graph.get("3", tempAll, true).obj.equals("3"));
		assertTrue(tempAll.size()==3);
		
	}
	
	@Test
	public void testNode(){
		
		Node<Object> nullWrapper = new Node<Object>(null);//Graph.getWrapper(null);
		assertTrue(nullWrapper.hashCode()==0);
		String t = "1";
		Node<Object> wrapper = Graph.getWrapper(t);
		assertTrue(wrapper.hashCode()==t.hashCode());
		
		assertTrue(wrapper.equals(wrapper));
		assertFalse(wrapper.equals(null));
		
		assertFalse(nullWrapper.equals(wrapper));
		
		assertFalse(Graph.getWrapper("2").equals(wrapper));
		
		assertTrue(Graph.getWrapper("1").equals(wrapper));
		
		assertTrue(Graph.Direction.valueOf("in")==Graph.Direction.in);
		assertTrue(Graph.Direction.valueOf("out")==Graph.Direction.out);
		assertTrue(Graph.Direction.values().length==2);
		
		assertTrue(Graph.Algo.valueOf("DFS")==Graph.Algo.DFS);
		assertTrue(Graph.Algo.valueOf("BFS")==Graph.Algo.BFS);
		assertTrue(Graph.Algo.values().length==2);
	}
}
