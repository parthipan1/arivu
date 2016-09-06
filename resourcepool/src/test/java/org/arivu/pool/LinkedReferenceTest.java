package org.arivu.pool;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class LinkedReferenceTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

//	private static final ReentrantLock writeLock = new ReentrantLock(true);
	
	@Test
	public void testClear() {
		final String headStr = "head";
		final String oneStr = "1";
		final String twoStr = "2";
		final String threeStr = "3";
		
		final LinkedReference<String> head = new LinkedReference<String>(headStr,new AtomicInteger(0));
		head.add(new LinkedReference<String>(oneStr,head.size));
		head.add(new LinkedReference<String>(twoStr,head.size));
		head.add(new LinkedReference<String>(threeStr,head.size));
		
		head.clear();
		
		assertTrue("Assert left failed!",head.left==head);
		assertTrue("Assert left failed!",head.right==head);
	}

//	@Test
//	public void testAddRight() {
//		final String headStr = "head";
//		final String oneStr = "1";
//		final String twoStr = "2";
//		final String threeStr = "3";
//		
//		final LinkedReference<String> head = new LinkedReference<String>(headStr);
//		head.addRight(new LinkedReference<String>(oneStr));
//		head.addRight(new LinkedReference<String>(twoStr));
//		head.addRight(new LinkedReference<String>(threeStr));
//		
//		StringBuffer buf = new StringBuffer();
//		
//		LinkedReference<String> ref = head;
//		do{
//			buf.append(ref.t);
//			ref = ref.left;
//		}while(ref!=head);
//		
//		String string = headStr+oneStr+twoStr+threeStr;
//		assertTrue("Failed in addRight exp::%"+string+"% out::%"+buf.toString()+"%", string.equals(buf.toString()) );
//	}

	@Test
	public void testAddLeft() {
		final String headStr = "head";
		final String oneStr = "1";
		final String twoStr = "2";
		final String threeStr = "3";
		
		final LinkedReference<String> head = new LinkedReference<String>(headStr,new AtomicInteger(0));
		head.add(new LinkedReference<String>(oneStr,head.size));
		head.add(new LinkedReference<String>(twoStr,head.size));
		head.add(new LinkedReference<String>(threeStr,head.size));
		
		StringBuffer buf = new StringBuffer();
		
		LinkedReference<String> ref = head;
		do{
			buf.append(ref.t);
			ref = ref.left;
		}while(ref!=head);
		
		String string = headStr+threeStr+twoStr+oneStr;
		assertTrue("Failed in addRight exp::%"+string+"% out::%"+buf.toString()+"%", string.equals(buf.toString()) );
	}

	@Test
	public void testSearch() {
		final String headStr = "head";
		final String oneStr = "1";
		final String twoStr = "2";
		final String threeStr = "3";
		
		final LinkedReference<String> head = new LinkedReference<String>(headStr,new AtomicInteger(0));
		LinkedReference<String> oneRef = new LinkedReference<String>(oneStr,head.size);
		head.add(oneRef);
		LinkedReference<String> twoRef = new LinkedReference<String>(twoStr,head.size);
		head.add(twoRef);
		LinkedReference<String> threeRef = new LinkedReference<String>(threeStr,head.size);
		head.add(threeRef);
		
		assertTrue("Failed on Search!", head.search(oneStr)==oneRef);
		assertTrue("Failed on Search!", head.search(twoStr)==twoRef);
		assertTrue("Failed on Search!", head.search(threeStr)==threeRef);
	}

	@Test
	public void testSize() {
		final String headStr = "head";
		final String oneStr = "1";
		final String twoStr = "2";
		final String threeStr = "3";
		
		final LinkedReference<String> head = new LinkedReference<String>(headStr,new AtomicInteger(0));
		LinkedReference<String> oneRef = new LinkedReference<String>(oneStr,head.size);
		head.add(oneRef);
		head.size.incrementAndGet();
		LinkedReference<String> twoRef = new LinkedReference<String>(twoStr,head.size);
		head.add(twoRef);
		head.size.incrementAndGet();
		LinkedReference<String> threeRef = new LinkedReference<String>(threeStr,head.size);
		head.add(threeRef);
		head.size.incrementAndGet();
		
		assertTrue("Failed on Search!", head.search(oneStr)==oneRef);
		assertTrue("Failed on Search!", head.search(twoStr)==twoRef);
		assertTrue("Failed on Search!", head.search(threeStr)==threeRef);
		
		assertTrue("Failed on Size! "+head.size(), head.size()==3);
		
	}
//	@Test
//	public void testSearchAvailable() {
//		final String headStr = "head";
//		final String oneStr = "1";
//		final String twoStr = "2";
//		final String threeStr = "3";
//		
//		final LinkedReference<String> head = new LinkedReference<String>(headStr);
//		LinkedReference<String> oneRef = new LinkedReference<String>(oneStr);
//		head.add(oneRef);
//		LinkedReference<String> twoRef = new LinkedReference<String>(twoStr);
//		head.add(twoRef);
//		LinkedReference<String> threeRef = new LinkedReference<String>(threeStr);
//		head.add(threeRef);
//		
//		assertTrue("Failed on searchAvailable!", head.searchAvailable()==null);
//		oneRef.available = true;
//		assertTrue("Failed on searchAvailable!", head.searchAvailable()==oneRef);
//		oneRef.available = false;
//		twoRef.available = true;
//		assertTrue("Failed on searchAvailable!", head.searchAvailable()==twoRef);
//		twoRef.available = false;
//		threeRef.available = true;
//		assertTrue("Failed on searchAvailable!", head.searchAvailable()==threeRef);
//		threeRef.available = false;
//		assertTrue("Failed on searchAvailable!", head.searchAvailable()==null);
//	}

//	@Test
//	public void testGetAllOrphaned() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testRemoveT() {
//		fail("Not yet implemented");
//	}

	@Test
	public void testRemove() {

		final String headStr = "head";
		final String oneStr = "1";
		final String twoStr = "2";
		final String threeStr = "3";
		
		final LinkedReference<String> head = new LinkedReference<String>(headStr,new AtomicInteger(0));
		LinkedReference<String> oneRef = new LinkedReference<String>(oneStr,head.size);
		head.add(oneRef);
		LinkedReference<String> twoRef = new LinkedReference<String>(twoStr,head.size);
		head.add(twoRef);
		LinkedReference<String> threeRef = new LinkedReference<String>(threeStr,head.size);
		head.add(threeRef);
		
		StringBuffer buf = new StringBuffer();
		
		LinkedReference<String> ref = head;
		do{
			buf.append(ref.t);
			ref = ref.left;
		}while(ref!=head);
		
		String string = headStr+threeStr+twoStr+oneStr;
		assertTrue("Failed in set exp::%"+string+"% out::%"+buf.toString()+"%", string.equals(buf.toString()) );
		
		oneRef.remove();
		buf = new StringBuffer();
		ref = head;
		do{
			buf.append(ref.t);
			ref = ref.left;
		}while(ref!=head);
		string = headStr+threeStr+twoStr;

		assertTrue("Failed in set exp::%"+string+"% out::%"+buf.toString()+"%", string.equals(buf.toString()) );
		
		threeRef.remove();
		buf = new StringBuffer();
		ref = head;
		do{
			buf.append(ref.t);
			ref = ref.left;
		}while(ref!=head);
		string = headStr+twoStr;

		assertTrue("Failed in set exp::%"+string+"% out::%"+buf.toString()+"%", string.equals(buf.toString()) );
		
	}

}
