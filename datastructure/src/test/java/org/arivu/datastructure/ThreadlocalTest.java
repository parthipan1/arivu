/**
 * 
 */
package org.arivu.datastructure;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author P
 *
 */
public class ThreadlocalTest {

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
//	 * Test method for {@link org.arivu.datastructure.Threadlocal#remove()}.
//	 */
//	@Test
//	public void testRemove() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link org.arivu.datastructure.Threadlocal#remove(java.lang.String)}.
//	 */
//	@Test
//	public void testRemoveString() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link org.arivu.datastructure.Threadlocal#set(java.lang.Object)}.
//	 */
//	@Test
//	public void testSet() {
//		fail("Not yet implemented");
//	}

	/**
	 * Test method for {@link org.arivu.datastructure.Threadlocal#get(java.util.Map)}.
	 */
	@Test
	@Ignore
	public void testGetMapOfStringObject() {
		final AtomicInteger create = new  AtomicInteger(0);
		final Threadlocal<String> threadlocal = new Threadlocal<String>(new Threadlocal.Factory<String>(){

			@Override
			public String create(Map<String, Object> params) {
				create.incrementAndGet();
				return create.toString();
			}
			
		}); 
		
		final int reqPerThread = 5;
		final int noOfThreads = 5;
		final Queue<Future<Integer>> listFuture = new DoublyLinkedList<Future<Integer>>();
		final ExecutorService exe = Executors.newFixedThreadPool(noOfThreads);
		final AtomicInteger c = new AtomicInteger(noOfThreads);
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch end = new CountDownLatch(1);

		for (int j = 1; j <= noOfThreads; j++) {
			final Callable<Integer> task = getTask(threadlocal, reqPerThread, c, start, end);
			listFuture.add(exe.submit(task));
		}

		start.countDown();
		try {
//			System.out.println("Waiting for end");
			end.await();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

//		System.out.println("Completed all threads");
		
		Future<Integer> poll = null;
		while ((poll = listFuture.poll()) != null) {
			try {
				System.out.println(" Completed :: "+poll.get());
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
				fail("failed in parallel run!");
			}
		}

		assertTrue("Failed in || run test exp::0 got::" + threadlocal.size(), threadlocal.size() == 0);
	}

//	/**
//	 * Test method for {@link org.arivu.datastructure.Threadlocal#get()}.
//	 */
//	@Test
//	public void testGet() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link org.arivu.datastructure.Threadlocal#evict()}.
//	 */
//	@Test
//	public void testEvict() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link org.arivu.datastructure.Threadlocal#clearAll()}.
//	 */
//	@Test
//	public void testClearAll() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link org.arivu.datastructure.Threadlocal#getAll()}.
//	 */
//	@Test
//	public void testGetAll() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link org.arivu.datastructure.Threadlocal#close()}.
//	 */
//	@Test
//	public void testClose() {
//		fail("Not yet implemented");
//	}
	
	private Callable<Integer> getTask(final Threadlocal<String> map, final int reqPerThread, final AtomicInteger c,
			final CountDownLatch start, final CountDownLatch end) {
		final Callable<Integer> task = new Callable<Integer>() {

			@Override
			public Integer call() {
				try {
					start.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				final String key = String.valueOf(Thread.currentThread().hashCode());
				final int decrementAndGet = c.decrementAndGet();
				for (int i = 0; i < reqPerThread; i++) {
					String value = String.valueOf(i);

					if (map.get() != null) {
						if (decrementAndGet <= 0) {
							end.countDown();
						}
						System.err.println("Failed in check1 thread " + key + " value " + value);
						throw new RuntimeException("Failed in check1 thread " + key + " value " + value);
					}

					map.set(value);

					if (map.get(null) != value) {
						if (decrementAndGet <= 0) {
							end.countDown();
						}
						System.err.println("Failed in check2 thread " + key + " value " + value);
						throw new RuntimeException("Failed in check2 thread " + key + " value " + value);
					}

					map.remove();
					System.out.println("Completed Thread "+decrementAndGet+" req "+value);
				}
				
//				System.out.println("completed "+decrementAndGet);
				
				if (decrementAndGet <= 0) {
					end.countDown();
				}
				return decrementAndGet;
			}
		};
		return task;
	}
}
