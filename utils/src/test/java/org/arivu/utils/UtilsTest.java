package org.arivu.utils;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class UtilsTest {

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
	public void testUnmodifiableMap() {
		Map<String,Object> map = new HashMap<String,Object>();
		assertTrue(Utils.unmodifiableMap(null)==null);
		assertTrue(Utils.unmodifiableMap(map)==null);

		map.put("test", "test");
		assertTrue(Utils.unmodifiableMap(map)!=null);
	}

	@Test
	public void testUnmodifiableCollection() {
		Collection<Object> collection = new HashSet<Object>();
		assertTrue(Utils.unmodifiableCollection(null)==null);
		assertTrue(Utils.unmodifiableCollection(collection)==null);

		collection.add("test");
		assertTrue(Utils.unmodifiableCollection(collection)!=null);
	}

	@Test
	public void testUnmodifiableList() {
		List<Object> list = new ArrayList<Object>();
		assertTrue(Utils.unmodifiableList(null)==null);
		assertTrue(Utils.unmodifiableList(list)==null);

		list.add("test");
		assertTrue(Utils.unmodifiableList(list)!=null);
	}

	@Test
	public void testReplaceAll() {
		assertTrue(Utils.replaceAll("test", "t", "1").equals("1es1"));
		assertTrue(Utils.replaceAll("12345", "t", "1").equals("12345"));
	}

	@Test
	public void testToStringMapOfKV() {
		Map<String,Object> map = new HashMap<String,Object>();
		assertTrue("{}".equals(Utils.toString(map)));
		assertTrue("null".equals(Utils.toString((Map<String,String>)null)));
		
		map.put("key", "value");
		assertTrue("{key=value}".equals(Utils.toString(map)));
		
		List<Object> v1 = new ArrayList<>();
		v1.add("value1");
		map.put("key1", v1);
		assertTrue("failed got :: "+Utils.toString(map),"{key1=[value1],key=value}".equals(Utils.toString(map)));
		
		Map<String,Object> v2 = new HashMap<String,Object>();
		v2.put("ikey", "ivalue");
		map.put("key2", v2);
		
		assertTrue("failed got :: "+Utils.toString(map),"{key1=[value1],key2={ikey=ivalue},key=value}".equals(Utils.toString(map)));
	}

	@Test
	public void testToStringCollectionOfQ() {
		Collection<Object> list = new ArrayList<Object>();
		assertTrue("[]".equals(Utils.toString(list)));
		assertTrue("null".equals(Utils.toString((Collection<String>)null)));
		
		list.add("value");
		assertTrue("[value]".equals(Utils.toString(list)));
		
		List<Object> v1 = new ArrayList<>();
		v1.add("value1");
		list.add(v1);
		assertTrue("[value,[value1]]".equals(Utils.toString(list)));
		
		Map<String,Object> v2 = new HashMap<String,Object>();
		v2.put("ikey", "ivalue");
		list.add(v2);
		assertTrue("[value,[value1],{ikey=ivalue}]".equals(Utils.toString(list)));
	}

}
