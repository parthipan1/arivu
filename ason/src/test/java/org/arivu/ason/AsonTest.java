/**
 * 
 */
package org.arivu.ason;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.Map;

import javax.script.ScriptException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author P
 *
 */
public class AsonTest {

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
	 * Test method for {@link org.arivu.ason.Ason#fromJson(java.lang.String)}.
	 * @throws ScriptException 
	 */
	@Test
	public void testFromJson_Case1() throws ScriptException {
		Ason a = new Ason();
		Map<String, Object> fromJson = a.fromJson("{\"test\":\"test\"}");
		assertTrue("Failed in size!", fromJson.size()==1);
		assertTrue("Failed in contains!", fromJson.containsKey("test"));
		assertTrue("Failed in value!", fromJson.get("test").equals("test"));
		assertTrue("Failed in datatype!", fromJson.get("test").getClass() == String.class);
	}
	
	/**
	 * Test method for {@link org.arivu.ason.Ason#fromJson(java.lang.String)}.
	 * @throws ScriptException 
	 */
	@Test
	public void testFromJson_Case2() throws ScriptException {
		Ason a = new Ason();
		Map<String, Object> fromJson = a.fromJson("{\"test\": 0 }");
		assertTrue("Failed in size!", fromJson.size()==1);
		assertTrue("Failed in contains!", fromJson.containsKey("test"));
		assertTrue("Failed in datatype!", fromJson.get("test").getClass() == Integer.class);
		assertTrue("Failed in value!", ((Integer)fromJson.get("test")) == 0);
	}

	/**
	 * Test method for {@link org.arivu.ason.Ason#fromJson(java.lang.String)}.
	 * @throws ScriptException 
	 */
	@Test
	public void testFromJson_Case3() throws ScriptException {
		Ason a = new Ason();
		Map<String, Object> fromJson = a.fromJson("{\"test\": 0.0 }");
		assertTrue("Failed in size!", fromJson.size()==1);
		assertTrue("Failed in contains!", fromJson.containsKey("test"));
		assertTrue("Failed in datatype! GOT :: "+fromJson.get("test").getClass(), fromJson.get("test").getClass() == Double.class);
		assertTrue("Failed in value!", ((Double)fromJson.get("test")) == 0d);
	}

	/**
	 * Test method for {@link org.arivu.ason.Ason#fromJson(java.lang.String)}.
	 * @throws ScriptException 
	 */
	@Test
	public void testFromJson_Case4() throws ScriptException {
		Ason a = new Ason();
		Map<String, Object> fromJson = a.fromJson("{\"test\": { \"testin\" : \"testin\" } }");
		assertTrue("Failed in size!", fromJson.size()==1);
		assertTrue("Failed in contains!", fromJson.containsKey("test"));
		assertTrue("Failed in datatype! GOT :: "+fromJson.get("test").getClass(), fromJson.get("test") instanceof Map );
//		assertTrue("Failed in value!", ((Double)fromJson.get("test")) == 0d);
	}
	
	/**
	 * Test method for {@link org.arivu.ason.Ason#fromJson(java.lang.String)}.
	 * @throws ScriptException 
	 */
	@Test
	@Ignore
	public void testFromJson_Case5() throws ScriptException {
		Ason a = new Ason();
		Map<String, Object> fromJson = a.fromJson("{\"test\": \"2012-04-23T18:25:43.511Z\" }");
		assertTrue("Failed in size!", fromJson.size()==1);
		assertTrue("Failed in contains!", fromJson.containsKey("test"));
		assertTrue("Failed in datatype! GOT :: "+fromJson.get("test").getClass(), fromJson.get("test").getClass() == Date.class);
//		assertTrue("Failed in value!", ((Double)fromJson.get("test")) == 0d);
	}
}
