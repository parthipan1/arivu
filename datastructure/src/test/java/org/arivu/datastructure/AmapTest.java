package org.arivu.datastructure;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
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

public class AmapTest {

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
	public void testSize() {
		Amap<String, String> map = new Amap<String, String>();
		map.put("test", "test");
		assertTrue("Failed on size ", map.size() == 1);
		map.put(null, "tes1");
		assertTrue("Failed on size ", map.size()==2);
		assertTrue("Failed on size ", "tes1".equals(map.remove(null)));
		assertTrue("Failed on size ", map.size() == 1);
		assertTrue("Failed on size ", "test".equals(map.remove("test")));
		assertTrue("Failed on size ", map.size() == 0);
	}

	@Test
	public void testIsEmpty() {
		Amap<String, String> map = new Amap<String, String>();
		assertTrue("Failed on isEmpty ", map.isEmpty());
		map.put("test", "test");
		assertFalse("Failed on isEmpty ", map.isEmpty());
	}

	@Test
	public void testContainsKey() {
		Amap<String, String> map = new Amap<String, String>();
		assertFalse("Failed on containsKey ", map.containsKey("test"));
		map.put("test", "test");
		assertTrue("Failed on containsKey ", map.containsKey("test"));
		map.remove("test");
		assertFalse("Failed on containsKey ", map.containsKey("test"));
	}

	@Test
	public void testContainsValue() {
		Amap<String, String> map = new Amap<String, String>();
		assertFalse("Failed on containsValue ", map.containsValue("test"));
		map.put("test", "test");
		assertTrue("Failed on containsValue ", map.containsValue("test"));
		map.remove("test");
		assertFalse("Failed on containsValue ", map.containsValue("test"));
	}

	@Test
	public void testGet() {
		Amap<String, String> map = new Amap<String, String>();
		String get = map.get("test");
		assertTrue("Failed on get ", get == null);
		map.put("test", "test");
		get = map.get("test");
		assertTrue("Failed on get exp:: test got::" + get, "test".equals(get));
		map.remove("test");
		get = map.get("test");
		assertTrue("Failed on get ", get == null);
		
		map.put("test", "test");
		get = map.get("test");
		assertTrue("Failed on get exp:: test got::" + get, "test".equals(get));
		map.put("test", null);
		get = map.get("test");
		assertTrue("Failed on get ", get == null);
		
		get = map.get(null);
		assertTrue("Failed on get ", get == null);
		
		map.put(null, "1");
		get = map.get(null);
		assertTrue("Failed on get ", "1".equals(get));
		map.put(null, null);
		get = map.get(null);
		assertTrue("Failed on get ", get == null);
		
		
		map.put("test", "test");
		get = map.get("test");
		assertTrue("Failed on get exp:: test got::" + get, "test".equals(get));
		map.put("test", "test1");
		get = map.get("test");
		assertTrue("Failed on get exp:: test got::" + get, "test1".equals(get));
		
		assertTrue("Failed on get exp:: test keySet :: size", map.keySet().size() == map.size());
		
	}
	//
	// @Test
	// public void testPut() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testRemove() {
	// fail("Not yet implemented");
	// }
	//
	 @Test
	 public void testPutAll() {
		Amap<String, String> map = new Amap<String, String>();
		String get = map.get("test");
		assertTrue("Failed on get ", get == null);
		map.put("test", "test");
		assertTrue(map.size()==1);
		
		Amap<String, String> map2 = new Amap<String, String>(map);
		assertTrue(map2.size()==1);
	 }
	 
	
	 @Test
	 public void testClear() {
		Amap<String, String> map = new Amap<String, String>();
		String get = map.get("test");
		assertTrue("Failed on get ", get == null);
		map.put(null, "test");
		assertTrue(map.size()==1);
		map.clear();
		assertTrue(map.size()==0);
		map.put(null, null);
		assertTrue(map.size()==0);
		map.remove(null);
		assertTrue(map.size()==0);
	 }

	 // @Test
	// public void testKeySet() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testValues() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testEntrySet() {
	// fail("Not yet implemented");
	// }

	/**
	 * @throws InterruptedException 
	 */
	@Test
	public void testRunParallel() throws IOException, InterruptedException {
		final Map<String, String> map = new Amap<String, String>();// new
																	// CopyOnWriteArraySet<String>();//

		final int reqPerThread = 100;
		final int noOfThreads = 500;
		final Queue<Future<Integer>> listFuture = new DoublyLinkedList<Future<Integer>>();
		final ExecutorService exe = Executors.newFixedThreadPool(noOfThreads);
		final AtomicInteger c = new AtomicInteger(noOfThreads);
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch end = new CountDownLatch(1);

		for (int j = 1; j <= noOfThreads; j++) {
			final Callable<Integer> task = getTask(map, reqPerThread, c, start, end);
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
				poll.get();
//				System.out.println(" Completed :: "+poll.get());
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
		assertTrue("Failed in || run test exp::0 got::" + map.size(), map.size() == 0);
	}

	private Callable<Integer> getTask(final Map<String, String> map, final int reqPerThread, final AtomicInteger c,
			final CountDownLatch start, final CountDownLatch end) {
		final Callable<Integer> task = new Callable<Integer>() {

			@Override
			public Integer call() {
				try {
					start.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				int decrementAndGet = c.decrementAndGet();
				try {
					final String key = String.valueOf(Thread.currentThread().hashCode());
					
					for (int i = 0; i < reqPerThread; i++) {
						String value = String.valueOf(i);
						map.remove(key);
						if (map.get(key) != null) {
							if (decrementAndGet <= 0) {
								end.countDown();
							}
							System.err.println("Failed in check1 thread " + key + " value " + value);
							throw new RuntimeException("Failed in check1 thread " + key + " value " + value);
						}

						map.put(key, value);
//					System.out.println("Key "+key+" value "+value);
						if (map.get(key) != value) {
							if (decrementAndGet <= 0) {
								end.countDown();
							}
							System.err.println("Failed in check2 thread " + key + " value " + value);
							throw new RuntimeException("Failed in check2 thread " + key + " value " + value);
						}

						map.remove(key);
//					System.out.println("Completed Thread "+decrementAndGet+" req "+value);
					}
				} catch (Throwable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
