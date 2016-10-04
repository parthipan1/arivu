package org.arivu.datastructure;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
		
		sb = new StringBuffer();
		for(String s:list.toArray(new String[]{"1","2","3","4"}))
			sb.append(s);
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
		
		assertFalse(list.containsAll(null));
		assertFalse(list.containsAll(new DoublyLinkedList<String>()));
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
		
		assertFalse(list.removeAll(null));
		assertFalse(list.removeAll(new DoublyLinkedList<String>()));
		
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
		
		list1.clear();
		list1.add(element1);
		
		list.retainAll(list1);
		assertFalse("Failed in clear", list.isEmpty());
		assertTrue("Failed in clear", list.size()==1);
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
		
		assertTrue("Failed in get! "+list.get(0), element4.equals(list.get(0)));
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
	 * @throws InterruptedException 
	 */
	@Test
	public void testRunParallel() throws IOException, InterruptedException {
		final List<String> list = new DoublyLinkedList<String>();
		
		final int reqPerThread = ThreadCounts.noOfRequests/ThreadCounts.maxThreads;
		final int noOfThreads = ThreadCounts.maxThreads;
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
					final List<String> tlist = new DoublyLinkedList<String>();
					for( int i=0;i<reqPerThread;i++ ){
						final String valueOf = String.valueOf(initialValue-cnt.getAndDecrement());
						list.add(valueOf);
						tlist.add(valueOf);
					}
//					System.out.println("Remaining count "+c.get());
					list.removeAll(tlist);
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
		exe.shutdownNow();
		if (!exe.awaitTermination(100, TimeUnit.MICROSECONDS)) {
//			String msg = "Still waiting after 100ms: calling System.exit(0)...";
//			System.err.println(msg);
		}
		assertTrue("Failed in || run test exp::"+initialValue+" got::"+list.size(), list.size()==0);
	}
	
	@Test
	public void testValidateIndex() {
		String element1 = "one";
		String element2 = "two";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		list.add(element1);
		list.offer(element2);
		assertFalse(list.add(null));
		assertTrue(list.peek().equals(element1));
		assertTrue(list.indexOf(element1)==0);
		int indexOf = list.indexOf("3");
		assertTrue("Failed in indexOf -3 got "+indexOf+" exp -1",indexOf==-1);
		try {
			list.validateIndex(-1);
			fail("Failed on ArrayIndexOutOfBoundsException -1");
		} catch (ArrayIndexOutOfBoundsException e) {
			assertTrue(e!=null);
		}
		
		try {
			list.validateIndex(2);
			fail("Failed on ArrayIndexOutOfBoundsException 2");
		} catch (ArrayIndexOutOfBoundsException e) {
			assertTrue(e!=null);
		}
		
		assertTrue(list.getLinked(2)==null);
		assertTrue(list.remove(2)==null);
		assertTrue(list.lastIndexOf("3")==-1);
		assertFalse(list.remove("3"));
		assertTrue(list.element()==null);
	}
	
	@Test
	public void testAddAll() {
		String element1 = "one";
		String element2 = "two";
		DoublyLinkedList<String> list1 = new DoublyLinkedList<String>();
		list1.add(element1);
		list1.add(element2);
		assertFalse("Failed in clear", list1.isEmpty());
		assertTrue("Failed in clear", list1.size()==2);

		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		
		assertFalse(list.addAll(null));
		assertFalse(list.addAll( new DoublyLinkedList<String>()));
		
		assertTrue(list.addAll(list1));
		
		StringBuffer sb = new StringBuffer();
		for(Object s:list.toArray())
			sb.append(s);
		
		assertTrue("Failed in Iterator!", (element1+element2).equals(sb.toString()));
	}
	
	@Test
	public void testAddAllIndex() {
		String element1 = "one";
		String element2 = "two";
		DoublyLinkedList<String> list1 = new DoublyLinkedList<String>();
		list1.add(element1);
		list1.add(element2);
		assertFalse("Failed in clear", list1.isEmpty());
		assertTrue("Failed in clear", list1.size()==2);

		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		
		assertFalse(list.addAll(0,null));
		assertFalse(list.addAll(0,new DoublyLinkedList<String>()));
		
		assertTrue(list.addAll(0,list1));
		
		StringBuffer sb = new StringBuffer();
		for(Object s:list.toArray())
			sb.append(s);
		
		assertTrue("Failed in Iterator!", (element1+element2).equals(sb.toString()));
	}
	
