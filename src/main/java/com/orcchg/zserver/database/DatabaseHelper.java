package com.orcchg.zserver.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
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
    /**
     * GET /test/
     */
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
            throw e;
        } finally {
            if (statement != null) { statement.close(); }
            if (connection != null) { connection.close(); }
        }
        return builder.toString();
    }

    /**
     * GET /customers/?limit={@param limit}&offset={@param offset}
     */
    public List<String> getCustomers(int limit, int offset) throws SQLException {
        Statement statement = null;
        Connection connection = null;
        List<String> customers = new ArrayList<String>();
        try {
            connection = DriverManager.getConnection(URL_DATABASE_DVDRENTAL, mProperties);
            String query = "SELECT customer_id,first_name,last_name,email FROM customer LIMIT " + limit + " OFFSET " + offset + ";";
            statement = connection.createStatement();
            ResultSet result = statement.executeQuery(query);
            while (result.next()) {
                String customer = new StringBuilder(result.getString("custemr_id"))
                        .append(" ").append(result.getString("first_name"))
                        .append(" ").append(result.getString("last_name"))
                        .append(" ").append(result.getString("email"))
                        .toString();
                customers.add(customer);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (statement != null) { statement.close(); }
            if (connection != null) { connection.close(); }
        }
        return customers;
    }

    /**
     * GET /address/{@param addressId}/
     */
    public List<String> getAddress(int addressId) throws SQLException {
        Statement statement = null;
        Connection connection = null;
        List<String> addresses = new ArrayList<String>();
        try {
            connection = DriverManager.getConnection(URL_DATABASE_DVDRENTAL, mProperties);
            String query = "SELECT address_id,address,district,city_id FROM address WHERE address_id = " + addressId + ";";
            statement = connection.createStatement();
            ResultSet result = statement.executeQuery(query);
            while (result.next()) {
                String address = new StringBuilder(result.getString("address_id"))
                        .append(" ").append(result.getString("address"))
                        .append(" ").append(result.getString("district"))
                        .append(" ").append(result.getString("city_id"))
                        .toString();
                addresses.add(address);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (statement != null) { statement.close(); }
            if (connection != null) { connection.close(); }
        }
        return addresses;
    }
}
