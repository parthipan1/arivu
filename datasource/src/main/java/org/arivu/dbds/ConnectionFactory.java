package org.arivu.dbds;

import java.sql.Connection;

public interface ConnectionFactory {
	Connection create(String user, String password, String driver, String url);
}
