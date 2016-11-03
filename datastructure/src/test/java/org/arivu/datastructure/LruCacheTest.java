package org.arivu.datastructure;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.arivu.datastructure.LruCache.CacheStrategy;
import org.arivu.datastructure.LruCache.Tracker;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author P
 *
 */
public class LruCacheTest {

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

	@Test
	public void testPut_Count() {
		LruCache<String, String> cache = new LruCache<String, String>(LruCache.CacheStrategy.COUNT_MOST, 2);
		assertTrue("Failed in size", cache.size()==0);
		
		assertTrue("Failed in remove", cache.remove()==null);
		
		String one = "1";
		String two = "2";
		String three = "3";
		
		cache.put(one, one);
		cache.put(two, two);
		assertTrue("Failed in size", cache.size()==2);
		
		String string = cache.get(one);
		assertTrue("Failed in get1 EXP :: "+one+" GOT :: "+string, string==one);
		assertTrue("Failed in get2", cache.get(two)==two);
		assertTrue("Failed in get3", cache.get(one)==one);
		
		assertTrue("Failed in size", cache.size()==2);
		
		cache.put(three, three);
		
		assertTrue("Failed in size", cache.size()==2);
		
		assertTrue("Failed in get1", cache.get(one)==one);
		assertTrue("Failed in get2", cache.get(two)==null);
		assertTrue("Failed in get3", cache.get(three)==three);
		
	}

	@Test
	public void testPut_Time() {
		LruCache<String, String> cache = new LruCache<String, String>(LruCache.CacheStrategy.TIME_MOST_RECENT, 2);
		assertTrue("Failed in size", cache.size()==0);
		
		assertTrue("Failed in remove", cache.remove()==null);
		
		String one = "1";
		String two = "2";
		String three = "3";
		
		cache.put(one, one);
		cache.put(two, two);
		assertTrue("Failed in size", cache.size()==2);
		
		String string = cache.get(one);
		assertTrue("Failed in get1 EXP :: "+one+" GOT :: "+string, string==one);
		assertTrue("Failed in get2", cache.get(two)==two);
		assertTrue("Failed in get3", cache.get(one)==one);
		
		assertTrue("Failed in size", cache.size()==2);
		
		cache.put(three, three);
		
		assertTrue("Failed in size", cache.size()==2);
		
		assertTrue("Failed in get1", cache.get(one)==one);
		assertTrue("Failed in get2", cache.get(two)==null);
		assertTrue("Failed in get3", cache.get(three)==three);
		
	}
	
	@Test
	public void testClear() {
		LruCache<String, String> cache = new LruCache<String, String>(LruCache.CacheStrategy.COUNT_MOST, 2);
		assertTrue("Failed in size", cache.size()==0);
		
		String one = "1";
		String two = "2";
		
		cache.put(one, one);
		cache.put(two, two);
		assertTrue("Failed in size", cache.size()==2);
		cache.clear();
		assertTrue("Failed in size", cache.size()==0);
	}

	@Test
	public void testRemove() {
		LruCache<String, String> cache = new LruCache<String, String>(LruCache.CacheStrategy.COUNT_MOST, 2);
		assertTrue("Failed in size", cache.size()==0);
		
		String one = "1";
		String two = "2";
		
		cache.put(one, one);
		cache.put(two, two);
		assertTrue("Failed in size", cache.size()==2);
		assertFalse("Failed in size", cache.isEmpty());
		
		cache.remove(one);
		assertTrue("Failed in size", cache.size()==1);
		cache.remove(two);
		assertTrue("Failed in size", cache.size()==0);
		assertTrue("Failed in size", cache.isEmpty());
		cache.remove(null);
		
	}

	@Test
	public void testcacheStrategy() {
		assertTrue(LruCache.CacheStrategy.COUNT_LEAST.other()==LruCache.CacheStrategy.COUNT_MOST);
		assertTrue(LruCache.CacheStrategy.COUNT_MOST.other()==LruCache.CacheStrategy.COUNT_LEAST);
		
		assertTrue(LruCache.CacheStrategy.TIME_LEAST_RECENT.other()==LruCache.CacheStrategy.TIME_MOST_RECENT);
		assertTrue(LruCache.CacheStrategy.TIME_MOST_RECENT.other()==LruCache.CacheStrategy.TIME_LEAST_RECENT);
		
		assertTrue(LruCache.CacheStrategy.valueOf("COUNT_LEAST")==LruCache.CacheStrategy.COUNT_LEAST);
		assertTrue(LruCache.CacheStrategy.valueOf("COUNT_MOST")==LruCache.CacheStrategy.COUNT_MOST);
		assertTrue(LruCache.CacheStrategy.valueOf("TIME_LEAST_RECENT")==LruCache.CacheStrategy.TIME_LEAST_RECENT);
		assertTrue(LruCache.CacheStrategy.valueOf("TIME_MOST_RECENT")==LruCache.CacheStrategy.TIME_MOST_RECENT);
		assertTrue(LruCache.CacheStrategy.values().length==4);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testConstructors() {
		new LruCache<String, String>(null, 0);
	}
	
	@Test
	public void testConstructors2() {
		LruCache<String, String> lruCache = new LruCache<String, String>(null, 1);
		assertTrue(lruCache.cacheStrategy==CacheStrategy.TIME_MOST_RECENT);
	}
	
	@Test
	public void testCompare1() {
		CacheStrategy countMost = CacheStrategy.COUNT_MOST;
		Tracker<String> t1 = countMost.create("1");
		Tracker<String> t2 = countMost.create("2");
		
//		System.out.println(" t1 "+t1.tracker+" t2 "+t2.tracker+" 0");
		
		assertFalse(countMost.compare(t1, t2));
		assertTrue(countMost.other().compare(t1, t2));
		
//		System.out.println(" t1 "+t1.tracker+" t2 "+t2.tracker+" 1");
		countMost.access(t2);
//		System.out.println(" t1 "+t1.tracker+" t2 "+t2.tracker+" 2");
		
		assertTrue(countMost.compare(t1, t2));
		assertFalse(countMost.other().compare(t1, t2));
		
	}
	
	@Test
	public void testCompare2() throws InterruptedException {
		CacheStrategy timeMost = CacheStrategy.TIME_MOST_RECENT;
		Tracker<String> t1 = timeMost.create("1");
		Thread.sleep(10);
		Tracker<String> t2 = timeMost.create("2");
		
//		System.out.println(" t1 "+t1.tracker+" t2 "+t2.tracker+" 0");
		
		assertTrue(timeMost.compare(t1, t2));
		assertFalse(timeMost.other().compare(t1, t2));

//		System.out.println(" t1 "+t1.tracker+" t2 "+t2.tracker+" 1");
		Thread.sleep(10);
		timeMost.other().access(t1);
//		System.out.println(" t1 "+t1.tracker+" t2 "+t2.tracker+" 2");
		
		assertFalse(timeMost.compare(t1, t2));
		assertTrue(timeMost.other().compare(t1, t2));
		
//		assertFalse(timeMost.other().compare(t2, t1));
		
	}
}
