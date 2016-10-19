package org.arivu.nioserver;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collection;

import org.arivu.datastructure.DoublyLinkedSet;
import org.arivu.utils.NullCheck;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PackageScannerTest {

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
	public void testGetPaths() throws ClassNotFoundException, IOException {
		System.setProperty("lightninglog.json", "./lightninglog.json");
		System.setProperty("arivu.nioserver.json", "./arivu.nioserver.json");
		System.setProperty("access.log", "./access.log");
		
		Collection<String> packageNames = new DoublyLinkedSet<String>();
		
		Collection<Route> paths = PackageScanner.getPaths(packageNames);
		assertTrue(NullCheck.isNullOrEmpty(paths));
		
		packageNames.add("org.arivu.nioserver");
		paths = PackageScanner.getPaths(packageNames);
		
		assertFalse(NullCheck.isNullOrEmpty(paths));
	}

	@Test
	public void testGetClassesForPackage() throws ClassNotFoundException {
		
		System.setProperty("lightninglog.json", "./lightninglog.json");
		System.setProperty("arivu.nioserver.json", "./arivu.nioserver.json");
		System.setProperty("access.log", "./access.log");
		
		Collection<Class<?>> classesForPackage = PackageScanner.getClassesForPackage("org.arivu.nioserver");
		assertFalse(NullCheck.isNullOrEmpty(classesForPackage));
//		for( Class<?> c:classesForPackage )
//			System.out.println(c.getName());
	}

	@Test
	public void testAddMethod() {
		Collection<Route> reqPaths = new DoublyLinkedSet<Route>();
		
		assertTrue(NullCheck.isNullOrEmpty(reqPaths));
		PackageScanner.addMethod(reqPaths, Connection.class);
		assertTrue(NullCheck.isNullOrEmpty(reqPaths));
		
		PackageScanner.addMethod(reqPaths, Server.class);
		assertFalse(NullCheck.isNullOrEmpty(reqPaths));
		
	}

}
