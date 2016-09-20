/**
 * 
 */
package org.arivu.log.queue;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.datastructure.Threadlocal;
import org.arivu.datastructure.Threadlocal.Factory;
import org.arivu.log.Appender;
import org.arivu.log.LightningLogger;
import org.arivu.log.Converter;
import org.arivu.log.LogMXBean;
import org.arivu.log.appender.Appenders;

/**
 * Single producer takes all the logs. Keeps all the consumers lock free logging
 * made possible using circular linked log consumer buffer.
 * 
 * @author P
 *
 * @param <T>
 */
public final class Producer<T> implements AutoCloseable {
	private static final long IDEAL_TIME_THREAD = 1000*30;//*60*5;
	
	/**
	 * 
	 */
	private volatile boolean closeFlag = false;
	/**
	 * List of all appenders -> file,console,rollingfile
	 */
	private final Collection<Appender> appenders;

	/**
	 * 
	 */
	private final Converter<T> converter;

	/**
	 * 
	 */
	private final AtomicInteger threadCnt = new AtomicInteger(0);

	/**
	 * @param converter
	 * @param appenders
	 */
	public Producer(Converter<T> converter, Collection<Appender> appenders) {
		this(1, converter, appenders);
	}

	/**
	 * @param threadCnt
	 * @param converter
	 * @param appenders
	 */
	private Producer(int threadCnt, Converter<T> converter, Collection<Appender> appenders) {
		super();
		this.appenders = appenders;
		this.converter = converter;
		this.registerMXBean(0);
	}

	/**
	 * 
	 */
	String beanNameStr = null;

	/**
	 * 
	 */
	private void registerMXBean(final int cnt) {
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			beanNameStr = "org.arivu.log:type=" + getClass().getSimpleName() + "." + String.valueOf(cnt);
			mbs.registerMBean(getLogMXBean(), new ObjectName(beanNameStr));
		} catch (InstanceAlreadyExistsException e) {
			System.err.println(e.toString());
			registerMXBean(cnt + 1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return
	 */
	private LogMXBean getLogMXBean() {
		final Producer<T> that = this;
		return new LogMXBean() {

			@Override
			public void flush() throws Exception {
				that.flush();
			}

			@Override
			public void addConsumer() {
				throw new RuntimeException("Cannot perform this funcion on Producer!");
			}

			@Override
			public void removeConsumer() throws Exception {
				throw new RuntimeException("Cannot perform this funcion on Producer!");
			}

			@Override
			public void close() throws Exception {
				that.close();
			}

			@Override
			public int getBatchSize() {
				return Consumer.BATCH_SIZE;
			}

			@Override
			public void setBatchSize(int size) {
				Consumer.BATCH_SIZE = size;
			}

			@Override
			public void addAppender(String customAppender, String fileName) {
				Appenders valueOf = Appenders.valueOf(customAppender);
				if (valueOf != null) {
					try {
						appenders.add(valueOf.get(fileName));
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				} else
					LightningLogger.addCustomAppender(appenders, customAppender);
			}

			@Override
			public int getBufferSize() {
				throw new RuntimeException("Cannot perform this funcion on Producer!");
			}

			@Override
			public String[] getAppenders() {
				Collection<String> apnames = new DoublyLinkedList<String>();
				if (appenders != null) {
					for (Appender a : appenders)
						apnames.add(a.getClass().getSimpleName());
				}
				return apnames.toArray(new String[] {});
			}

			@Override
			public void removeAppender(String name) {
				if (appenders != null) {
					Appender r = null;
					for (Appender a : appenders) {
						if (a.getClass().getSimpleName().equals(name)) {
							r = a;
							break;
						}
					}

					if (r != null)
						appenders.remove(r);
				}
			}

			@Override
			public void evictConsumer() throws Exception {
				that.threadlocal.evict();
			}

			@Override
			public int getConsumerCount() {
				return that.threadlocal.size();
			}
		};
	}

	/**
	 * @param t
	 */
	public void produce(T t) {
		if (closeFlag)
			return;
		long time = System.currentTimeMillis();
		threadlocal.get(null).consume(t,time);
	}

	String getThreadId() {
		return String.valueOf(Thread.currentThread().hashCode());
	}
	
	final Threadlocal<Consumer<T>> threadlocal = new Threadlocal<Consumer<T>>(new Factory<Consumer<T>>() {

		@Override
		public Consumer<T> create(Map<String, Object> params) {
			return new Consumer<T>(converter, appenders, threadCnt.getAndIncrement(), Producer.this);
		}
	}, IDEAL_TIME_THREAD);
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() throws Exception {
		if (!closeFlag) {
			closeFlag = true;
			closeCosumers();
			for (Appender w : appenders) {
				w.close();
			}
			unregisterMXBean();
		}
	}

	/**
	 * 
	 */
	private void unregisterMXBean() {
		if (beanNameStr != null) {
			try {
				ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName(beanNameStr));
			} catch (Exception e) {
				System.err.println(e.toString());
			}
		}
	}

	/**
	 * Completely flushed the log queue on all threads ( consumers)
	 * 
	 * @param parallel
	 *            TODO
	 * 
	 * @throws Exception
	 */
	public void flush() throws Exception {
		for ( Consumer<T> c : threadlocal.getAll()) {
			c.flush();
		}
	}

	/**
	 * @throws Exception
	 */
	private void closeCosumers() throws Exception {
		threadlocal.close();
	}

}
