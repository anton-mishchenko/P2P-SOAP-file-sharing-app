/*
 * 2023/10/13
 * Anton Mishchenko
 */

package server;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import org.apache.cxf.annotations.WSDLDocumentation;

/**
 * <p> P2PServiceImpl class is an implementation of a web service based P2P features dictated by P2PServiceSEI class.</p>
 * <p> This class acts as an intermediary between the Clients and SQL database. It provides methods for the Clients
 * to access Server resources via login and registration features, after which they can register, unregister their own
 * files and search, download files registered by other users. </p>
 * <p> Methods in this class output information to the Server GUI via OutputManager, SQL database connection is
 * provided by SQLConnectionManager to the methods that need it,  </p>
 * <p> This class also provides a heartbeat feature that is used to check if the users are active. Allowing the server
 * to display files from active users only. </p>
 * <p> Methods in this class use combination of token and username to verify users and prevent unauthorized access to
 * Server resources.</p>
 */
@WebService(targetNamespace = "http://server/", endpointInterface = "server.P2PServiceImplSEI", portName = "P2PServiceImplPort", serviceName = "P2PServiceImplService")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@WSDLDocumentation("This P2PService Web Service provides methods for clients to facilitate P2P file sharing. Methods allow clients to create basic accounts, manage their sessions, register, remove and search for hosted files.")
public class P2PServiceImpl implements P2PServiceImplSEI {
    private final int MAX_USER_FILES = 10;
    private final ServerGUI serverGUI;
    private ActiveUsers activeUsers;
    private Connection sqlConnection;
    private final SQLConnectionManager SQLConnectionManager;
    private final OutputManager outputManager;
    private final Lock dbLock = new ReentrantLock();

    /**
     * <p> Constructor of the P2PServiceImpl class. It initiates ServerGUI and sets the SQLConnectionManager and
     * OutputManager. It then acquires the SQLConnection from SQLConnectionManager appropriate variables.</p>
     *
     * <p> Calls: {@link Server.ServerGUI#getServerOutputArea}, {@link Server.ServerGUI#setOutputManager},
     * {@link Server.ServerGUI#setButtonListeners}, {@link Server.SQLConnectionManager#getSqlConnection}</p>
     */
    P2PServiceImpl(){
        ServerGUI gui = new ServerGUI();
    	OutputManager outputManager = new OutputManager(ServerGUI.getServerOutputArea());
    	SQLConnectionManager sqlManager = new SQLConnectionManager(outputManager);
        gui.setOutputManager(outputManager);
        gui.setButtonListeners(sqlManager);
        this.serverGUI = gui;
    	this.SQLConnectionManager = sqlManager;
    	this.outputManager = outputManager;
        this.sqlConnection = sqlManager.getSqlConnection();
    }

    /**
     * <p> This method simply verifies if server initiated the active users list.</p>
     *
     * <p> Called by: {@link Server.P2PServiceImpl#connectToServer}, {@link Server.P2PServiceImpl#resumeSession},
     * {@link Server.P2PServiceImpl#disconnectFromServer}, {@link Server.P2PServiceImpl#sendHeartBeat},
     * {@link Server.P2PServiceImpl#registerFile}, {@link Server.P2PServiceImpl#deregisterFile},
     * {@link Server.P2PServiceImpl#getUserFiles}, {@link Server.P2PServiceImpl#searchFile},
     * {@link Server.P2PServiceImpl#getFileHostInfo}</p>
     *
     * <p> Calls: {@link Server.ServerGUI#getActiveUsers}</p>
     *
     * @return true if active users list is ready, false otherwise.
     */
    private boolean isActiveUsersReady(){
        activeUsers = serverGUI.getActiveUsers();
        return activeUsers!=null;
    }

