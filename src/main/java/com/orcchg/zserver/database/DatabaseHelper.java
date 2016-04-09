package com.orcchg.zserver.database;

import java.sql.*;
import java.util.Properties;

public class DatabaseHelper {
    private static final String URL_DATABASE_DVDRENTAL = "jdbc:postgresql:dvdrental";

    private Properties mProperties;

    public DatabaseHelper() {
        try {
            Class.forName("org.postgresql.Driver");  // load PostgrSQL driver and register it to JDBC
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        mProperties = new Properties();
        mProperties.setProperty("user", "postgres");
        mProperties.setProperty("password", "111222qqq");
    }

    /* API */
    // --------------------------------------------------------------------------------------------
    public String testQuery() throws SQLException {
        StringBuilder builder = new StringBuilder("Data: \n");
        Statement statement = null;
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(URL_DATABASE_DVDRENTAL, mProperties);
            System.out.println("database connected");
            String query = "SELECT first_name,last_name FROM customer;";
            statement = connection.createStatement();
            ResultSet result = statement.executeQuery(query);
            while (result.next()) {
                builder.append(result.getString("first_name")).append(", ").append(result.getString("last_name")).append("\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (statement != null) { statement.close(); }
            if (connection != null) { connection.close(); }
        }
        return builder.toString();
    }
}
