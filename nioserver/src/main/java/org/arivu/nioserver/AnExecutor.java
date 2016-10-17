/**
 * 
 */
package org.arivu.nioserver;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.pool.ConcurrentPool;
import org.arivu.pool.PoolFactory;

/**
 * @author P
 *
 */
public class AnExecutor implements ExecutorService {

	static class Runner implements Runnable{
		static final AtomicInteger counter = new AtomicInteger(0);
		
		final Queue<Task<?>> queue;
		boolean stop = false;
		public Runner(Queue<Task<?>> queue) {
			super();
			this.queue = queue;
		}


		@Override
		public void run() {
			Task<?> t = null;
			while((t=queue.poll())!=null){
				try {
					t.run();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
			
			try {
				Thread.sleep(1000);
				run();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	final Queue<Task<?>> queue = new DoublyLinkedList<Task<?>>();
	
	final ConcurrentPool<Runner> pool = new ConcurrentPool<Runner>(new PoolFactory<Runner>() {

		@Override
		public Runner create(Map<String, Object> params) {
			return new Runner(queue);
		}

		@Override
		public void close(Runner t) {
			if (t != null) {
				t.stop = true;
			}
		}

		@Override
		public void clear(Runner t) {
			
		}
	}, Runner.class);


	/**
	 * 
	 */
	public AnExecutor(int size) {
		pool.setMaxPoolSize(size);
		pool.setIdleTimeout(-1);
		pool.setLifeSpan(-1);
		pool.setMaxReuseCount(-1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Executor#execute(java.lang.Runnable)
	 */
	@Override
	public void execute(Runnable command) {
		queue.add(new Task<Void>(command, queue));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.ExecutorService#shutdown()
	 */
	@Override
	public void shutdown() {
		try {
			pool.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.ExecutorService#shutdownNow()
	 */
	@Override
	public List<Runnable> shutdownNow() {
		shutdown();
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.ExecutorService#isShutdown()
	 */
	@Override
	public boolean isShutdown() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.ExecutorService#isTerminated()
	 */
	@Override
	public boolean isTerminated() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.ExecutorService#awaitTermination(long,
	 * java.util.concurrent.TimeUnit)
	 */
	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.ExecutorService#submit(java.util.concurrent.
	 * Callable)
	 */
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable,
	 * java.lang.Object)
	 */
	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable)
	 */
	@Override
	public Future<?> submit(Runnable task) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection)
	 */
	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection,
	 * long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection)
	 */
	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection,
	 * long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		// TODO Auto-generated method stub
		return null;
	}

	static class Task<T> {
		final Runnable run;
		final Callable<T> call;
		final Queue<Task<?>> queue;
		T t = null;
		CountDownLatch l = null;
		

		Task(Runnable run, Queue<Task<?>> queue) {
			super();
			this.run = run;
			this.call = null;
			this.queue = queue;
		}

		void run() throws Exception{
			l = new CountDownLatch(1);
			try {
				if( call != null ){
					t = call.call();
				}else if( run != null ){
					run.run();
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}finally {
				l.countDown();
			}
			isDone = true;
			l = null;
		}
		
		volatile boolean isCancelled = false;
		volatile boolean isDone = false;
		final Future<T> future = new Future<T>() {

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				isCancelled = queue.remove(Task.this);
				return isCancelled;
			}

			@Override
			public boolean isCancelled() {
				return isCancelled;
			}

			@Override
			public boolean isDone() {
				return isDone;
			}

			@Override
			public T get() throws InterruptedException, ExecutionException {
				
				if(l!=null)
					l.countDown();
				
				return t;
			}

			@Override
			public T get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				return get();
			}
		};
	}

}