    /**
     * <p> This WebMethod implementation is used to connect a user to the server. First it checks if server is ready and has space.
     * Following that it verifies if user with that username is already logged in. Then a search of provided username is made
     * and if no information found new user is registered, otherwise password if verified and IP and port are updated. When
     * user is logged in they are provided with a token to use for that session. </p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}</p>
     *
     * <p> Calls: {@link Server.P2PServiceImpl#isActiveUsersReady}, {@link Server.ActiveUsers#hasSpace},
     * {@link Server.ActiveUsers#findUser}, {@link Server.ClientData#getName},
     * {@link Server.P2PServiceImpl#getUserInformation}, {@link Server.P2PServiceImpl#generateToken},
     * {@link Server.SQLConnectionManager#getSqlConnection}, {@link Server.P2PServiceImpl#sqlExecuteUpdate},
     * {@link Server.ActiveUsers#addUser}, {@link Server.OutputManager#printToTextArea},
     * {@link Server.P2PServiceImpl#updateUserIp}, {@link Server.P2PServiceImpl#updateUserPort}</p>
     *
     * @param userName username provided by user wanting to log in.
     * @param userPassword password provided by user wanting to log in.
     * @param userIP IP address of the user wanting to log in.
     * @param userPort port of the user wanting to log in.
     *
     * @return <p><b> List <String> with two elements: </b></p>
     * <p> If request completed successfully method returns an OK string followed by a token string element:</p>
     *      <p> ["OK"] - if user logged in successfully.</p>
     *      <p> ["NEW"] - if new user was registered and logged in successfully.</p>
     *      <p> ["UPDATE"] - if user was logged in successfully and their port or IP was updated.</p>
     *      <p><i>and</i></p>
     *      <p> [token] - token string used to verify user session.</p>
     * <p></p>
     * <p> If request failed one of the error code strings followed by short string description element:</p>
     *      <p> ["ERROR"] - if there was a server error when fulfilling the request.</p>
     *      <p> ["FULL"] - if server reached maximum user limit.</p>
     *      <p> ["COPY"] - if user with that name is already logged in.</p>
     *      <p> ["PASSWORD"] - if password provided by the user dosent match database record.</p>
     *      <p><i>and</i></p>
     *      <p> [description] - short string describing the error.</p>
     */
	public List<String> connectToServer(String userName, String userPassword,String userIP, long userPort){
        List <String> response = new ArrayList<>();
        // check if active user list is set up
        if(!isActiveUsersReady()){
        	response.add("ERROR");
            response.add("Server is not ready for requests. Try again later.");
            return response;
        }
        // check if server is full
        if(!activeUsers.hasSpace()){
            response.add("FULL");
            response.add("Server is currently full. Try again later.");
            return response;
        }
        // check if user already logged in and active
        // triggered if user disconnects and restarts client
        // resulting in inability to provide token from previous session
        ClientData foundUser = activeUsers.findUser(userName);
        if (foundUser!= null && foundUser.getName().equals(userName)) {
            response.add("COPY");
            response.add("User with this name is already logged in." + "\n" +
                    " If you have experienced a disconnect and restarted your application," +
                    " please try logging in again in 2 minutes.");
            return response;
        }

        // check if user entry exists in the database
        String[] sqlResultArray = getUserInformation(userName);

        if(sqlResultArray == null){
            response.add("ERROR");
            response.add("Could not acquire user data for " + userName + ". Try again later.");
            return response;
        }

        // generate new session token
        String token = generateToken();

        // if user does not exist
        if (sqlResultArray[0] == null) {
            // add new user to the database
            try {
                String query = "insert into Users (User_Name, User_Password, User_IP, User_Port) " +
                        "values (\"" + userName + "\",\"" + userPassword + "\",\"" + userIP + "\"," + userPort + ");";
                // execute query
                sqlConnection = SQLConnectionManager.getSqlConnection();
                Statement statement = sqlConnection.createStatement();
                int result = sqlExecuteUpdate(query, statement);
                // close resources
                statement.close();
                // check if user was added
                if (result == 0) {
                    throw new SQLException();
                }
                // add user to active users
                activeUsers.addUser(token, userName);
                outputManager.printToTextArea("New user: " + userName + " was added to Database.");
                // return corresponding response
                response.add("NEW");
                response.add(token);
                return response;
            } catch (SQLException e) {
                outputManager.printToTextArea("SQL ERROR: occurred when adding user " + userName + ": " +
                        "\n" + e);
                response.add("ERROR");
                response.add("Could not add user " + userName + ". Try again later.");
                return response;
            }
            catch (Exception e) {
                outputManager.printToTextArea("ERROR: occurred when adding user " + userName + ": " +
                        "\n" + e);
                response.add("ERROR");
                response.add("Could not add user " + userName + ". Try again later.");
                return response;
            }
        }

        // user exists and password is correct
        if (sqlResultArray[0].equals(userName) && sqlResultArray[1].equals(userPassword)) {
            boolean ipUpdated = false;
            boolean portUpdated = false;
            // check/update user IP
            if (!Objects.equals(sqlResultArray[2], userIP)) {
                // update IP
                if(updateUserIp(userIP, userName)){
                    ipUpdated = true;
                }else {
                    response.add("ERROR");
                    response.add("Could not update IP of user " + userName + ". Try again later.");
                    return response;
                }
            }
            if (!Objects.equals(sqlResultArray[3], Long.toString(userPort))) {
                // update port
                if(updateUserPort(userPort, userName)){
                    portUpdated = true;
                }else {
                    response.add("ERROR");
                    response.add("Could not update port of user " + userName + ". Try again later.");
                    return response;
                }
            }
            // display appropriate message and add user
            if(portUpdated && !ipUpdated) {
                outputManager.printToTextArea("User: " + userName + " connected. Port updated.");
            } else if (ipUpdated && !portUpdated) {
                outputManager.printToTextArea("User: " + userName + " connected. IP updated.");
            } else if (ipUpdated && portUpdated) {
                outputManager.printToTextArea("User: " + userName + " connected. IP and port updated.");
            } else {
                // add user to active users without updates
                activeUsers.addUser(token, userName);
                outputManager.printToTextArea("User: " + userName + " connected. Information up to date.");
                response.add("OK");
                response.add(token);
                return response;
            }
            activeUsers.addUser(token, userName);
            response.add("UPDATE");
            response.add(token);
            return response;
        } else { // user exists but password is incorrect
            response.add("PASSWORD");
            response.add("Incorrect password was provided.");
        }
        return response;
    }

    /**
     * <p> This method is used to query the database for user information. It takes a username and returns an array
     * of strings containing user information.</p>
     *
     * <p> Called by: {@link Server.P2PServiceImpl#connectToServer}, {@link Server.P2PServiceImpl#resumeSession}</p>
     *
     * <p> Calls: {@link Server.P2PServiceImpl#sqlExecuteQuery}, {@link Server.SQLConnectionManager#getSqlConnection},
     * {@link Server.OutputManager#printToTextArea}</p>
     *
     * @param userName username provided by user wanting to log in.
     *
     * @return <p><b> String[] with four elements: </b></p>
     * <p> [User_Name] - username of the user.</p>
     * <p> [User_Password] - password of the user.</p>
     * <p> [User_IP] - IP address of the user.</p>
     * <p> [User_Port] - port of the user.</p>
     * <p> null - if user does not exist.</p>
     */
    private String[] getUserInformation(String userName){
        String query = "select * from Users where user_Name = \"" + userName + "\" ;";
        String[] sqlResultArray = new String[4];
        try {
            // acquire most recent sql connection
            sqlConnection = SQLConnectionManager.getSqlConnection();
            if(sqlConnection == null){
                throw new SQLException();
            }
            // execute query
            Statement statement = sqlConnection.createStatement();
            ResultSet resultSet = sqlExecuteQuery(query, statement);
            // test for database issues
            if (resultSet == null) {
                throw new SQLException();
            }
            // parse result
            while (resultSet.next()) {
                for (int i = 0; i < 4; i++) {
                    sqlResultArray[i] = resultSet.getString(i + 1);
                }
            }
            // close resources
            resultSet.close();
            statement.close();
            return sqlResultArray;
        } catch (SQLException e) {
            outputManager.printToTextArea("SQL ERROR: occurred when searching for user " + userName + ": " +
                    "\n" + e);
            return null;
        } catch (Exception e) {
            outputManager.printToTextArea("ERROR: occurred when searching for user " + userName + ": " +
                    "\n" + e);
            return null;
        }
    }

