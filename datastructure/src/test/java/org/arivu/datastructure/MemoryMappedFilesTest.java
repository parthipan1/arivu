/**
 * 
 */
package org.arivu.datastructure;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
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
	private static final String TEST_RESOURCE_BASE = "."+File.separator+"src"+File.separator+"test"+File.separator+"resources"+File.separator;

	static final String TEST_1 = TEST_RESOURCE_BASE+"1.txt";
	static final String TEST_2 = TEST_RESOURCE_BASE+"2.txt";
	static final String TEST_3 = TEST_RESOURCE_BASE+"3.txt";
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
		
		String one = TEST_1;
		String two = TEST_2;
		
		assertTrue(mmf.size()==0);
		
		mmf.add(one);
		mmf.add(two);
		mmf.add(one);
		
		assertTrue(mmf.size()==2);
		
		assertTrue("Failed on add got :: "+mmf.get(one),"1".equals(mmf.get(one)));
		assertTrue("2".equals(mmf.get(two)));
		
		mmf.remove(one);
		
		assertTrue(mmf.get(one)==null);
		
		mmf.clear();
		
		assertTrue(mmf.get(two)==null);
		
		try {
			mmf.add(TEST_3);
			fail("Failed to open "+TEST_3+"!");
		} catch (IOException e) {
			assertTrue(e!=null);
		}
		
		mmf.closeFile(null);
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