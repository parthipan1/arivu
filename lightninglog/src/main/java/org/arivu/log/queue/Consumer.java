package org.arivu.log.queue;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collection;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.log.Appender;
import org.arivu.log.LightningLogger;
import org.arivu.log.Converter;
import org.arivu.log.LogMXBean;
import org.arivu.log.appender.AppenderProperties;
import org.arivu.log.appender.Appenders;

/**
 * Consumer similar to actor design pattern , consumes all the logs produced by
 * log producer. Lock Free consumer Tread local.
 * 
 * @author P
 *
 * @param <T>
 */
public final class Consumer<T> implements AutoCloseable {

//	private static final char END_LINE = (char) 13;
	/**
	 * Default length used to push to appenders.
	 */
	public static int BATCH_SIZE = 100;
	/**
	 * Each ring buffers size
	 */
	public static int RINGBUFFER_LEN = 300;

	/**
	 * 
	 */
	final Object[] buffer = new Object[RINGBUFFER_LEN];
	/**
	 * 
	 */
	volatile int idx = 0;
	/**
	 * 
	 */
	private Collection<Appender> appenders;

	/**
	 * 
	 */
	private Producer<T> producer;

	/**
	 * 
	 */
	private final Object threadId;
	/**
	 * 
	 */
	volatile long lasttime = System.currentTimeMillis();
	
	volatile int batchSize = BATCH_SIZE;

	/**
	 * @param converter
	 * @param appenders
	 * @param id
	 * @param producer
	 */
	public Consumer(Converter<T> converter, Collection<Appender> appenders, int id, Producer<T> producer) {
		super();
		this.appenders = appenders;
		this.producer = producer;
		this.threadId = producer.getThreadId();
		this.registerMXBean(id);

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
			beanNameStr = "org.arivu.log:type=" + getClass().getSimpleName() + this.threadId + "." + cnt;
			mbs.registerMBean(getLogMXBean(), new ObjectName(beanNameStr));
		} catch (InstanceAlreadyExistsException e) {
			// System.err.println(e.getMessage());
			registerMXBean(cnt + 1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return
	 */
	private LogMXBean getLogMXBean() {
		@SuppressWarnings("resource")
		final Consumer<T> that = this;
		return new LogMXBean() {
			@Override
			public int getBufferSize() {
				return RINGBUFFER_LEN;
			}

			@Override
			public void flush() {
				that.flush();
			}

			@Override
			public void addConsumer() {
				throw new IllegalStateException("Cannot invoke addConsumer() on consumer !");
			}

			@Override
			public void removeConsumer() {
				throw new IllegalStateException("Cannot invoke removeConsumer() on consumer !");
			}

			@Override
			public void close() throws Exception {
				that.producer.threadlocal.remove(threadId);
			}

			@Override
			public int getBatchSize() {
				return that.batchSize;
			}

			@Override
			public void setBatchSize(int size) {
				that.batchSize = size;
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
				throw new IllegalStateException("Cannot invoke removeConsumer() on consumer !");
			}

			@Override
			public int getConsumerCount() {
				return producer.threadlocal.size();
			}
		};
	}

	public void consume(final T t, long time) {
		lasttime = time;
		if (t != null) {
			buffer[idx++] = t;
			if (idx == buffer.length) {
				idx = 0;
				flush();
			}
		}
	}

	public void flush() {
		final int length = buffer.length;
		if(length==1){
			write((String)buffer[0]);
			buffer[0] = null;
		}else{
			int limit = 0;
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < length; i++) {
				Object obj = buffer[i];
				buffer[i] = null;
				if (obj != null) {
					sb.append(obj).append(AppenderProperties.separator);
					++limit;
					if (limit == batchSize) {
						write(sb.toString());
						limit = 0;
						sb = new StringBuffer();
					}
				}
			}
			if (sb.length()>0) {
				write(sb.toString());
			}
		}
	}

	/**
	 * @return isEmpty
	 */
	public boolean isEmpty() {
		for (int i = 0; i < buffer.length; i++) {
			if (buffer[i] != null)
				return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() throws Exception {
		if (appenders == null)
			return;
		flush();
		unregisterMXBean();
		appenders = null;
		producer = null;
	}

	/**
	 * 
	 */
	private void unregisterMXBean() {
		if (beanNameStr != null) {
			try {
				ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName(beanNameStr));
			} catch (Exception e) {
//				System.err.println(e.toString());
			}
		}
	}

	/**
	 * @param sb
	 */
	private void write(String sb) {
		if (sb != null && sb.length() > 0) {
			if(appenders!=null){
				for (Appender writer : appenders) {
					writer.append(sb);
				}
			}else{
				System.out.println(sb);
			}
		}
	}

}