    /**
     * <p> This method is used to update User IP address in the database.</p>
     *
     * <Called by: {@link Server.P2PServiceImpl#connectToServer}, {@link Server.P2PServiceImpl#resumeSession}</p>
     *
     * <p> Calls: {@link Server.P2PServiceImpl#sqlExecuteUpdate}, {@link Server.SQLConnectionManager#getSqlConnection},
     * {@link Server.OutputManager#printToTextArea}</p>
     *
     * @param userIP new IP address of the user.
     * @param userName username of the user.
     *
     * @return true if update was successful, false otherwise.
     */
    private boolean updateUserIp(String userIP, String userName){
        try {
            // create query
            String query = "update Users set User_IP = \"" + userIP + "\" where User_Name = \"" + userName + "\";";
            sqlConnection = SQLConnectionManager.getSqlConnection();
            Statement statement = sqlConnection.createStatement();
            // execute query
            int result = sqlExecuteUpdate(query, statement);
            // close resources
            statement.close();
            if (result == 0) {
                throw new SQLException();
            }
            return true;
        } catch (SQLException e) {
            outputManager.printToTextArea("SQL ERROR: occurred when updating ip of user " + userName + ": " +
                    "\n" + e);
            return false;
        } catch (Exception e) {
            outputManager.printToTextArea("ERROR: occurred when updating ip of user " + userName + ": " +
                    "\n" + e);
            return false;
        }
    }


    /**
     * <p> This method is used to update User Port in the database.</p>
     *
     * <Called by: {@link Server.P2PServiceImpl#connectToServer}, {@link Server.P2PServiceImpl#resumeSession}</p>
     *
     * <p> Calls: {@link Server.P2PServiceImpl#sqlExecuteUpdate}, {@link Server.SQLConnectionManager#getSqlConnection},
     * {@link Server.OutputManager#printToTextArea}</p>
     *
     * @param userPort new port of the user.
     * @param userName username of the user.
     *
     * @return true if update was successful, false otherwise.
     */
    private boolean updateUserPort(long userPort, String userName){
        try {
            // create query
            String query = "update Users set User_Port = \"" + userPort + "\" where User_Name = \"" + userName + "\";";
            // execute query
            sqlConnection = SQLConnectionManager.getSqlConnection();
            Statement statement = sqlConnection.createStatement();
            int result = sqlExecuteUpdate(query, statement);
            // close resources
            statement.close();
            if (result == 0) {
                throw new SQLException();
            }
            return true;
        } catch (SQLException e) {
            outputManager.printToTextArea("SQL ERROR: occurred when updating port of user " + userName + ": " +
                    "\n" + e);
            return false;
        } catch (Exception e) {
            outputManager.printToTextArea("ERROR: occurred when updating port of user " + userName + ": " +
                    "\n" + e);
            return false;
        }
    }

    /**
     * <p> This WebMethod implementation is used to resume a user session on the server. It verifies the user via
     * combination of active users list and database records. If user is verified, their IP and port are updated if
     * necessary and a new token is generated for the new session.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}</p>
     *
     * <p> Calls: {@link Server.P2PServiceImpl#isActiveUsersReady}, {@link Server.ClientData#getName},
     * {@link Server.ClientData#getToken}, {@link Server.P2PServiceImpl#getUserInformation},
     * {@link Server.P2PServiceImpl#updateUserIp}, {@link Server.P2PServiceImpl#updateUserPort},
     * {@link Server.ActiveUsers#addUser}, {@link Server.OutputManager#printToTextArea},
     * {@link P2PServiceImpl#generateToken()}</p>
     *
     * @param token token provided by user.
     * @param userName username provided by user.
     * @param userIP IP address of the user.
     * @param userPort port of the user.
     *
     * @return <p><b> List <String> with two elements: </b></p>
     * <p> If request completed successfully method returns an OK string followed by short string description element:</p>
     *      <p> ["OK"] - if resume request was successfull.</p>
     *      <p> ["UPDATE"] - if user IP or port was updated and resume reqeust is successfull.</p>
     *      <p><i>and</i></p>
     *      <p> [description] - short string describing request completion.</p>
     * <p></p>
     * <p> If request failed one of the error code strings followed by short string description element:</p>
     *      <p> ["ERROR"] - if there was a server error when fulfilling the request.</p>
     *      <p> ["CRED"] - if token/username combination is incorrect.</p>
     *      <p><i>and</i></p>
     *      <p> [description] - short string describing the error.</p>
     */
    public List <String> resumeSession (String token, String userName, String userIP, long userPort){
        List <String> response = new ArrayList<>();
        // check if active user list is set up
        if(!isActiveUsersReady()){
            response.add("ERROR");
            response.add("Server is not ready for requests. Try again later.");
            return response;
        }
        // check if token was provided
        if(!token.isEmpty()){
            // find user in active users
            ClientData testedUser = activeUsers.findUser(userName);
            if (testedUser != null) {
                // check if username and token matches
                if ((testedUser.getName().equals(userName) && testedUser.getToken().equals(token))) {
                    String[] sqlResultArray = getUserInformation(userName);
                    // check if user exists in database
                    if(sqlResultArray == null){
                        response.add("ERROR");
                        response.add("Could not acquire user data for " + userName + ". Try again later.");
                        return response;
                    }
                    boolean ipUpdated = false;
                    boolean portUpdated = false;
                    // check/update user IP
                    if (!Objects.equals(sqlResultArray[2], userIP)) {
                        // update IP
                        if(updateUserIp(userIP, userName)){
                            ipUpdated = true;
                        }else {
                            response.add("ERROR");
                            response.add("Could not update IP of user " + userName + ". Try again later.");
                            return response;
                        }
                    }
                    if (!Objects.equals(sqlResultArray[3], Long.toString(userPort))) {
                        // update port
                        if(updateUserPort(userPort, userName)){
                            portUpdated = true;
                        }else {
                            response.add("ERROR");
                            response.add("Could not update port of user " + userName + ". Try again later.");
                            return response;
                        }
                    }
                    String newToken = generateToken();
                    // display appropriate message and add user
                    if(portUpdated && !ipUpdated) {
                        outputManager.printToTextArea("User: " + userName + " connected. Port updated.");
                    } else if (ipUpdated && !portUpdated) {
                        outputManager.printToTextArea("User: " + userName + " connected. IP updated.");
                    } else if (ipUpdated && portUpdated) {
                        outputManager.printToTextArea("User: " + userName + " connected. IP and port updated.");
                    } else {
                        // add user to active users without updates
                    	activeUsers.removeUser(token, userName);
                        activeUsers.addUser(newToken, userName);
                        outputManager.printToTextArea("User: " + userName + " connected. Information up to date.");
                        response.add("OK");
                        response.add(newToken);
                        return response;
                    }
                    activeUsers.removeUser(token, userName);
                    activeUsers.addUser(newToken, userName);
                    response.add("UPDATE");
                    response.add(newToken);
                    return response;
                }else {
                    response.add("CRED");
                    response.add("Could not continue session. Token/Username mismatch.");
                }
                return response;
            }else{
                response.add("ERROR");
                response.add("Could not find active user.");
                return response;
            }
        }else {
            response.add("ERROR");
            response.add("No token provided.");
            return response;
        }
    }

