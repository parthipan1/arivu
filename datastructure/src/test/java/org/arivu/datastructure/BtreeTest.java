/**
 * 
 */
package org.arivu.datastructure;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.locks.Lock;

import org.arivu.utils.lock.AtomicWFReentrantLock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author P
 *
 */
public class BtreeTest {

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

	@Test
	public void testDirectionGet() {
		assertTrue(Direction.left.get(null)==null);
		assertTrue(Direction.right.get(null)==null);
		Lock lock = new AtomicWFReentrantLock();
		LinkedReference l = new LinkedReference(CompareStrategy.EQUALS, lock);
		LinkedReference ll = new LinkedReference(CompareStrategy.EQUALS, lock);
		LinkedReference lr = new LinkedReference(CompareStrategy.EQUALS, lock);
		
		System.out.println(" l "+l);
		System.out.println(" ll "+ll);
		System.out.println(" lr "+lr);
		
		ll.obj = "l";
		lr.obj = "r";
		
		assertFalse(l.add(null));
		assertTrue(l.isEmpty());
		assertTrue(Direction.left.remove(l)==null);
		assertTrue(Direction.right.remove(l)==null);
		
		Direction.left.set(null, null);
		Direction.left.set(l, null);
		Direction.left.set(l, ll);
		Direction.right.set(null, null);
		Direction.right.set(l, null);
		Direction.right.set(l, lr);
		
		Direction.right.set(lr, ll);
		
		assertFalse(l.isEmpty());
		
		assertTrue(Direction.left.get(l)==ll);
		assertTrue(Direction.right.get(l)==lr);
		
		assertTrue(Direction.left.getOther()==Direction.right);
		assertTrue(Direction.right.getOther()==Direction.left);

		assertTrue(Direction.left.remove(null)==null);
		assertTrue(Direction.left.remove(l)==ll);
		
	}

	@Test
	public void testSearch() {
		Lock lock = new AtomicWFReentrantLock();
		LinkedReference l = new LinkedReference(CompareStrategy.EQUALS, lock);
		l.add("r");
		l.add("l");
		
		assertTrue(l.search(null)==null);
		assertTrue(l.search("1")==null);
		assertTrue(l.search("l")==l.left);
		assertTrue(l.search("r")==l.right);
	}
}
