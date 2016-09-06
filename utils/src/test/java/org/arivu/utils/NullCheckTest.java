package org.arivu.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class NullCheckTest {

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
	public void testIsNullOrEmptyMapOfQQ() {
		Map<?,?> map = null;
		assertTrue(NullCheck.isNullOrEmpty(map));
		map = new HashMap<String,String>();
		assertTrue(NullCheck.isNullOrEmpty(map));
		Map<String,String> map1 = new HashMap<String,String>();
		map1.put("1", "1");
		assertFalse(NullCheck.isNullOrEmpty(map1));
	}

	@Test
	public void testIsNullOrEmptyCharSequence() {
		String map = null;
		assertTrue(NullCheck.isNullOrEmpty(map));
		map = "";
		assertTrue(NullCheck.isNullOrEmpty(map));
		map = "qwe";
		assertFalse(NullCheck.isNullOrEmpty(map));
	}

	@Test
	public void testIsNullOrEmptyCollectionOfQ() {
		Collection<String> list = null;
		assertTrue(NullCheck.isNullOrEmpty(list));
		list = new ArrayList<String>();
		assertTrue(NullCheck.isNullOrEmpty(list));
		list.add("1");
		assertFalse(NullCheck.isNullOrEmpty(list));
	}

	@Test
	public void testIsNullOrEmptyTArray() {
		String[] list = null;
		assertTrue(NullCheck.isNullOrEmpty(list));
		list = new String[]{};
		assertTrue(NullCheck.isNullOrEmpty(list));
		list = new String[]{"1"};
		assertFalse(NullCheck.isNullOrEmpty(list));
	}

}