    /**
     * <p> This WebMethod implementation is used to disconnect user from the server. It verifies the user via token and
     * username, then removes them from the active users list. </p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}</p>
     *
     * <p> Calls: {@link Server.P2PServiceImpl#isActiveUsersReady}, {@link Server.P2PServiceImpl#verifyActiveUser},
     * {@link Server.ActiveUsers#getNumOfUsers}, {@link Server.ActiveUsers#removeUser},
     * {@link Server.OutputManager#printToTextArea}</p>
     *
     * @param token token provided by user.
     * @param userName username provided by user.
     *
     * @return <p><b> List <String> with two elements: </b></p>
     * <p> If request completed successfully method returns an OK string followed by short string description element:</p>
     *      <p> ["OK"] - if disconnect request was successfull.</p>
     *      <p><i>and</i></p>
     *      <p> [description] - short string describing request completion.</p>
     * <p></p>
     * <p> If request failed one of the error code strings followed by short string description element:</p>
     *      <p> ["ERROR"] - if there was a server error when fulfilling the request.</p>
     *      <p> ["CRED"] - if token/username combination is incorrect.</p>
     *      <p><i>and</i></p>
     *      <p> [description] - short string describing the error.</p>
     */
    public List <String> disconnectFromServer (String token,  String userName){
        List <String> response = new ArrayList<>();
        // check if active user list is set up
        if(!isActiveUsersReady()){
            response.add("ERROR");
            response.add("Server is not ready for requests. Try again later.");
            return response;
        }
        // check if there are active users
        if(activeUsers.getNumOfUsers()==0){
            response.add("ERROR");
            response.add("Not active user.");
            return response;
        }
        // verify user
        if(!verifyActiveUser(token, userName)){
            response.add("CRED");
            response.add("Could not disconnect. Token/Username mismatch.");
            return response;
        }
        // remove user from active users
        if(activeUsers.removeUser(token, userName) != null){
            outputManager.printToTextArea("User: " + userName + " disconnected.");
            response.add("OK");
            response.add("Disconnected form Server.");
            return response;
        } else {
            outputManager.printToTextArea("User: " + userName + " failed to disconnect.");
            response = new ArrayList<>();
            response.add("ERROR");
            response.add("Could not disconnect.");
        }
        return response;
    }

    /**
     * <p> This WebMethod implementation is used by Clients to send a heartbeat notification to the server. It verifies the
     * user via provided token and username and then sets the user as active, updating their last active time.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}</p>
     *
     * <p> Calls: {@link Server.P2PServiceImpl#isActiveUsersReady}, {@link Server.P2PServiceImpl#verifyActiveUser},
     * {@link Server.ActiveUsers#findUser}, {@link Server.ClientData#setLastActive},
     * {@link Server.OutputManager#printToTextArea}</p>
     *
     * @param token token provided by user.
     * @param userName username provided by user.
     *
     * @return <p><b> List <String> with two elements: </b></p>
     * <p> If request completed successfully method returns an OK string followed by short string description element:</p>
     *      <p> ["OK"] - if heartbeat request was successfull.</p>
     *      <p><i>and</i></p>
     *      <p> [description] - short string describing completion.</p>
     * <p></p>
     * <p> If request failed one of the error code strings followed by short string description element:</p>
     *      <p> ["ERROR"] - if there was a server error when fulfilling the request.</p>
     *      <p> ["CRED"] - if token/username combination is incorrect.</p>
     *      <p><i>and</i></p>
     *      <p> [description] - short string describing the error.</p>
     */
    public List <String> sendHeartBeat (String token,  String userName){
        List <String> response = new ArrayList<>();
        // check if active user list is set up
        if(!isActiveUsersReady()){
            response.add("ERROR");
            response.add("Server is not ready for requests. Try again later.");
            return response;
        }
        // verify user
        if(!verifyActiveUser(token, userName)){
            response.add("CRED");
            response.add("Could not send heart beat. Token/Username mismatch.");
            return response;
        }
        try {
            // set user as active, update last active time
            ClientData user = activeUsers.findUser(userName);
            user.setLastActive(true);
            response.add("OK");
            response.add("Heartbeat received by server.");
            return response;
        } catch (Exception e) {
            outputManager.printToTextArea("ERROR: occurred when user " + userName + " sent heart beat: " + "\n" + e);
            response.add("ERROR");
            response.add("Error sending heart beat.");
            return response;
        }
    }


