/**
 * 
 */
package org.arivu.datastructure;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
	 * @throws InterruptedException 
	 */
	@Test
	public void testGetMapOfStringObject() throws InterruptedException {
		final AtomicInteger create = new  AtomicInteger(0);
		final Threadlocal<String> threadlocal = new Threadlocal<String>(new Threadlocal.Factory<String>(){

			@Override
			public String create(Map<String, Object> params) {
				create.incrementAndGet();
				return create.toString();
			}
			
		}); 
		
		final int reqPerThread = 5;
		final int noOfThreads = 500;
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
//				Integer integer = 
						poll.get();
//				System.out.println(" Completed :: "+integer);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
				fail("failed in parallel run!");
			}
		}
		exe.shutdownNow();
		if (!exe.awaitTermination(100, TimeUnit.MICROSECONDS)) {
			String msg = "Still waiting after 100ms: calling System.exit(0)...";
			System.err.println(msg);
		}
		assertTrue("Failed in || run test exp::0 got::" + threadlocal.size(), threadlocal.size() == 0);
		assertTrue("Failed in || run test empty::true got::" + threadlocal.isEmpty(), threadlocal.isEmpty() );
	}

//	/**
//	 * Test method for {@link org.arivu.datastructure.Threadlocal#get()}.
//	 */
//	@Test
//	public void testGet() {
//		fail("Not yet implemented");
//	}
//
	/**
	 * Test method for {@link org.arivu.datastructure.Threadlocal#evict()}.
	 * @throws InterruptedException 
	 */
	@Test
	public void testEvict() throws InterruptedException {
		final AtomicInteger create = new  AtomicInteger(0);
		int threshold = 100;
		final Threadlocal<String> threadlocal = new Threadlocal<String>(new Threadlocal.Factory<String>(){

			@Override
			public String create(Map<String, Object> params) {
				create.incrementAndGet();
				return create.toString();
			}
			
		},threshold); 
		
		assertTrue(threadlocal.get(null)!=null);

		assertTrue("Failed in || run test exp::1 got::" + threadlocal.size(), threadlocal.size() == 1);
		assertFalse("Failed in || run test empty::false got::" + threadlocal.isEmpty(), threadlocal.isEmpty() );
		assertTrue("Failed in || run test exp::1 got::" + threadlocal.getAll(), threadlocal.getAll().size() == 1);
		
		Thread.sleep(threshold+10);
		
		threadlocal.evict();
		
		assertTrue(threadlocal.get()==null);
		
		assertTrue("Failed in || run test exp::0 got::" + threadlocal.size(), threadlocal.size() == 0);
		assertTrue("Failed in || run test empty::true got::" + threadlocal.isEmpty(), threadlocal.isEmpty() );
		assertTrue("Failed in || run test exp::0 got::" + threadlocal.getAll(), threadlocal.getAll().size() == 0);		
	}

	static class Something implements AutoCloseable{

		@Override
		public void close() throws Exception {
		}
		
	}
	
	/**
	 * Test method for {@link org.arivu.datastructure.Threadlocal#evict()}.
	 * @throws InterruptedException 
	 */
	@Test
	public void testEvict_Case1() throws InterruptedException {
		final AtomicInteger create = new  AtomicInteger(0);
		int threshold = 100;
		final Threadlocal<Something> threadlocal = new Threadlocal<Something>(new Threadlocal.Factory<Something>(){

			@Override
			public Something create(Map<String, Object> params) {
				create.incrementAndGet();
				return new Something();
			}
			
		},threshold); 
		
		assertTrue(threadlocal.get(null)!=null);

		assertTrue("Failed in || run test exp::1 got::" + threadlocal.size(), threadlocal.size() == 1);
		assertFalse("Failed in || run test empty::false got::" + threadlocal.isEmpty(), threadlocal.isEmpty() );
		assertTrue("Failed in || run test exp::1 got::" + threadlocal.getAll(), threadlocal.getAll().size() == 1);
		
		Thread.sleep(threshold+10);
		
		assertTrue(threadlocal.get()==null);
		
		assertTrue("Failed in || run test exp::0 got::" + threadlocal.size(), threadlocal.size() == 0);
		assertTrue("Failed in || run test empty::true got::" + threadlocal.isEmpty(), threadlocal.isEmpty() );
		assertTrue("Failed in || run test exp::0 got::" + threadlocal.getAll(), threadlocal.getAll().size() == 0);		
	}
	
	
	/**
	 * Test method for {@link org.arivu.datastructure.Threadlocal#evict()}.
	 * @throws InterruptedException 
	 */
	@Test
	public void testEvict_Case2() throws InterruptedException {
		final AtomicInteger create = new  AtomicInteger(0);
		int threshold = 100;
		final Threadlocal<String> threadlocal = new Threadlocal<String>(new Threadlocal.Factory<String>(){

			@Override
			public String create(Map<String, Object> params) {
				create.incrementAndGet();
				return create.toString();
			}
			
		},threshold); 
		
		assertTrue(threadlocal.get(null)!=null);

		assertTrue("Failed in || run test exp::1 got::" + threadlocal.size(), threadlocal.size() == 1);
		assertFalse("Failed in || run test empty::false got::" + threadlocal.isEmpty(), threadlocal.isEmpty() );
		assertTrue("Failed in || run test exp::1 got::" + threadlocal.getAll(), threadlocal.getAll().size() == 1);
		
		assertTrue(threadlocal.get()!=null);
		
		Thread.sleep(threshold+10);

		assertTrue(threadlocal.get()==null);

		assertTrue("Failed in || run test exp::0 got::" + threadlocal.size(), threadlocal.size() == 0);
		assertTrue("Failed in || run test empty::true got::" + threadlocal.isEmpty(), threadlocal.isEmpty() );
		assertTrue("Failed in || run test exp::0 got::" + threadlocal.getAll(), threadlocal.getAll().size() == 0);		
	}
	
	/**
	 * Test method for {@link org.arivu.datastructure.Threadlocal#evict()}.
	 * @throws InterruptedException 
	 */
	@Test
	public void testEvict_Case3() throws InterruptedException {
		final AtomicInteger create = new  AtomicInteger(0);
		int threshold = 100;
		final Threadlocal<String> threadlocal = new Threadlocal<String>(new Threadlocal.Factory<String>(){

			@Override
			public String create(Map<String, Object> params) {
				create.incrementAndGet();
				return create.toString();
			}
			
		},threshold); 
		
		assertTrue(threadlocal.getAndTrigger(null)==null);
	}
	
	/**
	 * Test method for {@link org.arivu.datastructure.Threadlocal#clearAll()}.
	 * @throws InterruptedException 
	 */
	@Test
	public void testClearAll() throws InterruptedException {
		final AtomicInteger create = new  AtomicInteger(0);
		final Threadlocal<String> threadlocal = new Threadlocal<String>(new Threadlocal.Factory<String>(){

			@Override
			public String create(Map<String, Object> params) {
				create.incrementAndGet();
				return create.toString();
			}
			
		}); 
		
		final int reqPerThread = 1;
		final int noOfThreads = 1;
		final Queue<Future<Integer>> listFuture = new DoublyLinkedList<Future<Integer>>();
		final ExecutorService exe = Executors.newFixedThreadPool(noOfThreads);
		final AtomicInteger c = new AtomicInteger(noOfThreads);
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch end = new CountDownLatch(1);

		assertTrue(threadlocal.get(null)!=null);
		
		for (int j = 1; j <= noOfThreads; j++) {
			final Callable<Integer> task = getTask(threadlocal, reqPerThread, c, start, end);
			listFuture.add(exe.submit(task));
		}

		start.countDown();
		try {
			end.await();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		Future<Integer> poll = null;
		while ((poll = listFuture.poll()) != null) {
			try {
				poll.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
				fail("failed in parallel run!");
			}
		}
		exe.shutdownNow();
		if (!exe.awaitTermination(100, TimeUnit.MICROSECONDS)) {
			String msg = "Still waiting after 100ms: calling System.exit(0)...";
			System.err.println(msg);
		}
		assertTrue("Failed in || run test exp::1 got::" + threadlocal.size(), threadlocal.size() == 1);
		assertFalse("Failed in || run test empty::false got::" + threadlocal.isEmpty(), threadlocal.isEmpty() );
		assertTrue("Failed in || run test exp::1 got::" + threadlocal.getAll(), threadlocal.getAll().size() == 1);
		
		threadlocal.close();
		
		assertTrue("Failed in || run test exp::0 got::" + threadlocal.size(), threadlocal.size() == 0);
		assertTrue("Failed in || run test empty::true got::" + threadlocal.isEmpty(), threadlocal.isEmpty() );
		assertTrue("Failed in || run test exp::0 got::" + threadlocal.getAll(), threadlocal.getAll().size() == 0);
	}
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
//					System.out.println("Completed Thread "+decrementAndGet+" req "+value);
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
