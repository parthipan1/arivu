/**
 * 
 */
package org.arivu.log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.arivu.log.appender.Appenders;
import org.arivu.log.converter.StringConverter;
import org.arivu.log.queue.Producer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author P
 *
 */
public class QueueTest {

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
	 * Test method for {@link org.arivu.log.queue.Consumer#run()}.
	 * @throws IOException 
	 */
	@Test
	public void testRunParallel_Null() throws IOException {//,new RollingFileAppender("logs"+File.separator+"test.log")
		Collection<Appender> appenders = new ArrayList<Appender>(Arrays.asList(new Appender[]{Appenders.no.get("logs"+File.separator+"testp.log")})) ;//,new ConsoleAppender()
		final Producer<String> logProducer = new Producer<String>(new StringConverter(), appenders);
		
		final int reqPerThread = 2000;//0000;//000;
		final int noOfThreads = 500;
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
						logProducer.produce(String.valueOf(initialValue-cnt.getAndDecrement()));
					}
//					System.out.println("Remaining count "+c.get()+" cnt "+cnt.get());
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
//		System.out.println("Producer close!");
		try {
			logProducer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testRunSerial_FileMax() throws IOException {//,new RollingFileAppender("logs"+File.separator+"test.log")
		Collection<Appender> appenders = new ArrayList<Appender>(Arrays.asList(new Appender[]{Appenders.zip.get("logs"+File.separator+"testsm.log")})) ;//,new ConsoleAppender()
		final Producer<String> logProducer = new Producer<String>(new StringConverter(), appenders);
		
		final int reqPerThread = 1000*1000*100;//0000;//000;
		final int noOfThreads = 1;
		
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
						logProducer.produce(String.valueOf(initialValue-cnt.getAndDecrement()));
//						logger.info(String.valueOf(initialValue-cnt.getAndDecrement()));
					}
//					System.out.println("Remaining count "+c.get()+" cnt "+cnt.get());
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
//		System.out.println("Producer close!");
		try {
			logProducer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

		/**
		 * Test method for {@link org.arivu.log.queue.Consumer#run()}.
		 * @throws IOException 
		 */
		@Test
		public void testRunParallel_FileMax() throws IOException {//,new RollingFileAppender("logs"+File.separator+"test.log")
			final int reqPerThread = 1000000;
			final int noOfThreads = 100;

			Collection<Appender> appenders = new ArrayList<Appender>(Arrays.asList(new Appender[]{Appenders.zip.get("logs"+File.separator+"testpm.log")})) ;//,new ConsoleAppender()
			final Producer<String> logProducer = new Producer<String>(new StringConverter(), appenders);
			
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
							logProducer.produce(String.valueOf(initialValue-cnt.getAndDecrement()));
//							logger.info(String.valueOf(initialValue-cnt.getAndDecrement()));
						}
//						System.out.println("Remaining count "+c.get()+" cnt "+cnt.get());
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
//			System.out.println("Producer close!");
			try {
				logProducer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	@Test
	public void testRunSerial_Null() throws IOException {//,new RollingFileAppender("logs"+File.separator+"test.log")
		Collection<Appender> appenders = new ArrayList<Appender>(Arrays.asList(new Appender[]{Appenders.no.get("logs"+File.separator+"tests.log")})) ;//,new ConsoleAppender()
		final Producer<String> logProducer = new Producer<String>(new StringConverter(), appenders);
		
		final int noOfThreads = 1000000;
		for( int j=0;j<noOfThreads;j++ ){
			logProducer.produce(String.valueOf(j));
		}
		try {
			logProducer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
		 * Test method for {@link org.arivu.log.queue.Consumer#run()}.
		 * @throws IOException 
		 */
		@Test
		public void testRunParallel_File() throws IOException {//,new RollingFileAppender("logs"+File.separator+"test.log")
			Collection<Appender> appenders = new ArrayList<Appender>(Arrays.asList(new Appender[]{Appenders.zip.get("logs"+File.separator+"testp.log")})) ;//,new ConsoleAppender()
			final Producer<String> logProducer = new Producer<String>(new StringConverter(), appenders);
			
			final int reqPerThread = 2000;//0000;//000;
			final int noOfThreads = 500;
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
							logProducer.produce(String.valueOf(initialValue-cnt.getAndDecrement()));
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
//			System.out.println("Producer close!");
			try {
				logProducer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	@Test
	public void testRunSerial_File() throws IOException {//,new RollingFileAppender("logs"+File.separator+"test.log")
		Collection<Appender> appenders = new ArrayList<Appender>(Arrays.asList(new Appender[]{Appenders.zip.get("logs"+File.separator+"tests.log")})) ;//,new ConsoleAppender()
		final Producer<String> logProducer = new Producer<String>(new StringConverter(), appenders);
		
		final int noOfThreads = 1000000;
		for( int j=0;j<noOfThreads;j++ ){
			logProducer.produce(String.valueOf(j));
		}
		try {
			logProducer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testRunSerial_NullMax() throws IOException {//,new RollingFileAppender("logs"+File.separator+"test.log")
		Collection<Appender> appenders = new ArrayList<Appender>(Arrays.asList(new Appender[]{Appenders.no.get("logs"+File.separator+"tests.log")})) ;//,new ConsoleAppender()
		final Producer<String> logProducer = new Producer<String>(new StringConverter(), appenders);
		
		final int reqPerThread = 1000*1000*100;//0000;//000;
		final int noOfThreads = 1;
		
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
						logProducer.produce(String.valueOf(initialValue-cnt.getAndDecrement()));
//						logger.info(String.valueOf(initialValue-cnt.getAndDecrement()));
					}
//					System.out.println("Remaining count "+c.get()+" cnt "+cnt.get());
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
//		System.out.println("Producer close!");
		try {
			logProducer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
		 * Test method for {@link org.arivu.log.queue.Consumer#run()}.
		 * @throws IOException 
		 */
		@Test
		public void testRunParallel_NullMax() throws IOException {//,new RollingFileAppender("logs"+File.separator+"test.log")
			Collection<Appender> appenders = new ArrayList<Appender>(Arrays.asList(new Appender[]{Appenders.no.get("logs"+File.separator+"testp.log")})) ;//,new ConsoleAppender()
			final Producer<String> logProducer = new Producer<String>(new StringConverter(), appenders);
			
			final int reqPerThread = 1000000;//0000;//000;
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
							logProducer.produce(String.valueOf(initialValue-cnt.getAndDecrement()));
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
//			System.out.println("Producer close!");
			try {
				logProducer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
}