    /**
     * <p> This WebMethod implementation is used by Clients to register a new file on the server and add it to the
     * database. It verifies the user via provided token and username. Then checks if the file is already registered by
     * the same user and if user us under allowed limit of files. If checks are completed it is added to the database.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}</p>
     *
     * <p> Calls: {@link Server.P2PServiceImpl#isActiveUsersReady}, {@link Server.P2PServiceImpl#verifyActiveUser},
     * {@link Server.P2PServiceImpl#sqlExecuteQuery}, {@link Server.SQLConnectionManager#getSqlConnection},
     * {@link Server.OutputManager#printToTextArea}, {@link Server.P2PServiceImpl#sqlExecuteUpdate}</p>
     *
     * @param token token provided by user.
     * @param userName username provided by user.
     * @param fileName name of the file to be registered.
     * @param fileType type of the file to be registered.
     * @param filePath path of the file to be registered.
     * @param fileSize size of the file to be registered.
     *
     * @return <p><b> List <String> with two elements: </b></p>
     * <p> If request completed successfully method returns an OK string followed by short string description element:</p>
     *      <p> ["OK"] - if file was registered.</p>
     *      <p><i>and</i></p>
     *      <p> [description] - short string describing completion.</p>
     * <p></p>
     * <p> If request failed one of the error code strings followed by short string description element:</p>
     *      <p> ["ERROR"] - if there was a server error when fulfilling the request.</p>
     *      <p> ["CRED"] - if token/username combination is incorrect.</p>
     *      <p> ["FULL"] - if user has reached maximum number of files (10).</p>
     *      <p> ["COPY"] - if file already exists.</p>
     *      <p><i>and</i></p>
     *      <p> [description] - short string describing the error.</p>
     */
    public List <String> registerFile (String token,  String userName,  String fileName,  String fileType,  String filePath,  long fileSize) {
        List <String> response = new ArrayList<>();
        // check if active user list is set up
        if(!isActiveUsersReady()){
            response.add("ERROR");
            response.add("Server is not ready for requests. Try again later.");
            return response;
        }
        // verify user
        if(!verifyActiveUser(token, userName)){
            response.add("CRED");
            response.add("Could not register file. Token/Username mismatch.");
            return response;
        }

        String query = "select count(*) from UserFiles where User_Name = \""+userName+"\";";
        // check number of files user has registered
        try{
            sqlConnection = SQLConnectionManager.getSqlConnection();
            Statement statement = sqlConnection.createStatement();
            ResultSet result = sqlExecuteQuery(query,statement);
            // test for database issues
            if (result == null) {
                throw new SQLException();
            }
            // parse result
            while (result.next()) {
                if(result.getInt(1) >= MAX_USER_FILES){
                    response.add("FULL");
                    response.add("User has reached maximum number of files (" + MAX_USER_FILES + ").");
                    return response;
                }
            }
        }catch (SQLException e) {
            outputManager.printToTextArea("SQL ERROR: occurred when acquiring number of user files for " + userName + ": " +
                    "\n" + e);
            response.add("ERROR");
            response.add("Could not register file " + fileName + ". Try again later.");
            return response;
        }catch (Exception e) {
            outputManager.printToTextArea("ERROR: occurred when acquiring number of user files for " + userName + ": " +
                    "\n" + e);
            response.add("ERROR");
            response.add("Could not register file " + fileName + ". Try again later.");
            return response;
        }

        filePath = filePath.replace("\\","\\\\");
        query = "select * from UserFiles where User_Name = \""+userName+"\" and File_Name = \""+fileName+"\" and File_Type = \""+fileType+"\" and File_Path = \""+filePath+"\";";
        // find file in case of duplicate
        try{
            Statement statement = sqlConnection.createStatement();
            ResultSet result = sqlExecuteQuery(query,statement);
            // test for database issues
            if (result == null) {
                throw new SQLException();
            } else if (result.next()) {
                response.add("COPY");
                response.add("Could not register chosen file. File already exists.");
                return response;
            }
            // close resources
            result.close();
            statement.close();
        } catch (SQLException e) {
            outputManager.printToTextArea("SQL ERROR: occurred when verifying file copies for " + userName + ": " +
                    "\n" + e);
            response.add("ERROR");
            response.add("Could not register file " + fileName + ". Try again later.");
            return response;
        } catch (Exception e) {
            outputManager.printToTextArea("ERROR: occurred when verifying file copies for " + userName + ": " +
                    "\n" + e);
            response.add("ERROR");
            response.add("Could not register file " + fileName + ". Try again later.");
            return response;
        }

        Random rand = new Random();
        ResultSet sqlIdResult;
        int fileId;
        // generate file id and check if is not already in use
        try{
            do{
                fileId = rand.nextInt(1000000);
                String fileIdQuery = "select * from UserFiles where File_ID = \""+fileId+"\";";
                Statement statement = sqlConnection.createStatement();
                sqlIdResult = sqlExecuteQuery(fileIdQuery,statement);
                if (sqlIdResult == null) {
                    throw new SQLException();
                }
            }while (sqlIdResult.next());
        }catch (SQLException e){
            outputManager.printToTextArea("ERROR: occurred when generating new File_ID for " + userName + ": " +
                    "\n" + e);
            response.add("ERROR");
            response.add("Could not register file " + fileName + ". Try again later.");
            return response;
        }catch (Exception e){
            outputManager.printToTextArea("SQL ERROR: occurred when generating new File_ID for " + userName + ": " +
                    "\n" + e);
            response.add("ERROR");
            response.add("Could not register file " + fileName + ". Try again later.");
            return response;
        }

        // add new file to the database
        query = "insert into UserFiles values (\""+fileId+"\",\""+fileName+"\",\""+fileType+"\",\""+filePath+"\",\""+fileSize+"\",\""+userName+"\");";
        try {
            Statement statement = sqlConnection.createStatement();
            int result = sqlExecuteUpdate(query,statement);
            // close resources
            statement.close();
            // check if file was added
            if (result == 0) {
                throw new SQLException();
            }
            response.add("OK");
            response.add("File successfully registered on the server.");
            return response;
        } catch (SQLException e) {
            outputManager.printToTextArea("SQL ERROR: occurred when registering file in the database: " +
                    "\n" + e);
            response.add("ERROR");
            response.add("Could not register file " + fileName + ". Try again later.");
            return response;
        } catch (Exception e) {
            outputManager.printToTextArea("ERROR: occurred when registering file in the database: " +
                    "\n" + e);
            response.add("ERROR");
            response.add("Could not register file " + fileName + ". Try again later.");
            return response;
        }
    }


