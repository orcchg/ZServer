package com.orcchg.zserver.sample;

import java.sql.*;
import java.util.Properties;

public class DbQueryTest {
    private static final String URL_DATABASE_DVDRENTAL = "jdbc:postgresql:dvdrental";

    void testConnection() {
        System.out.println("Enter testConnection()");

        try {
            Class.forName("org.postgresql.Driver");  // load PostgrSQL driver and register it to JDBC
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("class found");

        Properties properties = new Properties();
        properties.setProperty("user", "postgres");
        properties.setProperty("password", "111222qqq");

        StringBuilder builder = new StringBuilder("Data: \n");
        Statement statement = null;
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(URL_DATABASE_DVDRENTAL, properties);
            System.out.println("database connected");
            String query = "SELECT first_name,last_name FROM customer;";
            statement = connection.createStatement();
            ResultSet result = statement.executeQuery(query);
            System.out.println("statement executed");
            while (result.next()) {
                builder.append(result.getString("first_name")).append(", ").append(result.getString("last_name")).append("\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) { statement.close(); }
                if (connection != null) { connection.close(); }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        System.out.println(builder.toString());
        System.out.println("Exit testConnection()");
    }

    public static void main(String[] args) {
        DbQueryTest test = new DbQueryTest();
        test.testConnection();
    }
}
