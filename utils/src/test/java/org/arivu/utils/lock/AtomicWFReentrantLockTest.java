/**
 * 
 */
package org.arivu.utils.lock;

import static org.junit.Assert.*;

import java.util.concurrent.locks.Lock;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author P
 *
 */
public class AtomicWFReentrantLockTest {

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
	 * Test method for {@link org.arivu.utils.lock.AtomicWFReentrantLock#lock()}.
	 */
	@Test
	public void testLock() {
		Lock lock = new AtomicWFReentrantLock();
		lock.lock();
		lock.lock();
		assertTrue("Failed in reentrance lock!", true);
		lock.unlock();
		lock.unlock();
	}


}
