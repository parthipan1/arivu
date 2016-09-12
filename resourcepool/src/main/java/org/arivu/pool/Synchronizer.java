/**
 * 
 */
package org.arivu.pool;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author P
 *
 */
final class Synchronizer {
	private static final Logger logger = LoggerFactory.getLogger(Synchronizer.class);
	private final LinkedReference<CountDownLatch> waitLatches;

	/**
	 * 
	 */
	public Synchronizer() {
		super();
		this.waitLatches = new LinkedReference<CountDownLatch>();
	}

	void youShallNotPass() {

		final CountDownLatch wait = new CountDownLatch(1);
		int i = waitLatches.size.get();
		while (!waitLatches.size.compareAndSet(i, i + 1)) {
		}
		waitLatches.add(new LinkedReference<CountDownLatch>(wait, waitLatches.size));
		try {
			logger.debug(Thread.currentThread().getName()+" Thread goes on Wait!");
			wait.await();
		} catch (InterruptedException e) {
			logger.error("Error on wait::", e);
		}
		logger.debug(Thread.currentThread().getName()+" Thread released from Wait!");
	}

	void youShallPass() {

		int i = waitLatches.size.get();
		if (i > 0) {
			while (!waitLatches.size.compareAndSet(i, i - 1)) {
			}
			LinkedReference<CountDownLatch> qwl = waitLatches.poll();
			if (qwl != null)
				qwl.t.countDown();
		}

	}

	void allShallPass() {
		int i = waitLatches.size.get();
		if (i > 0) {
			while (!waitLatches.size.compareAndSet(i, 0)) {
			}
			LinkedReference<CountDownLatch> ref = waitLatches.right;
			while (ref != null) {
				if (ref == waitLatches) {
					break;
				}
				ref.t.countDown();
				ref = ref.right;
			}
			waitLatches.clear();
		}
	}
}
