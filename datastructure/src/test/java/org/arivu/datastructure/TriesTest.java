package org.arivu.datastructure;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TriesTest {

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
	public void testAdd() {
		Tries tries = new Tries();
		tries.add("salary speaking");
		Set<String> words = tries.getWords("s");
		assertTrue("Failed on size!"+words.size(), words.size()==2);
	}

	@Test
	public void testGetWords() {
		Tries tries = new Tries("India being an English speaking country has one of the poorest living standards than say Australia or US which are "
				+ "first world countries with high living standards . India also has more English speaking population who are ready to undergo harsh "
				+ "treatment at the hands of any one who can send them to these countries for cheap dollar amount while the actual job might reward a "
				+ "person with higher salary in those native countries.");
//		tries.add("salary speaking");
		Set<String> words = tries.getWords("s");
		assertTrue("Failed on size!"+words.size(), words.size()==5);
		
		words = tries.getWords("sa");
		assertTrue("Failed on size!", words.size()==2);
		
		words = tries.getWords("t");
		for(String t:words)
			System.out.println(t);
	}

	@Test
	public void testSearchIndexes() {
		Tries tries = new Tries("India being an English speaking country has one of the poorest living standards than say Australia or US which are "
				+ "first world countries with high living standards . India also has more English speaking population who are ready to undergo harsh "
				+ "treatment at the hands of any one who can send them to these countries for cheap dollar amount while the actual job might reward a "
				+ "person with higher salary in those native countries.");
		
		int[] searchIndexes = tries.searchIndexes("speaking");
//		System.out.println("searchIndexes :: "+searchIndexes);
		assertTrue("Failed on searchIndexes", searchIndexes.length==2);
		assertTrue("Failed on searchIndexes"+searchIndexes[0], searchIndexes[0]==23);
		assertTrue("Failed on searchIndexes", searchIndexes[1]==194);
	}
	
	@Test
	public void testSearchIndexes2() {
		Tries tries = new Tries("India being an English speaking country has one of the poorest living standards than say Australia or US which are "
				+ "first world countries with high living standards . India also has more English speaking population who are ready to undergo harsh "
				+ "treatment at the hands of any one who can send them to these countries for cheap dollar amount while the actual job might reward a "
				+ "person with higher salary in those native countries.");

		int[] searchIndexes = tries.searchIndexes("th");
		assertTrue("Failed on searchIndexes", searchIndexes.length==7);
		
		assertTrue("Failed on searchIndexes"+searchIndexes[0], searchIndexes[0]==51);
		assertTrue("Failed on searchIndexes", searchIndexes[1]==80);
		assertTrue("Failed on searchIndexes"+searchIndexes[2], searchIndexes[2]==258);
		assertTrue("Failed on searchIndexes", searchIndexes[3]==292);
		assertTrue("Failed on searchIndexes"+searchIndexes[4], searchIndexes[4]==300);
		assertTrue("Failed on searchIndexes", searchIndexes[5]==346);
		assertTrue("Failed on searchIndexes"+searchIndexes[6], searchIndexes[6]==405);
	}
	
}
