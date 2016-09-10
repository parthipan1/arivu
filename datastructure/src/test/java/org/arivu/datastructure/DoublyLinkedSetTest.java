/**
 * 
 */
package org.arivu.datastructure;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author P
 *
 */
public class DoublyLinkedSetTest {

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

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#clear()}.
	 */
	@Test
	public void testClear() {
		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		set.add("one");
		assertFalse("Failed in clear", set.isEmpty());
		assertTrue("Failed in clear", set.size()==1);
		set.clear();
		assertTrue("Failed in clear", set.isEmpty());
		assertTrue("Failed in clear", set.size()==0);
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#isEmpty()}.
	 */
	@Test
	public void testIsEmpty() {
		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		set.add("one");
		assertFalse("Failed in isEmpty", set.isEmpty());
		assertTrue("Failed in isEmpty", set.size()==1);
		set.clear();
		assertTrue("Failed in isEmpty", set.isEmpty());
		assertTrue("Failed in isEmpty", set.size()==0);
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#poll()}.
	 */
	@Test
	public void testPoll() {
		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		String element1 = "one";
		String element2 = "two";
		set.add(element1);
		set.add(element2);
		assertFalse("Failed in poll", set.isEmpty());
		assertTrue("Failed in poll", set.size()==2);
		String poll = set.poll();
		assertTrue("Failed in poll", element1.equals(poll));
		poll = set.poll();
		assertTrue("Failed in poll", element2.equals(poll));
		assertTrue("Failed in poll", set.isEmpty());
		assertTrue("Failed in poll", set.size()==0);
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#remove()}.
	 */
	@Test
	public void testRemove() {
		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		String element1 = "one";
		String element2 = "two";
		set.add(element1);
		set.add(element2);
		assertFalse("Failed in remove", set.isEmpty());
		assertTrue("Failed in remove", set.size()==2);
		String poll = set.remove();
		assertTrue("Failed in remove exp :: "+element1+" got::"+poll, element1.equals(poll));
		poll = set.remove();
		assertTrue("Failed in remove", element2.equals(poll));
		assertTrue("Failed in remove", set.isEmpty());
		assertTrue("Failed in remove", set.size()==0);
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#size()}.
	 */
	@Test
	public void testSize() {
		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		String element1 = "one";
		String element2 = "two";
		set.add(element1);
		set.add(element2);
		assertFalse("Failed in size", set.isEmpty());
		assertTrue("Failed in size", set.size()==2);
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#contains(java.lang.Object)}.
	 */
	@Test
	public void testContains() {
		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		set.add(element1);
		set.add(element2);
		assertTrue("Failed in contains", set.contains(element1));
		assertTrue("Failed in contains", set.contains(element2));
		assertFalse("Failed in contains", set.contains(element3));
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#iterator()}.
	 */
	@Test
	public void testIterator() {
		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		set.add(element1);
		set.add(element2);
		set.add(element3);
		
		StringBuffer sb = new StringBuffer();
		for(String s:set)
			sb.append(s);
		
		assertTrue("Failed in Iterator!", (element1+element2+element3).equals(sb.toString()));
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#toArray()}.
	 */
	@Test
	public void testToArray() {
		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		set.add(element1);
		set.add(element2);
		set.add(element3);
		
		StringBuffer sb = new StringBuffer();
		for(Object s:set.toArray())
			sb.append(s);
		
		assertTrue("Failed in toArray!", (element1+element2+element3).equals(sb.toString()));		
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#toArray(T[])}.
	 */
	@Test
	public void testToArrayTArray() {
		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		set.add(element1);
		set.add(element2);
		set.add(element3);
		
		StringBuffer sb = new StringBuffer();
		for(String s:set.toArray(new String[]{}))
			sb.append(s);
		
		assertTrue("Failed in toArray!", (element1+element2+element3).equals(sb.toString()));	
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#add(java.lang.Object)}.
	 */
	@Test
	public void testAdd() {
		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		set.add(element1);
		set.add(element2);
		set.add(element3);
		
		StringBuffer sb = new StringBuffer();
		for(String s:set.toArray(new String[]{}))
			sb.append(s);
		
		assertTrue("Failed in toArray!", (element1+element2+element3).equals(sb.toString()));
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#remove(java.lang.Object)}.
	 */
	@Test
	public void testRemoveObject() {

		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		
		Collection<String> set1 = new ArrayList<String>();
		set1.add(element1);
		set1.add(element2);
		set1.add(element3);
		
		set.addAll(set1);
		
		assertTrue("Size", set.size()==3);
		StringBuffer sb = new StringBuffer();
		for(String s:set.toArray(new String[]{}))
			sb.append(s);
		
//		DoublyLinkedSetInt<String> search = set.search(element1);
//		System.out.println("set.search(element1) "+search.obj+" "+search+" set "+set);
//		search = set.search(element2);
//		System.out.println("set.search(element2) "+search.obj+" "+search+" set "+set);
//		search = set.search(element3);
//		System.out.println("set.search(element3) "+search.obj+" "+search+" set "+set);
		
		set.remove(element1);
		set.remove(element2);
		set.remove(element3);
//		set.removeAll(set1);
		assertTrue("Size exp::0 got::"+set.size(), set.size()==0);		
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#containsAll(java.util.Collection)}.
	 */
	@Test
	public void testContainsAll() {
		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		
		Collection<String> set1 = new ArrayList<String>();
		set1.add(element1);
		set1.add(element2);
		set1.add(element3);
		
		set.addAll(set1);
		
		assertTrue("Size", set.size()==3);
		StringBuffer sb = new StringBuffer();
		for(String s:set.toArray(new String[]{}))
			sb.append(s);
		
		assertTrue("Failed in containsAll", set.containsAll(set1));
		
		set1.clear();
		set1.add("four");
		
		assertFalse("Failed in containsAll", set.containsAll(set1));
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#addAll(java.util.Collection)}.
	 */
	@Test
	public void testAddAll() {

		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		
		Collection<String> set1 = new ArrayList<String>();
		set1.add(element1);
		set1.add(element2);
		set1.add(element3);
		
		set.addAll(set1);
		
		assertTrue("Size", set.size()==3);
		StringBuffer sb = new StringBuffer();
		for(String s:set.toArray(new String[]{}))
			sb.append(s);
		
		
		set.clear();
		set1.clear();
		
		set1 = new ArrayList<String>();
		set1.add(element1);
		set1.add(element2);
		set1.add(element2);
		
		assertTrue("Size", set1.size()==3);
		set.addAll(set1);
		assertTrue("Size", set.size()==2);
		
		sb = new StringBuffer();
		for(String s:set.toArray(new String[]{}))
			sb.append(s);
		
		assertTrue("Failed in toArray!", (element1+element2).equals(sb.toString()));
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#removeAll(java.util.Collection)}.
	 */
	@Test
	public void testRemoveAll() {

		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		
		Collection<String> set1 = new ArrayList<String>();
		set1.add(element1);
		set1.add(element2);
		set1.add(element3);
		
		set.addAll(set1);
		
		assertTrue("Size", set.size()==3);
		StringBuffer sb = new StringBuffer();
		for(String s:set.toArray(new String[]{}))
			sb.append(s);
		
//		DoublyLinkedSetInt<String> search = set.search(element1);
//		System.out.println("set.search(element1) "+search.obj+" "+search+" set "+set);
//		search = set.search(element2);
//		System.out.println("set.search(element2) "+search.obj+" "+search+" set "+set);
//		search = set.search(element3);
//		System.out.println("set.search(element3) "+search.obj+" "+search+" set "+set);
		
//		set.remove(element1);
//		set.remove(element2);
//		set.remove(element3);
		set.removeAll(set1);
		assertTrue("Size exp::0 got::"+set.size(), set.size()==0);
		
//		set.clear();
//		set1.clear();
//		
//		set1 = new ArrayList<String>();
//		set1.add(element1);
//		set1.add(element2);
//		set1.add(element2);
//		
//		assertTrue("Size", set1.size()==3);
//		set.addAll(set1);
//		assertTrue("Size", set.size()==2);
//		
//		sb = new StringBuffer();
//		for(String s:set.toArray(new String[]{}))
//			sb.append(s);
//		
//		assertTrue("Failed in toArray!", (element1+element2).equals(sb.toString()));		
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#retainAll(java.util.Collection)}.
	 */
	@Test
	public void testRetainAll() {
		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		
		Collection<String> set1 = new ArrayList<String>();
		set1.add(element1);
		set1.add(element2);
		set1.add(element3);
		
		set.addAll(set1);
		
		assertTrue("Size", set.size()==3);
		StringBuffer sb = new StringBuffer();
		for(String s:set.toArray(new String[]{}))
			sb.append(s);
		
		set.retainAll(set1);
		assertTrue("Size", set.size()==3);

		set1.clear();
		set1.add(element1);
		
		set.retainAll(set1);
		assertTrue("Size", set.size()==1);

		
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#offer(java.lang.Object)}.
	 */
	@Test
	public void testOffer() {
		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		
//		Collection<String> set1 = new ArrayList<String>();
		set.offer(element1);
		set.offer(element2);
		set.offer(element3);
		
//		set.addAll(set1);
		
		assertTrue("Size", set.size()==3);
		StringBuffer sb = new StringBuffer();
		for(String s:set.toArray(new String[]{}))
			sb.append(s);
		
		DoublyLinkedSet<String> search = set.search(element1);
		assertTrue("Failed in element", search.element().equals(element1));
		search = set.search(element2);
		assertTrue("Failed in element", search.element().equals(element2));
		search = set.search(element3);
		assertTrue("Failed in element", search.element().equals(element3));
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#element()}.
	 */
	@Test
	public void testElement() {

		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		
		Collection<String> set1 = new ArrayList<String>();
		set1.add(element1);
		set1.add(element2);
		set1.add(element3);
		
		set.addAll(set1);
		
		assertTrue("Size", set.size()==3);
		StringBuffer sb = new StringBuffer();
		for(String s:set.toArray(new String[]{}))
			sb.append(s);
		
		DoublyLinkedSet<String> search = set.search(element1);
		assertTrue("Failed in element", search.element().equals(element1));
		search = set.search(element2);
		assertTrue("Failed in element", search.element().equals(element2));
		search = set.search(element3);
		assertTrue("Failed in element", search.element().equals(element3));
		
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#peek()}.
	 */
	@Test
	public void testPeek() {
		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		
		set.offer(element1);
		set.offer(element2);
		set.offer(element3);
		
		assertTrue("Size", set.size()==3);
		StringBuffer sb = new StringBuffer();
		for(String s:set.toArray(new String[]{}))
			sb.append(s);
		
		assertTrue("Failed in element", set.peek().equals(element1));
		set.remove();
		assertTrue("Failed in element", set.peek().equals(element2));
		set.remove();
		assertTrue("Failed in element", set.peek().equals(element3));
	}
	
	/**
	 */
	@Test
	public void testRunParallel() throws IOException {
		final Set<String> set = new DoublyLinkedSet<String>();//new CopyOnWriteArraySet<String>();//
		
		final int reqPerThread = 100;
		final int noOfThreads = 100;
		final ExecutorService exe = Executors.newFixedThreadPool(noOfThreads);
		final AtomicInteger c = new AtomicInteger(noOfThreads);
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch end = new CountDownLatch(1);
		final int initialValue = noOfThreads*reqPerThread;
		final AtomicInteger cnt = new AtomicInteger(initialValue);
		for( int j=1;j<=noOfThreads;j++ ){
			exe.submit(new Runnable() {
				
				@Override
				public void run() {
					try {
						start.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					for( int i=0;i<reqPerThread;i++ ){
						set.add(String.valueOf(initialValue-cnt.getAndDecrement()));
					}
//					System.out.println("Remaining count "+c.get());
					if( c.decrementAndGet()<=0 ){
						end.countDown();
					}
				}
			});
		}
		
		start.countDown();
		try {
			end.await();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		assertTrue("Failed in || run test exp::"+initialValue+" got::"+set.size(), set.size()==initialValue);
	}
}