    /**
     * <p> This WebMethod implementation is used by Clients to remove a file the database. It verifies the user via
     * provided token and username. Then it searches for the specified file and removes it from the database. </p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}</p>
     *
     * <p> Calls: {@link Server.P2PServiceImpl#isActiveUsersReady}, {@link Server.P2PServiceImpl#verifyActiveUser},
     * {@link Server.P2PServiceImpl#sqlExecuteUpdate}, {@link Server.SQLConnectionManager#getSqlConnection},
     * {@link Server.OutputManager#printToTextArea}</p>
     *
     * @param token token provided by user.
     * @param userName username provided by user.
     * @param fileName name of the file to be removed.
     * @param fileType type of the file to be removed.
     * @param filePath path to the file to be removed.
     *
     * @return <p><b> List <String> with two elements: </b></p>
     * <p> If request completed successfully method returns an OK string followed by short string description element:</p>
     *      <p> ["OK"] - if file was removed.</p>
     *      <p><i>and</i></p>
     *      <p> [description] - short string describing completion. </p>
     * <p></p>
     * <p> If request failed one of the error code strings followed by short string description element:</p>
     *      <p> ["ERROR"] - if there was a server error when fulfilling the request.</p>
     *      <p> ["CRED"] - if token/username combination is incorrect. </p>
     *      <p><i>and</i></p>
     *      <p> [description] - short string describing the error. </p>
     */
    public List <String> deregisterFile ( String token,  String userName,  String fileName,  String fileType,  String filePath) {
        List <String> response = new ArrayList<>();
        if(!isActiveUsersReady()){
            response.add("ERROR");
            response.add("Server is not ready for requests. Try again later.");
            return response;
        }
        // verify user
        if(!verifyActiveUser(token, userName)){
            response.add("CRED");
            response.add("Could not deregister specified file. Token/Username mismatch.");
            return response;
        }

        filePath = filePath.replace("\\","\\\\");
        String query = "delete from UserFiles where User_Name = \""+userName+"\" and File_Name = \""+fileName+"\" and File_Type = \""+fileType+"\" and File_Path = \""+filePath+"\";";
        // remove file from the database
        try {
            sqlConnection = SQLConnectionManager.getSqlConnection();
            Statement statement = sqlConnection.createStatement();
            int result = sqlExecuteUpdate(query,statement);
            // close resources
            statement.close();
            // check if file was removed
            if (result == 0) {
                throw new SQLException();
            }
            response.add("OK");
            response.add("File deregistered from server.");
            return response;
        } catch (SQLException e) {
            outputManager.printToTextArea("SQL ERROR: occurred when removing file of " + userName + ": " +
                    "\n" + e);
            response.add("ERROR");
            response.add("Could not remove file " + fileName + ". Try again later.");
            return response;
        } catch (Exception e) {
            outputManager.printToTextArea("ERROR: occurred when removing file of " + userName + ": " +
                    "\n" + e);
            response.add("ERROR");
            response.add("Could not remove file " + fileName + ". Try again later.");
            return response;
        }
    }

    /**
     * <p> This WebMethod implementation is used by Clients to get a list of files registered by the user. It verifies
     * the user via provided token and username. Then it searches the database for the files registered by the user
     * and returns them. </p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}</p>
     *
     * <p> Calls: {@link Server.P2PServiceImpl#isActiveUsersReady}, {@link Server.P2PServiceImpl#verifyActiveUser},
     * {@link Server.P2PServiceImpl#sqlExecuteQuery}, {@link Server.SQLConnectionManager#getSqlConnection},
     * {@link Server.OutputManager#printToTextArea}</p>
     *
     * @param token token provided by user.
     * @param userName username provided by user.
     *
     * @return <p><b> List <String> with two or more elements: </b></p>
     * <p> If request completed successfully method returns an OK string followed by five more string elements:</p>
     *      <p> ["OK"] - if user files were found.</p>
     *      <p><i>and</i></p>
     *      <p> [File_ID] - ID of the file.</p>
     *      <p> [File_Name] - name of the file.</p>
     *      <p> [File_Type] - type of the file.</p>
     *      <p> [File_Path] - path to the file.</p>
     *      <p> [File_Size] - size of the file.</p>
     * <p></p>
     * <p> If request failed one of the error code strings followed by short string description element:</p>
     *      <p> ["404"] - if no user files were found.</p>
     *      <p> ["ERROR"] - if there was a server error when fulfilling the request.</p>
     *      <p> ["CRED"] - if token/username combination is incorrect. </p>
     *      <p><i>and</i></p>
     *      <p> [description] - short string describing the error. </p>
     */
    public List <String> getUserFiles ( String token,  String userName) {
        List <String> response = new ArrayList<>();
        if(!isActiveUsersReady()){
            response.add("ERROR");
            response.add("Server is not ready for requests. Try again later.");
            return response;
        }
        // verify user
        if(!verifyActiveUser(token, userName)){
            response.add("CRED");
            response.add("Could not return user files. Token/Username mismatch.");
            return response;
        }

        String query = "select File_ID, File_Name, File_Type, File_Path, File_Size from UserFiles where User_Name = \""+userName+"\";";
        // get user files from the database
        try {
            sqlConnection = SQLConnectionManager.getSqlConnection();
            Statement statement = sqlConnection.createStatement();
            ResultSet result = sqlExecuteQuery(query,statement);
            // test for database issues
            if (result == null) {
                throw new SQLException();
            }
            // handle empty result
            if (!result.next()) {
                response.add("404");
                response.add("No files found.");
                return response;
            }
            response.add("OK");
            // parse sql result
            do {
                response.add(result.getString(1));
                response.add(result.getString(2));
                response.add(result.getString(3));
                response.add(result.getString(4));
                response.add(result.getString(5));
            } while (result.next());
            // close resources
            result.close();
            statement.close();
            // parse result
            return response;
        } catch (SQLException e) {
            outputManager.printToTextArea("SQL ERROR: occurred when fetching files of " + userName + ": " +
                    "\n" + e);
            response.add("ERROR");
            response.add("Could not acquire user files. Try again later.");
            return response;
        } catch (Exception e) {
            outputManager.printToTextArea("ERROR: occurred when fetching files of " + userName + ": " +
                    "\n" + e);
            response.add("ERROR");
            response.add("Could not acquire user files. Try again later.");
            return response;
        }
    }


