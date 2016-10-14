package org.arivu.dbds;

import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.log.LightningLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TestDataSourcesHelper {
	static final Logger logger = LoggerFactory.getLogger(TestDataSourcesHelper.class);

	int maxThreadsCnt = 500;
	int nThreads = 1000000;// 000;
	int poolSize = 625;
	int reuseCount = 2500;
	int lifeSpan = 30000;
	ExecutorService exe = null;

	public void setUpBeforeClass() throws Exception {
		exe = Executors.newFixedThreadPool(Math.min(maxThreadsCnt, nThreads));
	}

	public void tearDownAfterClass() throws Exception {
		exe.shutdownNow();
		if (!exe.awaitTermination(100, TimeUnit.MICROSECONDS)) {
			String msg = "Still waiting after 100ms: calling System.exit(0)...";
			logger.debug(msg);
			// System.err.println(msg);
		}
	}

	AtomicInteger noofConnection = null;
	ConnectionFactory factory = null;
	long time = 0;

	public void setUp() throws Exception {
		noofConnection = new AtomicInteger(0);
		factory = new ConnectionFactory() {

			@Override
			public Connection create(String user, String password, String driver, String url) {
				noofConnection.incrementAndGet();
				// return Mockito.mock(Connection.class);
				return new Conn();
			}
		};
		time = System.currentTimeMillis();
	}

	public void tearDown() throws Exception {
		String msg = "No of resource :: " + noofConnection.get() + " " + (-time + System.currentTimeMillis())
				+ " millisecs! ";
		logger.info(msg);
		System.out.println(msg);
		LightningLogger.flush();
		// System.out.println("After Flush!");
	}

	void testDataSource(final AbstractDataSource ds, final int verifyCnt, final boolean checkMin, boolean absoluteWait)
			throws InterruptedException {
		ds.setName("test");
		ds.setMaxPoolSize(poolSize);
		ds.setMaxReuseCount(reuseCount);
		ds.setMaxReuseTime(lifeSpan);
		ds.registerMXBean();

		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch end = new CountDownLatch(1);
		final AtomicInteger f = new AtomicInteger(nThreads);
		final Random rd = new Random(System.currentTimeMillis());
		Queue<Future<Integer>> listFuture = new DoublyLinkedList<Future<Integer>>();

		for (int i = 0; i < nThreads; i++) {
			final Callable<Integer> r = getTask(ds, start, end, f, rd);
			Future<Integer> submit = exe.submit(r);
			listFuture.add(submit);
		}
		start.countDown();
		end.await();

		if (absoluteWait) {
			for (Future<Integer> fu : listFuture)
				try {
					fu.get();
					fu.cancel(true);
				} catch (ExecutionException e) {
					e.printStackTrace();
				} 
		}
		listFuture.clear();

		assertTrue("Failed in allconnections! " + ds.getMaxPoolSize(), ds.getMaxPoolSize() <= verifyCnt);
		ds.close();
	}

	private Callable<Integer> getTask(final AbstractDataSource ds, final CountDownLatch start, final CountDownLatch end,
			final AtomicInteger f, final Random rd) {
		return new TestResults(ds, start, end, f, rd, exe);
	}

}

final class TestResults implements Callable<Integer> {
	static final Logger logger = LoggerFactory.getLogger(TestResults.class);
	final AbstractDataSource ds;
	final CountDownLatch start;
	final CountDownLatch end;
	final AtomicInteger f;
	final Random rd;
	final ExecutorService exe;

	TestResults(AbstractDataSource ds, CountDownLatch start, CountDownLatch end, AtomicInteger f, Random rd,
			ExecutorService exe) {
		super();
		this.ds = ds;
		this.start = start;
		this.end = end;
		this.f = f;
		this.rd = rd;
		this.exe = exe;
	}

	@Override
	public Integer call() throws Exception {
		final int id = f.decrementAndGet();
		try {
			start.await();
			int y = rd.nextInt(100);
			long s = System.currentTimeMillis();
			final Connection connection = ds.getConnection();
			try {
				connection.prepareStatement("test" + y);
				logger.debug("Acquired(" + ((System.currentTimeMillis() - s)) + ") :: " + connection);
			} finally {
				connection.close();
			}
		} catch (Throwable e) {
			System.out.println("failed with err :: " + e);
		} finally {
			logger.debug(" Remaining count " + f + " exe " + exe.isTerminated());
			if (id == 0) {
				end.countDown();
			}
		}

		return id;
	}
}