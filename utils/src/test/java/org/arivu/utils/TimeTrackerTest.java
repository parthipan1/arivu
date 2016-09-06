package org.arivu.utils;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TimeTrackerTest {

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
	public void testNextTrack() throws InterruptedException {
		TimeTracker tt = TimeTracker.start("test");
		assertTrue(tt.tracks.size()==0);
		Thread.sleep(100);
		tt.nextTrack("1");
		assertTrue(tt.tracks.size()==1);
		assertTrue(((Number)tt.tracks.get("1")).intValue()>=100);
		Thread.sleep(100);
		tt.totalTrack("2");
		assertTrue(((Number)tt.tracks.get("2")).intValue()>=200);
		Thread.sleep(100);
		tt.stop();
//		System.out.println(tt.toString());
		assertTrue(((Number)tt.tracks.get("total")).intValue()>=300);
	}
	
}
