package org.arivu.datastructure;

import static org.junit.Assert.assertTrue;

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


}
