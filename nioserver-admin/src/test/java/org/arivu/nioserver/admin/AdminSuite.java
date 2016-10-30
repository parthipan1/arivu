package org.arivu.nioserver.admin;

import org.arivu.nioserver.admin.client.AdminTest;
import com.google.gwt.junit.tools.GWTTestSuite;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AdminSuite extends GWTTestSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for Admin");
//		suite.addTestSuite(AdminTest.class);
		return suite;
	}
}
