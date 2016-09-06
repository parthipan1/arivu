package org.arivu.datastructure;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

//@Ignore
public class DoublyLinkedListTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testClear() {
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		list.add("one");
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==1);
		list.clear();
		assertTrue("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==0);
	}

	@Test
	public void testIsEmpty() {
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		list.add("one");
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==1);
		list.clear();
		assertTrue("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==0);
	}

	@Test
	public void testPoll() {
		String element1 = "one";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		list.add(element1);
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==1);
		String poll = list.poll();
		assertTrue("Failed in clear", poll.equals(element1));
		assertTrue("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==0);
	}

	@Test
	public void testRemove() {
		String element1 = "one";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		list.add(element1);
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==1);
		String poll = list.remove();
		assertTrue("Failed in clear", poll.equals(element1));
		assertTrue("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==0);

	}

	@Test
	public void testSize() {
		String element1 = "one";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		list.add(element1);
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==1);
		String poll = list.remove();
		assertTrue("Failed in clear", poll.equals(element1));
		assertTrue("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==0);
	}

	@Test
	public void testContains() {
		String element1 = "one";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		list.add(element1);
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==1);
		assertTrue("Failedin contains", list.contains(element1));
		
		String poll = list.remove();
		assertTrue("Failed in clear", poll.equals(element1));
		assertTrue("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==0);
	}

	@Test
	public void testIterator() {
		String element1 = "one";
		String element2 = "two";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		list.add(element1);
		list.add(element2);
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==2);
		

		StringBuffer sb = new StringBuffer();
		for(String s:list)
			sb.append(s);
		
		assertTrue("Failed in Iterator! "+sb.toString(), (element1+element2).equals(sb.toString()));
	}

	@Test
	public void testToArray() {
		String element1 = "one";
		String element2 = "two";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		list.add(element1);
		list.add(element2);
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==2);
		

		StringBuffer sb = new StringBuffer();
		for(Object s:list.toArray())
			sb.append(s);
		
		assertTrue("Failed in Iterator!", (element1+element2).equals(sb.toString()));
	}

	@Test
	public void testToArrayTArray() {
		String element1 = "one";
		String element2 = "two";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		list.add(element1);
		list.add(element2);
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==2);
		

		StringBuffer sb = new StringBuffer();
		for(Object s:list.toArray(new String[]{}))
			sb.append(s);
		
		assertTrue("Failed in Iterator!", (element1+element2).equals(sb.toString()));
	}

	@Test
	public void testAddT() {
		String element1 = "one";
		String element2 = "two";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		list.add(element1);
		list.add(element2);
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==2);
		

		StringBuffer sb = new StringBuffer();
		for(Object s:list.toArray())
			sb.append(s);
		
		assertTrue("Failed in Iterator!", (element1+element2).equals(sb.toString()));
	}

	@Test
	public void testRemoveObject() {
		String element1 = "one";
		String element2 = "two";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		list.add(element1);
		list.add(element2);
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==2);
		

		StringBuffer sb = new StringBuffer();
		for(Object s:list.toArray())
			sb.append(s);
		
		assertTrue("Failed in Iterator!", (element1+element2).equals(sb.toString()));
		
		list.remove(element1);
		
		sb = new StringBuffer();
		for(Object s:list.toArray())
			sb.append(s);
		
		assertTrue("Failed in Iterator!", (element2).equals(sb.toString()));
		
	}

	@Test
	public void testContainsAll() {
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		
		Collection<String> list1 = new ArrayList<String>();
		
		list1.add(element1);
		list1.add(element2);
		
		list.addAll(list1);
		
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==2);
		
		assertTrue("list.contains(element1)  ",list.contains(element1));
		assertTrue("list.contains(element2)  ",list.contains(element2));
		
		assertTrue("Failed on contains", list.containsAll(list1));
		
		list1.add(element3);
		assertFalse("Failed on contains", list.containsAll(list1));
	}

	@Test
	public void testAddAllCollectionOfQextendsT() {
		String element1 = "one";
		String element2 = "two";
//		String element3 = "three";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		
		Collection<String> list1 = new ArrayList<String>();
		
		list1.add(element1);
		list1.add(element2);
		
		list.addAll(list1);
		
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==2);
	}

	@Test
	public void testAddAllIntCollectionOfQextendsT() {
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		
		Collection<String> list1 = new ArrayList<String>();
		
		list1.add(element1);
		list1.add(element2);
		
		list.addAll(list1);
		
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==2);
		
		list1.clear();
		list1.add(element3);
		
		list.addAll(0,list1);
		assertTrue("Failed in clear", list.size()==3);
		
		StringBuffer sb = new StringBuffer();
		for(Object s:list.toArray())
			sb.append(s);
		
		assertTrue("Failed in Iterator! "+sb.toString(), (element3+element1+element2).equals(sb.toString()));
	}

	@Test
	public void testRemoveAll() {
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		
		Collection<String> list1 = new ArrayList<String>();
		
		list1.add(element1);
		list1.add(element2);
		list1.add(element3);
		
		list.addAll(list1);
		
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==3);
		
		list.removeAll(list1);
		
		assertTrue("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==0);
		
	}

	@Test
	public void testRetainAll() {
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		
		Collection<String> list1 = new ArrayList<String>();
		
		list1.add(element1);
		list1.add(element2);
		list1.add(element3);
		
		list.addAll(list1);
		
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==3);
		
		list.retainAll(list1);
		
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==3);
	}

	@Test
	public void testGet() {
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		
		list.add(element1);
		list.add(element2);
		list.add(element3);
		
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==3);
		
		assertTrue("Failed in get!", element1.equals(list.get(0)));
		assertTrue("Failed in get!", element2.equals(list.get(1)));
		assertTrue("Failed in get!", element3.equals(list.get(2)));
	}

	@Test
	public void testSet() {
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		String element4 = "four";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		
		list.add(element1);
		list.add(element2);
		list.add(element3);
		
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==3);
		
		assertTrue("Failed in get!", element1.equals(list.get(0)));
		assertTrue("Failed in get!", element2.equals(list.get(1)));
		assertTrue("Failed in get!", element3.equals(list.get(2)));
		
		list.set(2, element4);
		assertFalse("Failed in get!", element3.equals(list.get(2)));
		assertTrue("Failed in get!", element4.equals(list.get(2)));
	}

	@Test
	public void testAddIntT() {
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		String element4 = "four";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		
		list.add(element1);
		list.add(element2);
		list.add(element3);
		
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==3);
		
		list.add(0, element4);
		
		assertTrue("Failed in get!", element4.equals(list.get(0)));
		assertTrue("Failed in get!", element1.equals(list.get(1)));
		assertTrue("Failed in get!", element2.equals(list.get(2)));
		assertTrue("Failed in get!", element3.equals(list.get(3)));
	}

	@Test
	public void testRemoveInt() {
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		String element4 = "four";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		
		list.add(element1);
		list.add(element2);
		list.add(element3);
		list.add(element4);
		
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==4);
		
		assertTrue("Failed in get!", element1.equals(list.get(0)));
		assertTrue("Failed in get!", element2.equals(list.get(1)));
		assertTrue("Failed in get!", element3.equals(list.get(2)));
		assertTrue("Failed in get!", element4.equals(list.get(3)));
		
		list.remove(1);
		
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==3);
		
		assertTrue("Failed in get!", element1.equals(list.get(0)));
		assertTrue("Failed in get!", element3.equals(list.get(1)));
		assertTrue("Failed in get!", element4.equals(list.get(2)));
		
	}

	@Test
	public void testIndexOf() {
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		String element4 = "four";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		
		list.add(element1);
		list.add(element2);
		list.add(element3);
		list.add(element4);
		
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==4);
		
		assertTrue("Failed in get!", list.indexOf(element1) == 0);
		assertTrue("Failed in get!", list.indexOf(element2) == 1);
		assertTrue("Failed in get!", list.indexOf(element3) == 2);
		assertTrue("Failed in get!", list.indexOf(element4) == 3);
		
		list.remove(1);
		
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==3);
		
		assertTrue("Failed in get!", list.indexOf(element1) == 0 );
		assertTrue("Failed in get!", list.indexOf(element3) == 1 );
		assertTrue("Failed in get!", list.indexOf(element4) == 2 );
	}

	@Test
	public void testLastIndexOf() {
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		String element4 = "four";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		
		list.add(element1);
		list.add(element2);
		list.add(element3);
		list.add(element4);
		
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==4);
		
		assertTrue("Failed in get!", list.lastIndexOf(element1) == 0);
		assertTrue("Failed in get!", list.lastIndexOf(element2) == 1);
		assertTrue("Failed in get!", list.lastIndexOf(element3) == 2);
		assertTrue("Failed in get!", list.lastIndexOf(element4) == 3);
		
		list.remove(1);
		
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==3);
		
		assertTrue("Failed in get!", list.lastIndexOf(element1) == 0 );
		assertTrue("Failed in get!", list.lastIndexOf(element3) == 1 );
		assertTrue("Failed in get!", list.lastIndexOf(element4) == 2 );
	}

	/**
	 */
	@Test
	public void testRunParallel() throws IOException {
		final List<String> list = new DoublyLinkedList<String>();
		
		final int reqPerThread = 10000;
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
						list.add(String.valueOf(initialValue-cnt.getAndDecrement()));
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
		assertTrue("Failed in || run test exp::"+initialValue+" got::"+list.size(), list.size()==initialValue);
	}
	
//	@Test
//	public void testListIterator() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testListIteratorInt() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSubList() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testOffer() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testElement() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testPeek() {
//		fail("Not yet implemented");
//	}

}
