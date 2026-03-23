package util;


import java.sql.*;

/**
 * Singleton database connection utility for UMS.
 * Connects to SQL Server UMS_DB.
 */
public class DBConnection {

    private static final String URL      = "jdbc:sqlserver://localhost:1433;databaseName=UMS_DB;encrypt=false";
    private static final String USER     = "sa";
    private static final String PASSWORD = "YourPasswordHere";

    private static Connection connection = null;

    private DBConnection() {}

    /**
     * Returns a singleton Connection instance.
     * Re-opens if closed.
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQL Server JDBC driver not found.", e);
            }
        }
        return connection;
    }

    /** Closes the shared connection (call on app shutdown). */
    public static void closeConnection() {
        if (connection != null) {
            try { connection.close(); }
            catch (SQLException ignored) {}
            connection = null;
        }
    }
}
