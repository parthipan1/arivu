package org.arivu.pool;

import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.DoublyLinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestHelper {

	 private static final Logger logger = LoggerFactory.getLogger(TestHelper.class);

	int maxThreadCnt = 50;
	int nThreads = 100000;
	int poolSize = 62;
	int reuseCount = 250;
	int lifeSpan = 3000;
	
	ExecutorService exe = null;

	public void setUpBeforeClass() throws Exception {
		exe = Executors.newFixedThreadPool(Math.min(maxThreadCnt, nThreads));// Executors.newCachedThreadPool();//
	}

	public void tearDownAfterClass() throws Exception {
		exe.shutdownNow();
		if (!exe.awaitTermination(100, TimeUnit.MICROSECONDS)) {
			// System.out.println("Still waiting after 100ms: calling
			// System.exit(0)...");
		}
	}

	AtomicInteger noOfCreate = null;
	PoolFactory<Resource> factory = null;
	long s = 0l;

	public void setUp() throws Exception {
		// exe = Executors.newFixedThreadPool(Math.min(maxThreadCnt, nThreads));
		s = System.currentTimeMillis();
		noOfCreate = new AtomicInteger(0);
		factory = new PoolFactory<Resource>() {
			@Override
			public Resource create(Map<String, Object> params) {
				noOfCreate.incrementAndGet();
				return new ResourceImp();
			}

			@Override
			public void close(Resource t) {
				if (t != null) {
					try {
						t.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void clear(Resource t) {
				if (t != null) {
					t.clear();
				}
			}
		};
	}

	public void tearDown() throws Exception {
		String msg = "No of Resources Created :: " + noOfCreate + " time millisecs " + (System.currentTimeMillis() - s);
		System.out.println(msg);
		logger.info(msg);
//		LightningLogger.flush();
	}

	void testPool(final Pool<Resource> pool, final int verifyCnt, boolean absoluteWait) throws InterruptedException, Exception {
		pool.setMaxPoolSize(poolSize);
		pool.setMaxReuseCount(reuseCount);
		pool.setLifeSpan(lifeSpan);
		Queue<Future<Integer>> listFuture = new DoublyLinkedList<Future<Integer>>();
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch end = new CountDownLatch(1);
		final AtomicInteger f = new AtomicInteger(nThreads);
		for (int i = 0; i < nThreads; i++) {
			Callable<Integer> task = getTask(pool, start, f, end);
			Future<Integer> submit = exe.submit(task);
			listFuture.add(submit);
		}
		start.countDown();
		end.await();
//		System.out.println("End signalled!");
		if( absoluteWait ){
			Future<Integer> poll = null;
			while((poll=listFuture.poll())!=null){
				try {
					logger.debug(" Completed :: "+poll.get());
					poll.cancel(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		listFuture.clear();

		assertTrue(pool.getMaxPoolSize() <= verifyCnt);
		pool.close();
	}

	private Callable<Integer> getTask(final Pool<Resource> pool, final CountDownLatch start, final AtomicInteger f,
			final CountDownLatch end) {
		return new TestResult(pool, start, f, end);
	}

}
class TestResult implements Callable<Integer>{
	private static final Logger logger = LoggerFactory.getLogger(TestResult.class);
	final Pool<Resource> pool;
	final CountDownLatch start;
	final AtomicInteger f;
	final CountDownLatch end;
	/**
	 * @param pool
	 * @param start
	 * @param f
	 * @param end
	 */
	TestResult(Pool<Resource> pool, CountDownLatch start, AtomicInteger f, CountDownLatch end) {
		super();
		this.pool = pool;
		this.start = start;
		this.f = f;
		this.end = end;
	}
	@Override
	public Integer call() throws Exception {
		final int id = f.decrementAndGet();
		try {
			start.await();
			Resource connection = null;
			Map<String, Object> params = new Amap<String, Object>();
			params.put("rc", id);
			while ((connection = pool.get(params)) == null) {
				try {
					if (connection == null)
						Thread.sleep(100);
				} catch (Exception e) {
					logger.error("Error getConn :: ", e);
				}
			}

			try {
				connection.perform();
			} finally {
				connection.close();
			}
		} catch (Throwable e) {
			logger.error("Error perform :: ", e);
			System.out.println("Failed with err :: " + e);
			e.printStackTrace();
		} finally {
			if (id == 0) {
				end.countDown();
			}
		}

		return id;
	}
	
}

interface Resource extends AutoCloseable {
	int perform();

	void clear();
}

class ResourceImp implements Resource {
//	private static final Logger logger = LoggerFactory.getLogger(ResourceImp.class);
	volatile boolean c = false;
	final String name = Thread.currentThread().getName();
	@Override
	public void close() throws Exception {
		// System.out.println("Closed "+toString());
		c = true;
	}

	public ResourceImp() {
		super();
		// System.out.println("Created "+toString());
	}

	@Override
	public void clear() {
//		if (c){
//			throw new IllegalStateException("ResourceImp" + hashCode() + " already closed!");
////			System.err.println("ResourceImp" + hashCode() + " already closed!");
//		}
	}

	@Override
	public int perform() {
		if (c){
//			logger.info("ResourceImp" + hashCode() + " already closed! name "+name+" Thread "+Thread.currentThread().getName());
			throw new IllegalStateException("ResourceImp" + hashCode() + " already closed!");
//			System.err.println("ResourceImp" + hashCode() + " already closed! name "+name+" Thread "+Thread.currentThread().getName());
		}
			
		int y = Math.round(1) * 1000000;
		for (int j = 0; j < 1000; j++) {
			for (int k = 0; k < 1000; k++) {
				y++;
			}
		}
		return y;
	}

}