//	@Test
//	public void testRemoveAll() {
//		String element1 = "one";
//		String element2 = "two";
//		DoublyLinkedList<String> list1 = new DoublyLinkedList<String>();
//		list1.add(element1);
//		list1.add(element2);
//		assertFalse("Failed in clear", list1.isEmpty());
//		assertTrue("Failed in clear", list1.size()==2);
//
//		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
//		
//		assertFalse(list.addAll(null));
//		assertFalse(list.addAll( new DoublyLinkedList<String>()));
//		
//		assertTrue(list.addAll(list1));
//		
//		StringBuffer sb = new StringBuffer();
//		for(Object s:list.toArray())
//			sb.append(s);
//		
//		assertTrue("Failed in Iterator!", (element1+element2).equals(sb.toString()));
//	}
	
	@Test
	public void testListIterator() {
		DoublyLinkedList<Integer> list1 = new DoublyLinkedList<Integer>();
		list1.add(23);
		list1.add(98);
        list1.add(29);
        list1.add(71);
//        list1.add(5);
		StringBuffer buf = new StringBuffer();
		ListIterator<Integer> listIterator = list1.listIterator();
		listIterator.add(5);
		while(listIterator.hasNext()){
			listIterator.set(3);
			Integer next = listIterator.next();
			buf.append(next);
//			System.out.println(next);
//			listIterator.add(next);
//			listIterator.remove();
		}
//		System.out.println("next -> prev");
//		listIterator.previous();
		while(listIterator.hasPrevious()){
			Integer previous = listIterator.previous();
			buf.append(previous);
//			System.out.println(previous);
//			listIterator.set(3);
//			listIterator.add(previous);
//			listIterator.remove();
        }
		
		listIterator = list1.listIterator(0);
		while(listIterator.hasNext()){
			Integer next = listIterator.next();
			buf.append(next);
//			System.out.println(next);
//			listIterator.set(3);
//			listIterator.remove();
//			listIterator.add(next);
		}
		while(listIterator.hasPrevious()){
			Integer previous = listIterator.previous();
//			System.out.println(previous);
			buf.append(previous);
//			listIterator.set(3);
//			listIterator.remove();
//			listIterator.add(previous);
        }
//		
		listIterator = list1.listIterator();
		while(listIterator.hasNext()){
			listIterator.remove();
//			Integer next = listIterator.next();
//			buf.append(next);
//			System.out.println(next);
//			listIterator.set(3);
//			listIterator.add(next);
//			System.out.println("Before size "+list1.size());
//			System.out.println("After size "+list1.size());
		}
		assertTrue("list1 size "+list1.size(),list1.size()==0);
////		
		list1.add(23);
		list1.add(98);
        list1.add(29);
        list1.add(71);
//        list1.add(5);
        listIterator = list1.listIterator(0);
        listIterator.add(5);
		while(listIterator.hasNext()){
			listIterator.remove();
//			Integer next = listIterator.next();
//			buf.append(next);
//			listIterator.set(3);
//			listIterator.add(next);
		}
		assertTrue("list1 size "+list1.size(),list1.size()==0);
////		
////		list1.clear();
		list1.add(23);
		list1.add(98);
        list1.add(29);
        list1.add(71);
        list1.add(5);
		listIterator = list1.listIterator(4);
		while(listIterator.hasPrevious()){
//			System.out.println("Before size "+list1.size());
			listIterator.remove();
//			System.out.println("After size "+list1.size());
//			Integer previous = listIterator.previous();
//			buf.append(previous);
//			System.out.println(previous);
//			listIterator.set(3);
//			listIterator.add(next);
		}
		listIterator.remove();
		assertTrue("list1 size "+list1.size(),list1.size()==0);
	}
	
//
//	@Test
//	public void testListIteratorInt() {
//		fail("Not yet implemented");
//	}
//
	@Test
	public void testSubList() {
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		DoublyLinkedList<String> list = new DoublyLinkedList<String>();
		list.add(element1);
		list.add(element2);
		list.add(element3);
		
		try {
			list.subList(-1, 0);
			fail("Failed on index!");
		} catch (ArrayIndexOutOfBoundsException e) {
			assertTrue(e!=null);
		}

		try {
			list.subList(1, 3);
			fail("Failed on index!");
		} catch (ArrayIndexOutOfBoundsException e) {
			assertTrue(e!=null);
		}
		
		try {
			list.subList(1, 0);
			fail("Failed on index!");
		} catch (ArrayIndexOutOfBoundsException e) {
			assertTrue(e!=null);
		}
		
		List<String> subList = list.subList(1, 2);
		assertTrue(subList.size()==2);
	}
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
