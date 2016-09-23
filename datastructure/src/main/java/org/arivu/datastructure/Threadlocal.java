/**
 * 
 */
package org.arivu.datastructure;

import java.util.Collection;
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
	final Map<String, Ref<T>> threadLocal = new Amap<String, Ref<T>>();
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
	
	public Threadlocal(Factory<T> factory) {
		this(factory,THRESHOLD_TIME);
	}

	public Threadlocal(Factory<T> factory,long threshold) {
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

	public void remove(){
		remove(getId());
	}

	private String getId() {
		return String.valueOf(Thread.currentThread().hashCode());
	}

	public void remove(final String id) {
		Ref<T> remove = threadLocal.remove(id);
		if( remove!=null )
			close(remove);
	}
	
	public void set(T t){
		remove();
		if (t!=null) {
			final String id = getId();
			threadLocal.put(id, new Ref<T>(t));
		}
	}
	
	
	public T get(Map<String, Object> params) {
		final String id = getId();
		Ref<T> ref = threadLocal.get(id);
		if (ref == null) {
			ref = new Ref<T>(factory.create(params));
//			System.out.println("get params Threadlocal id "+id+" "+System.currentTimeMillis());
			threadLocal.put(id, ref);
		}
		final long ti = System.currentTimeMillis();
		final T t = ref.get(ti);
		
		if(threshold>0)
			trigger.trigger(ti);
		
		return t;
	}

	public T get() {
		final String id = getId();
		Ref<T> ref = threadLocal.get(id);
		if (ref != null) {
			final long ti = System.currentTimeMillis();
			final T t = ref.get(ti);
			
			if(threshold>0)
				trigger.trigger(ti);
			
			return t;
		}
		return null;
	}
	
	public void evict() {
		final long currentTimeMillis = System.currentTimeMillis();
		Set<Entry<String, Ref<T>>> entrySet = threadLocal.entrySet();
		Collection<Entry<String, Ref<T>>> list = new DoublyLinkedList<Entry<String, Ref<T>>>();
		for (Entry<String, Ref<T>> e : entrySet) {
			Ref<T> value = e.getValue();
			if (value.isExpired(currentTimeMillis,threshold)) {
				list.add(e);
			}
		}
		for (Entry<String, Ref<T>> e : list) {
			threadLocal.remove(e.getKey());
			close(e.getValue());
		}
	}

	public void clearAll(){
		threadLocal.clear();
	}
	
	public Collection<T> getAll(){
		Collection<T> all = new DoublyLinkedList<T>();
		for( Ref<T> ref:threadLocal.values() ){
			if( ref.t != null )
				all.add(ref.t);
		}
		return all;
	}
	
	public void close() {
//		final Map<String, Ref<T>> threadLocalTmp = new Amap<String, Ref<T>>();
//		threadLocalTmp.putAll(threadLocal);
//		threadLocal.clear();
		for (Entry<String, Ref<T>> e : threadLocal.entrySet()) {
			Ref<T> value = e.getValue();
			e.setValue(null);
			close(value);
		}
//		threadLocalTmp.clear();
		try {
			Runtime.getRuntime().removeShutdownHook(hook);
		} catch (Throwable e1) {
			System.err.println(e1.toString());
		}
	}

	private void close(Ref<T> value) {
		if ( value !=null && value.t!=null && value.t instanceof AutoCloseable) {
			try {
				((AutoCloseable) value.t).close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * @author P
	 *
	 * @param <T>
	 */
	private static final class Ref<T> {
		private final T t;
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

		boolean isExpired(final long ctm,long thr) {
			return (ctm - time) > thr;
		}

	}

	/**
	 * @author P
	 *
	 */
	private static final class Trigger {
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
							if (tr != null) {
								tr.cancel(true);
								tr = null;
							}
							exe.shutdownNow();
						}
					}
				});
			}
		}

	}
}
