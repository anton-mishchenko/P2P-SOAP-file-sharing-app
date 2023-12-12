/*
 * 2023/10/13
 * Anton Mishchenko
 */

package server;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * <p> This class is responsible for managing the connection to the SQL database. It is used to establish
 * connection to the database, close it and monitor its status.</p>
 */
public class SQLConnectionManager {
    private Connection sqlConnection;
    private OutputManager outputManager;

    /**
     * <p> Constructor of SQLConnectionManager class. Sets the output manager used for printing messages to the GUI.</p>
     *
     * <p> Called by: {@link Server.P2PServiceImpl}</p>
     *
     * @param outputManager manager object used for printing messages to the GUI.
     */
    public SQLConnectionManager(OutputManager outputManager) {
        this.outputManager = outputManager;
    }

    /**
     * <p> This method is responsible for establishing connection to the SQL database.</p>
     *
     * <p> Called by: {@link Server.ServerGUI}</p>
     *
     * <p> Calls: {@link OutputManager#printToTextArea}</p>
     *
     * @param url SQL database access URL.
     * @param userName SQL database administrator username.
     * @param password SQL database administrator password.
     * @return true if connection worked, false otherwise.
     */
    public boolean connectToDatabase(String url, String userName, String password) {
        try {
            outputManager.printToTextArea("Connecting to SQL database...");
            sqlConnection = DriverManager.getConnection(url, userName, password);
            return true;
        } catch (SQLException e) {
            outputManager.printToTextArea("Error connecting to SQL database: " + e.getMessage());
            return false;
        }
    }

    /**
     * <p> Returns the current connection to the SQL database.</p>
     *
     * <p> Called by: {@link Server.P2PServiceImpl} </p>
     *
     * @return sqlConnection current connection to the SQL database.
     */
    public Connection getSqlConnection() {
        return sqlConnection;
    }

    /**
     * <p> This method is responsible for closing connection to the SQL database.</p>
     *
     * <p> Called by: {@link Server.ServerGUI}</p>
     *
     * <p> Calls: {@link Server.OutputManager#printToTextArea}</p>
     */
    public void closeSqlConnection() {
        try {
            if (sqlConnection != null) {
                sqlConnection.close();
            }
        } catch (SQLException e) {
            outputManager.printToTextArea("Error closing SQL connection: " + e.getMessage());
        }
    }

    /**
     * <p> This method tests if the SQL driver is working and its registered.</p>
     *
     * <p> Called by: {@link Server.ServerGUI}</p>
     *
     * <p> Calls: {@link Server.OutputManager#printToTextArea}</p>
     *
     * @return true if driver was found and registered, false if error occurs.
     */
    public boolean testSQLDriver() {
        outputManager.printToTextArea("Testing SQL driver...");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Driver driver = new com.mysql.cj.jdbc.Driver();
            DriverManager.registerDriver(driver);
            outputManager.printToTextArea("SQL driver test successful, driver registered");
            return true;
        } catch (ClassNotFoundException e) {
            outputManager.printToTextArea("Error finding SQL driver: " + e.getMessage());
            return false;
        } catch (SQLException e) {
            outputManager.printToTextArea("Error registering SQL driver: " + e.getMessage());
            return false;
        }
    }

    /**
     * <p> This blocking method tests if the SQL connection is working. It is intended for a worker thread from
     * ServerGUI to continuously be testing status of the connection to the database.</p>
     *
     * <p> Called by: {@link Server.ServerGUI}</p>
     *
     * <p> Calls: {@link Server.OutputManager#printToTextArea}</p>
     *
     * @param url SQL database access URL.
     * @param userName SQL database administrator username.
     * @param password SQL database administrator  password.
     * @return true if connection is working, false if error occurs.
     */
    public boolean checkSQLConnection(String url, String userName, String password) {
        while (true) {
            try {
                // test connection
                DriverManager.getConnection(url, userName, password);
                if(sqlConnection.isClosed()) {
                    sqlConnection = DriverManager.getConnection(url, userName, password);
                    outputManager.printToTextArea("Connection to database reestablished.");
                }
                Thread.sleep(100); // wait 100ms before trying again
            } catch (SQLException | InterruptedException e) {
                outputManager.printToTextArea("Lost connection to SQL database");
                break;
            }
        }
        return false;
    }
}