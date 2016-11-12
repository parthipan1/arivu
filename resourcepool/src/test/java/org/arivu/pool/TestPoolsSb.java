package org.arivu.pool;

import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.arivu.datastructure.DoublyLinkedList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
public class TestPoolsSb {
	static final Logger logger = LoggerFactory.getLogger(TestPoolsSb.class);
	static final int nThreads = 100;
	static final int poolSize = 2;
	static final int reuseCount = -1;
	static final int lifeSpan = -1;
	static ExecutorService exe = null;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		final AtomicInteger min = new AtomicInteger(Math.min(500, nThreads)) ;
		exe = Executors.newFixedThreadPool(min.get());
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch end = new CountDownLatch(1);
		int f = min.get();
		for (int i = 0; i < f; i++) {
			exe.submit(new Runnable(){
				@Override
				public void run() {
					try {
						start.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}finally {
						int decrementAndGet = min.decrementAndGet();
//						System.out.println("min :: "+decrementAndGet );
						if(decrementAndGet<=1){
							end.countDown();
						}
					}
				}
				
			});
		}
		start.countDown();
		end.await();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		exe.shutdownNow();
		if (!exe.awaitTermination(100, TimeUnit.MICROSECONDS)) {
//	        System.out.println("Still waiting after 100ms: calling System.exit(0)...");
	    }
	}
	
	AtomicInteger noOfCreate = null;
	//@SuppressWarnings("rawtypes")
	PoolFactory<StringBuffer> factory = null;
	long s = 0l;
	//@SuppressWarnings("rawtypes")
	@Before
	public void setUp() throws Exception {
		s = System.currentTimeMillis();
		noOfCreate = new AtomicInteger(0);
		factory = new PoolFactory<StringBuffer>() {
				@Override
				public StringBuffer create(Map<String, Object> params) {
					noOfCreate.incrementAndGet();
//					ArrayList<Object> arrayList = new ArrayList<Object>();
//					System.out.println("ceated "+arrayList.hashCode());
//					return arrayList;
					return new StringBuffer();
				}

				@Override
				public void close(StringBuffer t) {
					
				}

				@Override
				public void clear(StringBuffer t) {
					if (t!=null) {
//						t.clear();
						t.delete(0, t.length());
					}
				}
			};
	}

	@After
	public void tearDown() throws Exception {
		System.out.println("No of Resources Created :: "+noOfCreate+" time millisecs "+(System.currentTimeMillis()-s));
	}

	static Collection<String> values = new HashSet<String>(); 
	
	@Test
	public void test4ConcurrentPool() throws Exception {
		final Pool<StringBuffer> pool = new ConcurrentPool<StringBuffer>(factory , StringBuffer.class);
		testPool(nThreads, poolSize, reuseCount, lifeSpan, pool,  new Random(), poolSize, false, false);
	}
	
	@Test
//	@Ignore
	public void test2NonBlockingPool() throws Exception {
		//@SuppressWarnings("rawtypes")
		final Pool<StringBuffer> pool = new NonBlockingPool<StringBuffer>(factory, StringBuffer.class);//
		testPool(nThreads, poolSize, reuseCount, lifeSpan, pool, new Random(), poolSize, false, true);
	}
	
	@Test
	public void test6NoPool() throws Exception {
		final Pool<StringBuffer> pool = new NoPool<StringBuffer>(factory, StringBuffer.class);//
		testPool(nThreads, poolSize, reuseCount, lifeSpan, pool, new Random(), nThreads, false, false);
	}
	
	@Test
	public void test5ThreadLocalPool() throws Exception {
		//@SuppressWarnings("rawtypes")
		final Pool<StringBuffer> pool = new ThreadLocalPool<StringBuffer>(factory, StringBuffer.class);//
		testPool(nThreads, poolSize, reuseCount, lifeSpan, pool, new Random(), nThreads, true, false);
	}
	
	
	volatile int finishCnt = 0;
	private void testPool(final int nThreads, final int poolSize, final int reuseCount, final int lifeSpan,
			final Pool<StringBuffer> pool, final Random random, final int verifyCnt, boolean verifyCntCheck, boolean nullCheck) throws InterruptedException, Exception {
			values.clear();
			pool.setMaxPoolSize(poolSize);
			pool.setMaxReuseCount(reuseCount);
			pool.setLifeSpan(lifeSpan);
			
			final CountDownLatch start = new CountDownLatch(1);
			final CountDownLatch end = new CountDownLatch(1);
			finishCnt = nThreads;
			final AtomicInteger f = new AtomicInteger(nThreads);
			Queue<Future<Integer>> listFuture = new DoublyLinkedList<Future<Integer>>();
			for (int i = 0; i < nThreads; i++) {
				Callable<Integer> task = getTask(pool, random, start, end, f, nullCheck);
				Future<Integer> submit = exe.submit(task);
				listFuture.add(submit);
			}
			start.countDown();
			end.await();
			
			Future<Integer> poll = null;
			while((poll=listFuture.poll())!=null){
				try {
					logger.debug(" Completed :: "+poll.get());
					poll.cancel(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			listFuture.clear();
			assertTrue(pool.getMaxPoolSize()<=verifyCnt);
			assertTrue(pool.getMaxPoolSize()<=noOfCreate.get());
			if (verifyCntCheck) {
				assertTrue("Failed in Object creation Expected :: " + verifyCnt + " got :: " + values.size(),
						values.size() <= verifyCnt);
			}
			pool.close();
			
		}

	private Callable<Integer> getTask(final Pool<StringBuffer> pool, final Random random, final CountDownLatch start,
			final CountDownLatch end, final AtomicInteger f, final boolean nullCheck) {
		return new CustomTestResult(pool, random, start, end, f, nullCheck, values);
	}

}
class CustomTestResult implements Callable<Integer>{
	final Pool<StringBuffer> pool;
	final Random random;
	final CountDownLatch start;
	final CountDownLatch end;
	final AtomicInteger f;
	final boolean nullCheck;
	final Collection<String> values;
	/**
	 * @param pool
	 * @param random
	 * @param start
	 * @param end
	 * @param f
	 * @param nullCheck
	 */
	CustomTestResult(Pool<StringBuffer> pool, Random random, CountDownLatch start, CountDownLatch end,
			AtomicInteger f, boolean nullCheck, Collection<String> values) {
		super();
		this.pool = pool;
		this.random = random;
		this.start = start;
		this.end = end;
		this.f = f;
		this.nullCheck = nullCheck;
		this.values = values;
	}
	@Override
	public Integer call() throws Exception {
		final int id  = f.decrementAndGet();
		try {
			start.await();
			StringBuffer sb = null;
			if (nullCheck) {
				while ((sb = pool.get(null)) == null) {
					try {
						if (sb == null)
							if (random == null)
								Thread.sleep(100);
							else
								Thread.sleep(random.nextInt(100));
					} catch (Exception e) {
						//									e.printStackTrace();
					}
				} 
			}else{
				sb = pool.get(null);
			}
			int a=(int)(Math.random()*1000);//random.nextInt(poolSize);
			for(int i=0;i<10;i++){
				sb.append((a+i));
			}
			values.add(String.valueOf(sb.hashCode()) );
			pool.put(sb);
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			if(id==0){
				end.countDown();
			}
		}
		return id;
	}
}