/**
 * 
 */
package org.arivu.data;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;

import org.arivu.data.RowMapper.Identifier;
import org.arivu.datastructure.Amap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author P
 *
 */
//@Ignore
public class RowMapperTest {

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

	static class TestVo{
		@Column(name="date")
		Date date;
		
		@Column(name="cnt")
		Integer count;

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}

		public Integer getCount() {
			return count;
		}

		public void setCount(Integer count) {
			this.count = count;
		}
		
	}
	
	/**
	 * Test method for {@link org.arivu.data.RowMapper#set(java.util.Map, java.lang.Class, org.arivu.data.RowMapper.Identifier, java.lang.Class)}.
	 */
	@Test
	public void testSetMapOfStringObjectClassOfTIdentifierClassOfR() {
		Map<String,Object> row = new Amap<String,Object>();
		
		Date valueDate = new Date();
		row.put("date",valueDate);
		int valueInt = -1;
		row.put("cnt",valueInt);
		
		RowMapper<Column, TestVo> rm = new RowMapper<Column, TestVo>();
		
		Identifier identifier = new Identifier(){
			@Override
			public String getIdentity(Field field) {
				return field.getAnnotation(Column.class).name();
			}
			
		};
		try {
			TestVo set = rm.set(row, Column.class, identifier , TestVo.class);
			assertTrue(set.date==valueDate);
			assertTrue(set.count==valueInt);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Fail on rowmapper set");
		}
	}

	/**
	 * Test method for {@link org.arivu.data.RowMapper#set(java.util.Map, java.lang.Class, boolean, org.arivu.data.RowMapper.Identifier, java.lang.Class)}.
	 */
	@Test
	public void testSetMapOfStringObjectClassOfTBooleanIdentifierClassOfR() {
		Map<String,Object> row = new Amap<String,Object>();
		
		Date valueDate = new Date();
		row.put("date",valueDate);
		int valueInt = -1;
		row.put("cnt",valueInt);
		
		RowMapper<Column, TestVo> rm = new RowMapper<Column, TestVo>();
		
		Identifier identifier = new Identifier(){
			@Override
			public String getIdentity(Field field) {
				return field.getAnnotation(Column.class).name();
			}
			
		};
		try {
			TestVo set = rm.set(row, Column.class, true, identifier , TestVo.class);
			assertTrue(set.date==valueDate);
			assertTrue(set.count==valueInt);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Fail on rowmapper set");
		}
	}

	/**
	 * Test method for {@link org.arivu.data.RowMapper#get(java.lang.Object, java.lang.Class, org.arivu.data.RowMapper.Identifier)}.
	 */
	@Test
	public void testGetObjectClassOfTIdentifier() {
		
		Date valueDate = new Date();
		Integer valueInt = -1;
		TestVo vo = new TestVo();
		vo.setDate(valueDate);
		vo.setCount(valueInt);
		
		RowMapper<Column, TestVo> rm = new RowMapper<Column, TestVo>();
		
		Identifier identifier = new Identifier(){
			@Override
			public String getIdentity(Field field) {
				return field.getAnnotation(Column.class).name();
			}
			
		};
		try {
			Map<String,Object> row = rm.get(vo, Column.class, identifier);
			assertTrue(row.get("date")==valueDate);
			assertTrue(row.get("cnt")==valueInt);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Fail on rowmapper get");
		}
	
	}

	/**
	 * Test method for {@link org.arivu.data.RowMapper#get(java.lang.Object, java.lang.Class, boolean, org.arivu.data.RowMapper.Identifier)}.
	 */
	@Test
	public void testGetObjectClassOfTBooleanIdentifier() {

		Date valueDate = new Date();
		Integer valueInt = -1;
		TestVo vo = new TestVo();
		vo.setDate(valueDate);
		vo.setCount(valueInt);
		
		RowMapper<Column, TestVo> rm = new RowMapper<Column, TestVo>();
		
		Identifier identifier = new Identifier(){
			@Override
			public String getIdentity(Field field) {
				return field.getAnnotation(Column.class).name();
			}
			
		};
		try {
			Map<String,Object> row = rm.get(vo, Column.class, true, identifier);
			assertTrue(row.get("date")==valueDate);
			assertTrue(row.get("cnt")==valueInt);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Fail on rowmapper get");
		}
	}

}
