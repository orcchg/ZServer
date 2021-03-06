package com.orcchg.zserver.database;

import com.orcchg.zserver.model.Address;
import com.orcchg.zserver.model.Customer;
import com.orcchg.zserver.server.DataProvider;
import com.orcchg.zserver.utility.Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rx.Observable;

import java.sql.*;
import java.util.Properties;

public class DatabaseHelper implements DataProvider {
    private static final String URL_DATABASE_DVDRENTAL = "jdbc:postgresql:dvdrental";
    private static Logger sLogger = LogManager.getLogger(DatabaseHelper.class);

    private Properties mProperties;

    public DatabaseHelper() {
        try {
            Class.forName("org.postgresql.Driver");  // load PostgrSQL driver and register it to JDBC
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        mProperties = new Properties();
        mProperties.setProperty("user", "postgres");
        mProperties.setProperty("password", DatabaseAuth.retrievePassword());
    }

    /* API */
    // --------------------------------------------------------------------------------------------
    /**
     * GET /test/
     */
    public String testQuery() {
        StringBuilder builder = new StringBuilder("Data: \n");
        Statement statement;
        Connection connection;
        try {
            connection = DriverManager.getConnection(URL_DATABASE_DVDRENTAL, mProperties);
            sLogger.debug("database connected");
            String query = "SELECT first_name,last_name FROM customer;";
            statement = connection.createStatement();
            ResultSet result = statement.executeQuery(query);
            while (result.next()) {
                builder.append(result.getString("first_name")).append(", ").append(result.getString("last_name")).append("\n");
            }
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    @Override
    public Observable<String> addCustomer(Customer customer) {
        Statement statement;
        Connection connection;
        try {
            connection = DriverManager.getConnection(URL_DATABASE_DVDRENTAL, mProperties);
            String query = "INSERT INTO customer (" +
                    "customer_id," +
                    "store_id," +
                    "first_name," +
                    "last_name," +
                    "email," +
                    "address_id," +
                    "activebool," +
                    "create_date," +
                    "last_update," +
                    "active) " +
                    "VALUES (" +
                    customer.getCustomerId() + "," +
                    customer.getStoreId() + "," +
                    "\'" + customer.getFirstName() + "\'," +
                    "\'" + customer.getLastName() + "\'," +
                    "\'" + customer.getEmail() + "\'," +
                    customer.getAddressId() + "," +
                    (customer.isIsActive() ? "true" : "false") + "," +
                    "\'" + customer.getCreateDate() + "\'," +
                    "\'" + customer.getLastUpdate() + "\'," +
                    customer.getActive() + ") RETURNING customer_id;";

            sLogger.debug(query);
            statement = connection.createStatement();
            statement.executeQuery(query);
            statement.close();
            connection.close();
        } catch (SQLException exception) {
            exception.printStackTrace();
            return Observable.just(Utility.messageJson(exception.getMessage()));
        }
        return Observable.just(Utility.messageJson("success"));
    }

    /**
     * GET /customers/?limit={@param limit}&offset={@param offset}
     */
    @Override
    public Observable<Customer> getCustomers(int limit, int offset) {
        return Observable.create(subscriber -> {
            Statement statement = null;
            Connection connection = null;
            try {
                connection = DriverManager.getConnection(URL_DATABASE_DVDRENTAL, mProperties);
                String query = "SELECT * FROM customer LIMIT " + limit + " OFFSET " + offset + ";";
                statement = connection.createStatement();
                ResultSet result = statement.executeQuery(query);
                while (result.next()) {
                    Customer customer = new Customer.Builder(result.getInt("customer_id"))
                            .setStoreId(result.getInt("store_id"))
                            .setFirstName(result.getString("first_name"))
                            .setLastName(result.getString("last_name"))
                            .setEmail(result.getString("email"))
                            .setAddressId(result.getInt("address_id"))
                            .setIsActive(result.getBoolean("activebool"))
                            .setCreateDate(result.getString("create_date"))
                            .setLastUpdate(result.getString("last_update"))
                            .setActive(result.getInt("active"))
                            .build();
                    subscriber.onNext(customer);
                }
                statement.close();
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
                subscriber.onError(e);
            } finally {
                subscriber.onCompleted();
            }
        });
    }

    /**
     * GET /address/?address_id={@param addressId}/
     */
    @Override
    public Observable<Address> getAddress(int addressId) {
        return Observable.create(subscriber -> {
            Statement statement = null;
            Connection connection = null;
            try {
                connection = DriverManager.getConnection(URL_DATABASE_DVDRENTAL, mProperties);
                String query = "SELECT * FROM address WHERE address_id = " + addressId + ";";
                statement = connection.createStatement();
                ResultSet result = statement.executeQuery(query);
                while (result.next()) {
                    Address address = new Address.Builder(result.getInt("address_id"))
                            .setAddress("address")
                            .setAddress2(result.getString("address2"))
                            .setDistrict(result.getString("district"))
                            .setCityId(result.getInt("city_id"))
                            .setPostalCode(result.getString("postal_code"))
                            .setPhone(result.getString("phone"))
                            .setLastUpdate(result.getString("last_update"))
                            .build();
                    subscriber.onNext(address);
                }
                statement.close();
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
                subscriber.onError(e);
            } finally {
                subscriber.onCompleted();
            }
        });
    }
}
