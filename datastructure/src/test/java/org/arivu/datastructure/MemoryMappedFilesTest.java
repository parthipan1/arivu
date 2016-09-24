/**
 * 
 */
package org.arivu.datastructure;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author P
 *
 */
public class MemoryMappedFilesTest {

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
	 * Test method for {@link org.arivu.datastructure.MemoryMappedFiles#add(java.lang.String)}.
	 * @throws IOException 
	 */
	@Test
	public void testAdd() throws IOException {
		MemoryMappedFiles mmf = new MemoryMappedFiles();
		
		String one = "1.txt";
		String two = "2.txt";
		
		assertTrue(mmf.size()==0);
		
		mmf.add(one);
		mmf.add(two);
		
		assertTrue(mmf.size()==2);
		
		assertTrue("1".equals(mmf.get(one)));
		assertTrue("2".equals(mmf.get(two)));
		
		mmf.remove(one);
		
		assertTrue(mmf.get(one)==null);
		
		mmf.clear();
		
		assertTrue(mmf.get(two)==null);
	}

//	/**
//	 * Test method for {@link org.arivu.datastructure.MemoryMappedFiles#get(java.lang.String)}.
//	 */
//	@Test
//	public void testGet() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link org.arivu.datastructure.MemoryMappedFiles#remove(java.lang.String)}.
//	 */
//	@Test
//	public void testRemove() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link org.arivu.datastructure.MemoryMappedFiles#clear()}.
//	 */
//	@Test
//	public void testClear() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link org.arivu.datastructure.MemoryMappedFiles#size()}.
//	 */
//	@Test
//	public void testSize() {
//		fail("Not yet implemented");
//	}

}
