package org.arivu.datastructure;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DoublyLinkedStackTest {

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
		DoublyLinkedStack<String> stack = new DoublyLinkedStack<String>();
		stack.push("one");
		assertFalse("Failed in clear", stack.isEmpty());
		assertTrue("Failed in clear", stack.size()==1);
		stack.clear();
		assertTrue("Failed in clear", stack.isEmpty());
		assertTrue("Failed in clear", stack.size()==0);
	}

	@Test
	public void testSearch() {
		String element1 = "one";
		
		DoublyLinkedStack<String> stack = new DoublyLinkedStack<String>();
		stack.push(element1);
		assertFalse("Failed in search", stack.isEmpty());
		assertTrue("Failed in search", stack.size()==1);
		
		int search = stack.search(element1);
		
		assertTrue("Failed in search", search==0);
	}

	@Test
	public void testisEmpty() {
		DoublyLinkedStack<String> stack = new DoublyLinkedStack<String>();
		stack.push("one");
		assertFalse("Failed in isEmpty", stack.isEmpty());
		assertTrue("Failed in isEmpty", stack.size()==1);
		stack.clear();
		assertTrue("Failed in isEmpty", stack.isEmpty());
		assertTrue("Failed in isEmpty", stack.size()==0);
	}

	@Test
	public void testPoll() {
		String element1 = "one";
		String element2 = "two";
		DoublyLinkedStack<String> stack = new DoublyLinkedStack<String>();
		stack.push(element1);
		assertFalse("Failed in poll", stack.isEmpty());
		assertTrue("Failed in poll", stack.size()==1);
		stack.push(element2);
		
		String poll = stack.poll();
		assertTrue("Failed in poll", poll.equals(element2));
		assertTrue("Failed in poll", stack.size()==1);
		
		poll = stack.poll();
		assertTrue("Failed in poll", poll.equals(element1));
		assertTrue("Failed in poll", stack.size()==0);
	}

	@Test
	public void testPush() {
		DoublyLinkedStack<String> stack = new DoublyLinkedStack<String>();
		stack.push("one");
		assertFalse("Failed in push", stack.isEmpty());
		assertTrue("Failed in push", stack.size()==1);
		stack.clear();
		assertTrue("Failed in push", stack.isEmpty());
		assertTrue("Failed in push", stack.size()==0);
	}

	@Test
	public void testPeek() {
		String element1 = "one";
		DoublyLinkedStack<String> stack = new DoublyLinkedStack<String>();
		stack.push(element1);
		assertFalse("Failed in peek", stack.isEmpty());
		assertTrue("Failed in peek", stack.size()==1);
		String poll = stack.peek();
		assertTrue("Failed in peek", poll.equals(element1));
		assertTrue("Failed in peek", stack.size()==1);
		
	}

	@Test
	public void testSize() {
		DoublyLinkedStack<String> stack = new DoublyLinkedStack<String>();
		stack.push("one");
		assertFalse("Failed in size", stack.isEmpty());
		assertTrue("Failed in size", stack.size()==1);
		stack.clear();
		assertTrue("Failed in size", stack.isEmpty());
		assertTrue("Failed in size", stack.size()==0);
	}

	@Test
	public void testIterator() {
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		DoublyLinkedStack<String> stack = new DoublyLinkedStack<String>();
		stack.push(element1);
		stack.push(element2);
		stack.push(element3);

		StringBuffer sb = new StringBuffer();
		for(String s:stack)
			sb.append(s);
		
		assertTrue("Failed in Iterator! exp :: "+(element1+element2+element3)+" got :: "+sb.toString(), (element1+element2+element3).equals(sb.toString()));
	}

	@Test
	public void testToArray() {
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		DoublyLinkedStack<String> stack = new DoublyLinkedStack<String>();
		stack.push(element1);
		stack.push(element2);
		stack.push(element3);

		StringBuffer sb = new StringBuffer();
		for(Object s:stack.toArray())
			sb.append(s);
		
		assertTrue("Failed in Iterator! exp :: "+(element1+element2+element3)+" got :: "+sb.toString(), (element1+element2+element3).equals(sb.toString()));
	}

	@Test
	public void testToArrayTArray() {
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		DoublyLinkedStack<String> stack = new DoublyLinkedStack<String>();
		stack.push(element1);
		stack.push(element2);
		stack.push(element3);

		StringBuffer sb = new StringBuffer();
		for(String s:stack.toArray(new String[]{}))
			sb.append(s);
		
		assertTrue("Failed in Iterator! exp :: "+(element1+element2+element3)+" got :: "+sb.toString(), (element1+element2+element3).equals(sb.toString()));
	}

	@Test
	public void testGet() {
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		DoublyLinkedStack<String> stack = new DoublyLinkedStack<String>();
		stack.push(element1);
		stack.push(element2);
		stack.push(element3);

		assertTrue("Failed at getIndex 0 ", stack.get(0).equals(element1));
		assertTrue("Failed at getIndex 1 GOT :: "+stack.get(1), stack.get(1).equals(element2));
		assertTrue("Failed at getIndex 2 ", stack.get(2).equals(element3));
	}

	@Test
	public void testIndexOf() {
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		DoublyLinkedStack<String> stack = new DoublyLinkedStack<String>();
		stack.push(element1);
		stack.push(element2);
		stack.push(element3);
		
		assertTrue("Failed at getIndex 0 ", stack.indexOf(element1)==0);
		assertTrue("Failed at getIndex 1 GOT :: "+stack.get(1), stack.indexOf(element2)==1);
		assertTrue("Failed at getIndex 2 ", stack.indexOf(element3)==2);
	}

	@Test
	public void testLastIndexOf() {
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		DoublyLinkedStack<String> stack = new DoublyLinkedStack<String>();
		stack.push(element1);
		stack.push(element2);
		stack.push(element3);
		
		assertTrue("Failed at getIndex 0 ", stack.lastIndexOf(element1)==0);
		assertTrue("Failed at getIndex 1 GOT :: "+stack.get(1), stack.lastIndexOf(element2)==1);
		assertTrue("Failed at getIndex 2 ", stack.lastIndexOf(element3)==2);
		
		stack.push(element1);
		assertTrue("Failed at getIndex 0 ", stack.lastIndexOf(element1)==3);
		
	}

	@Test
	public void testAddAll() {
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";
		Collection<String> list = new ArrayList<String>();
		list.add(element1);
		list.add(element2);
		list.add(element3);
		
		DoublyLinkedStack<String> stack = new DoublyLinkedStack<String>();
		
		stack.addAll(list);
		
		assertTrue("Failed at getIndex 0 ", stack.lastIndexOf(element1)==0);
		assertTrue("Failed at getIndex 1 GOT :: "+stack.get(1), stack.lastIndexOf(element2)==1);
		assertTrue("Failed at getIndex 2 ", stack.lastIndexOf(element3)==2);
	}
	
	/**
	 */
	@Test
	public void testRunParallel() throws IOException {
		final DoublyLinkedStack<String> list = new DoublyLinkedStack<String>(false,CompareStrategy.EQUALS);
		
		final int reqPerThread = 100;
		final int noOfThreads = 10;
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
					final DoublyLinkedStack<String> tlist = new DoublyLinkedStack<String>(false,CompareStrategy.EQUALS);
					for( int i=0;i<reqPerThread;i++ ){
						final String valueOf = String.valueOf(initialValue-cnt.getAndDecrement());
						list.push(valueOf);
						tlist.push(valueOf);
					}
					list.removeAll(tlist);
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
		assertTrue("Failed in || run test exp::"+initialValue+" got::"+list.size(), list.size()==0);
	}
	
}
