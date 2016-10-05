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
import java.util.concurrent.TimeUnit;
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
		assertTrue("Failed in clear", set.size() == 1);
		set.clear();
		assertTrue("Failed in clear", set.isEmpty());
		assertTrue("Failed in clear", set.size() == 0);
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#isEmpty()}
	 * .
	 */
	@Test
	public void testIsEmpty() {
		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		set.add("one");
		assertFalse("Failed in isEmpty", set.isEmpty());
		assertTrue("Failed in isEmpty", set.size() == 1);
		set.clear();
		assertTrue("Failed in isEmpty", set.isEmpty());
		assertTrue("Failed in isEmpty", set.size() == 0);
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
		assertTrue("Failed in poll", set.size() == 2);
		String poll = set.poll();
		assertTrue("Failed in poll", element1.equals(poll));
		poll = set.poll();
		assertTrue("Failed in poll", element2.equals(poll));
		assertTrue("Failed in poll", set.isEmpty());
		assertTrue("Failed in poll", set.size() == 0);
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
		assertTrue("Failed in remove", set.size() == 2);
		String poll = set.remove();
		assertTrue("Failed in remove exp :: " + element1 + " got::" + poll, element1.equals(poll));
		poll = set.remove();
		assertTrue("Failed in remove", element2.equals(poll));
		assertTrue("Failed in remove", set.isEmpty());
		assertTrue("Failed in remove", set.size() == 0);
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
		assertTrue("Failed in size", set.size() == 2);
	}

	/**
	 * Test method for
	 * {@link org.arivu.datastructure.DoublyLinkedSet#contains(java.lang.Object)}
	 * .
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
	 * Test method for
	 * {@link org.arivu.datastructure.DoublyLinkedSet#iterator()}.
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
		for (String s : set)
			sb.append(s);

		assertTrue("Failed in Iterator!", (element1 + element2 + element3).equals(sb.toString()));
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#toArray()}
	 * .
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
		for (Object s : set.toArray())
			sb.append(s);

		assertTrue("Failed in toArray!", (element1 + element2 + element3).equals(sb.toString()));
	}

	/**
	 * Test method for
	 * {@link org.arivu.datastructure.DoublyLinkedSet#toArray(T[])}.
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
		for (String s : set.toArray(new String[] {}))
			sb.append(s);

		assertTrue("Failed in toArray!", (element1 + element2 + element3).equals(sb.toString()));

		sb = new StringBuffer();
		for (String s : set.toArray(new String[] { "1", "2", "3", "4" }))
			sb.append(s);
		// System.out.println(" sb :: "+sb.toString());
	}

	/**
	 * Test method for
	 * {@link org.arivu.datastructure.DoublyLinkedSet#add(java.lang.Object)}.
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

		assertFalse(set.add(null));

		StringBuffer sb = new StringBuffer();
		for (String s : set.toArray(new String[] {}))
			sb.append(s);

		assertTrue("Failed in toArray!", (element1 + element2 + element3).equals(sb.toString()));
	}

	/**
	 * Test method for
	 * {@link org.arivu.datastructure.DoublyLinkedSet#remove(java.lang.Object)}.
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

		assertTrue("Size", set.size() == 3);
		StringBuffer sb = new StringBuffer();
		for (String s : set.toArray(new String[] {}))
			sb.append(s);

		// DoublyLinkedSetInt<String> search = set.search(element1);
		// System.out.println("set.search(element1) "+search.obj+" "+search+"
		// set "+set);
		// search = set.search(element2);
		// System.out.println("set.search(element2) "+search.obj+" "+search+"
		// set "+set);
		// search = set.search(element3);
		// System.out.println("set.search(element3) "+search.obj+" "+search+"
		// set "+set);

		set.remove(element1);
		set.remove(element2);
		set.remove(element3);
		// set.removeAll(set1);
		assertTrue("Size exp::0 got::" + set.size(), set.size() == 0);
	}

	/**
	 * Test method for
	 * {@link org.arivu.datastructure.DoublyLinkedSet#containsAll(java.util.Collection)}
	 * .
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

		assertTrue("Size", set.size() == 3);
		StringBuffer sb = new StringBuffer();
		for (String s : set.toArray(new String[] {}))
			sb.append(s);

		assertTrue("Failed in containsAll", set.containsAll(set1));
		assertFalse("Failed in containsAll", set.containsAll(null));

		set1.clear();
		set1.add("four");

		assertFalse("Failed in containsAll", set.containsAll(set1));
	}

	/**
	 * Test method for
	 * {@link org.arivu.datastructure.DoublyLinkedSet#addAll(java.util.Collection)}
	 * .
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

		assertTrue("Size", set.size() == 3);
		StringBuffer sb = new StringBuffer();
		for (String s : set.toArray(new String[] {}))
			sb.append(s);

		set.clear();
		set1.clear();

		set1 = new ArrayList<String>();
		set1.add(element1);
		set1.add(element2);
		set1.add(element2);

		assertTrue("Size", set1.size() == 3);
		set.addAll(set1);
		assertTrue("Size", set.size() == 2);

		sb = new StringBuffer();
		for (String s : set.toArray(new String[] {}))
			sb.append(s);

		assertTrue("Failed in toArray!", (element1 + element2).equals(sb.toString()));
	}

	/**
	 * Test method for
	 * {@link org.arivu.datastructure.DoublyLinkedSet#removeAll(java.util.Collection)}
	 * .
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

		assertTrue("Size", set.size() == 3);
		StringBuffer sb = new StringBuffer();
		for (String s : set.toArray(new String[] {}))
			sb.append(s);

		// DoublyLinkedSetInt<String> search = set.search(element1);
		// System.out.println("set.search(element1) "+search.obj+" "+search+"
		// set "+set);
		// search = set.search(element2);
		// System.out.println("set.search(element2) "+search.obj+" "+search+"
		// set "+set);
		// search = set.search(element3);
		// System.out.println("set.search(element3) "+search.obj+" "+search+"
		// set "+set);

		// set.remove(element1);
		// set.remove(element2);
		// set.remove(element3);
		set.removeAll(set1);
		assertTrue("Size exp::0 got::" + set.size(), set.size() == 0);

		// set.clear();
		// set1.clear();
		//
		// set1 = new ArrayList<String>();
		// set1.add(element1);
		// set1.add(element2);
		// set1.add(element2);
		//
		// assertTrue("Size", set1.size()==3);
		// set.addAll(set1);
		// assertTrue("Size", set.size()==2);
		//
		// sb = new StringBuffer();
		// for(String s:set.toArray(new String[]{}))
		// sb.append(s);
		//
		// assertTrue("Failed in toArray!",
		// (element1+element2).equals(sb.toString()));
	}

	/**
	 * Test method for
	 * {@link org.arivu.datastructure.DoublyLinkedSet#retainAll(java.util.Collection)}
	 * .
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

		assertTrue("Size", set.size() == 3);
		StringBuffer sb = new StringBuffer();
		for (String s : set.toArray(new String[] {}))
			sb.append(s);

		set.retainAll(set1);
		assertTrue("Size", set.size() == 3);

		set1.clear();
		set1.add(element1);

		set.retainAll(set1);
		assertTrue("Size", set.size() == 1);

	}

	/**
	 * Test method for
	 * {@link org.arivu.datastructure.DoublyLinkedSet#offer(java.lang.Object)}.
	 */
	@Test
	public void testOffer() {
		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";

		// Collection<String> set1 = new ArrayList<String>();
		set.offer(element1);
		set.offer(element2);
		set.offer(element3);

		// set.addAll(set1);

		assertTrue("Size", set.size() == 3);
		StringBuffer sb = new StringBuffer();
		for (String s : set.toArray(new String[] {}))
			sb.append(s);

		DoublyLinkedSet<String> search = set.search(element1);
		assertTrue("Failed in element", search.element().equals(element1));
		search = set.search(element2);
		assertTrue("Failed in element", search.element().equals(element2));
		search = set.search(element3);
		assertTrue("Failed in element", search.element().equals(element3));
	}

	/**
	 * Test method for {@link org.arivu.datastructure.DoublyLinkedSet#element()}
	 * .
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

		assertTrue("Size", set.size() == 3);
		StringBuffer sb = new StringBuffer();
		for (String s : set.toArray(new String[] {}))
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

		assertTrue("Size", set.size() == 3);
		StringBuffer sb = new StringBuffer();
		for (String s : set.toArray(new String[] {}))
			sb.append(s);

		assertTrue("Failed in element", set.peek().equals(element1));
		set.remove();
		assertTrue("Failed in element", set.peek().equals(element2));
		set.remove();
		assertTrue("Failed in element", set.peek().equals(element3));
	}

	/**
	 * @throws InterruptedException
	 */
	@Test
	public void testRunParallel() throws IOException, InterruptedException {
		// final Set<String> set = new
		// java.util.concurrent.CopyOnWriteArraySet<String>();//
		final Set<String> set = new DoublyLinkedSet<String>();//

		final int reqPerThread = ThreadCounts.noOfRequests / ThreadCounts.maxThreads;
		final int noOfThreads = ThreadCounts.maxThreads;
		final ExecutorService exe = Executors.newFixedThreadPool(noOfThreads);
		final AtomicInteger c = new AtomicInteger(noOfThreads);
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch end = new CountDownLatch(1);
		final int initialValue = noOfThreads * reqPerThread;
		final AtomicInteger cnt = new AtomicInteger(initialValue);
		for (int j = 1; j <= noOfThreads; j++) {
			exe.submit(new Runnable() {

				@Override
				public void run() {
					try {
						start.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					try {
						// final Set<String> tset = new
						// java.util.concurrent.CopyOnWriteArraySet<String>();
						final Set<String> tset = new DoublyLinkedSet<String>();
						for (int i = 0; i < reqPerThread; i++) {
							final String valueOf = String.valueOf(initialValue - cnt.getAndDecrement());
							set.add(valueOf);
							tset.add(valueOf);
						}
						set.removeAll(tset);
					} catch (Throwable e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// System.out.println("Remaining count "+c.get());
					if (c.decrementAndGet() <= 0) {
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
			// String msg = "Still waiting after 100ms: calling
			// System.exit(0)...";
			// System.err.println(msg);
		}
		for (String s : set)
			System.out.println("set Obj :: " + s);
		assertTrue("Failed in || run test exp::" + initialValue + " got::" + set.size(), set.size() == 0);
	}

	@Test
	public void testCompare() {
		String one = new String("1");
		String two = new String("1");
		assertTrue(CompareStrategy.EQUALS.compare(one, two));
		assertFalse(CompareStrategy.REF.compare(one, two));
		assertTrue(CompareStrategy.REF.compare(one, one));

		assertTrue(CompareStrategy.EQUALS.compare(null, null));
		assertTrue(CompareStrategy.REF.compare(null, null));

		assertFalse(CompareStrategy.EQUALS.compare(one, null));
		assertFalse(CompareStrategy.REF.compare(one, null));

		assertFalse(CompareStrategy.EQUALS.compare(null, two));
		assertFalse(CompareStrategy.REF.compare(null, two));
	}

	@Test
	public void testRefEquals() {
		String one = new String("1");
		String two = new String("2");

		Ref ref1 = new Ref(one);
		Ref ref2 = new Ref(new DoublyLinkedSet<String>(one, null, null, null, null));
		Ref ref3 = new Ref(one);

		Ref ref4 = new Ref((String) null);
		Ref ref5 = new Ref(two);

		assertTrue(ref1.equals(ref1));
		assertFalse(ref1.equals(null));
		assertFalse(ref1.equals(new Object()));
		assertTrue(ref1.equals(ref2));
		assertTrue(ref1.equals(ref3));

		assertFalse(ref4.equals(ref1));
		assertFalse(ref1.equals(ref5));

		assertTrue(ref4.hashCode() == 0);
		assertTrue(ref1.hashCode() == one.hashCode());

		Object obj = null;
		Ref ref6 = new Ref(obj);
		Ref ref7 = new Ref(new DoublyLinkedSet<Object>(obj, null, null, null, null));
		Ref ref8 = new Ref(new DoublyLinkedList<Object>(obj, null, null, null, null));
		Ref ref9 = new Ref(new DoublyLinkedStack<Object>(obj, null, false, null, null, null));

		assertTrue(ref6.hashCode() == 0);
		assertTrue(ref7.hashCode() == 0);
		assertTrue(ref8.hashCode() == 0);
		assertTrue(ref9.hashCode() == 0);

		obj = "10";
		ref6 = new Ref(obj);
		ref7 = new Ref(new DoublyLinkedSet<Object>(obj, null, null, null, null));
		ref8 = new Ref(new DoublyLinkedList<Object>(obj, null, null, null, null));
		ref9 = new Ref(new DoublyLinkedStack<Object>(obj, null, false, null, null, null));

		assertTrue(ref6.hashCode() == obj.hashCode());
		assertTrue(ref7.hashCode() == obj.hashCode());
		assertTrue(ref8.hashCode() == obj.hashCode());
		assertTrue(ref9.hashCode() == obj.hashCode());

	}

	@Test
	public void testSize1() {
		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		assertTrue("Failed in clear", set.isEmpty());
		assertTrue("Failed in clear", set.size() == 0);
		set.size = null;
		assertTrue("Failed in clear", set.size() == 0);
	}

	@Test
	public void testSize2() {

		DoublyLinkedSet<String> set = new DoublyLinkedSet<String>();
		String element1 = "one";
		String element2 = "two";
		String element3 = "three";

		set.offer(element1);
		set.offer(element2);
		set.offer(element3);

		DoublyLinkedSet<String> set1 = new DoublyLinkedSet<String>();
		assertTrue("Failed in clear", set1.isEmpty());
		assertTrue("Failed in clear", set1.size() == 0);

		assertTrue(set1.remove() == null);

		DoublyLinkedSet<String> set2 = new DoublyLinkedSet<String>(set);

		assertFalse("Failed in clear", set2.isEmpty());
		assertTrue("Failed in clear", set2.size() == set.size());

	}

	@Test
	public void testSize3() {

		DoublyLinkedSet<String> set1 = new DoublyLinkedSet<String>();
		assertTrue("Failed in clear", set1.isEmpty());
		assertTrue("Failed in clear", set1.size() == 0);
		set1.cas = null;
		assertFalse(set1.remove("test"));

	}

	@Test
	public void testCompareStrategy() {
		assertTrue(CompareStrategy.valueOf("REF") == CompareStrategy.REF);
		assertTrue(CompareStrategy.valueOf("EQUALS") == CompareStrategy.EQUALS);
		assertTrue(CompareStrategy.values().length == 2);
	}
}