    /**
     * <p> This WebMethod implementation is used by Clients to search for a file on the server. It verifies the user
     * via provided token and username. Then it searches the database for the files matching the search string and
     * makes sure that only files registered by currently active users are returned not including files registered by
     * the searching user.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}</p>
     *
     * <p> Calls: {@link Server.P2PServiceImpl#isActiveUsersReady}, {@link Server.P2PServiceImpl#verifyActiveUser},
     * {@link Server.P2PServiceImpl#sqlExecuteQuery}, {@link Server.SQLConnectionManager#getSqlConnection},
     * {@link Server.ActiveUsers#findUser}, {@link Server.OutputManager#printToTextArea}</p>
     *
     * @param token token provided by user.
     * @param userName username provided by user.
     * @param searchQuery String used to search for files in the database.
     *
     * @return <p><b> List <String> with two or more elements: </b></p>
     * <p> If request completed successfully method returns an OK string followed by four more string elements:</p>
     *      <p> ["OK"] - if file was found.</p>
     *      <p><i>and</i></p>
     *      <p> [File_ID] - ID of the file.</p>
     *      <p> [File_Name] - name of the file.</p>
     *      <p> [File_Type] - type of the file.</p>
     *      <p> [File_Size] - size of the file.</p>
     * <p></p>
     * <p> If request failed one of the error code strings followed by short string description element:</p>
     *      <p> ["404"] - if no file was found.</p>
     *      <p> ["ERROR"] - if there was a server error when fulfilling the request.</p>
     *      <p> ["CRED"] - if token/username combination is incorrect. </p>
     *      <p><i>and</i></p>
     *      <p> [description] - short string describing the error. </p>
     */
    public List <String> searchFile ( String token,  String userName,  String searchQuery){
        List <String> response = new ArrayList<>();
        if(!isActiveUsersReady()){
            response.add("ERROR");
            response.add("Server is not ready for requests. Try again later.");
            return response;
        }
        // verify user
        if(!verifyActiveUser(token, userName)){
            response.add("CRED");
            response.add("Could not search for specified files. Token/Username mismatch.");
            return response;
        }

        String query = "SELECT File_ID, File_Name, File_Type, File_Size, User_Name " +
                "FROM UserFiles WHERE CONCAT_WS('', File_Name, File_Type) LIKE '%" + searchQuery + "%' AND User_Name != \""+userName+"\";";
        // search for a file in the database
        try {
            sqlConnection = SQLConnectionManager.getSqlConnection();
            Statement statement = sqlConnection.createStatement();
            ResultSet result = sqlExecuteQuery(query,statement);
            // test for database issues
            if (result == null) {
                throw new SQLException();
            }
            // handle empty result
            if (!result.next()) {
                response.add("404");
                response.add("No files containing \"" + searchQuery + "\" found.");
                return response;
            }
            response.add("OK");
            // parse sql result
            do {
                // check if host is active
                String userWithFile = result.getString(5);
                if(activeUsers.findUser(userWithFile) != null && !userWithFile.equals(userName)){
                	response.add(result.getString(1));
                	response.add(result.getString(2));
                    response.add(result.getString(3));
                    response.add(result.getString(4));
                }
            }while (result.next());
            // check if any files were found
            if(response.size() < 2){
                response = new ArrayList<>();
                response.add("404");
                response.add("No files containing \"" + searchQuery + "\" found.");
                return response;
            }
            // close resources
            result.close();
            statement.close();
            // parse result
            return response;
        } catch (SQLException e) {
            outputManager.printToTextArea("SQL ERROR: occurred when searching for a query of " + userName + ": " +
                    "\n" + e);
            response.add("ERROR");
            response.add("Could not search for \"" + searchQuery + "\". Try again later.");
            return response;
        } catch (Exception e) {
            outputManager.printToTextArea("ERROR: occurred when searching for a query of " + userName + ": " +
                    "\n" + e);
            response.add("ERROR");
            response.add("Could not search for \"" + searchQuery + "\". Try again later.");
            return response;
        }
    }

    /**
     * <p> This WebMethod implementation is used by Clients to get information about the host of a file they are looking
     * to download. It verifies the user via provided token and username and makes sure that the server is ready for interaction.
     * Then it searches the database for the users that match the file ID provided and makes sure that only active
     * users are returned. If an error occurs, method returns an error code and a short description.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}</p>
     *
     * <p> Calls: {@link Server.P2PServiceImpl#isActiveUsersReady}, {@link Server.P2PServiceImpl#verifyActiveUser},
     * {@link Server.P2PServiceImpl#sqlExecuteQuery}, {@link Server.SQLConnectionManager#getSqlConnection},
     * {@link Server.ActiveUsers#findUser}, {@link Server.OutputManager#printToTextArea}</p>
     *
     * @param token token provided by user.
     * @param userName username provided by user.
     * @param fileID ID of the file to be downloaded.
     *
     * @return <p><b> List <String> with two or more elements: </b></p>
     * <p> If request completed successfully method returns an OK string followed by three more string elements:</p>
     *      <p> ["OK"] - if host was found.</p>
     *      <p> [User_IP] - IP address of the active user with the file. </p>
     *      <p> [User_Port] - port number of the active user with the file. </p>
     *      <p> [File_Path] - path of the file on the active user's machine. </p>
     * <p></p>
     * <p> If request failed one of the error code strings followed by short string description element:</p>
     *      <p> ["404"] - if no available host was found.</p>
     *      <p> ["ERROR"] - if there was a server error when fulfilling the request.</p>
     *      <p> ["CRED"] - if token/username combination is incorrect. </p>
     *      <p> [description] - short string describing the error. </p>
     */
    public List <String> getFileHostInfo ( String token,  String userName,  int fileID){
        List <String> response = new ArrayList<>();
        if(!isActiveUsersReady()){
            response.add("ERROR");
            response.add("Server is not ready for requests. Try again later.");
            return response;
        }
        // verify user
        if(!verifyActiveUser(token, userName)){
            response.add("CRED");
            response.add("Could not return file owner info. Token/Username mismatch.");
            return response;
        }

        String query = "select Users.User_Name, User_IP, User_Port, File_Path from Users" +
                " inner join UserFiles ON Users.User_Name = UserFiles.User_Name" +
                " where File_ID = \""+fileID+"\"" +
                " and UserFiles.User_Name != \""+userName+"\";";
        // get file host info from the database
        try {
            sqlConnection = SQLConnectionManager.getSqlConnection();
            Statement statement = sqlConnection.createStatement();
            ResultSet result = sqlExecuteQuery(query,statement);
            // test for database issues
            if (result == null) {
                throw new SQLException();
            }
            // handle empty result
            if (!result.next()) {
                response.add("404");
                response.add("No file hosts were found.");
                return response;
            }
            response.add("OK");
            // parse sql result
            do {
                // check if host is active
                ClientData dataUserWithFile = activeUsers.findUser(result.getString(1));
                if(dataUserWithFile != null) {
                    response.add(result.getString(2));
                    response.add(result.getString(3));
                    response.add(result.getString(4));
                }
            }while (result.next());
            // check if any files were found
            if(response.size() < 2){
                response = new ArrayList<>();
                response.add("404");
                response.add("No active file hosts were found. Or file was removed.");
                return response;
            }
            // close resources
            result.close();
            statement.close();
            // parse result
            return response;
        } catch (SQLException e) {
            outputManager.printToTextArea("SQL ERROR: occurred when completing host search request for " + userName + ": " +
                    "\n" + e);
            response.add("ERROR");
            response.add("Could not complete a search for hosts. Try again later.");
            return response;
        } catch (Exception e) {
            outputManager.printToTextArea("ERROR: occurred when completing host search request for " + userName + ": " +
                    "\n" + e);
            response.add("ERROR");
            response.add("Could not complete a search for hosts. Try again later.");
            return response;
        }
    }

