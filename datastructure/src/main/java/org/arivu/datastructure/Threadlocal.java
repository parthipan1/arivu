/**
 * 
 */
package org.arivu.datastructure;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author P
 *
 */
public final class Threadlocal<T> {

	private static final long THRESHOLD_TIME = 1000 * 60;

	public interface Factory<T> {
		T create(Map<String, Object> params);
	}

	final Factory<T> factory;
	final Map<Object, Ref<T>> threadLocal = new Amap<Object, Ref<T>>();
	final Trigger trigger;
	final long threshold;
	final Thread hook = new Thread(new Runnable() {
		@Override
		public void run() {
			close();
		}
	});
	private final Runnable triggerRunnable = new Runnable() {

		@Override
		public void run() {
			evict();
		}
	};

	/**
	 * @param factory
	 */
	public Threadlocal(Factory<T> factory) {
		this(factory, THRESHOLD_TIME);
	}

	/**
	 * @param factory
	 * @param threshold
	 */
	public Threadlocal(Factory<T> factory, long threshold) {
		super();
		this.factory = factory;
		this.threshold = threshold;
		this.trigger = new Trigger(triggerRunnable, threshold);
		Runtime.getRuntime().addShutdownHook(hook);
	}

	public int size() {
		return threadLocal.size();
	}

	public boolean isEmpty() {
		return threadLocal.isEmpty();
	}

	public void remove() {
		remove(getId());
	}

	private Object getId() {
//		return String.valueOf(Thread.currentThread().hashCode());
//		return Thread.currentThread().getName();
		return Thread.currentThread().hashCode();
	}

	public void remove(final Object id) {
		Ref<T> remove = threadLocal.remove(id);
		if (remove != null)
			close(remove);
	}

	public void set(T t) {
		remove();
		if (t != null) {
			final Object id = getId();
			threadLocal.put(id, new Ref<T>(t));
		}
	}

	public T get(Map<String, Object> params) {
		final Object id = getId();
		Ref<T> ref = threadLocal.get(id);
//		if (ref == null) {
//			ref = new Ref<T>(factory.create(params));
//			// System.out.println("get params Threadlocal id "+id+"
//			// "+System.currentTimeMillis());
//			threadLocal.put(id, ref);
//		}
		T andTrigger = getAndTrigger(ref);
		if(andTrigger==null){
			ref = new Ref<T>(factory.create(params));
			threadLocal.put(id, ref);
			return ref.t;
		}
		return andTrigger;
	}

	T getAndTrigger(final Ref<T> ref) {
		if (ref == null)
			return null;
		
		final long ti = System.currentTimeMillis();

		if (threshold > 0) {
			if (ref.isExpired(ti, threshold)) {
				remove();
				close(ref);
				trigger.trigger(ti);
				return null;
			}
			trigger.trigger(ti);
		}

		return ref.get(ti);
	}

	public T get() {
		return getAndTrigger(threadLocal.get(getId()));
	}

	public void evict() {
		final long currentTimeMillis = System.currentTimeMillis();
		for (final Entry<Object, Ref<T>> e : threadLocal.entrySet()) {
			Ref<T> value = e.getValue();
			if (value.isExpired(currentTimeMillis, threshold)) {
				e.setValue(null);
				close(value);
			}
		}
	}

	public void clearAll() {
		closeAll();
		threadLocal.clear();
	}

	public Set<Entry<Object, T>> getAll() {
//		Collection<T> all = new DoublyLinkedList<T>();
//		for (Ref<T> ref : threadLocal.values()) {
//			if (ref.t != null)
//				all.add(ref.t);
//		}
//		return all;
		
		Set<Entry<Object, Ref<T>>> entrySet = threadLocal.entrySet();
		Set<Entry<Object, T>> ret = new DoublyLinkedSet<>();
		
		for(final Entry<Object, Ref<T>> e:entrySet ){
			ret.add(new Entry<Object, T>() {

				@Override
				public Object getKey() {
					return e.getKey();
				}

				@Override
				public T getValue() {
					return e.getValue().t;
				}

				@Override
				public T setValue(T value) {
					if(value == null){
						e.setValue(null);
						return null;
					}else{
						e.getValue().t = value;
						return value;
					}
				}
			});
		}
		
		return ret;
	}
//	volatile boolean closed = false; 
	public void close() {
		clearAll();
		try {
			Runtime.getRuntime().removeShutdownHook(hook);
		} catch (Throwable e1) {
//			e1.printStackTrace();
//			System.err.println(e1.toString());
		}
	}

	private void closeAll() {
		for (Entry<Object, Ref<T>> e : threadLocal.entrySet()) {
			Ref<T> value = e.getValue();
			e.setValue(null);
			close(value);
		}
	}

	void close(Ref<T> value) {
		if (value != null && value.t != null && value.t instanceof AutoCloseable) {
			try {
				((AutoCloseable) value.t).close();
			} catch (Exception e1) {
				System.err.println(e1.toString());
			}
		}
	}

	/**
	 * @author P
	 *
	 * @param <T>
	 */
	static final class Ref<T> {
		private T t;
		volatile long time = System.currentTimeMillis();

		/**
		 * @param t
		 */
		Ref(T t) {
			super();
			this.t = t;
		};

		T get(long ti) {
			time = ti;
			return t;
		}

		boolean isExpired(final long ctm, long thr) {
			return (ctm - time) > thr;
		}

	}

	/**
	 * @author P
	 *
	 */
	static final class Trigger {
		private final Runnable trigger;
		private final long onceInMiliSecs;
		private volatile long last = System.currentTimeMillis();

		/**
		 * @param trigger
		 * @param onceInMiliSecs
		 */
		Trigger(Runnable trigger, long onceInMiliSecs) {
			super();
			this.trigger = trigger;
			this.onceInMiliSecs = onceInMiliSecs;
		}

		private Future<?> tr = null;

		void trigger(long time) {
			long diff = time - last;
			last = time;
			if (diff >= onceInMiliSecs) {
				final ExecutorService exe = Executors.newFixedThreadPool(1);
				tr = exe.submit(new Runnable() {

					@Override
					public void run() {
						try {
							trigger.run();
						} finally {
							close(exe);
						}
					}
				});
			}
		}

		void close(final ExecutorService exe) {
			if (tr != null) {
				tr.cancel(true);
				tr = null;
			}
			exe.shutdownNow();
		}

	}
}
