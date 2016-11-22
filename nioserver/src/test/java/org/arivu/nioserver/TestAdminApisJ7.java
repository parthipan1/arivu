package org.arivu.nioserver;

import java.io.IOException;

import javax.script.ScriptException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestAdminApisJ7 {
	
	static TestAdminApis instance = new TestAdminApis();
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestHttpMethodsMultiThreaded.useJ7Nio = "true";
		TestAdminApis.init("true");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		TestAdminApis.tearDownAfterClass();
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRoutes() throws ScriptException {
		instance.testRoutes();
	}
	
	@Test
	public void testAppsGet() throws ScriptException {
		instance.testAppsGet();
	}

	@Test
	public void testIconGet() throws ScriptException, IOException {
		instance.testIconGet();
	}
	
	@Test
	public void testDeployAndUnDeploy() throws IOException {
		instance.testDeployAndUnDeploy();
	}
}


