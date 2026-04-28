package util;

import java.sql.*;

/**
 * Database connection factory for UMS.
 * Connects to SQL Server UMS_DB.
 *
 * Design note: each call to getConnection() returns a *new* Connection.
 * Callers are responsible for closing it (try-with-resources).
 * This avoids the shared-singleton race condition that arises when
 * multiple SwingWorker threads call a DAO concurrently.
 */
public class DBConnection {

	private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=UMS_DB;encrypt=false";
    private static final String USER     = "sa";
    private static final String PASSWORD = "Admin@1234";

    private DBConnection() {}

    /**
     * Opens and returns a new JDBC Connection.
     * Always wrap the caller in try-with-resources:
     *
     *   try (Connection con = DBConnection.getConnection()) { ... }
     *
     * @throws SQLException if the driver is missing or the server is unreachable
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQL Server JDBC driver not found.", e);
        }
    }
}