    /**
     * <p> This method is used by this class methods to verify identity of the user making a request to the server.
     * This provides authentication and prevents unauthorized access to the server resources.</p>
     *
     * <p> Called by: {@link Server.P2PServiceImpl#disconnectFromServer}, {@link Server.P2PServiceImpl#sendHeartBeat},
     * {@link Server.P2PServiceImpl#registerFile}, {@link Server.P2PServiceImpl#deregisterFile},
     * {@link Server.P2PServiceImpl#getUserFiles}, {@link Server.P2PServiceImpl#searchFile},
     * {@link Server.P2PServiceImpl#getFileHostInfo}</p>
     *
     * <p> Calls: {@link Server.ActiveUsers#findUser}, {@link Server.OutputManager#printToTextArea},
     * {@link Server.ClientData#getName}, {@link Server.ClientData#getToken}, {@link Server.ActiveUsers#getNumOfUsers}</p>
     *
     * @param token token provided by user.
     * @param userName username provided by user.
     *
     * @return boolean true if user is active and token matches, false otherwise.
     */
    private boolean verifyActiveUser(String token, String userName) {
        // no active users
        if(activeUsers.getNumOfUsers() == 0){
        	return false;
        }
    	try {
            ClientData testedUser = activeUsers.findUser(userName);
            if (testedUser == null) {
                return false;
            }
            if ((testedUser.getName().equals(userName) && testedUser.getToken().equals(token))) {
                return true;
            }
        }catch (NullPointerException e){
            outputManager.printToTextArea("ERROR: occurred in active user list. Please restart server.");
        }
        return false;
    }

    /**
     * <p> This method is used by this class methods to execute a query on the database using executeQuery method.
     * It acquires a dbLock used to manage database access and to prevent multiple threads from accessing it at
     * the same time. </p>
     *
     * <p> Called by: {@link Server.P2PServiceImpl#getUserInformation}, {@link Server.P2PServiceImpl#registerFile},
     * {@link Server.P2PServiceImpl#getUserFiles}, {@link Server.P2PServiceImpl#searchFile},
     * {@link Server.P2PServiceImpl#getFileHostInfo}</p>
     *
     * @param query query to be executed.
     * @param statement statement to be executed.
     *
     * @return ResultSet result of the query being executed.
     *
     * @throws SQLException if there is a problem with the SQL connection.
     */
    private ResultSet sqlExecuteQuery(String query, Statement statement) throws SQLException {
        try {
            dbLock.lock();
            return statement.executeQuery(query);
        }finally {
            dbLock.unlock();
        }
    }

    /**
     * <p> This method is used by this class methods to execute a query on the database using executeUpdate method. It
     * acquires a dbLock used to manage database access and to prevent multiple threads from accessing it at the same
     * time. </p>
     *
     * <p> Called by: {@link Server.P2PServiceImpl#connectToServer}, {@link Server.P2PServiceImpl#updateUserIp},
     * {@link Server.P2PServiceImpl#updateUserPort}, {@link Server.P2PServiceImpl#registerFile},
     * {@link Server.P2PServiceImpl#deregisterFile}</p>
     *
     * @param query query to be executed.
     * @param statement statement to be executed.
     *
     * @return int result of the query being executed.
     *
     * @throws SQLException if there is a problem with the SQL connection.
     */
    private int sqlExecuteUpdate(String query, Statement statement) throws SQLException {
        try {
            dbLock.lock();
            return statement.executeUpdate(query);
        }finally {
            dbLock.unlock();
        }
    }

    /**
     * <p> This method is used to generate a token for user authentication. It generates a long integer that gets
     * converted to string. Additionally it verifies that no duplicate token is created.</p>
     *
     * <p> Called by: {@link Server.P2PServiceImpl#connectToServer}, {@link Server.P2PServiceImpl#resumeSession}</p>
     *
     * <p> Calls: {@link Server.ActiveUsers#getNumOfUsers()}, {@link Server.ActiveUsers#listActiveUsers()}</p>
     *
     * @return String token to be used for user authentication.
     */
    private String generateToken() {
        // generate large positive integer to be used as a token
        SecureRandom random = new SecureRandom();
        long longToken;
        do {
            longToken = Math.abs(random.nextLong());
            for(int i = 0; i<activeUsers.getNumOfUsers(); i++){
                if(activeUsers.listActiveUsers()[i].getToken().equals(Long.toString(longToken, 16))){
                    longToken = -1;
                }
            }
        } while (longToken < 0);
        return Long.toString(longToken, 16);
    }
}