/*
 * 2023/10/13
 * Anton Mishchenko
 */

package server;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.apache.cxf.annotations.WSDLDocumentation;

@WebService(name = "P2PServiceImplSEI", targetNamespace = "http://server/")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface P2PServiceImplSEI {

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
	@WebMethod(operationName = "connectToServer", action = "urn:ConnectToServer")
	@WebResult(name = "return")
	@WSDLDocumentation("Connects to the server, starting a new session.")
	public List<String> connectToServer(@WebParam(name = "userName", partName = "userName") String userName, @WebParam(name = "userPassword", partName = "userPassword") String userPassword,
			@WebParam(name = "userIP", partName = "userIP") String userIP, @WebParam(name = "userPort", partName = "userPort") long userPort);

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
	@WebMethod(operationName = "resumeSession", action = "urn:ResumeSession")
	@WebResult(name = "return")
	@WSDLDocumentation("Resumes previously interrupted session with the server.")
	public List<String> resumeSession(@WebParam(name = "token", partName = "token") String token, @WebParam(name = "userName", partName = "userName") String userName,
			@WebParam(name = "userIP", partName = "userIP") String userIP, @WebParam(name = "userPort", partName = "userPort") long userPort);

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
	@WebMethod(operationName = "disconnectFromServer", action = "urn:DisconnectFromServer")
	@WebResult(name = "return")
	@WSDLDocumentation("Notifies the server that the client wants to disconnect ending their session.")
	public List<String> disconnectFromServer(@WebParam(name = "token", partName = "token") String token, @WebParam(name = "userName", partName = "userName") String userName);

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
	@WebMethod(operationName = "sendHeartBeat", action = "urn:SendHeartBeat")
	@WebResult(name = "return")
	@WSDLDocumentation("Sends a heartbeat message to the server.")
	public List<String> sendHeartBeat(@WebParam(name = "token", partName = "token") String token, @WebParam(name = "userName", partName = "userName") String userName);

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
	@WebMethod(operationName = "registerFile", action = "urn:RegisterFile")
	@WebResult(name = "return")
	@WSDLDocumentation("Registeres user's file in the database.")
	public List<String> registerFile(@WebParam(name = "token", partName = "token") String token, @WebParam(name = "userName", partName = "userName") String userName,
			@WebParam(name = "fileName", partName = "fileName") String fileName, @WebParam(name = "fileType", partName = "fileType") String fileType, 
			@WebParam(name = "filePath", partName = "filePath") String filePath, @WebParam(name = "fileSize", partName = "fileSize") long fileSize);

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
	@WebMethod(operationName = "deregisterFile", action = "urn:DeregisterFile")
	@WebResult(name = "return")
	@WSDLDocumentation("Deregisters specific user file from the database.")
	public List<String> deregisterFile(@WebParam(name = "token", partName = "token") String token, @WebParam(name = "userName", partName = "userName") String userName,
			@WebParam(name = "fileName", partName = "fileName") String fileName, @WebParam(name = "fileType", partName = "fileType") String fileType, @WebParam(name = "filePath", partName = "filePath") String filePath);

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
	@WebMethod(operationName = "getUserFiles", action = "urn:GetUserFiles")
	@WebResult(name = "return")
	@WSDLDocumentation("Fetches a list of user's registered files.")
	public List<String> getUserFiles(@WebParam(name = "token", partName = "token") String token, @WebParam(name = "userName", partName = "userName") String userName);

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
	@WebMethod(operationName = "searchFile", action = "urn:SearchFile")
	@WebResult(name = "return")
	@WSDLDocumentation("Searches server records for files matching the query.")
	public List<String> searchFile(@WebParam(name = "token", partName = "token") String token, @WebParam(name = "userName", partName = "userName") String userName,
			@WebParam(name = "searchQuery", partName = "searchQuery") String searchQuery);

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
	@WebMethod(operationName = "getFileHostInfo", action = "urn:GetFileHostInfo")
	@WebResult(name = "return")
	@WSDLDocumentation("Aquires information of the file host.")
	public List<String> getFileHostInfo(@WebParam(name = "token", partName = "token") String token, @WebParam(name = "userName", partName = "userName") String userName,
			@WebParam(name = "fileID", partName = "fileID") int fileID);


}